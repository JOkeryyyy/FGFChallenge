# One UI Macrobenchmark Runbook

Operating procedure for the optional performance work in
[`implementationRoadMap.md`](implementationRoadMap.md) §12 and
[`ARCHITECTURE.md`](ARCHITECTURE.md) "Optional performance test". It describes how to prepare the
baseline phone, seed the benchmark snapshot, run the four scenarios, retain the artifacts, and read
the numbers.

**Nothing in this document is an acceptance gate.** Running the suite, obtaining a particular value,
or acting on an observation must not block Roadmap #13, acceptance, delivery, a pull request, or CI.
No threshold here is a pass/fail criterion.

## Scope

- **Physical device only.** The baseline is one documented One UI 2.5 phone. Emulator numbers are
  not a substitute and no "emulator trend baseline" is retained.
- **The suite observes; it does not tune.** It measures the current `LazyColumn`, the current Paging
  window, the literal `%term%` `LIKE` query, the full-result aggregate, and the current combined
  predicates. It adds no FTS, no extra indexes, and no query-plan work. An observed regression
  becomes a separate, separately measured follow-up.
- **Production behavior is unchanged.** Only the snapshot-refresh strategy differs in the
  `benchmark` variant, which installs a deterministic 100,000-row Room fixture instead of fetching
  the remote snapshot. Debug and release keep the shipping remote-to-Room launch refresh.

### Official references

- [Write a Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
  — profileable/non-debuggable target, ProfileInstaller, release-like variant, JSON/traces, UI
  Automator, and the `CompilationMode.Ignore()` workaround below API 34.
- [Capture Macrobenchmark metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics)
  — `frameDurationCpuMs`, percentile output, the API 31 requirement for `frameOverrunMs`,
  `TraceSectionMetric`, and the Pixel-only power-rail limitation.
- [Macrobenchmark instrumentation arguments](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
  — `SideEffectRunListener`, dry-run mode, output handling, and configuration errors.

## Device preparation checklist

Complete every line before a recorded run, and record any intentional deviation in the run record.

- [ ] Battery at 80% or more.
- [ ] Battery Saver / power-saving mode off; performance or power mode fixed and named.
- [ ] Background apps closed.
- [ ] Brightness fixed at a recorded level (not adaptive).
- [ ] Refresh rate fixed at a recorded setting.
- [ ] Window, transition, and animator scales each `1.0`.
- [ ] Device cooled to the documented starting range; prefer running unplugged after charging so
      charging heat is not part of the measurement.
- [ ] `androidx.benchmark.junit4.SideEffectRunListener` passed to every dry and recorded run.

Animation scales, if they are not already `1.0`:

```bash
adb shell settings put global window_animation_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global animator_duration_scale 1.0
```

When more than one device or emulator is attached, pin every command to the baseline phone:

```bash
export ANDROID_SERIAL=<baseline-device-serial>
```

## Recording system state

Run these before measurement and paste the values into the run record. They are read-only.

```bash
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.display.id
adb shell getprop ro.build.version.security_patch
adb shell settings get global low_power
adb shell settings get system screen_brightness
adb shell settings get system peak_refresh_rate
adb shell settings get global window_animation_scale
adb shell settings get global transition_animation_scale
adb shell settings get global animator_duration_scale
adb shell dumpsys battery
adb shell dumpsys thermalservice
adb shell dumpsys display
git rev-parse HEAD
```

Record by hand, because their reliable source is device- and model-specific: the One UI version, the
Samsung performance/power-mode label, the fixed refresh-rate choice, and the ambient temperature.

## Seeding and the dry run

The first `benchmark` launch generates and inserts the deterministic 100,000-row fixture. Every later
launch finds the row count and both sentinel IDs already present and does nothing, which is what lets
iterations reuse the seeded database.

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true \
  -Pandroid.testInstrumentationRunnerArguments.listener=androidx.benchmark.junit4.SideEffectRunListener
```

Expected: PASS with one non-measured iteration per method, and every method reaching its exact
expected UI state. Dry-run mode verifies selectors and the fixture. **Its numbers mean nothing** and
are never compared or recorded.

The fixture's fixed counts, and the app-bar label each scenario waits for:

| Scenario | Query | Result label |
| --- | --- | --- |
| Default / scroll | none | `100 of 100,000 Logs` |
| Paging boundary | none, after the first append | `200 of 100,000 Logs` |
| Search | message/ID contains `timed out` | `100 of 20,020 Logs` |
| Combined filter | `tag = network AND severity IN (ERROR, FATAL) AND is_ai_generated = 1` | `100 of 2,858 Logs` |

## Recorded runs

Run one scenario at a time and cool the device back to the documented starting range between
scenarios. Copy the artifacts before starting the next method — a later run overwrites the Gradle
output directory.

```bash
BENCHMARK_RUN_ID=$(date -u +%Y-%m-%d)-oneui25-baseline-01
mkdir -p "benchmark-results/$BENCHMARK_RUN_ID"
cp documentation/performance-run-template.md "benchmark-results/$BENCHMARK_RUN_ID/run.md"
```

Then, per scenario — substituting `scrollInitialWindow` / `crossFirstPagingBoundary` /
`searchTimedOut` / `applyCombinedFilter` and the matching folder `scroll` / `paging` / `search` /
`filters`:

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.fgfchallenge.benchmark.LogViewerMacrobenchmark#scrollInitialWindow \
  -Pandroid.testInstrumentationRunnerArguments.listener=androidx.benchmark.junit4.SideEffectRunListener
```

```bash
mkdir -p "benchmark-results/$BENCHMARK_RUN_ID/scroll"
cp -R benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/. "benchmark-results/$BENCHMARK_RUN_ID/scroll/"
```

Confirm the copied folder holds one benchmark JSON and ten Perfetto traces before running the next
method. `benchmark-results/` is git-ignored; it is local evidence, not repository content.

Each iteration presses home, kills the process, relaunches, and waits for `100 of 100,000 Logs`
before the measured block. It never calls `clearAppData()`, never reinstalls the target, and never
reseeds Room once the fixture is valid — `CompilationMode.Ignore()` is used precisely because an
Android 10 reinstall would reset compilation state and drop the seeded database.

## Metrics

`FrameTimingMetric` → `frameDurationCpuMs` P50, P90, P95, and P99. How long the app spent producing
each frame on the CPU. Record all four percentiles.

`TraceSectionMetric` → one named end-to-end interaction latency per scenario, emitted from the test
process, so `targetPackageOnly = false`:

| Scenario | Trace section | Covers |
| --- | --- | --- |
| `scrollInitialWindow` | `scrollInteraction` | Gesture through a stable list, still 100 loaded. |
| `crossFirstPagingBoundary` | `pagingBoundaryInteraction` | First boundary-directed gesture through the Room/Paging append and the stable 200-loaded result. |
| `searchTimedOut` | `searchInteraction` | Text injection through the 300 ms debounce, the `%timed out%` message/ID scan, the full-result aggregate, the first page, and the UI update. |
| `applyCombinedFilter` | `combinedFilterInteraction` | Apply through the combined predicate, aggregate, first page, and rendered result. Draft chip taps are outside the measured block because they issue no query. |

### Deliberately excluded

- **`frameOverrunMs`** requires API 31+. The baseline device is API 29, so the metric is not
  collected. Leave its rows out of the record rather than filling them with `0` or "pass".
- **`PowerMetric`** reads system power rails, which are available on Pixel 6 / Pixel 6 Pro and later
  only. It is not collected on this device.

## Reading and comparing results

A comparison is valid only when **the same device** reports matching device model, build and security
patch, refresh rate, brightness, power mode, animation scales, and starting temperature range. If any
of those differ, the runs are not comparable — say so instead of computing a delta.

```text
relative delta (%) = (candidate - baseline) / baseline * 100
```

Report raw values first, then the relative delta, then the direction and the trace evidence behind
it. Do not convert a delta into a CI threshold or a pass/fail claim. Thermal drift, One UI background
work, and a phone this age all move these numbers between runs; a single delta describes one pair of
runs, not the app.

Every recorded run is filed as `benchmark-results/<run-id>/run.md` from
[`performance-run-template.md`](performance-run-template.md), with the JSON and traces beside it.
