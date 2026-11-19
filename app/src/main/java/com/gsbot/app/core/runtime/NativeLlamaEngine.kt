package com.gsbot.app.core.runtime

import android.content.Context
import com.gsbot.app.core.model.ModelDescriptor
import com.gsbot.app.core.download.ModelDownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device llama.cpp adapter. Model weights remain in GsBot's private storage.
 *
 * The initial bridge returns a completed response; token-by-token streaming and
 * multi-turn KV-cache retention will be added after the first device validation.
 */
class NativeLlamaEngine(context: Context) : LocalInferenceEngine {
    private val appContext = context.applicationContext
    private val downloads = ModelDownloadRepository(appContext)

    override suspend fun loadModel(model: ModelDescriptor): Unit = withContext(Dispatchers.IO) {
        check(downloads.isInstalled(model)) {
            "模型尚未安装或完整性校验失败，请先从模型库下载"
        }
        val file = File(appContext.filesDir, "models/${model.fileName}")
        nativeLoadModel(file.absolutePath)?.let { message ->
            throw IllegalStateException(message)
        }
        Unit
    }

    override suspend fun generate(prompt: String, onToken: (String) -> Unit) {
        require(prompt.isNotBlank()) { "输入内容不能为空" }
        val response = withContext(Dispatchers.Default) {
            nativeGenerate(prompt, MAX_OUTPUT_TOKENS)
        }
        onToken(response)
    }

    override suspend fun cancelGeneration() {
        nativeCancelGeneration()
    }

    override suspend fun unloadModel() = withContext(Dispatchers.IO) {
        nativeUnloadModel()
    }

    override fun isModelLoaded(): Boolean = nativeIsModelLoaded()

    private external fun nativeLoadModel(modelPath: String): String?
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeCancelGeneration()
    private external fun nativeUnloadModel()
    private external fun nativeIsModelLoaded(): Boolean

    companion object {
        private const val MAX_OUTPUT_TOKENS = 384

        init {
            System.loadLibrary("gsbot_llama")
        }
    }
}
