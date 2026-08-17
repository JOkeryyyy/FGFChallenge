# AI Semantic Log Product Requirement

## Document status

This document contains the product requirements for the Android take-home prototype. The original challenge brief is preserved at the end for provenance. The **approved large-dataset revision** below is the implementation target and resolves the original brief wherever it is more specific.

The supplied 5,000-record endpoint remains the schema and development fixture. It is not the architectural size limit. Acceptance targets one complete remote snapshot of approximately **100,000 log records**.

## Product outcome

Build a responsive Jetpack Compose log viewer that:

- fetches one complete structured log snapshot on every app launch;
- decodes and maps the response, then atomically replaces a feature-owned Room snapshot;
- queries Room for all search, filtering, ordering, aggregates, details, and list data;
- initially displays the newest 100 matching logs and progressively loads later pages of 100;
- keeps the complete matching collection out of presentation state;
- presents accurate counts and error density for the complete filtered result, not only the loaded pages.

## Required architecture and data lifecycle

- Use a multi-module or clearly package-separated MVVM/MVI architecture with unidirectional data flow.
- Use Retrofit/OkHttp for the one-shot remote request, Hilt or Koin for dependency injection, Kotlin Coroutines/Flow for asynchronous work, Room as the local source of truth, and Paging 3 for list delivery.
- Attempt exactly one complete remote refresh when the app launches. An explicit Retry action may start another attempt after failure.
- Decode and map the complete response before mutating Room.
- Replace the prior Room snapshot in one transaction. Network, decoding/mapping, cancellation, or database failure must leave the prior complete snapshot intact and present a retryable launch failure; retained data must not be silently reported as current.
- After a successful refresh, Room is the only source for rows, result counts, severity counts, filter options, details lookup, and the severity indicator.
- Remote pagination, incremental synchronization, and streaming are not introduced.

## Search, filters, and ordering

### Free-text search

- Search-as-you-type performs **case-insensitive literal substring matching** over `message` or `id` only.
- Search does not inspect `tag`, `severity`, timestamps, latency, or AI-generated state.
- User-entered `%` and `_` characters are literals, not database wildcard syntax.
- A blank search adds no text predicate.

### Structured filters

Provide independently editable filters for:

- one or more tags;
- one or more of `DEBUG`, `INFO`, `WARN`, `ERROR`, and `FATAL`;
- AI-generated state: Any, Yes, or No;
- a UTC date/time range;
- an inclusive latency range in milliseconds.

The query rules are:

- active categories combine with **AND**;
- multiple values within tags or severities combine with **OR**/`IN`;
- an inactive category contributes no predicate;
- filter-sheet edits are drafts and do not query Room until Apply;
- Clear All restores the default unfiltered draft and applies it when confirmed.

The user-facing end minute is inclusive and becomes the next minute as an exclusive database bound. A date-only end includes the complete selected UTC day.

### Default and paging behavior

- The default has a blank search, no structured filters, and newest-first ordering.
- Ordering is deterministic by UTC timestamp and then log ID in the selected direction.
- The initial page contains the most recent 100 matching records, or every match when fewer than 100 exist.
- Subsequent pages contain up to 100 additional matching records and are prefetched before the current window is exhausted.
- Placeholders are not displayed for unloaded database rows.

## Transformations and aggregates

- Group paged rows into regular, non-collapsible UTC minute buckets.
- Preserve deterministic timestamp/ID order and correct minute headers across page boundaries.
- Compute total result count and every severity count over the **complete filtered database result**.
- Define error density as `(ERROR + FATAL) / complete filtered result count`.
- Define empty-result density as `0%`.
- Preserve unexpected severity values as `UNKNOWN`; they contribute to the total denominator but not the error numerator.
- Use the same immutable query criteria and logically identical predicates for paged rows and aggregate summaries.

## Required Compose experience

- Build a smooth, responsive list using one flat paging-aware `LazyColumn` with stable keys and content types.
- Keep search, Filter with an active-filter count, full-result summary, and sort controls visible while scanning.
- Provide distinct UI for startup loading, startup failure with Retry, content, no results, page refresh, append loading, append failure with retry, and log details.
- An append failure must preserve already loaded rows.
- Show no results only after the current query completes with a full-result count of zero.
- Build a custom Compose Canvas severity indicator for ERROR + FATAL density and pair color with text labels.
- Open structured, read-only log details by stable log ID. Support close, swipe-down, and Back dismissal even when the selected row came from a later page.
- Keep the screen readable and responsive.

## Performance boundary

The approximately 100,000-record architecture target must not cause the app to materialize all stored rows or all matching rows in `LogViewerUiState`. Database work, mapping, and imports remain main-safe. No generated performance fixture, device benchmark, optimization exercise, or recorded timing evidence is required for delivery; investigate performance only if the supplied fixture reveals a visible problem.

## Scope boundaries

The following remain out of scope:

- remote pagination or remote streaming;
- background refresh, delta synchronization, and offline-first behavior;
- runtime AI or LLM integration;
- semantic/vector search, fuzzy search, regex, or advanced query syntax;
- anomaly detection, clustering, and inferred log correlations;
- analytics and production observability infrastructure;
- editing, sharing, or raw-JSON tooling in log details.

Room persistence, database-backed filtering, and Paging 3 are required by this revision and are not non-goals. A separate domain layer is optional and must not be added solely to satisfy this document.

## Testing and delivery

- Keep focused unit or data tests for supported query behavior, snapshot replacement, retry, and ViewModel state. Full combinatorial coverage is not required.
- Add one representative screenshot test per screen to demonstrate the visual-test approach. The existing instrumented coverage is sufficient; no additional interaction-test work is required.
- Include readable code, focused comments for non-obvious constraints, setup instructions, an app screen recording, and brief architecture notes in `README.md`.
- Record material AI assistance in `PROMPTS.md`.
- Deliver the project through GitHub.

## Original take-home baseline (preserved)

The supplied brief requested an **AI-Driven Log Viewer** that:

- uses clean MVVM/MVI architecture, Retrofit/OkHttp, Hilt or Koin, and Coroutines/Flow;
- fetches the provided [`logs_5k.json`](https://firebasestorage.googleapis.com/v0/b/fieldinspectiondev.firebasestorage.app/o/data%2Flogs_5k.json?alt=media&token=15c66bf6-9716-44da-b3d1-ba9bb241baf8) payload;
- supports near-instant search over 5,000+ logs;
- transforms raw entries into UI-ready groups by session ID or timestamp;
- displays a polished Compose list, a Canvas severity indicator, and a details sheet;
- includes core-business-logic and ViewModel unit tests;
- documents AI use in `PROMPTS.md` and supplies setup instructions, a recording, and a GitHub link.

The revised sections above retain those goals while defining how the app must scale beyond the supplied fixture.
