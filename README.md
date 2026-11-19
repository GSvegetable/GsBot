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

This repository contains the initial Android application scaffold and a first Compose home screen. It is not yet a complete AI application and does not yet contain an inference engine, downloadable model catalog, Linux root filesystem, or installable APK artifact.

## Initial technical choices

- Android native application with Kotlin and Jetpack Compose.
- Minimum Android API: 26.
- Java 17 toolchain.
- Native inference integration will be evaluated around llama.cpp (Android NDK/JNI) and GGUF models.
- Linux execution will be evaluated separately; a PRoot-based user-space runtime is a candidate, not yet bundled.
- Large model and rootfs assets must be downloaded on demand rather than packaged inside the APK.

## Planned milestones

1. Foundation — Android project, app shell, navigation, persistent settings, download infrastructure.
2. Model runtime — select an inference backend, integrate native libraries, add model metadata, download/resume, SHA-256 verification, and local chat.
3. Execution runtime — sandboxed local workspace, Linux user-space bootstrap, process lifecycle, storage controls, and permission prompts.
4. Agent layer — tool registry, task execution, audit trail, cancellation, and user approval for sensitive actions.
5. Release hardening — device compatibility, ABI-specific builds, offline behavior, license notices, security review, and signed APK.

## Important constraints

Open source does not mean every model or binary can be redistributed without conditions. Each runtime, model, and root filesystem must have its own source, license, version, checksum, and redistribution review. GsBot must not silently execute arbitrary agent commands with unrestricted access to user data.

## Build

Open this repository in Android Studio with JDK 17 and Android SDK 35 installed, then build the app configuration.
