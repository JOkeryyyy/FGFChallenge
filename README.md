# AI Semantic Log

Android take-home prototype that fetches a single structured payload of
approximately 5,000 log entries and presents them in a responsive Jetpack
Compose log viewer. The core experience is client-side search across
message, tag, and severity; UTC minute grouping; chronological sorting; a
Canvas-based `ERROR + FATAL` density indicator; loading, error, content, and
no-results states; and a details bottom sheet.

Despite the product name, this milestone and the overall prototype
deliberately exclude runtime AI, semantic/vector search, anomaly detection,
pagination, streaming, offline persistence, and production observability.

This document covers the **project foundation milestone**: toolchain,
module graph, dependency wiring, quality gates, and a verified launch shell.
It does not cover feature UI, networking behavior, or business logic, which
land in later roadmap milestones.

## Prerequisites

- Android Studio compatible with Android Gradle Plugin 9.3.1
- JDK 21
- Android SDK with platform 37 installed

## Clone setup

Activate the repository-owned pre-commit quality gate after cloning:

```bash
git config core.hooksPath .githooks
```

The hook runs `ktlintCheck` and `lintDebug` before every commit. It only
checks; it never formats, modifies, or stages files.

## Commands

| Purpose | Command |
| --- | --- |
| Assemble the debug app | `./gradlew :app:assembleDebug` |
| Run JVM unit tests | `./gradlew testDebugUnitTest` |
| Run Paparazzi visual verification | `./gradlew :core:designsystem:verifyPaparazziDebug` |
| Check Kotlin/Gradle formatting | `./gradlew ktlintCheck` |
| Apply Kotlin/Gradle formatting | `./gradlew ktlintFormat` |
| Run Android Lint | `./gradlew lintDebug` |
| Compile instrumented test sources | `./gradlew :app:compileDebugAndroidTestSources` |
| Install the debug app on a device/emulator | `./gradlew :app:installDebug` |
| Run instrumented tests on a device/emulator | `./gradlew :app:connectedDebugAndroidTest` |

## Module graph

```
:app -> :feature:logs
:app -> :core:designsystem
:feature:logs -> :core:network
:feature:logs -> :core:designsystem
```

Core modules never depend on app or feature modules.

| Module | Responsibility |
| --- | --- |
| `:app` | Composition root: Hilt application, single activity, themes and composes `:feature:logs`. |
| `:feature:logs` | Public `LogsFeature()` entry point for the log viewer feature. |
| `:core:network` | Retrofit/OkHttp/Kotlinx Serialization networking infrastructure. |
| `:core:designsystem` | Shared Compose Material 3 theme (`FGFChallengeTheme`) and design-system building blocks. |

## Foundation scope note

Screenshots/recording, performance findings, and complete test evidence are
release-milestone deliverables and are not claimed by this foundation
milestone.
