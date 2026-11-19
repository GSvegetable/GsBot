# GsBot architecture

## Runtime boundaries

The Android UI is the product shell, not the runtime itself. It communicates with stable Kotlin interfaces so model inference and the Linux execution environment can evolve independently.

### Model subsystem

- Model metadata includes an immutable upstream revision, exact artifact URL, file name, byte size, SHA-256, format, and license details.
- Model files are stored in app-private storage by default.
- A model is not marked installed until the download is complete and its checksum matches.
- The starter catalog pins ggml-org/Qwen3-1.7B-GGUF Q4_K_M to an immutable Hugging Face revision.
- The downloader supports HTTPS, HTTP Range resume, progress callbacks, cancellation, exact byte-length checks, and SHA-256 verification.
- The starter model uses the Apache-2.0 license declared by its upstream model repository.

### Inference subsystem

- The initial backend is llama.cpp, pinned to an immutable upstream commit and built with the Android NDK.
- The first native target is arm64-v8a, CPU-only, Android API 28+.
- Kotlin calls a small JNI boundary; model weights stay in GsBot app-private storage.
- The initial proof-of-integration uses a 2048-token context and returns a completed response. Streaming, multi-turn KV-cache retention, hardware acceleration, and device-specific memory tuning remain pending.
- The llama.cpp MIT license is included in app assets and listed in THIRD_PARTY_NOTICES.md.

### Execution subsystem

- Linux user space is an optional, separately downloadable component.
- The first implementation should be headless and app-scoped. A graphical desktop is a later optional package.
- Agent commands run with explicit user consent and constrained filesystem access.
- The app must expose start, stop, status, and cleanup operations.

### Download subsystem

The downloader will need durable background scheduling, resumable transfers, progress reporting, free-space checks, checksum verification, cancellation, retry, and safe cleanup of partial files. Model weights and root filesystems are never bundled into the base APK.

## Delivery sequence

1. Validate the Android Gradle + NDK build and native llama.cpp bridge.
2. Verify model loading and inference on arm64 Android hardware.
3. Add streaming output, conversation history, memory checks, and device tuning.
4. Implement the Linux user-space bootstrap and process supervisor.
5. Add Agent tools, permission prompts, task history, and release hardening.

## Non-goals for the first milestone

- No cloud inference dependency.
- No requirement for users to install Termux separately.
- No root requirement.
- No unrestricted shell access.
