package com.gsbot.app.core.model

/**
 * Curated starter catalog. Entries are pinned to immutable upstream revisions
 * and include upstream file checksums; never add guessed artifact metadata.
 */
object ModelCatalog {
    val starter: ModelDescriptor = ModelDescriptor(
        id = "qwen3-1.7b-q4-k-m",
        displayName = "Qwen3 1.7B · Q4_K_M",
        description = "轻量入门模型，适合先验证设备端推理。模型文件约 1.19 GiB。",
        artifactUrl = "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/daeb8e2d528a760970442092f6bf1e55c3b659eb/Qwen3-1.7B-Q4_K_M.gguf",
        fileName = "Qwen3-1.7B-Q4_K_M.gguf",
        sizeBytes = 1_282_439_264L,
        sha256 = "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5",
        format = ModelFormat.GGUF,
        licenseName = "Apache-2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    )
}
