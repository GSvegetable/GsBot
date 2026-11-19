# GsBot

A local-first AI workspace for Android.

GsBot is being built from an empty repository. The goal is to let users install one Android app, then download and manage open model weights and a local execution environment inside GsBot—without asking users to install Termux or configure command-line tools themselves.

## Product direction

- Local inference: run compatible open-weight models on-device without a hosted inference API.
- In-app model library: download, verify, select, and remove model files from inside GsBot.
- Local execution: provide a managed Linux/user-space environment for agent tools and development tasks.
- Agent workspace: connect conversation, model inference, tools, files, and execution permissions.
- Transparent costs: distinguish free/open components from network downloads and device resource use.

## Repository status

This repository contains the initial Android application scaffold, a Compose home screen, a pinned starter-model catalog entry, and an in-app resumable model downloader with SHA-256 verification. It is not yet a complete AI application: the native inference engine and Linux runtime are not integrated, and no APK artifact has been published.

## Initial technical choices

- Android native application with Kotlin and Jetpack Compose.
- Minimum Android API: 26.
- Java 17 toolchain.
- The starter catalog currently pins ggml-org/Qwen3-1.7B-GGUF Q4_K_M to an immutable Hugging Face revision. Its upstream metadata declares Apache-2.0 and publishes the artifact SHA-256.
- The downloader supports HTTPS, Range-based resume, app-private storage, exact byte-length checks, SHA-256 verification, and cleanup of corrupt partial files.
- Native inference integration will use llama.cpp Android bindings / NDK once the app build baseline is validated.
- Linux execution will be evaluated separately; a PRoot-based user-space runtime is a candidate, not yet bundled.
- Large model and rootfs assets must be downloaded on demand rather than packaged inside the APK.

## Planned milestones

1. Foundation — Android project, app shell, navigation, persistent settings, download infrastructure.
2. Model runtime — integrate llama.cpp through Android NDK/JNI, load the verified starter GGUF from app-private storage, and implement local chat.
3. Execution runtime — sandboxed local workspace, Linux user-space bootstrap, process lifecycle, storage controls, and permission prompts.
4. Agent layer — tool registry, task execution, audit trail, cancellation, and user approval for sensitive actions.
5. Release hardening — device compatibility, ABI-specific builds, offline behavior, license notices, security review, and signed APK.

## Important constraints

Open source does not mean every model or binary can be redistributed without conditions. Each runtime, model, and root filesystem must have its own source, license, version, checksum, and redistribution review. GsBot must not silently execute arbitrary agent commands with unrestricted access to user data.

## Build

Open this repository in Android Studio with JDK 17 and Android SDK 35 installed, then build the app configuration.
