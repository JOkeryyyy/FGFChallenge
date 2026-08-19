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
| Verify log viewer screen Paparazzi goldens | `./gradlew :feature:logs:presentation:verifyPaparazziDebug` |
| Re-record log viewer screen goldens after an intended visual change | `./gradlew :feature:logs:presentation:recordPaparazziDebug` |
| Check Kotlin/Gradle formatting | `./gradlew ktlintCheck` |
| Apply Kotlin/Gradle formatting | `./gradlew ktlintFormat` |
| Run Android Lint | `./gradlew lintDebug` |
| Compile instrumented test sources | `./gradlew :app:compileDebugAndroidTestSources` |
| Compile log viewer interaction test sources | `./gradlew :feature:logs:presentation:compileDebugAndroidTestSources` |
| Install the debug app on a device/emulator | `./gradlew :app:installDebug` |
| Run instrumented tests on a device/emulator | `./gradlew :app:connectedDebugAndroidTest` |
| Run log viewer interaction tests on a device/emulator | `./gradlew :feature:logs:presentation:connectedDebugAndroidTest` |
| Run design-system component tests on a device/emulator | `./gradlew :core:designsystem:connectedDebugAndroidTest` |
| Assemble the release-like Macrobenchmark target | `./gradlew :app:assembleBenchmark` |
| Run benchmark-variant unit tests (100k fixture contract) | `./gradlew :feature:logs:data:testBenchmarkUnitTest` |

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

### Latest run highlights

From the most recent recorded run (`2026-08-19-oneui25-run-03`, ten measured
iterations per scenario, physical Galaxy S9+ / API 29, 100,000-row fixture):

- **Combined filter is the fastest end-to-end interaction of the four
  scenarios: 521.6 ms median** to apply `tag = network AND severity IN
  (ERROR, FATAL) AND is_ai_generated = 1` across all 100,000 rows — query,
  full-result aggregate, first page, and redraw included.
- **Scrolling is close to frame-perfect: only 1.9% of frames (7 of 370)**
  exceeded the 16.7 ms / 60 Hz budget while scrolling the 100,000-row Room/Paging snapshot, with a 7.9 ms median frame
  time.
- **Search resolves a full literal scan of all 100,000 rows in 1.35 s
  median** — debounce, the `%timed out%` message/ID scan, the severity
  aggregate, and the first page, for 20,020 matches.

Both the filter and search figures beat this device's first recorded
baseline run. Full per-iteration numbers, frame-timing percentiles for every
scenario (including the weaker `crossFirstPagingBoundary` and the
frame-count caveats on `applyCombinedFilter`), and run incidents are filed in
`benchmark-results/2026-08-19-oneui25-run-03/run.md` — git-ignored local
evidence, per the note above, not part of this repository's tracked history.

### Manual profiler capture

In addition to the automated Macrobenchmark suite above, Filtering, Search,
and Scroll were manually profiled with the Android Studio Profiler's System
Trace capture against the same simulated ~100,000-row dataset (seeded via the
`benchmark` build variant), on a physical 2018 Samsung Galaxy S9. No
significant dropped frames were observed in any of the three captures.

| Filtering (latency range) | Search | Scroll |
| --- | --- | --- |
| ![Filtering profiler trace](documentation/profiler/filtering-latency.png) | ![Search profiler trace](documentation/profiler/search.png) | ![Scroll profiler trace](documentation/profiler/scroll.png) |

This was an exploratory manual check, not the recorded Macrobenchmark
scenarios above; it is likewise observational and not a delivery gate.

## Architecture

The app follows Google's recommended (layered, unidirectional-data-flow)
Android app architecture — not Uncle Bob's Clean Architecture — combined with
a **hybrid/grid modularization**: two infrastructure modules at the base, and
one feature module split vertically into `data` and `presentation` layers on
top of them.

```
:app -> :feature:logs:presentation
:app -> :core:designsystem
:feature:logs:presentation -> :feature:logs:data
:feature:logs:presentation -> :core:designsystem
:feature:logs:data -> :core:network
```

Core modules never depend on app or feature modules.

| Module | Responsibility |
| --- | --- |
| `:app` | Composition root: Hilt application, single activity, themes and composes `:feature:logs:presentation`. |
| `:feature:logs:presentation` | Public `LogsFeature()` entry point, ViewModel/query coordination, display mapping, Compose UI, and feature visual/interaction tests. |
| `:feature:logs:data` | Feature-owned Room database, remote API/DTOs, repository contract and implementation, query builder, and data/benchmark tests. |
| `:core:network` | Retrofit/OkHttp/Kotlinx Serialization networking infrastructure, shared by any feature that needs it. |
| `:core:designsystem` | Shared Compose Material 3 theme (`FGFChallengeTheme`) and stateless design-system building blocks, independent of any feature, data, Room, or Paging type. |
| `:benchmark` | Optional out-of-process Macrobenchmark test APK. It measures `:app`'s benchmark variant and is never part of a shipping build. |

`:core:network` and `:core:designsystem` are infrastructure modules — networking
plumbing and reusable UI components — with no feature knowledge. The single
`:feature:logs` slice is further split into `:data` and `:presentation` so the
Room/Retrofit/mapping layer and the ViewModel/Compose layer are independently
buildable and testable, and depends on `:core:network`. `:feature:logs:presentation`
depends on `:feature:logs:data`'s repository/model contract and on
`:core:designsystem`, never the reverse.

For a single-screen prototype this is admittedly more ceremony than the
feature strictly needs — a package-private split inside one module would work
just as well functionally. The module boundary was chosen deliberately anyway,
to demonstrate the module graph, dependency direction, and layer boundaries a
production-sized app would use, rather than to solve a real reuse problem this
prototype has today.

Search, filters, minute grouping, ordering, aggregates, and details all read
from Room after the one-shot launch refresh; there is no standalone domain
layer, since query coordination in the ViewModel is the appropriately small
solution at this size.

## Data flow and paging

On every app launch, the app makes one call to the supplied endpoint, decodes
and maps the response, and atomically replaces the feature-owned Room
snapshot with it in a single transaction. From that point on, Room — not the
network response and not in-memory state — is the source of truth for rows,
counts, severity density, filter options, and details.

Rows are grouped by UTC minute for display. For a dataset this size, remote
(server-side) pagination would normally be the right call, but it is out of
scope for this prototype per `requirement.md`; only client-side pagination
over Room via Paging 3 is implemented. The list loads the newest 100 rows
initially and loads 100 more at a time as the user scrolls, prefetching
before the last 20–30 visible rows are exhausted. Loading is purely a list
concern, though: search, filters, ordering, and the full-result/severity
counts always operate over the *complete* matching set in Room, regardless of
how many rows are currently loaded into the list.

## Search and filtering design

The search bar performs a case-insensitive literal substring match against
`message` or log `id` only. Every other structured condition — tag,
severity, the AI-generated flag, a UTC date/time range, and a latency
range — lives in a separate Filter sheet instead of the search field, for two
reasons:

- **Better UX per input shape.** Categorical values (tag, severity,
  AI-generated) and ranges (time, latency) are naturally chip/toggle/range
  controls, not something a user should have to type and spell correctly in
  a text field.
- **Cheaper search-as-you-type.** Because the search bar only ever needs a
  `LIKE`-style substring scan over two columns, every keystroke stays a light
  query. Structured predicates are applied once on Filter → Apply rather than
  re-evaluated on every character typed, which keeps the two concerns from
  competing for the same query on the hot path.

Active filter categories combine with AND; multiple selected values within
one category (e.g. several tags) combine with OR. Filter-sheet edits are
drafts and only reach Room on Apply.

## Testing

- **UI** is covered by screenshot (golden) tests via Paparazzi — one
  representative test per screen state (loading, error, populated, filtered,
  no-results, page-refresh, append progress/retry, collapsed group, light/dark,
  narrow/wide) — run with `./gradlew :feature:logs:presentation:verifyPaparazziDebug`.
- **Business logic** (query building, mapping, repository/refresh behavior,
  ViewModel state and query coordination) is covered by focused JVM unit
  tests, run with `./gradlew testDebugUnitTest`.
- The existing instrumented Compose interaction tests (`:core:designsystem`
  and `:feature:logs:presentation`) are retained; no additional
  interaction-test work was required.

## AI assistance

Claude Code, ChatGPT, and Codex materially contributed to this project's
architecture, implementation, debugging, and documentation. Every session's
tool, task, and key prompt are recorded in [`PROMPTS.md`](PROMPTS.md).

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
