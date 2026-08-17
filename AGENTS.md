## AI Usage Documentation

When AI assistance materially contributes to implementation,
architecture, debugging, testing, or documentation, append an entry
to PROMPTS.md containing:

- Tool
- Task
- Key prompt

Do not record trivial interactions.

## Project Context

FGFChallenge is an Android take-home prototype called **AI Semantic Log**. It imports one complete structured payload per app launch, decodes and maps it, atomically replaces a feature-owned Room snapshot, and presents a responsive Jetpack Compose viewer backed by Paging 3. The core experience is case-insensitive literal search over message or log ID, independent structured filters, UTC minute grouping, deterministic sorting, full-result counts and `ERROR + FATAL` density, paging-aware load states, and stable-ID details.

The supplied 5,000-record endpoint remains the primary schema and development fixture. Remote pagination/streaming, background or delta sync, offline-first behavior, runtime AI, semantic/vector search, anomaly detection, analytics, and production observability remain outside scope. The planned implementation uses the existing four modules, unidirectional bounded presentation state, Retrofit/OkHttp, Room, Paging 3, Kotlinx Serialization, Hilt, Coroutines/Flow, Material 3, focused data/ViewModel checks, one representative screenshot test per screen, and the existing instrumented coverage. It does not require a standalone domain layer or formal performance benchmarking.

## Documentation Authority

Use the documentation in this order when decisions overlap:

1. [`documentation/requirement.md`](documentation/requirement.md) defines the challenge's explicit deliverables and must-have requirements.
2. [`documentation/api_and_requirement_gap_assumptions.md`](documentation/api_and_requirement_gap_assumptions.md) resolves behavior the challenge leaves unspecified and records facts derived from the supplied dataset.
3. [`documentation/UIWireframe.png`](documentation/UIWireframe.png) is the visual and interaction reference for the required screen states.
4. [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) is the approved technical design derived from those product inputs.
5. [`documentation/implementationRoadMap.md`](documentation/implementationRoadMap.md) gives the intended delivery sequence; it is not a dependency-version matrix or a file-by-file implementation plan.

For an explicit product requirement, follow `requirement.md`. For an ambiguity, use the documented assumption rather than inventing new scope. For implementation structure and dependency direction, follow `ARCHITECTURE.md`. Use the wireframe for visual behavior and the roadmap for sequencing. If a requested change conflicts with this hierarchy, call out the conflict before proceeding.

**Data Layer coding convention:** [`documentation/conventions/data-layer.md`](documentation/conventions/data-layer.md) elaborates `ARCHITECTURE.md` with Android Data Layer coding style (Repository boundary, optional Domain Layer, Data Source scoping, model/mapping placement, error handling, DI, naming). It does not override `ARCHITECTURE.md`'s concrete package layout or names. MUST read `documentation/conventions/data-layer.md` before implementing, modifying, or reviewing Data Layer code (repositories, data sources, DTOs/entities, mappers, error types, or `data/di` providers). Do not read it for unrelated work, e.g. pure UI/design-system or presentation-only changes.

## Documentation Reference Guide

| Document | Summary | Refer to it when... |
| --- | --- | --- |
| [`documentation/requirement.md`](documentation/requirement.md) | Preserves the original take-home brief and defines the prototype scope: one complete launch refresh, atomic Room replacement, message/ID literal search, combined structured filters, 100-row Paging, full-result aggregates, revised Compose states, focused tests, and delivery obligations. | Checking acceptance criteria, required technologies/capabilities, query behavior, load states, or final submission obligations. Start here for any feature-scope question. |
| [`documentation/api_and_requirement_gap_assumptions.md`](documentation/api_and_requirement_gap_assumptions.md) | Resolves snapshot replacement, Room source-of-truth, search/filter AND/OR, UTC/inclusive-range, Paging, summary, details, and basic failure semantics. It retains the 5k sample schema/distributions as fixture evidence and records revised non-goals. | Implementing or testing refresh replacement, queries, grouping, ordering, aggregates, filter options, retry, details, or interpreting the API fixture. |
| [`documentation/UIWireframe.png`](documentation/UIWireframe.png) | Low-fidelity behavioral reference for startup loading/error, default paged content, full-result summary, message/ID search, Filter with active count, draft filter controls, append progress/retry, no results, and contextual details. | Building, reviewing, previewing, or visually testing Compose UI, layout, Paging states, filter interaction, aggregate communication, and scanability. Use it as a behavioral target, not a literal pixel specification. |
| [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) | Approved technical source of truth. Defines the unchanged module graph; feature-owned Room/Paging; remote-to-Room atomic refresh; repository, query, aggregate, options, and details contracts; bounded UDF state; Paging-aware UI; typed failures/cancellation; UTC behavior; Hilt; prototype responsiveness boundary; and focused verification. | Making module/package/API decisions, adding dependencies, implementing data/presentation flows, handling errors/concurrency, placing UI components, defining test coverage, or checking platform/build constraints. Read the relevant section before architectural changes. |
| [`documentation/implementationRoadMap.md`](documentation/implementationRoadMap.md) | Thirteen incremental milestones: five delivered baseline steps followed by contract alignment, Room queries, refresh coordination, simple ViewModel query coordination, Paging presentation, filter UI, basic responsiveness, and focused verification/release. | Planning work order, identifying the next coherent increment, writing a detailed implementation plan, or checking that delivery scope remains prototype-sized. Do not use it as a substitute for the architecture or exact task plan. |
| [`documentation/conventions/data-layer.md`](documentation/conventions/data-layer.md) | Android Data Layer coding conventions: Repository as the Data Layer's sole public boundary, optional Domain Layer/Use Case criteria, Data Source scoping, DTO/Entity/application-model boundaries, mapping placement, immutability, `suspend`/`Flow` API shape and main-safety, typed `DataError` handling with cancellation preservation, constructor DI, naming, recommended package layout, and a testing/architecture checklist. Elaborates `ARCHITECTURE.md`; does not override its concrete package layout or class names. | Implementing, modifying, or reviewing Data Layer code — repositories, data sources, DTOs/entities, mappers, error types, or `data/di` providers. MUST be read before such changes; skip it for unrelated (e.g. UI-only) work. |

## Code Comment Conventions

- Every new source file must open with a short header comment (KDoc `/** ... */` for Kotlin, or the language's standard doc-comment form) summarizing what the file contains and its role in the module, so both humans and agents can identify a file's purpose without opening its full contents (e.g. during search/retrieval).
- Add inline comments only where the code's intent isn't obvious from names and structure alone — non-trivial logic, deliberate deviations from an obvious approach, or constraints tied to a documented requirement/assumption. Don't restate what the code already says.
- When substantially rewriting an existing file, add or update its header comment to match; don't leave undocumented files as you touch them.

## High-Value Project Rules

- Keep remote DTOs, Room entities/DAOs/query builders, and infrastructure exceptions inside data; presentation depends on the repository and immutable application models.
- Decode and map the remote response before one transactional Room replacement. A failed or cancelled refresh preserves the prior snapshot but remains a retryable launch failure.
- Make Room the post-refresh source of truth. One immutable `LogQuery` must drive logically identical Paging and full-result aggregate predicates.
- Search only message or ID as a case-insensitive literal substring. Combine active filter categories with AND, selected values within a category with OR/`IN`, and omit inactive predicates.
- Expose one bounded immutable `StateFlow<LogViewerUiState>` plus a separate `Flow<PagingData<LogViewerListItem>>`; never place all database rows, all matches, or `PagingData` in UI state.
- Use 100 rows for initial/subsequent pages, prefetch before the last 20–30 rows, and one flat paging-aware `LazyColumn` with stable keys/content types and correct cross-page minute headers.
- Calculate result/severity counts and density over the complete filtered database result, never loaded or visible rows. Resolve selected details by stable ID.
- Keep design-system components stateless and independent of feature/data/Room/Paging types.
- Preserve UTC throughout: minute buckets and `HH:mm` headers, `ss.SSS` row times, and full UTC timestamps in details.
- Preserve cancellation and one retryable error path as basic failure handling. Do not make a theme/width test matrix or measured performance evidence acceptance concerns.
- Keep query coordination in the ViewModel for this one-screen prototype; do not add a domain layer, per-action use cases, or pass-through wrappers without genuine reuse.
- Before expanding scope, check the documented non-goals and assumptions.
