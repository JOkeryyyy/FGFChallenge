# Macrobenchmark Run Record — `<run-id>`

Copy this file to `benchmark-results/<run-id>/run.md` and fill it in **while the run happens**. Blank
cells stay blank until they are observed; never infer or carry a value over from another run.
Procedure: [`performanceBenchmark.md`](performanceBenchmark.md). These results are observational and
gate nothing.

## Device and system state

| Field | Recorded value |
| --- | --- |
| Device manufacturer/model | |
| Android / API / One UI | |
| Build number / security patch | |
| Refresh-rate setting / observed mode | |
| Brightness | |
| Performance/power mode / Battery Saver | |
| Battery % / charging state / battery temperature | |
| Ambient temperature | |
| Animation scales | |
| Commit SHA | |
| Run start/end | |

Preparation checklist confirmed (battery ≥ 80%, Battery Saver off, background apps closed, brightness
and refresh rate fixed, animation scales `1.0`, device cooled, `SideEffectRunListener` passed):
`yes` / `no` + deviations:

## Results

One row per scenario. `frameDurationCpuMs` percentiles come from the Macrobenchmark JSON; the
interaction figure is the named trace section's ten raw values and their median.

| Scenario | Interaction latency median (ms) | Interaction latency raw ×10 (ms) | frameDurationCpuMs P50 | P90 | P95 | P99 | Artifacts | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `scrollInitialWindow` (`scrollInteraction`) | | | | | | | `scroll/` | |
| `crossFirstPagingBoundary` (`pagingBoundaryInteraction`) | | | | | | | `paging/` | |
| `searchTimedOut` (`searchInteraction`) | | | | | | | `search/` | |
| `applyCombinedFilter` (`combinedFilterInteraction`) | | | | | | | `filters/` | |

`frameOverrunMs` and `PowerMetric` have no rows here on purpose: the first needs API 31+, the second
needs Pixel 6-class power rails. Leave them out rather than recording a zero.

### Artifact paths

| Scenario | Benchmark JSON | Perfetto traces (10) |
| --- | --- | --- |
| `scrollInitialWindow` | | |
| `crossFirstPagingBoundary` | | |
| `searchTimedOut` | | |
| `applyCombinedFilter` | | |

### Thermal and battery observations

| Scenario | Battery % start/end | Battery temperature start/end | Cooling gap before run | Notes |
| --- | --- | --- | --- | --- |
| `scrollInitialWindow` | | | | |
| `crossFirstPagingBoundary` | | | | |
| `searchTimedOut` | | | | |
| `applyCombinedFilter` | | | | |

### Visible stalls

| Scenario | Observed stall | Representative trace iteration |
| --- | --- | --- |
| | | |

## Comparison against a baseline run

Only fill this in when the baseline is the **same device** with matching model, build/security patch,
refresh rate, brightness, power mode, animation scales, and starting temperature range. If any field
differs, write "not comparable" and stop.

Baseline run ID: <br>
Matching-state check: `matched` / `not comparable` (state which field differs)

| Scenario | Metric | Baseline | Candidate | Relative delta (%) | Direction and trace evidence |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

`relative delta (%) = (candidate - baseline) / baseline * 100`. A delta describes this pair of runs.
It is not a threshold, and it is not a pass/fail result.
