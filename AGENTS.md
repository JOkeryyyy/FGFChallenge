## AI Usage Documentation

When AI assistance materially contributes to implementation,
architecture, debugging, testing, or documentation, append an entry
to PROMPTS.md containing:

- Tool
- Task
- Key prompt

Do not record trivial interactions.

## Project Context

FGFChallenge is an Android take-home prototype called **AI Semantic Log**. It fetches a single structured payload of approximately 5,000 log entries and presents a responsive Jetpack Compose log viewer. The core experience is client-side search across message, tag, and severity; UTC minute grouping; chronological sorting; a Canvas-based `ERROR + FATAL` density indicator; loading, error, content, and no-results states; and a details bottom sheet.

Despite the product name, runtime AI, semantic/vector search, anomaly detection, pagination, streaming, offline persistence, and production observability are outside the prototype scope. The planned implementation uses a multi-module Android architecture, unidirectional presentation state, Retrofit/OkHttp, Kotlinx Serialization, Hilt, Coroutines/Flow, Material 3, and focused JVM, repository, ViewModel, visual, and Compose tests.

## Documentation Authority

Use the documentation in this order when decisions overlap:

1. [`documentation/requirement.md`](documentation/requirement.md) defines the challenge's explicit deliverables and must-have requirements.
2. [`documentation/api_and_requirement_gap_assumptions.md`](documentation/api_and_requirement_gap_assumptions.md) resolves behavior the challenge leaves unspecified and records facts derived from the supplied dataset.
3. [`documentation/UIWireframe.png`](documentation/UIWireframe.png) is the visual and interaction reference for the required screen states.
4. [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) is the approved technical design derived from those product inputs.
5. [`documentation/implementationRoadMap.md`](documentation/implementationRoadMap.md) gives the intended delivery sequence; it is not a dependency-version matrix or a file-by-file implementation plan.

For an explicit product requirement, follow `requirement.md`. For an ambiguity, use the documented assumption rather than inventing new scope. For implementation structure and dependency direction, follow `ARCHITECTURE.md`. Use the wireframe for visual behavior and the roadmap for sequencing. If a requested change conflicts with this hierarchy, call out the conflict before proceeding.

## Documentation Reference Guide

| Document | Summary | Refer to it when... |
| --- | --- | --- |
| [`documentation/requirement.md`](documentation/requirement.md) | Original Android take-home brief. Requires clean MVVM/MVI architecture, Retrofit/OkHttp networking, Hilt or Koin, Coroutines/Flow, fast search over 5,000+ logs, grouped UI models, a polished Compose list, a custom Canvas severity component, log details, unit tests, README/setup/recording, `PROMPTS.md`, and GitHub delivery. | Checking acceptance criteria, required technologies/capabilities, or final submission obligations. Start here for any feature-scope question. |
| [`documentation/api_and_requirement_gap_assumptions.md`](documentation/api_and_requirement_gap_assumptions.md) | Resolves unspecified behavior: one bounded 5,000-record fetch; case-insensitive substring search over message/tag/severity; UTC minute grouping; newest-first default; current-result `ERROR + FATAL` density; typed schema with `UNKNOWN` severity support; initial fetch plus retry; structured read-only details. It also documents the sample schema, distributions, one-second timestamp cadence, single response-level session, and explicit non-goals. | Implementing or testing search, grouping, sorting, density, schema mapping, retry, or details behavior; interpreting the API fixture; deciding whether an unrequested feature is in scope. |
| [`documentation/UIWireframe.png`](documentation/UIWireframe.png) | Low-fidelity visual reference for four states: skeleton loading, populated grouped list, filtered search results, and error/details. It specifies result count and sort placement, non-collapsible minute headers, fixed-width severity/tag pills, `ss.SSS` row times, filtered density updates, retry UI, and a contextual details bottom sheet. | Building, reviewing, previewing, or visually testing Compose UI, layout, component states, interaction placement, and scanability. Use it as a behavioral layout target, not as a literal pixel specification. |
| [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) | Approved technical source of truth. Defines `:app`, `:feature:logs`, `:core:network`, and `:core:designsystem`; layer ownership and dependency rules; repository/data contracts; typed failures and cancellation; processing and UDF state policies; UI/accessibility behavior; UTC formatting; Hilt boundaries; test strategy; pre-commit/CI gates; build constraints; and delivery documentation. It deliberately avoids premature domain use cases and excludes navigation, Room, runtime AI, and dynamic color. | Making module/package/API decisions, adding dependencies, implementing data or presentation flows, handling errors/concurrency, placing UI components, defining test coverage, or checking platform/build constraints. Read the relevant section before architectural changes. |
| [`documentation/implementationRoadMap.md`](documentation/implementationRoadMap.md) | Nine incremental milestones: project foundation; design system; fixture-backed UI; interaction; network/repository; mapping; business processing; measured performance optimization; and final testing/release. It lists stable interfaces, acceptance scenarios, assumptions, and deferred choices. | Planning work order, identifying the next coherent increment, writing a detailed implementation plan, or checking that performance and release verification are not skipped. Do not use it as a substitute for the architecture or exact task plan. |

## High-Value Project Rules

- Keep remote DTOs and transport exceptions inside the data layer; presentation depends on the repository contract and application models.
- Expose one immutable `StateFlow<LogViewerUiState>` and model user input with `LogViewerAction`; selected-log state drives the details sheet.
- Prepare the dataset once and keep filtering, grouping, sorting, severity counts, and density deterministic, main-safe, and directly testable.
- Use one flat `LazyColumn` with stable keys/content types. Keep design-system components stateless and independent of feature/data types.
- Preserve UTC throughout: minute buckets and `HH:mm` headers, `ss.SSS` row times, and full UTC timestamps in details.
- Treat accessibility, light/dark determinism, cancellation preservation, typed error mapping, and measured performance as acceptance concerns rather than cleanup work.
- Before expanding scope, check the documented non-goals and assumptions. Add a domain use case only when complexity or reuse meets the criteria in `ARCHITECTURE.md`.
