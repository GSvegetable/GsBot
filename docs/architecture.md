# GsBot architecture

## Runtime boundaries

The Android UI is the product shell, not the runtime itself. It should communicate with stable Kotlin interfaces so that model inference and the Linux execution environment can be replaced without rewriting the UI.

### Model subsystem

- Model metadata includes the exact upstream artifact URL, file name, byte size, SHA-256, format, and license details.
- Model files are stored in app-private storage by default.
- A model is not marked installed until the download is complete and its checksum matches.
- The catalog must not contain guessed URLs or unverified license claims.
- The first supported format is GGUF; the first inference backend candidate is llama.cpp through Android NDK/JNI.

### Execution subsystem

- Linux user space is an optional, separately downloadable component.
- The first implementation should be headless and app-scoped. A graphical desktop is a later optional package.
- Agent commands run with explicit user consent and constrained filesystem access.
- The app must expose start, stop, status, and cleanup operations.

### Download subsystem

The downloader will need resumable transfers, progress reporting, free-space checks, checksum verification, cancellation, retry, and safe cleanup of partial files. Model weights and root filesystems are never bundled into the base APK.

## Delivery sequence

1. Android app foundation and typed runtime boundaries.
2. Model download manager and a verified small-model catalog.
3. Native inference backend and local chat.
4. Linux user-space bootstrap and process supervisor.
5. Agent tool permissions, task history, and release hardening.

## Non-goals for the first milestone

- No cloud inference dependency.
- No requirement for users to install Termux separately.
- No root requirement.
- No unrestricted shell access.
