# Android Data Layer Coding Conventions

## Purpose and precedence

This document is a general Android Data Layer style guide: how to shape repositories,
data sources, models, mapping, error handling, DI, and naming. It elaborates on
[`documentation/ARCHITECTURE.md`](../ARCHITECTURE.md); it does not replace it.

Where a concrete project decision already made in `ARCHITECTURE.md` differs from an
illustrative example below (module/package layout, a specific class name, whether a
`DataSource` abstraction exists for the logs feature), `ARCHITECTURE.md` wins. For this
project specifically:

- The approved data package layout is `data/remote`, `data/local`, `data/model`,
  `data/mapper`, `data/repository`, `data/error`, and `data/di` (see
  `ARCHITECTURE.md` → *Gradle modules*). This one-screen prototype does not require a
  `domain/query` package. The flat `data/LogsRepository.kt` layouts below remain
  illustrative rather than literal.
- Step 5's `NetworkLogsRepository` is a transitional remote-only baseline. Step 8
  replaces it with `SnapshotLogsRepository`, named for its complete-remote-snapshot to
  Room strategy, rather than the generic `DefaultLogsRepository` used in examples.
- The revised repository coordinates `LogsApi` and the feature-owned Room database.
  Separate remote/local Data Source interfaces are still optional: add one only when
  multiple implementations or meaningful test/lifecycle isolation justify it, not
  merely because two infrastructure sources now exist.
- Room is the post-refresh source of truth, and Paging 3 carries the bounded row working
  set. The immutable `LogQuery` repository input remains in `data/model`; the ViewModel
  derives it and coordinates page/aggregate streams without moving the repository
  contract into another layer.
- Hilt is the project's DI framework (see §12); the framework-agnostic examples below
  apply equally to Hilt constructor injection.
- Use this project's existing typed `Result<T, E : Error>` / `EmptyResult` wrapper and
  its `map`/`onSuccess`/`onFailure` helpers rather than redefining a parallel `Result`
  type — see §11. It lives in `:feature:logs`, not `:core:network`.
- The feature's repository failure is a single `LogsDataError` value, not the multi-case
  hierarchy the illustrative example in §11 shows — see *Match error granularity to
  behavior* in that section.
- Data-layer Hilt bindings for the API, Room database/DAO, repository, and required
  dispatchers live in one feature module (`LogsDataModule`), not one module per binding
  — see §12.

## 1. Architectural Baseline

Follow Android Architecture Guidance, not strict Clean Architecture.
The default application architecture consists of:

```text
UI Layer
   ↓
Data Layer
```

A Domain Layer may be added when it provides clear value:

```text
UI Layer
   ↓
Domain Layer   // optional
   ↓
Data Layer
```

The Domain Layer is not mandatory. It exists primarily to encapsulate complex business logic or logic reused by multiple ViewModels.
Do not create a Domain Layer, Use Case, or Domain Model solely to satisfy an architectural template.

### Dependency Direction

Allowed:

```text
ViewModel → Repository

ViewModel → UseCase → Repository
```

Not allowed:

```text
ViewModel → DataSource
ViewModel → DAO
ViewModel → Network Client
ViewModel → DataStore
```

Repositories are the public entry points into the Data Layer.
UI and Domain components must not access Data Sources directly.

## 2. ViewModel Dependencies

A ViewModel may depend directly on:

* Repository interfaces; or
* Use Cases when a Domain Layer is justified.

Example without a Domain Layer:

```kotlin
class LogsViewModel(
    private val logsRepository: LogsRepository
) : ViewModel()
```

Example with a Domain Layer:

```kotlin
class LogsViewModel(
    private val getFilteredLogs: GetFilteredLogsUseCase
) : ViewModel()
```

Do not inject Data Sources into ViewModels:

```kotlin
// Do not do this.
class LogsViewModel(
    private val logsRemoteDataSource: LogsRemoteDataSource
) : ViewModel()
```

ViewModels should generally:

* Consume `Flow` for observable data.
* Call `suspend` functions for one-shot operations.
* Launch screen-related coroutine work from `viewModelScope`.

## 3. Domain Layer and Use Cases

The Domain Layer is optional.
Create a Use Case when at least one of the following applies:

* Business logic is sufficiently complex that keeping it in the ViewModel would make the ViewModel difficult to understand.
* The same business operation is reused by multiple ViewModels.
* An operation coordinates several repositories.
* The operation represents a meaningful application-level action.

Example:

```kotlin
class GetFilteredLogsUseCase(
    private val logsRepository: LogsRepository
) {
    operator fun invoke(
        query: String,
        severities: Set<Severity>
    ): Flow<List<LogEntry>> {
        // Meaningful reusable business logic.
    }
}
```

Avoid pass-through Use Cases that provide no abstraction or business logic:

```kotlin
// Usually unnecessary.
class GetLogsUseCase(
    private val repository: LogsRepository
) {
    operator fun invoke() = repository.getLogs()
}
```

Do not create a Domain Layer merely to move Repository interfaces away from the Data Layer.
Under this convention, the optional Domain Layer depends on Repository contracts owned by the Data Layer.

## 4. Repository

A Repository is the public API of the Data Layer for a type of application data.
Repositories may be responsible for:

* Exposing application data.
* Abstracting concrete sources of data.
* Centralizing changes to application data.
* Coordinating one or more Data Sources.
* Resolving conflicts between sources.
* Defining the source of truth.
* Performing data-related transformations or business logic.

A Repository may use zero, one, or multiple Data Sources.
A Repository must not be reserved only for multi-source implementations.

### Single Data Source

A single source still uses a Repository boundary:

```text
ViewModel
   ↓
LogsRepository
   ↓
LogsRemoteDataSource
   ↓
Network API
```

Example:

```kotlin
interface LogsRepository {
    suspend fun getLogs(): Result<List<LogEntry>, DataError>
}

class DefaultLogsRepository(
    private val remoteDataSource: LogsRemoteDataSource
) : LogsRepository {

    override suspend fun getLogs(): Result<List<LogEntry>, DataError> {
        return remoteDataSource.getLogs()
    }
}
```

Do not expose the Data Source directly merely because there is currently only one source.
For very simple implementations, the Data Source responsibility may be merged into the Repository instead of creating a redundant Data Source class:

```text
ViewModel
   ↓
LogsRepository
   ↓
Network API
```

The important invariant is:
Higher layers interact with the Repository, not the underlying data source.

## 5. Repository Interfaces

Repository contracts belong to the Data Layer, not the Domain Layer.
Example:

```text
feature/logs/
├── data/
│   ├── LogsRepository.kt
│   └── DefaultLogsRepository.kt
├── domain/                    // optional
│   └── GetFilteredLogsUseCase.kt
└── presentation/
    └── LogsViewModel.kt
```

```kotlin
// data/LogsRepository.kt
interface LogsRepository {
    fun observeLogs(): Flow<List<LogEntry>>

    suspend fun refresh(): Result<Unit, DataError>
}
```

```kotlin
// domain/GetFilteredLogsUseCase.kt
class GetFilteredLogsUseCase(
    private val repository: LogsRepository
)
```

The Repository is part of the Data Layer's public contract.
Do not move Repository interfaces into `domain` merely to enforce Dependency Inversion.

## 6. Data Sources

A Data Source represents access to one concrete category of data source, such as:

* Network
* Local database
* DataStore
* File
* Memory cache
* Bluetooth
* Location provider

Each Data Source should work with one source only.
Examples:

```kotlin
interface LogsRemoteDataSource {
    suspend fun getLogs(): List<LogDto>
}

interface LogsLocalDataSource {
    fun observeLogs(): Flow<List<LogEntity>>

    suspend fun upsertAll(logs: List<LogEntity>)
}
```

### Data Source Interfaces Belong to Data

If a Data Source interface is needed, both its interface and implementation belong to the Data Layer:

```text
data/
├── remote/
│   ├── LogsRemoteDataSource.kt
│   └── DefaultLogsRemoteDataSource.kt
└── local/
    ├── LogsLocalDataSource.kt
    └── DefaultLogsLocalDataSource.kt
```

Do not place:

```text
LogsRemoteDataSource
LogsLocalDataSource
```

inside the Domain Layer.
Concepts such as:

```text
remote
local
API
database
cache
```

are implementation concerns of the Data Layer.

### Do Not Create Unnecessary Data Source Abstractions

A separate Data Source interface is useful when:

* Multiple implementations exist.
* An implementation is expected to change.
* Migration between storage or networking technologies is occurring.
* The abstraction materially improves testing or isolation.

It does not need to exist merely because an architecture diagram contains a `DataSource` box.

## 7. Models and Mapping

Do not enforce a mandatory:

```text
DTO → Domain Model → Entity
```

pipeline for every feature.
Different models should exist when the boundary has a meaningful reason.

### Network DTO

Network serialization models belong to the Data Layer:

```kotlin
@Serializable
data class LogDto(
    val id: String,
    val timestamp: String,
    val severity: String,
    val tag: String,
    val message: String
)
```

### Persistence Entity

Database models belong to the Data Layer:

```kotlin
@Entity
data class LogEntity(
    @PrimaryKey val id: String,
    val timestamp: Instant,
    val severity: String,
    val tag: String,
    val message: String
)
```

### Application / Data Model

Repositories should expose application-oriented immutable models:

```kotlin
data class LogEntry(
    val id: String,
    val timestamp: Instant,
    val severity: Severity,
    val tag: String,
    val message: String
)
```

A separate Domain Model is not required simply because a Domain Layer exists.
Introduce another model when there is a real difference in:

* Semantics
* Lifecycle
* Ownership
* Persistence representation
* Serialization representation
* Security boundary
* Business invariants
* Data required by consumers

Avoid mechanically creating several identical data classes and mappers.

## 8. Mapping

Mappings should normally happen close to the boundary that owns the source representation.
Example:

```kotlin
fun LogDto.toLogEntry(): LogEntry =
    LogEntry(
        id = id,
        timestamp = Instant.parse(timestamp),
        severity = Severity.valueOf(severity),
        tag = tag,
        message = message
    )
```

```kotlin
fun LogEntity.toLogEntry(): LogEntry = ...
```

```kotlin
fun LogEntry.toEntity(): LogEntity = ...
```

Simple mapper extension functions should live in the Data Layer near the source model they convert.
Do not allow network DTOs or persistence Entities to leak through the Repository API by accident.

## 9. Immutable Data

Data exposed from the Data Layer should be immutable from the consumer's perspective.
Prefer:

```kotlin
data class LogEntry(
    val id: String,
    val message: String
)
```

over models whose state can be mutated externally.
Mutable state should remain owned by the component responsible for maintaining its invariants.

## 10. Coroutines and Flow

Data and Domain APIs should generally expose:

```text
suspend function
```

for one-shot operations and:

```text
Flow<T>
```

for values that change over time.
Example:

```kotlin
interface LogsRepository {

    fun observeLogs(): Flow<List<LogEntry>>

    suspend fun refresh(): Result<Unit, DataError>
}
```

### Main Safety

Repository and Data Source APIs must be safe to call from the main thread.
A class performing blocking or CPU-intensive work is responsible for moving that work to an appropriate dispatcher rather than requiring its caller to know its threading requirements.
Prefer injected dispatchers when explicit dispatcher switching is necessary:

```kotlin
class LogParser(
    private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun parse(input: String): List<LogEntry> =
        withContext(defaultDispatcher) {
            // CPU-intensive parsing
        }
}
```

Do not hardcode dispatchers throughout production classes when injection provides meaningful testability.

## 11. Error Handling

Expected failures should be translated into application-understandable error types at the Data Layer boundary rather than leaking framework-specific exceptions into ViewModels.
For example:

```kotlin
sealed interface DataError {

    sealed interface Network : DataError {
        data object NoInternet : Network
        data object RequestTimeout : Network
        data object Unauthorized : Network
        data object ServerError : Network
        data object Unknown : Network
    }

    sealed interface Local : DataError {
        data object DiskFull : Local
        data object DatabaseError : Local
        data object Unknown : Local
    }
}
```

A project-specific typed result may be used:

```kotlin
Result<T, DataError>
```

This is a project convention rather than a requirement of Android Architecture Guidance.
In this project, reuse the existing `Result<T, E : Error>` / `EmptyResult` wrapper and its
`map`/`onSuccess`/`onFailure` helpers instead of introducing a second, parallel `Result`
type — one typed-result convention per codebase avoids ambiguity between it and
`kotlin.Result` from the standard library.

The wrapper stays in `:feature:logs`. It is a result convention, not network
infrastructure, so it does not belong in `:core:network`; promote it to a neutral shared
module only when a second feature genuinely reuses it.

### Match Error Granularity to Behavior

The hierarchy above illustrates the shape of a typed error, not a target to reproduce.
Split a failure into separate cases only where a consumer behaves differently for each —
different retry policy, different user-facing copy, different recovery path. Cases that
every caller handles identically add naming, mapping, and test surface while changing no
behavior, and they invite `when` branches that all do the same thing.

In this project, `LogsDataError` is a single value: a failed load produces the same
retryable error state whatever caused it. Connectivity loss, timeout, a non-2xx response,
undecodable JSON, and a mapping failure therefore collapse into one failure. Classify
further only when a caller can act on the difference.

### Preserve Coroutine Cancellation

Never blindly convert every `Exception` into a data error.
Bad:

```kotlin
catch (e: Exception) {
    Result.Error(DataError.Local.Unknown)
}
```

Cancellation in Kotlin coroutines uses `CancellationException`. Cancellation must remain transparent rather than being converted into an ordinary application failure.
If broad exception handling is unavoidable:

```kotlin
import kotlinx.coroutines.CancellationException

catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    Result.Error(mapNetworkError(e))
}
```

Import `kotlinx.coroutines.CancellationException` explicitly. `java.util.concurrent.CancellationException`
is a different, unrelated type — catching it instead does not rethrow the coroutine's real
cancellation signal and silently breaks structured concurrency (the coroutine looks
"handled" instead of actually cancelling).

Prefer catching expected operational exceptions instead of using `Exception` as the normal control boundary.

## 12. Dependency Injection

Repositories receive their dependencies through constructor injection:

```kotlin
class DefaultLogsRepository(
    private val remoteDataSource: LogsRemoteDataSource
) : LogsRepository
```

Data Sources receive their concrete infrastructure dependencies in the same way:

```kotlin
class DefaultLogsRemoteDataSource(
    private val api: LogsApi
) : LogsRemoteDataSource
```

The architecture convention does not mandate a Dependency Injection framework.
The application may use:

* Hilt
* Koin
* Manual dependency injection
* Another project-approved mechanism

Architecture rules should not unnecessarily depend on a particular DI library. This
project uses Hilt (see `ARCHITECTURE.md` → *Dependency injection*); apply the constructor
injection shown above via `@Inject constructor` and provide bindings/modules under each
feature's `data/di` package.

### One Module Per Owner, Not Per Binding

Group a feature's data-layer bindings into a single module — in this project,
`LogsDataModule` holds the endpoint API, Room database/DAO, repository binding, and
required data dispatchers. Splitting them across a module per binding adds files,
imports, and headers without introducing a boundary: they share one component, one
lifetime, and one owner.

Add a second module when something real separates it — a different component or scope, a
binding another module must be able to replace in tests, or an owner outside the feature.

Since `@Binds` methods must be abstract and `@Provides` methods must not be, one module
holding both is an abstract class whose `@Provides` bindings live in its companion object.

## 13. Source of Truth

Each Repository should have a clearly defined Single Source of Truth for the data it exposes.
The source of truth may be:

* A database
* An in-memory cache
* A network source
* Another authoritative source

It is not automatically a local database.
The Repository is responsible for ensuring that data exposed to higher layers represents its chosen source of truth.

For this feature, `ARCHITECTURE.md` makes the concrete choice: after a successful launch
refresh, Room is the source of truth for paged rows, aggregate summaries, filter options,
and details. The network supplies replacement snapshots; it is not queried for repeated
screen reads.

## 14. Naming

Prefer names that explain behavior or role.

### Repository

```text
LogsRepository
DefaultLogsRepository
InMemoryLogsRepository
```

### Data Source

```text
LogsRemoteDataSource
LogsLocalDataSource
```

When multiple implementations make the underlying technology relevant:

```text
ApiLogsRemoteDataSource
FakeLogsRemoteDataSource
DatabaseLogsLocalDataSource
```

Avoid implementation names that communicate nothing:

```text
LogsRepositoryImpl
LogsDataSourceImpl
```

If no more meaningful implementation name exists, prefer:

```text
DefaultLogsRepository
```

### Models

```text
LogDto       // network representation
LogEntity    // database representation
LogEntry     // application/data model
LogUiModel   // presentation-specific model, when needed
```

Do not introduce `LogDomainModel` simply because a Domain Layer exists.

## 15. Recommended Feature Structure

### Simple Feature — No Domain Layer

```text
feature/logs/
├── data/
│   ├── model/
│   │   └── LogEntry.kt
│   │
│   ├── remote/
│   │   ├── LogDto.kt
│   │   ├── LogsRemoteDataSource.kt
│   │   └── DefaultLogsRemoteDataSource.kt
│   │
│   ├── LogsRepository.kt
│   └── DefaultLogsRepository.kt
│
└── presentation/
    ├── LogsViewModel.kt
    ├── LogsUiState.kt
    └── LogsScreen.kt
```

Dependency:

```text
presentation
     ↓
    data
```

### Feature with a Justified Domain Layer

```text
feature/logs/
├── data/
│   ├── LogsRepository.kt
│   ├── DefaultLogsRepository.kt
│   └── ...
│
├── domain/
│   └── GetFilteredLogsUseCase.kt
│
└── presentation/
    └── LogsViewModel.kt
```

Dependency:

```text
presentation
     ↓
   domain
     ↓
    data
```

The Repository interface remains in `data`.

> This project's approved `feature/logs` layout is documented in `ARCHITECTURE.md` →
> *Gradle modules* (`data/remote`, `data/local`, `data/model`, `data/mapper`,
> `data/repository`, `data/error`, `data/di`). Treat the layouts above as generic
> illustrations of the dependency direction and the "Repository interface lives in
> `data`" rule, not as a literal restructuring of this project.

## 16. Testing Boundaries

Test at meaningful architectural boundaries.

### Repository

Test:

* Mapping
* Source selection
* Error translation
* Source-of-truth behavior
* Data coordination behavior

### Data Source

Test:

* API request/response handling
* DAO interaction
* Serialization
* Infrastructure-specific behavior

### Use Case

Test only meaningful business behavior.
Do not create a Use Case solely to make something mockable.
Use fake implementations with meaningful names:

```kotlin
class FakeLogsRepository : LogsRepository
```

rather than leaking infrastructure into ViewModel tests.

## 17. Architecture Checklist

Before adding or modifying a data feature:

* UI/ViewModel depends only on a Repository or justified Use Case.
* ViewModel does not directly depend on a Data Source, DAO, API client, DataStore, or database.
* A Repository exists even when only one explicit Data Source exists.
* Repository interfaces and implementations belong to the Data Layer.
* Data Source interfaces and implementations belong to the Data Layer.
* Each Data Source represents only one source category.
* Domain Layer is added only when business complexity or reuse justifies it.
* No unnecessary pass-through Use Cases are created.
* DTOs and persistence Entities remain inside the Data Layer.
* Additional models are introduced only when a real boundary or semantic difference exists.
* Data exposed by repositories is immutable from the consumer's perspective.
* One-shot operations use `suspend`; observable changing data uses `Flow`.
* Data and Domain APIs are main-safe.
* Coroutine cancellation is never converted into a normal application error.
* Error cases exist only where a consumer behaves differently for each of them.
* Data-source or framework exceptions do not accidentally leak into the UI.
* DI modules are grouped by owner and lifetime rather than one module per binding.
* Implementations use meaningful names rather than generic `Impl` suffixes.

## Core Rule

When deciding where code belongs, optimize for clear ownership and useful boundaries rather than architectural ceremony.
The default mental model is:

```text
UI
 ↓
ViewModel
 ↓
Use Case           ← optional
 ↓
Repository         ← Data Layer public boundary
 ↓
Data Source(s)     ← Data Layer implementation details
 ↓
Network / Database / DataStore / Files / Device APIs
```

Repository is the boundary.
Data Source is an implementation detail.
Domain is optional.
Abstractions should exist because they provide value, not because strict Clean Architecture requires another layer.
