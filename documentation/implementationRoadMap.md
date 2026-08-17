# AI Semantic Log Implementation Roadmap

## Summary

Build the app as a sequence of usable increments: establish the project foundation, create the visual system and fixture-backed UI, add details interaction, connect the network and mapping pipeline, implement log processing, measure performance, and finish verification and delivery.

[ARCHITECTURE.md](/Users/gao/Documents/fgfChallenge/ARCHITECTURE.md) remains the source of truth. The roadmap intentionally avoids file-by-file tasks, dependency versions, and commit-level detail.

## Implementation Roadmap

1. **Project foundation**
   - Initialize the Android project and Git repository with `:app`, `:feature:logs`, `:core:network`, and `:core:designsystem`.
   - Configure the version catalog, compatible toolchain, Compose, Hilt/KSP, Retrofit, serialization, and test dependencies.
   - Establish module dependency rules, theme entry point, CI checks, and the repository-owned pre-commit quality gate.
   - Confirm a minimal application builds and launches before feature work begins.

2. **Design system and UI components**
   - Define deterministic light/dark colors, typography, shapes, spacing, and severity styling.
   - Implement stateless reusable components for log rows, minute headers, badges, search, loading, errors, empty results, and the Canvas severity indicator.
   - Add representative previews while each component is isolated.
   - Plan the layout of each component. For example, LogRow is a row layout with two badges, an information text, and a second+ms timestamp. 
   - Consider responsive design also. UI does not need to be perfect under any font size or screen size, the goal is simply stay organized 

3. **Fixture-backed screen UI**
   - Assemble the full screen from realistic sample UI state before networking is introduced.
   - Cover loading, error, populated content, and filtered-empty layouts.
   - Render grouped content through one flat `LazyColumn` with stable keys and content types.
   - Match the supplied wireframe while supporting different widths and both themes.

4. **Basic UI interaction**
   - Introduce immutable screen state and user actions for query changes, sorting, retry, log selection, and dismissal.
   - Connect row selection to the modal details sheet and support close, swipe, and Back dismissal.
   - Keep selection in durable screen state; avoid a separate event stream for this interaction.

5. **Network and repository boundary**
   - Configure the shared Retrofit/OkHttp client and feature-owned logs endpoint.
   - Fetch the supplied payload as a one-shot suspending request.
   - Classify connectivity, timeout, HTTP, serialization, schema, and unknown failures without leaking transport exceptions.
   - Wire the API and repository through Hilt while preserving cancellation.

6. **DTO, application-model, and UI-state mapping**
   - Define serializable DTOs matching the supplied response.
   - Validate required values, parse timestamps, map severities—including `UNKNOWN`—and produce repository-facing models.
   - Convert application models into display-ready UI models with UTC formatting.
   - Connect repository results to the ViewModel’s loading, content, error, and retry states.

7. **Business-logic pipeline**
   - Prepare loaded logs once for repeated in-memory processing.
   - Add cancellable search-as-you-type across message, tag, and severity.
   - Implement UTC minute grouping, chronological ordering, deterministic tie-breaking, severity counts, and error-density calculation.
   - Recompute displayed results and summaries when the query or sort direction changes.
   - Keep this logic in focused pure helpers; introduce a domain use case only if complexity or reuse provides a concrete reason.

8. **Performance review and targeted optimization**
   - Profile initial loading, filtering, state production, recomposition, and list scrolling with the complete 5,000-entry payload.
   - Confirm CPU-heavy mapping and processing remain off the main thread.
   - Optimize only measured bottlenecks, prioritizing prepared search values, reuse of sorted data, immutable display models, stable list identity, and reduced recomposition.
   - Re-run the same measurements after changes to confirm an improvement rather than relying on assumptions.

9. **Test completion and release verification**
   - Develop focused tests alongside networking, mapping, processing, and ViewModel work, then complete the broader suite in this milestone.
   - Run JVM processing and ViewModel tests, MockWebServer repository tests, Paparazzi visual tests, and targeted Compose interaction tests.
   - Execute lint, ktlint, application assembly, and instrumented-test compilation in CI; run the critical Compose flows on a local device or emulator.
   - Verify the pre-commit hook both passes clean code and blocks an intentional violation.
   - Complete `README.md`, `PROMPTS.md`, screenshots or recording, performance notes, and final delivery checks.

## Important Interfaces and Types

- `LogsFeature` is the only feature entry point exposed to `:app`.
- `LogsRepository.getLogs()` is the presentation-facing one-shot data contract and returns application models through a typed result.
- `LogViewerUiState` represents mutually exclusive loading, error, or content state together with query, sort, and selected-log state.
- `LogViewerAction` covers all user input; details-sheet visibility is derived from the selected log.
- UI list models represent minute headers and log rows as a flat collection ready for Compose.
- Design-system components accept display-ready values and callbacks and remain independent of feature, DTO, and repository types.

## Test and Acceptance Scenarios

- Successful load, retry after failure, invalid payload, unknown severity, cancellation, and count mismatch.
- Blank and nonblank search, case-insensitive matching, no results, rapid query replacement, and sort changes.
- UTC minute boundaries, deterministic timestamp/ID ties, all severity counts, and empty-result density.
- Loading, error, content, no-results, light/dark, and details-sheet visual states.
- Row selection opens the correct details; close, swipe, and Back dismiss it.
- The complete dataset loads, filters, and scrolls responsively without observable main-thread stalls.

## Assumptions and Defaults

- Use the architecture-selected Hilt, Retrofit/OkHttp, Kotlinx Serialization, Coroutines/Flow, Material 3, JUnit 4, AssertK, Turbine, MockWebServer, and Paparazzi stack.
- Do not add navigation, Room, pagination, runtime AI, analytics, production observability, a build-logic module, or an initial domain package.
- Testing is incremental during implementation, with the final milestone used for coverage completion and end-to-end verification.
- Dependency versions are selected and pinned during setup, but are intentionally not prescribed by this roadmap.
