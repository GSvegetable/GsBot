package com.gsbot.app.core.model

/**
 * Metadata for a model artifact. Catalog entries must be backed by a verified
 * upstream source, license, and checksum before they are offered to users.
 */
data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val description: String,
    val artifactUrl: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val format: ModelFormat,
    val licenseName: String,
    val licenseUrl: String,
    val minimumRecommendedRamBytes: Long? = null
)

enum class ModelFormat {
    GGUF
}
