package com.gsbot.app.core.runtime

import com.gsbot.app.core.model.ModelDescriptor

/**
 * Boundary between the app and its on-device inference backend.
 * The first implementation will be selected after an Android NDK device
 * compatibility spike; UI code must not depend on a specific native engine.
 */
interface LocalInferenceEngine {
    suspend fun loadModel(model: ModelDescriptor)
    suspend fun generate(prompt: String, onToken: (String) -> Unit)
    suspend fun cancelGeneration()
    suspend fun unloadModel()
    fun isModelLoaded(): Boolean
}
