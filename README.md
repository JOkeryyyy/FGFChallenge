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

Remaining roadmap work is the final verification/delivery pass; the optional
performance suite is in place and may be run at any time.

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
| Assemble the release-like Macrobenchmark target | `./gradlew :app:assembleBenchmark` |
| Run benchmark-variant unit tests (100k fixture contract) | `./gradlew :feature:logs:testBenchmarkUnitTest` |

### Optional performance benchmarks

Physical device only, and **not a delivery gate**: no result here blocks CI,
acceptance, or delivery. Prepare the device first — battery ≥ 80%, Battery
Saver off, brightness and refresh rate fixed, animation scales `1.0`, cooled —
following [`documentation/performanceBenchmark.md`](documentation/performanceBenchmark.md).

Verify the selectors and seed the deterministic 100,000-row fixture with a
non-measured dry run:

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true -Pandroid.testInstrumentationRunnerArguments.listener=androidx.benchmark.macro.junit4.SideEffectRunListener
```

Then record one scenario at a time, cooling the device between them
(`scrollInitialWindow`, `crossFirstPagingBoundary`, `searchTimedOut`,
`applyCombinedFilter`):

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fgfchallenge.benchmark.LogViewerMacrobenchmark#scrollInitialWindow -Pandroid.testInstrumentationRunnerArguments.listener=androidx.benchmark.macro.junit4.SideEffectRunListener
```

Copy the JSON and the ten Perfetto traces out of
`benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/`
into `benchmark-results/<run-id>/`, which is git-ignored, and fill in
[`documentation/performance-run-template.md`](documentation/performance-run-template.md).
Compare only runs from the same device under matching recorded system state.

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
| `:benchmark` | Optional out-of-process Macrobenchmark test APK. It measures `:app`'s benchmark variant and is never part of a shipping build. |

## Prototype scope note

The Room/Paging flow keeps basic retryable failure handling and no standalone
domain layer: query coordination is one pure function plus the ViewModel.
An optional Macrobenchmark suite measures the viewer on one documented physical
device against a benchmark-only deterministic 100,000-row Room fixture; only
the snapshot-refresh strategy differs in that variant, and the results are
observational rather than delivery requirements.
One representative screenshot test per screen demonstrates the visual-test
approach; the existing instrumented coverage is retained without new
interaction-test work. A screen recording remains a delivery artifact.
