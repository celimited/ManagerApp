# ManagerApp — Compose Multiplatform (Android + iOS)

## Scope
- Targets: Android, iOS only. No desktop/web/watchOS.
- Structure: single Gradle module (`composeApp` or `shared` — confirm actual name) for now.
  Multi-module is a future decision, not a current one — do not suggest splitting modules
  unless explicitly asked.
- Goal: lightweight, fast-startup, low-overhead app. Avoid heavy abstractions that don't
  pay for themselves at this size. Don't add a pattern "for scale" that isn't needed yet.

## Source sets
- `commonMain`: all shared code — UI, ViewModels, UseCases, Repositories, DB, DI. Default
  location for everything unless a platform API forces otherwise.
- `androidMain` / `iosMain`: ONLY platform-specific implementations (e.g. platform DB driver,
  platform HTTP engine config, permission APIs). Keep these as thin as possible.
- Prefer interfaces in commonMain + Koin-provided platform implementations over `expect`/`actual`.
  Use `expect`/`actual` only for genuinely simple platform primitives (e.g. `expect fun now(): Long`).

## Navigation — Navigation 3
- Use JetBrains' multiplatform port of Navigation 3, NOT Google's raw AndroidX artifacts:
  `org.jetbrains.androidx.navigation3:navigation3-ui` (only artifact with a separate CMP
  implementation; `navigation3-common` comes in transitively).
- Requires Compose Multiplatform 1.10+ — confirm the project's CMP version before assuming
  this is wired up; on anything older, Navigation 3 multiplatform support does not exist.
- Nav keys/routes and back stack live in `commonMain` — this is genuinely shared, not a
  per-platform concern.
- `NavDisplay` composable setup lives in `commonMain` too; do not duplicate it per platform.
- For adaptive list/detail layouts (if/when needed), use the Material 3 Adaptive Navigation 3
  library (`org.jetbrains.compose.material3.adaptive:adaptive-navigation3`) with a
  `SceneStrategy` rather than hand-rolling breakpoint logic.
- Navigation actions (navigate/pop) are triggered from the ViewModel via `UiEffect`
  (see MVVM section) — the ViewModel decides when to navigate, the Composable executes it by
  observing the effect, not the other way around.
- Do not add a second navigation library (e.g. Voyager, Decompose) alongside this — one
  navigation solution only.

## Architecture — Clean Architecture, layer-first packages
Layers are top-level packages (not nested per feature); each layer is subdivided by feature
where a folder would otherwise mix multiple features' files:

```
com.app.manager/
  feature/
    <feature_name>/      -> Screen (Compose UI), ViewModel, UiState, UiEffect — flat,
                             no presentation/domain/data sub-packages here
  model/
    <feature_name>/       -> domain models
  domain/
    repository/           -> repository interfaces (flat — names are already unique,
                             e.g. AuthRepository)
    usecase/
      <feature_name>/      -> UseCases
  data/
    remote/
      dto/
        <feature_name>/    -> request/response DTOs
      <feature_name>/      -> remote data sources
    local/                 -> local data sources (SQLDelight, once added)
    mapper/
      <feature_name>/      -> DTO <-> domain mappers
    repository/            -> repository implementations (flat, mirrors domain/repository/)
  core/
    network/               -> Ktor client setup, interceptors, error mapping
    database/               -> SQLDelight setup, DAOs/queries
    di/                     -> Koin modules
    navigation/             -> Navigation 3 keys/routes, NavDisplay setup, SceneStrategy
    common/                 -> shared utils, Result wrapper, base classes (only genuinely
                             cross-cutting/reusable infra — nothing feature-specific)
```

Rule: presentation never talks to data directly. Presentation -> domain (UseCase) ->
data (Repository). Repository interface lives in `domain/repository/`, implementation lives in
`data/repository/`. Add a new feature's subfolder under each relevant top-level layer as needed —
don't collapse unrelated features' files into one flat folder within a layer.

## MVVM
- One ViewModel per screen/feature, exposing a single immutable `UiState` data class per
  screen as `StateFlow<UiState>` — no multiple loose/parallel StateFlows per screen.
- One-shot effects (navigation, snackbars, toasts) go through a separate
  `Channel`/`SharedFlow` of `UiEffect` — NOT through UiState. State is for what's rendered;
  effects are for what happens once.
- ViewModel exposes intent-named public functions (e.g. `fun onRefresh()`, `fun onSubmit(name:
  String)`) that internally update state — not a single generic `onIntent()` dispatcher (that's
  MVI; we're deliberately not doing that here) and not raw setter-style state mutation from the UI.
- State updates always produce a new `UiState` copy (`state.update { it.copy(...) }`) — never
  mutate fields in place.
- ViewModel depends on UseCases only, never directly on Repository or data sources.
- Use `viewModelScope` for coroutine launches; no manual scope management in ViewModels.

## Compose performance rules
Recomposition cost is a measurable, diagnosable problem, not an inherent Compose tax — apply
these rules by default, and use Layout Inspector's recomposition counts to verify before
adding anything beyond this list:
- UiState fields that are collections MUST use `kotlinx.collections.immutable`
  (`ImmutableList`, `persistentListOf`, etc.), never plain `List`/`Map` — plain collections are
  unstable to the compose compiler and defeat skipping.
- Mark hand-written data classes used in UiState `@Immutable` or `@Stable` when the guarantee
  actually holds (all `val`, immutable field types).
- Composables read only the specific UiState fields/sub-objects they need, passed as
  parameters from the screen root — never pass the entire `UiState` object down into child
  composables "for convenience."
- Use `derivedStateOf` for any value computed from state that changes less often than the
  state itself (e.g. a boolean derived from a list size).
- Confirm Compose compiler strong skipping mode is enabled in `build.gradle.kts` before
  manually `remember`-wrapping lambdas — under strong skipping this is usually unnecessary.
- `LazyColumn`/`LazyRow` items always get a stable, unique `key`.
- No non-trivial computation directly in a composable body — wrap in `remember(keys)`.

## UseCase pattern
- One UseCase = one business operation. Class name states the action:
  `GetUserProfileUseCase`, `SubmitOrderUseCase` — not generic `UserUseCase` grab-bags.
- UseCase invoked via `operator fun invoke(...)`, called as `useCase(params)` from the
  ViewModel.
- UseCase orchestrates repositories; it does not contain platform-specific code.
- UseCase is called only from the ViewModel — never directly from a Composable.

## DI — Koin
- Koin modules organized per feature under `core/di/`, aggregated into one root module list
  at app start.
- Provide platform-specific implementations via Koin's platform-specific module (`androidMain`
  Koin module vs `iosMain` Koin module), not via manual factory branching in shared code.
- ViewModels resolved via Koin's `koinViewModel()` — don't hand-instantiate.

## Error handling — single strategy, no per-feature variants
- All Repository/UseCase calls that can fail return a single shared `Result<T>` /
  `DataResult<T>` sealed type (Success / Error(AppError) / Loading if needed) — not raw
  exceptions bubbling to the ViewModel, and not a different error shape per feature.
- Define one `AppError` sealed hierarchy (Network, Server, Unauthorized, Unknown, etc.) in
  `core/common/`. All network/DB errors get mapped to this at the data layer boundary —
  ViewModels only ever see `AppError`, never raw Ktor/SQLDelight exceptions.
- ViewModel maps `AppError` to user-facing UI state/message inside the relevant state-updating
  function, producing the updated `UiState` — don't invent ad hoc error strings inline in
  composables.
  Transient errors (snackbar/toast) go through `UiEffect`; persistent errors (e.g. "failed to
  load screen") belong in `UiState`.

## Single source of truth — local DB
- Local DB (assume SQLDelight for Android+iOS parity — flag if Room is intended instead,
  since Room's iOS/KMP support is less mature) is the source of truth for state the UI reads.
- Pattern: network fetch -> write to DB -> UI observes DB via Flow. UI does not read
  network results directly.
- Repository exposes `Flow<T>` from DB reads; one-shot network calls trigger a DB write,
  not a direct return-to-UI.

## Coroutines
- All async work via coroutines/Flow — no callbacks, no platform-native async APIs leaking
  into commonMain.
- Repositories expose `Flow` for observed data, `suspend fun` for one-shot actions.
- Use `Dispatchers.IO`-equivalent (via Koin-provided dispatcher, not hardcoded
  `Dispatchers.IO` directly, since iOS/Native dispatcher handling differs) for DB/network work.

## Dependencies
- Check `gradle/libs.versions.toml` before adding any dependency or version — do not
  introduce a new version of something already declared there.
- Don't add a new library for something an existing dependency already covers.

## Build / verify
- `./gradlew :composeApp:compileDebugKotlin` for a fast Android-side check (confirm actual
  module/task name against this project's build.gradle.kts — don't assume).
- iOS target CANNOT be build-verified on this machine (Windows, no Xcode/simulator). Do not
  claim iOS compiles or runs — flag iOS-side changes as unverified.
- Do not run `./gradlew clean` without asking first.

## Do not
- Do not suggest splitting into multiple Gradle modules unless explicitly asked.
- Do not put business logic in Composables or platform source sets.
- Do not introduce a second error-handling pattern alongside the shared `Result`/`AppError`
  approach, even "just for this one feature."
- Do not have ViewModel or UI code depend on Ktor/SQLDelight types directly.
