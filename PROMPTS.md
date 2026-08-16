# Prompts

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
