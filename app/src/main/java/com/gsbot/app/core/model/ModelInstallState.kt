package com.gsbot.app.core.model

sealed interface ModelInstallState {
    data object NotInstalled : ModelInstallState
    data class Queued(val artifactId: String) : ModelInstallState
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ModelInstallState
    data object Verifying : ModelInstallState
    data object Installed : ModelInstallState
    data class Failed(val reason: String) : ModelInstallState
}
