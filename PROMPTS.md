# AI Assistance Record

This submission-facing register records material use of AI during the project. Each key prompt is an edited summary of the instruction, not a verbatim session transcript. Product requirements and implementation decisions are defined by the current documents in `documentation/` and the source code; older entries describe the work completed at that point in the project.

## 2026-08-16

- **Tool:** ChatGPT · **Task:** Analyze requirement ambiguities and define pragmatic assumptions for the AI Semantic Log prototype.
  **Key prompt:** Review the requirements, feedback, sample payload, and dataset analysis. Document practical, example-backed assumptions that keep the work prototype-sized; defer implementation planning and wireframes until the assumptions are reviewed.

- **Tool:** ChatGPT Image Generation · **Task:** Create an initial low-fidelity log-viewer wireframe.
  **Key prompt:** Generate a grayscale product-review wireframe covering loading, grouped content, filtered results, retryable error, and log details. Base it on the agreed prototype scope and clearly show the intended search, filtering, grouping, sorting, and severity-summary interactions.

- **Tool:** ChatGPT · **Task:** Define reusable Jetpack Compose components for the log viewer.
  **Key prompt:** Review the approved wireframe and propose appropriately scoped Compose components for the summary, search, filters, timestamp headers, badges, rows, list, loading, error, empty, and details states.

- **Tool:** ChatGPT · **Task:** Analyze the supplied log dataset.
  **Key prompt:** Identify the payload schema, categorical values, time range, field consistency, and distributions relevant to the prototype.

- **Tool:** ChatGPT · **Task:** Add project and documentation guidance to `AGENTS.md`.
  **Key prompt:** Read the documentation set and add a concise reference guide so contributors know each document's purpose and when to consult it.

- **Tool:** ChatGPT · **Task:** Refine the prototype requirements using the 5,000-record fixture.
  **Key prompt:** Resolve review questions with scope-appropriate assumptions grounded in the supplied data. Keep the solution small enough for a take-home exercise and prepare the result for product review before wireframing.

- **Tool:** Codex · **Task:** Create a high-level implementation roadmap.
  **Key prompt:** Plan a pragmatic delivery sequence covering project setup, UI components and interactions, networking, mapping, query logic, performance review, and tests without over-specifying the implementation.

- **Tool:** Codex · **Task:** Review the initial Android architecture proposal and prepare an implementation plan.
  **Key prompt:** Assess the proposal against Google's layered Android app architecture and the documented requirements. Clarify module boundaries, MVI state flow, dependencies, UI components, test strategy, and quality gates before implementation.

- **Tool:** Codex · **Task:** Create a file-by-file project-foundation plan.
  **Key prompt:** Define the initial `:app`, `:feature:logs`, `:core:network`, and `:core:designsystem` files, packages, dependencies, module rules, theme entry point, CI, and build verification.

- **Tool:** Claude Code · **Task:** Implement the Android project foundation.
  **Key prompt:** Execute the approved foundation plan while preserving the Hilt, Retrofit, and Compose module architecture and completing the defined verification gates before feature work.

- **Tool:** Codex · **Task:** Implement the initial network and repository boundary.
  **Key prompt:** Configure shared Retrofit and OkHttp infrastructure, add the feature-owned logs API and data models, preserve cancellation, contain transport failures within data, and cover mapping and repository behavior with focused tests.

- **Tool:** ChatGPT · **Task:** Review the project-foundation pull request.
  **Key prompt:** Review the implementation for correctness, Android and Compose practices, Gradle configuration, CI, testing, maintainability, and alignment with the documented architecture; distinguish blocking from non-blocking findings.

- **Tool:** Codex · **Task:** Plan the `:core:designsystem` UI components.
  **Key prompt:** Define neutral light and dark tokens, responsive stateless log-viewer components, accessibility semantics, previews, and focused component tests without adding unapproved scope.

## 2026-08-17

- **Tool:** Claude Code · **Task:** Diagnose and fix two design-system Compose Preview issues.
  **Key prompt:** Investigate the blank `LogDetailsSheet` preview and missing `NoResultsContent` background, then implement a preview-safe fix without changing the public component contract.

- **Tool:** Codex · **Task:** Plan the network and repository milestone.
  **Key prompt:** Produce a data-layer-convention-compliant plan for the one-shot logs request, typed data failures, Hilt wiring, and cancellation-preserving repository boundary.

- **Tool:** ChatGPT · **Task:** Review the design-system pull request.
  **Key prompt:** Evaluate component boundaries, statelessness, responsive layout, accessibility, testing, scope, and alignment with the approved product and architecture; provide actionable findings without changing the repository.

- **Tool:** Claude Code · **Task:** Validate pull-request findings and plan confirmed fixes.
  **Key prompt:** Independently verify P0 and P1 findings against the code and project sources of truth. Plan fixes for confirmed accessibility, palette, loading, header, CI, and details-sheet issues; defer unrelated cleanup.

- **Tool:** Claude Code · **Task:** Implement the approved design-system fixes.
  **Key prompt:** Apply the confirmed palette, shimmer, severity-summary, static minute-header, details-sheet, and CI changes while keeping lower-priority work out of scope.

- **Tool:** Codex · **Task:** Plan the fixture-backed log-viewer screen.
  **Key prompt:** Build on the design system to plan UI-ready state, deterministic fixtures, one flat `LazyColumn`, previews, JVM fixture checks, Paparazzi goldens, and supporting CI work. Defer networking and live query behavior.

- **Tool:** Claude Code · **Task:** Implement the fixture-backed log-viewer screen.
  **Key prompt:** Implement the approved Roadmap #3 screen using the existing design-system components, with deterministic fixtures, previews, screenshots, and focused tests.

- **Tool:** Claude Code · **Task:** Implement basic log-viewer interactions.
  **Key prompt:** Implement Roadmap #4's immutable UI state, actions, Hilt-backed ViewModel, selection and dismissal behavior, and focused JVM and Compose interaction coverage. Keep live filtering and data loading deferred.

- **Tool:** Claude Code · **Task:** Implement the revised design-system plan.
  **Key prompt:** Build the agreed log-viewer tokens, components, shimmer, showcase, previews, semantics checks, and instrumented tests while retaining static minute headers.

- **Tool:** Claude Code · **Task:** Simplify the feature data boundary after review.
  **Key prompt:** Consolidate feature data DI into one owner module, keep `Result` feature-local, use error types only where application behavior requires them, preserve cancellation, and align the architecture, conventions, and tests.

- **Tool:** Codex · **Task:** Revise the roadmap for the Room-backed, locally paginated viewer.
  **Key prompt:** Update the roadmap for Room as the source of truth, atomic launch refresh, 100-row Paging, database-backed search and filters, and complete-result aggregates while preserving completed baseline work.

- **Tool:** Codex · **Task:** Align the product and architecture contracts.
  **Key prompt:** Replace superseded in-memory assumptions with the approved Room and Paging contract, define literal message-or-ID search and structured filters, and update the architecture, contributor guidance, and wireframe.

- **Tool:** Claude Code · **Task:** Implement Room persistence and the reusable query engine.
  **Key prompt:** Implement Roadmap #7's entity, indexes, DAO, immutable `LogQuery`, shared predicate builder, mappings, DI, and host-side DAO tests.
  - **AI contribution:** Established one parameterized SQL predicate for both Paging and aggregates, so the result summary and list always apply identical criteria. Literal search escapes SQL wildcard characters, and the database layer keeps UTC ranges half-open for consistent filtering.
  - **Verification:** DAO tests cover representative query combinations, including literal `%`, `_`, and backslash searches, plus list-to-summary parity.

- **Tool:** Codex · **Task:** Reduce documentation to a prototype-sized delivery scope.
  **Key prompt:** Remove non-essential validation and test requirements, retain focused screenshot coverage and existing instrumentation, preserve retryable failures, and update the wireframe to reflect the approved scope.

- **Tool:** Codex · **Task:** Make performance work optional.
  **Key prompt:** Reclassify performance measurement as an optional post-core smoke test that informs future work but does not gate acceptance or delivery.

- **Tool:** Claude Code · **Task:** Implement network-to-Room refresh and repository coordination.
  **Key prompt:** Implement Roadmap #8's repository contract, atomic snapshot replacement, Paging configuration, data-source coordination, and repository tests.
  - **AI contribution:** Made Room the post-refresh source of truth: the full response is decoded, validated, and mapped before a single transaction replaces the snapshot. The repository exposes Paging, summary, filter-option, and details operations without leaking network or database types.
  - **Verification:** Focused repository tests cover successful replacement, failed-refresh preservation of the previous snapshot, and query/summary behavior through the repository boundary.

- **Tool:** Codex · **Task:** Diagnose the outside-tap keyboard behavior.
  **Key prompt:** Identify why the software keyboard remains open after the user taps outside `LogSearchField`.

- **Tool:** Codex · **Task:** Propose a Compose-safe keyboard-dismissal solution.
  **Key prompt:** Recommend a focused, feature-owned outside-tap solution that preserves normal child gestures and existing test scope.

- **Tool:** Codex · **Task:** Implement the approved keyboard-dismissal fix.
  **Key prompt:** Add the approved screen-level outside-tap focus-clearing behavior without changing the reusable search component.

- **Tool:** Claude Code · **Task:** Implement query coordination in `LogViewerViewModel`.
  **Key prompt:** Implement Roadmap #9's applied filters, normalized `LogQuery` derivation, and coordinated Paging and aggregate streams driven by one query value.
  - **AI contribution:** Centralized query normalization in the ViewModel, including inactive filters, UTC end bounds, and invalid ranges. A single distinct query drives both Paging and the complete-result aggregate, while old work is cancelled when criteria change.
  - **Verification:** ViewModel tests cover query derivation, summary lifecycle, and coordination rules that prevent stale counts from being shown for new criteria.

- **Tool:** Claude Code · **Task:** Implement the structured filter sheet.
  **Key prompt:** Add the stateless design-system filter sheet, feature-side state mapping and actions, draft-versus-applied semantics, and the active-filter indicator.

- **Tool:** Claude Code · **Task:** Implement paging-aware presentation.
  **Key prompt:** Replace fixture-backed content with repository Paging data, inserted UTC-minute headers, launch-refresh retry behavior, and stable-ID detail lookup.
  - **AI contribution:** Kept `PagingData` outside immutable screen state and transformed rows before caching. UTC-minute headers are inserted across page boundaries, and details resolve by stable ID so selection still works after a page is evicted.
  - **Verification:** Paging-aware UI tests and screenshots cover grouped content, load states, retry behavior, empty results, and details presentation.

- **Tool:** Claude Code · **Task:** Audit and complete the log-viewer UI.
  **Key prompt:** Review the Roadmap #10 and #11 UI against the documented requirements, then correct remaining search, empty-state, fixture, and filter-indicator gaps.

- **Tool:** Claude Code · **Task:** Audit and complete structured-filter and Paging integration.
  **Key prompt:** Review the delivered filter UI and paged list against the requirements and architecture, then implement any missing Paging error state and update stale documentation.

- **Tool:** Claude Code · **Task:** Reduce unnecessary database queries from filter editing.
  **Key prompt:** Keep filter edits as a draft and run database work only after the user selects Apply.

- **Tool:** Codex · **Task:** Bound the active Paging window.
  **Key prompt:** Address the Paging review finding by configuring a finite `maxSize` so a long scroll does not retain every loaded row in presentation memory.

## 2026-08-18

- **Tool:** Codex · **Task:** Audit search and filtering performance for a 100,000-record snapshot.
  **Key prompt:** Review the current query path, identify likely bottlenecks at 100,000 records, and propose appropriately scoped performance actions.

- **Tool:** Codex · **Task:** Plan an optional Macrobenchmark suite.
  **Key prompt:** Plan a reproducible physical-device benchmark for scrolling, Paging, search, and combined filtering over a deterministic 100,000-row Room snapshot. Keep it non-blocking and document the protocol.
  - **AI contribution:** Defined a release-like, physical-device benchmark protocol with a deterministic 100,000-row fixture and independent scroll, Paging, search, and combined-filter scenarios. The plan deliberately treats performance evidence as informative rather than an acceptance gate.
  - **Verification:** Scenario completion conditions and device-state capture are documented so runs can be reproduced and compared only on the same device class.

- **Tool:** Claude Code · **Task:** Consolidate viewer controls in a Material 3 app bar.
  **Key prompt:** Replace stacked title and controls with a pinned small `TopAppBar` that presents the full-result and loaded counts clearly while retaining search, filter, and sort actions.

- **Tool:** Claude Code · **Task:** Implement the optional Macrobenchmark suite.
  **Key prompt:** Implement the approved release-like benchmark variant, deterministic data fixture, UI Automator scenarios, and reproducible protocol; record a run on the target device when available.
  - **AI contribution:** Added a benchmark-only snapshot refresher so the production read path remains unchanged while the benchmark variant uses deterministic local data. The suite uses UI Automator and recorded trace sections to measure the four user-facing scenarios.
  - **Verification:** The deterministic fixture has unit coverage for row identity and expected search/filter result counts; benchmark runs remain optional and are documented separately from functional acceptance.

- **Tool:** Claude Code · **Task:** Reduce recomposition caused by uncommitted filter edits.
  **Key prompt:** Diagnose the recomposition behavior during filter interactions and move uncommitted edit state to the feature-owned sheet host so list content does not recompose unnecessarily.

- **Tool:** Claude Code · **Task:** Optimize the search path's UI recomposition behavior.
  **Key prompt:** Review `LogViewerScreen` for UI performance bottlenecks and implement the highest-impact safe improvement.

- **Tool:** Claude Code · **Task:** Validate and implement a screen-file split.
  **Key prompt:** Evaluate the suggestion to split `LogViewerScreen.kt`, identify its benefits and risks, then make the refactor only if it preserves behavior and improves maintainability.

- **Tool:** Codex · **Task:** Establish the temporary Logs data and presentation module skeleton.
  **Key prompt:** Create independently configurable `:feature:logs:data` and `:feature:logs:presentation` modules, retain a temporary migration bridge, and verify the structural setup before moving sources.

- **Tool:** Codex · **Task:** Move the Logs data layer into `:feature:logs:data`.
  **Key prompt:** Relocate the data implementation, tests, and benchmark-specific refresh sources while exposing only the approved repository, model, and result contract.

- **Tool:** Codex · **Task:** Correct review findings in the temporary module bridge.
  **Key prompt:** Restore the feature-local typed result contract and remove data-owned dependencies from the temporary presentation bridge without changing behavior.

- **Tool:** Codex · **Task:** Move presentation into `:feature:logs:presentation`.
  **Key prompt:** Preserve presentation packages, resources, tests, and screenshots while removing the legacy module and verifying debug and benchmark variants.
  - **AI contribution:** Completed the final module boundary: `:app` depends on presentation and the design system, presentation depends on data and the design system, and data depends on network. The migration preserved feature ownership of Room and the repository-only data boundary.
  - **Verification:** Module-specific compilation, lint, unit, screenshot, and application-build checks were used to validate the migration without changing product behavior.

- **Tool:** Codex · **Task:** Synchronize delivery documentation and CI after the module split.
  **Key prompt:** Update active architecture, data-layer, roadmap, README, and CI documentation for the final data and presentation modules while retaining the repository-only data boundary.

- **Tool:** Codex · **Task:** Verify final module-split acceptance.
  **Key prompt:** Run the defined quality, unit, screenshot, build, and Android-test compilation checks; distinguish known baseline issues from regressions and avoid unrelated product changes.

- **Tool:** Codex · **Task:** Correct the final typed-result contract and ownership documentation.
  **Key prompt:** Bind result helpers directly to `LogsDataError`, remove the unnecessary public error marker, add a contract regression check, and align related documentation without changing repository behavior.

## 2026-08-19

- **Tool:** Codex · **Task:** Reorganize the README for interview review.
  **Key prompt:** Add the screen recording and prioritize concise evidence for architecture, Room and Paging flow, search and filtering behavior, and the documented performance work.
  - **AI contribution:** Reworked the README into an interview-facing narrative that leads with the demo, module boundaries, snapshot-to-Room/Paging flow, exact search and filter semantics, and test/performance evidence.
  - **Verification:** Documentation links, media references, Markdown structure, and the README's claims were checked against the current project documentation and build configuration.

- **Tool:** Codex · **Task:** Prepare the AI assistance record for submission.
  **Key prompt:** Standardize the record's format and language, retain material task and prompt summaries, and remove transient internal context or superseded experiments that would not help an interviewer evaluate the final project.
