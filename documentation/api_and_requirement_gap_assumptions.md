# API and Product Requirement Gap Assumptions

## Status and purpose

This document resolves implementation-significant gaps in the original take-home brief. Its working assumptions are normative unless `requirement.md` states otherwise. Facts from the supplied 5,000-record payload are retained as fixture evidence, but they do not cap the revised approximately 100,000-record product target.

## Approved working assumptions

| Area | Requirement gap | Approved assumption |
| --- | --- | --- |
| **AI / semantic behavior** | The product name does not define an AI capability. | Logs may represent AI-generated events, but runtime AI, semantic/vector search, anomaly detection, clustering, and inferred correlations are out of scope. |
| **Target scale** | The original brief says 5,000+ without a maximum. | The product must handle one complete snapshot of approximately 100,000 records. The supplied 5,000 records remain the schema/development fixture. |
| **Remote shape** | The brief does not define remote pagination, deltas, or streaming. | The endpoint returns one authoritative complete snapshot in one request. There is no remote pagination, streaming, or incremental synchronization. |
| **Startup refresh** | Refresh cadence is unspecified. | Every app launch attempts one complete refresh. No automatic second request is made; Retry is explicit after a failed attempt. |
| **Snapshot integrity** | Partial-response and replacement behavior are unspecified. | Decode, validate, and map the entire response before Room mutation. Replace the old snapshot in one transaction so observers never see an empty or partial snapshot. |
| **Failure with retained data** | The brief does not define stale-data behavior. | Network, validation, cancellation, or database failure leaves the prior complete snapshot intact, but the launch remains a retryable failure and retained rows are not represented as current. |
| **Local source of truth** | Local persistence is unspecified. | After refresh succeeds, feature-owned Room is the only source for list rows, aggregates, filter options, and details. This is local snapshot storage, not a broader offline-first promise. |
| **Searchable fields** | “Search across logs” does not identify fields. | Free text searches `message` or `id` only. Tag and severity are available exclusively through structured filters. |
| **Search semantics** | Fuzzy, tokenized, regex, wildcard, and semantic behavior are undefined. | Use case-insensitive literal substring matching. Escape SQLite wildcard characters so `%` and `_` remain literal input. Blank search is inactive. |
| **Structured filters** | The brief does not define filter categories or combination rules. | Support tags, severity, AI-generated state, UTC date/time range, and inclusive latency range. Active categories combine with AND; selected tags and severities combine with OR/`IN`; inactive categories add no predicate. |
| **Filter editing** | It is unclear whether partial edits query immediately. | Search remains search-as-you-type. Structured filter edits remain draft values until Apply; Clear All resets the draft to the unfiltered default. |
| **Time range** | Time-zone, inclusivity, and date-only behavior are unspecified. | Interpret UI choices in UTC. Store one half-open interval; convert an inclusive end minute to the next minute as the exclusive bound, and include the complete selected day for a date-only end. |
| **Latency range** | Inclusivity is unspecified. | Applied minimum and maximum latency are inclusive. The category is inactive when the full available range is selected or no constraint is applied. |
| **Grouping** | Session or timestamp grouping is allowed without a required granularity. | Group displayed rows by UTC minute. Session grouping would produce one group for the supplied fixture and is not used. |
| **Ordering** | Default and tie behavior are unspecified. | Default to newest first. Sort deterministically by timestamp and then ID in the same selected direction. |
| **Paging** | The original brief does not define a bounded UI working set. | Load 100 rows initially and 100 per subsequent page, with placeholders disabled and prefetch before the current window is exhausted. |
| **Result summary** | The scope of counts and density is unclear under Paging. | Total count, every severity count, and error density describe the complete filtered database result, never only loaded pages. |
| **Severity density** | The error numerator and empty behavior are unspecified. | Error density is `(ERROR + FATAL) / complete filtered count`; an empty result is `0%`. |
| **Unexpected severity** | Forward compatibility is undefined. | Map unrecognized values to `UNKNOWN`. Include them in total and UNKNOWN counts, but not the error numerator. The first filter UI exposes the five known severities. |
| **No-results timing** | Paging can temporarily have zero loaded rows while work is active. | Show no results only after the current aggregate query completes with total count zero. |
| **Details lookup** | It is unclear whether details depend on loaded rows. | Resolve details by stable log ID through the repository so later-page rows remain selectable without retaining all rows in UI state. |
| **UI quality** | “Pixel-perfect” has no supplied production design. | Follow `UIWireframe.png` as the behavioral low-fidelity contract and produce polished, deterministic Material 3 UI across representative widths and light/dark themes. |

## Canonical query semantics

One immutable query value represents:

```text
search text
selected tags
selected severities
AI-generated constraint
UTC start-inclusive instant
UTC end-exclusive instant
inclusive minimum latency
inclusive maximum latency
sort direction
```

Blank search and empty multi-select sets normalize to inactive conditions. AI-generated Any is inactive. An absent time range and an unconstrained latency range are inactive.

For a log to match, every active category must match:

```text
(
    search is inactive
    OR message contains search literally, ignoring case
    OR id contains search literally, ignoring case
)
AND (tag filter is inactive OR tag IN selected tags)
AND (severity filter is inactive OR severity IN selected severities)
AND (AI filter is inactive OR is_ai_generated = selected value)
AND (time filter is inactive OR start <= timestamp < end)
AND (latency filter is inactive OR minimum <= latency_ms <= maximum)
```

The OR between `message` and `id` belongs inside the search category. The OR/`IN` among selected tags or severities belongs inside its category. All active categories then combine with AND.

### Examples

- Search `timeout` with tags `{network, sync}` means `(message OR id contains "timeout") AND tag IN (network, sync)`.
- Severities `{ERROR, FATAL}` with AI-generated No means `severity IN (ERROR, FATAL) AND is_ai_generated = false`.
- Search `%_` looks for those two literal characters; it does not match arbitrary database text.
- A blank search, empty selections, AI Any, no date/time range, and the full latency range produce no filtering predicate.

## Snapshot refresh and transactional replacement

The launch sequence is:

1. Enter startup loading.
2. Make one complete network request.
3. Decode and validate every required response and entry field.
4. Verify the reported count describes the decoded entry collection and IDs do not silently collapse distinct rows.
5. Map valid values, including UTC timestamps and `UNKNOWN` severity fallback.
6. In one Room transaction, delete the old snapshot and insert the complete new snapshot.
7. Treat Room invalidation as the source of subsequent query updates.
8. Present current content only after the transaction commits.

Cancellation is rethrown rather than converted to a data error. Any failure before commit leaves the old database unchanged. A transaction failure rolls back both deletion and insertion. Retry repeats the complete attempt; it is not a partial resume.

The response-level session ID is stored with each persisted entry or through an equivalent relation that allows an ID details lookup without retaining the remote batch in memory.

## Paging and aggregate consistency

The default query has no search or structured filters and sorts newest first. It initially returns the most recent 100 matching records. Later loads request up to 100 more rows and begin before the user reaches the end of the loaded window.

The paged select and aggregate select must use the same canonical query instance and logically identical predicates. The aggregate includes:

```text
complete filtered result count
DEBUG count
INFO count
WARN count
ERROR count
FATAL count
UNKNOWN count when present
```

The Canvas value is derived as:

```text
if total == 0:
    density = 0%
else:
    density = (ERROR count + FATAL count) / total
```

`LazyPagingItems.itemCount`, loaded-row count, and viewport-visible count are never substitutes for the aggregate total.

Rapid search, filter, or sort replacement cancels obsolete work. Rows and summaries from an older canonical query must not be labelled as results of the newer query.

## Paging-aware UI state assumptions

- `LogViewerUiState` contains bounded screen state only: immediate query text, applied filters, filter draft/visibility, sort, startup refresh state, aggregate summary, filter options, and selected-log state.
- Paged rows travel separately as `Flow<PagingData<LogViewerListItem>>`.
- Startup loading/error is distinct from Paging refresh and append states.
- Append loading appears after retained rows; append failure preserves those rows and exposes retry at the boundary.
- Filter controls show an active-category count. Draft changes do not affect that count until Apply.
- The summary explicitly indicates that counts and density cover all matching records.
- Minute headers remain regular list items and stay correct when a minute spans two pages.
- Details-sheet visibility derives from selected-log state and supports close, swipe-down, and Back dismissal.

## Facts from the supplied 5,000-record fixture

The supplied endpoint response has this shape:

```json
{
  "total_count": 5000,
  "session_id": "session-666",
  "data": [
    {
      "id": "...",
      "timestamp": "...",
      "severity": "ERROR",
      "tag": "network",
      "message": "Connection timed out",
      "metadata": {
        "latency_ms": 2040,
        "is_ai_generated": true
      }
    }
  ]
}
```

All 5,000 sample records use the same required structure:

```text
id: String
timestamp: String
severity: String
tag: String
message: String
metadata:
    latency_ms: Integer
    is_ai_generated: Boolean
```

No missing fields, null values, duplicate IDs, duplicate records, or schema variations were found in that fixture. The sample's `total_count` equals its decoded record count.

### Observed severity distribution

```text
ERROR   20.78% (1,039)
FATAL   20.22% (1,011)
WARN    20.12%
INFO    20.10%
DEBUG   18.78%
```

For the complete unfiltered fixture:

```text
ERROR + FATAL
= 1,039 + 1,011
= 2,050 / 5,000
≈ 41%
```

This remains a useful deterministic visual/test fixture. It does not imply that a 100,000-record acceptance snapshot must have the same distribution.

### Observed tags and messages

The fixture has seven nearly balanced tags:

```text
ui
db
cache
network
auth
sync
neural_engine
```

It has five nearly balanced messages:

```text
User logged in
Cache miss
Connection timed out
Inference complete
Invalid token
```

These small domains support deterministic UI fixtures but must not be encoded as the complete future option set. Available tag options come from the unfiltered Room snapshot.

### Observed timestamps and session

The 5,000 timestamps occur at one-second intervals, with no gaps, duplicates, or out-of-order values, spanning approximately 1 hour 23 minutes 19 seconds. Minute grouping produces roughly 84 useful groups; session grouping produces one group because `session_id` occurs once at response level.

That evidence supports UTC minute grouping, but implementation must still handle arbitrary valid timestamp spacing and deterministic ties by ID.

### Independence of fixture fields

Fixture values appear to be generated independently from small predefined sets. Combinations such as `FATAL + User logged in` are valid sample data, and apparent relationships such as `auth -> Invalid token` do not establish domain rules.

The product must not infer correlations, automatic categories, anomaly relationships, or severity/message mappings from the sample.

## Scope boundary

The approved revision requires Room, Paging 3, database-backed combined queries, aggregate queries, and one focused domain query-policy boundary. It still does not require:

```text
remote pagination
WebSocket or live streaming
incremental or delta API synchronization
background refresh
offline-first behavior
runtime AI or LLM integration
semantic/vector search
fuzzy, regex, token-only, or advanced search syntax
anomaly detection or clustering
arbitrary nested metadata
configurable grouping rules
analytics or production observability infrastructure
```

The architecture may begin with parameterized SQLite `LIKE` for literal substring search. A different search implementation is justified only by measured performance and must preserve the same arbitrary-substring behavior.

The resulting documentation position is:

> **The app imports one validated approximately 100,000-record snapshot per launch, atomically replaces feature-owned Room storage, and answers all repeated user queries through bounded Paging plus full-result aggregates. The supplied 5,000-record payload remains evidence about schema and fixtures, not permission to materialize the complete target dataset in presentation state.**
