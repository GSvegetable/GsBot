package com.gsbot.app.core.runtime

/**
 * Boundary for the optional local Linux/user-space environment.
 * Implementations must keep the workspace scoped to app-owned storage and
 * must not expose unrestricted device access to agent-generated commands.
 */
interface ExecutionEnvironment {
    suspend fun inspect(): EnvironmentStatus
    suspend fun install()
    suspend fun start()
    suspend fun stop()
}

data class EnvironmentStatus(
    val installed: Boolean,
    val running: Boolean,
    val runtimeVersion: String? = null,
    val detail: String? = null
)
