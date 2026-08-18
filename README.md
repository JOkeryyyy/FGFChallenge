# AI Semantic Log

Android take-home prototype that fetches a single structured payload of
approximately 5,000 log entries on every launch, atomically replaces a
feature-owned Room snapshot with it, and presents the result in a responsive
Jetpack Compose log viewer backed by Paging 3.

The core experience is case-insensitive literal search over **message or log
ID**; independent structured filters for tag, severity, AI-generated, UTC
date/time range, and latency; UTC minute grouping; deterministic newest- or
oldest-first ordering; full-result counts and a Canvas-based `ERROR + FATAL`
density indicator computed over the complete filtered database result; 100-row
pages with paging-aware load states; and a details bottom sheet resolved by
stable log ID.

Structured conditions belong to the filter sheet, not the search field: free
text never matches tag or severity.

Despite the product name, this prototype deliberately excludes runtime AI,
semantic/vector search, anomaly detection, remote pagination or streaming,
background/delta sync, offline-first behavior, and production observability.
Paging is client-side over Room; the endpoint still returns one complete
snapshot per launch.

This document covers work through the **structured-filter UI and paged list
integration milestone**: the toolchain, module graph, dependency wiring, and
quality gates; the `:core:designsystem` component set; the Retrofit/OkHttp
one-shot remote source and the transactional Room snapshot replacement; the
shared query builder driving logically identical paged and aggregate
predicates; the Hilt-backed `LogViewerViewModel` with one bounded
`StateFlow<LogViewerUiState>` beside a separate `Flow<PagingData<…>>`; and the
assembled screen with its startup loading/error, populated, filtered,
no-results, page-refresh, and append states.

Remaining roadmap work is the optional performance smoke test and the final
verification/delivery pass.

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
| Verify log viewer screen Paparazzi goldens | `./gradlew :feature:logs:verifyPaparazziDebug` |
| Re-record log viewer screen goldens after an intended visual change | `./gradlew :feature:logs:recordPaparazziDebug` |
| Check Kotlin/Gradle formatting | `./gradlew ktlintCheck` |
| Apply Kotlin/Gradle formatting | `./gradlew ktlintFormat` |
| Run Android Lint | `./gradlew lintDebug` |
| Compile instrumented test sources | `./gradlew :app:compileDebugAndroidTestSources` |
| Compile log viewer interaction test sources | `./gradlew :feature:logs:compileDebugAndroidTestSources` |
| Install the debug app on a device/emulator | `./gradlew :app:installDebug` |
| Run instrumented tests on a device/emulator | `./gradlew :app:connectedDebugAndroidTest` |
| Run log viewer interaction tests on a device/emulator | `./gradlew :feature:logs:connectedDebugAndroidTest` |
| Run design-system component tests on a device/emulator | `./gradlew :core:designsystem:connectedDebugAndroidTest` |

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
| `:feature:logs` | Public `LogsFeature()` entry point, and the feature-owned Room database, repository, query builder, and presentation. |
| `:core:network` | Retrofit/OkHttp/Kotlinx Serialization networking infrastructure. |
| `:core:designsystem` | Shared Compose Material 3 theme (`FGFChallengeTheme`) and design-system building blocks. |

## Prototype scope note

The Room/Paging flow keeps basic retryable failure handling and no standalone
domain layer: query coordination is one pure function plus the ViewModel.
A performance smoke test may be run now that the core flow works, but it and
any 100k benchmark evidence are optional rather than delivery requirements.
One representative screenshot test per screen demonstrates the visual-test
approach; the existing instrumented coverage is retained without new
interaction-test work. A screen recording remains a delivery artifact.
