# Prompts

- Date: 2026-08-16
- Tool: ChatGPT
- Task: Analyze requirement ambiguities from refinement/grooming and define reasonable implementation assumptions for the AI Semantic Log Android take-home prototype.
- Key prompt: Review the requirement document, attached critiques, sample API response, and dataset analysis. For each ambiguity raised during refinement/grooming, make a reasonable assumption with examples that allows implementation to proceed. Keep the solution appropriate for a prototype that must be completed within a few days; avoid over-engineering or introducing unnecessary production-scale architecture. Do not create the implementation plan or UI wireframes yet, as the assumptions will be reviewed first.

- Date: 2026-08-16
- Tool: ChatGPT (Image Generation)
- Task: Create a low-fidelity wireframe for the AI Semantic Log Android prototype based on the agreed requirement assumptions.
- Key prompt: Generate a grayscale, product-review wireframe showing the primary UI states for the log viewer: loading/skeleton, main grouped log list, search/filtered results, error/retry, and log-details bottom sheet. Reflect the agreed assumptions: ~5,000 logs fetched in one request, client-side search across message/tag/severity, timestamp grouping by one-minute buckets, newest-first sorting, an ERROR + FATAL severity-density indicator, and a minimal non-overengineered UI suitable for a take-home prototype.

- Date: 2026-08-16
- Tool: ChatGPT
- Task: Define reusable Jetpack Compose UI components for the AI Semantic Log prototype.
- Key prompt: Review the approved log-viewer wireframe and identify appropriate reusable Compose components without over-engineering the take-home prototype. Define component responsibilities and boundaries for the severity density indicator, search bar, result/sort summary, static timestamp group headers, aligned severity/tag badges, log rows, grouped lazy list, loading skeleton, error state, and log-details bottom sheet.

- Date: 2026-08-16
- Tool: CHATGPT
- Task: Analyze the supplied log dataset for its schema, categorical values, and temporal coverage.
- Key prompt: Analyze this dataset, providing insights includes how many types of severity, and tag. The time spread. Are data object structure identical?

- Date: 2026-08-16
- Tool: CHATGPT
- Task: Add project and documentation guidance to `AGENTS.md`
- Key prompt: Read every file in `documentation/`, then save a reference list and summary of each document in `AGENTS.md` so agents understand the project and know when to consult each source.

- Date: 2026-08-16
- Tool: CHATGPT
- Task: Refine the AI Semantic Log prototype requirements and document pragmatic assumptions from the supplied 5,000-record log dataset.
- Key prompt: This is the requirement document. After requirement refiendment/gromming, attached critiques are raised. Your task is to make some reasonable assumptioins with examples to those issues, do not over complex the requirement or over engineering since this is a prototpye application that needs to be submitted in a few days. The supplied sample response contains 5,000 consistently structured log records with `id`, UTC `timestamp`, `severity`, `tag`, `message`, and `metadata` fields. The full dataset analysis identifies five balanced severities, seven balanced tags, five messages, one-second ordered timestamps spanning about 83 minutes, and independent field combinations; use those facts to make scope-appropriate assumptions. Next step, don't do it in this turn: I will review those assumptioins, once confirmed, we will make low fiedelity prototpyes/ wireframes, so we can use let product to review the design, and build UI according to them.

- Date: 2026-08-16
- Tool: CODEX
- Task: Plan a high-level implementation roadmap for the AI Semantic Log prototype.
- Key prompt: Plan an implementation roadmap. Do not be too specific to avoid over planning. Ideally basic set ups - ui components - UI - simple UI interaction ( open the detail sheet) -  Network call - DTO to UI state mapping - business logic implementation  (filtering, grouping, density or error logs calculation etc)  - performance check and potential optimization -   test implementation.

- Date: 2026-08-16
- Tool: CODEX
- Task: Review the initial Android architecture proposal and prepare an implementation plan for the coding challenge.
- Key prompt: We are going to complete the coding challenge, requirements are specified in [requirement.md](requirement.md) .
The additional information about API and the requirement and grooming can be found in [api_and_requirement_gap_assumptions.md](api_and_requirement_gap_assumptions.md).
Now we will going to make the implementation plan.
I have the initial proposal. Before plan the whole architecture, let's review this proposal. You need to review if this proposal follows the clean architecture( Goole,  NOT Robert Martin) and best practices. For anything not specified in this proposal, raise it and discuss it with me. 
Overall: Kotlin, Compose, Retrofit for network call, Coroutine for concurrency, Flow for reactive programming. Hilt for DI
Architecture: MVI. ViewModel expose the single immutable data class as UI state. User interaction flow to the ViewModel as onAction callback.
Modularization: Network module for Retrofit infrastructure. UI Component module for reusable UI components. A single feature module for vertical slicing. Inside the feature module, the data layer and presentation layer is separated by package instead of module to avoid over engineering. Optional domain layer when there is complex or reusable business logic.
UI components:  Severity indicators, network&tag Badge, search bar, app top bar, loading shimmer effect(might can be done by a modifier extension function), bottom sheet. 
Testing: Need to decide weather to use mockk/mohito to mock the data, or use the testing repository. Screenshot testing using paparazzi. I do not think Espresso testing is required, justify this decision.
Code quality: use ktlint on every commit 

- Date: 2026-08-16
- Tool: CODEX
- Task: Create a file-by-file implementation plan for the Android project foundation milestone.
- Key prompt: Plan the `:app`, `:feature:logs`, `:core:network`, and `:core:designsystem` foundation, including exact files, packages, function signatures, compatible dependencies, module rules, theme entry point, CI, pre-commit checks, and build/launch verification.

- Date: 2026-08-16
- Tool: Claude Code
- Task: Implement the Android project foundation and verify its toolchain, module boundaries, quality gates, CI, and launch shell.
- Key prompt: Execute plan `docs/superpowers/plans/2026-08-16-project-foundation.md`, preserving the four-module Hilt/Retrofit/Compose architecture and completing every verification gate before feature work.

- Date: 2026-08-17
- Tool: Claude Code
- Task: Implement the revised Log UI Components & Design System plan inside `:core:designsystem` — tokens, all ten log-viewer components, the shimmer modifier, a private-fixture showcase with its full preview matrix, and Compose instrumented tests.
- Key prompt: Implement this plan via `/android-compose-ui`: neutral light/dark tokens plus high-recognition severity/density colors only; `SeverityBadge`, `TagBadge`, `LogRow`, `LogMinuteHeader` (chevron only when a collapse callback is supplied, no internal state), `LogSearchField`, a Canvas-based `SeverityIndicator` (segmented ERROR/FATAL ring, responsive legend, full contentDescription), `LoadingContent` (shimmer skeleton, single merged "Loading logs" description), `ErrorDialog`/`NoResultsContent`, and `LogDetailsSheet`; a non-`@Composable` `shimmerEffect` modifier built on `composed`/`rememberInfiniteTransition`/`drawWithCache`; Compose semantics tests per component; and a `LogComponentsShowcase` with 360dp light/dark, 320dp narrow, 760dp wide, 0/41/100% density, and collapsed/expanded header previews.
- AI contribution: Implemented all listed components, tokens, and tests from the plan's exact public signatures; resolved several compile-time API mismatches against the project's actual resolved dependency versions (Compose 1.11.3/Material3 1.4.0) that the plan's pseudocode didn't anticipate — `RowScope`/`ColumnScope.weight` is now a scope member requiring no import (an explicit import shadowed it with an internal symbol), `Icons.Default.ExpandMore` isn't in `material-icons-core` (swapped for `KeyboardArrowDown`), and `createComposeRule()`'s non-v2 overload is deprecated in this BOM (switched all test rules to the `v2` package to match `MainActivityLaunchTest`'s existing convention).
- Human verification: `ktlintCheck`, `:core:designsystem:lintDebug` (0 findings after fixing a `ModifierParameter` ordering issue and an accepted `PluralsCandidate` false positive on "percent"), full `assembleDebug`, and all 14 new instrumented tests run and passed on a connected emulator (which also surfaced and required pinning `espresso-core` to 3.7.0, matching `:app`'s existing pin, since the transitive 3.5.0 crashed on the device's API 37 image). Visually confirmed every component by temporarily pointing `MainActivity` at the showcase and at a targeted screen for the remaining components, screenshotting both, then reverting `MainActivity.kt` to its original committed content (confirmed via empty `git diff`).

- Date: 2026-08-16
- Tool: ChatGPT
- Task: Review GitHub PR #1 (`Project Foundation Implementation`) for the Android project foundation milestone.
- Key prompt: Review https://github.com/JOkeryyyy/FGFChallenge/pull/1 for correctness, architecture, Android/Compose/Kotlin best practices, Gradle and module configuration, CI, testing, maintainability, and alignment with the documented project architecture and implementation roadmap. Identify blocking and non-blocking issues and draft actionable GitHub review comments in English.

- Date: 2026-08-16
- Tool: CODEX
- Task: Design the `:core:designsystem` UI component plan for the AI Semantic Log viewer.
- Key prompt: Plan deterministic neutral light/dark tokens with colourful severity badges; responsive Compose layouts with fixed 20dp horizontal padding; stateless log rows, minute headers with opt-in collapse support, badges, search, loading skeletons with a reusable shimmer modifier, error dialog, text-only no-results state, Canvas severity indicator, and details sheet; define public composable signatures, Compose Semantics test coverage, and a component showcase preview without adding screenshot tests before UI review.
