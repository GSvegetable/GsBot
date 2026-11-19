package com.gsbot.app.core.runtime

import com.gsbot.app.core.model.ModelDescriptor

/**
 * Boundary between the app and its on-device inference backend.
 * The first adapter is llama.cpp through Android NDK/JNI; the UI remains
 * independent of the native engine implementation.
 */
interface LocalInferenceEngine {
    suspend fun loadModel(model: ModelDescriptor)
    suspend fun generate(prompt: String, onToken: (String) -> Unit)
    suspend fun cancelGeneration()
    suspend fun unloadModel()
    fun isModelLoaded(): Boolean
}
