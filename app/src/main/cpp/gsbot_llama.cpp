#include <jni.h>
#include <android/log.h>

#include "llama.h"

#include <algorithm>
#include <atomic>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#define LOG_TAG "GsBotLlama"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::mutex g_mutex;
std::atomic<bool> g_cancel{false};
bool g_backend_initialized = false;
llama_model * g_model = nullptr;
llama_context * g_context = nullptr;

void throw_java(JNIEnv * env, const char * message) {
    jclass type = env->FindClass("java/lang/IllegalStateException");
    if (type != nullptr) env->ThrowNew(type, message);
}

void release_model_locked() {
    if (g_context != nullptr) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

void set_batch(llama_batch_ext * batch, const llama_token * tokens, int count, llama_pos start_pos) {
    llama_batch_ext_clear(batch);
    for (int i = 0; i < count; ++i) {
        const int index = llama_batch_ext_add_token(batch, 0, tokens[i]);
        if (index < 0) throw std::runtime_error("llama.cpp could not add a token to the batch");
        const llama_pos position = start_pos + i;
        llama_batch_ext_set_pos(batch, index, &position);
    }
    if (count > 0) llama_batch_ext_set_output_logits(batch, count - 1, true);
}

std::string make_prompt(const llama_model * model, const std::string & user_text) {
    const char * chat_template = llama_model_chat_template(model, nullptr);
    if (chat_template == nullptr) {
        return "<|im_start|>user\n" + user_text + "<|im_end|>\n<|im_start|>assistant\n";
    }

    llama_chat_message message = {"user", user_text.c_str()};
    int required = llama_chat_apply_template(chat_template, &message, 1, true, nullptr, 0);
    if (required < 0) {
        return "<|im_start|>user\n" + user_text + "<|im_end|>\n<|im_start|>assistant\n";
    }

    std::vector<char> formatted(static_cast<size_t>(required) + 1);
    const int written = llama_chat_apply_template(
        chat_template, &message, 1, true, formatted.data(), formatted.size()
    );
    if (written < 0 || static_cast<size_t>(written) >= formatted.size()) {
        throw std::runtime_error("Failed to apply the model chat template");
    }
    return std::string(formatted.data(), static_cast<size_t>(written));
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_gsbot_app_core_runtime_NativeLlamaEngine_nativeLoadModel(
    JNIEnv * env, jobject, jstring model_path
) {
    const char * path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) return nullptr;

    std::lock_guard<std::mutex> lock(g_mutex);
    release_model_locked();

    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
    }

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (g_model == nullptr) {
        return env->NewStringUTF("llama.cpp 无法加载模型文件");
    }

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = 2048;
    context_params.n_batch = 2048;
    context_params.n_threads = std::max(2, static_cast<int>(std::thread::hardware_concurrency() / 2));
    context_params.n_threads_batch = context_params.n_threads;
    g_context = llama_init_from_model(g_model, context_params);

    if (g_context == nullptr) {
        release_model_locked();
        return env->NewStringUTF("无法为模型创建推理上下文，设备可用内存可能不足");
    }

    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_gsbot_app_core_runtime_NativeLlamaEngine_nativeGenerate(
    JNIEnv * env, jobject, jstring prompt_text, jint max_tokens
) {
    const char * input = env->GetStringUTFChars(prompt_text, nullptr);
    if (input == nullptr) return nullptr;
    const std::string user_text(input);
    env->ReleaseStringUTFChars(prompt_text, input);

    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model == nullptr || g_context == nullptr) {
        throw_java(env, "请先加载一个本地模型");
        return nullptr;
    }

    g_cancel.store(false);
    try {
        llama_memory_clear(llama_get_memory(g_context), true);
        const std::string prompt = make_prompt(g_model, user_text);
        const llama_vocab * vocab = llama_model_get_vocab(g_model);

        const int token_count = -llama_tokenize(
            vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()), nullptr, 0, true, true
        );
        if (token_count <= 0 || token_count >= static_cast<int>(llama_n_ctx(g_context))) {
            throw std::runtime_error("输入内容超过当前模型的上下文长度");
        }

        std::vector<llama_token> prompt_tokens(static_cast<size_t>(token_count));
        const int tokenized = llama_tokenize(
            vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()),
            prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()), true, true
        );
        if (tokenized < 0) throw std::runtime_error("模型输入分词失败");

        llama_sampler * sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
        llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

        llama_batch_ext * batch = llama_batch_ext_init(g_context);
        if (batch == nullptr) {
            llama_sampler_free(sampler);
            throw std::runtime_error("无法创建推理批次");
        }

        set_batch(batch, prompt_tokens.data(), static_cast<int>(prompt_tokens.size()), 0);
        if (llama_process(g_context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) {
            llama_batch_ext_free(batch);
            llama_sampler_free(sampler);
            throw std::runtime_error("模型无法处理输入内容");
        }

        std::string output;
        llama_pos position = static_cast<llama_pos>(prompt_tokens.size());
        const int limit = std::clamp(static_cast<int>(max_tokens), 1, 512);

        for (int generated = 0; generated < limit && !g_cancel.load(); ++generated) {
            const llama_token token = llama_sampler_sample(sampler, g_context, -1);
            if (llama_vocab_is_eog(vocab, token)) break;

            char piece[1024];
            const int piece_size = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
            if (piece_size > 0) output.append(piece, static_cast<size_t>(piece_size));

            set_batch(batch, &token, 1, position++);
            if (llama_process(g_context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) break;
        }

        llama_batch_ext_free(batch);
        llama_sampler_free(sampler);
        return env->NewStringUTF(output.c_str());
    } catch (const std::exception & error) {
        LOGE("Inference failed: %s", error.what());
        throw_java(env, error.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_gsbot_app_core_runtime_NativeLlamaEngine_nativeCancelGeneration(JNIEnv *, jobject) {
    g_cancel.store(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gsbot_app_core_runtime_NativeLlamaEngine_nativeUnloadModel(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    release_model_locked();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_gsbot_app_core_runtime_NativeLlamaEngine_nativeIsModelLoaded(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_model != nullptr && g_context != nullptr ? JNI_TRUE : JNI_FALSE;
}
