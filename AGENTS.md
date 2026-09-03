# Project Instructions

- Stick to clean architecture.
- Do not write tests unless explicitly asked.

## Domain Structure

- Keep every public domain model, enum, sealed contract, repository contract, and use case in its
  own same-named Kotlin file.
- Split each feature's `.domain` package into `.domain.model`, `.domain.repository`, and
  `.domain.usecase`. Never leave files directly in `.domain`.
    - `.domain.model` — domain models, enums, sealed contracts, and their extension helpers
      (e.g. sorting).
    - `.domain.repository` — repository interfaces only (`XxxRepository`).
    - `.domain.usecase` — use cases only (`XxxUseCase`).
    - Do not create a sub-package that would stay empty.
- Add KDoc to every use case describing the domain operation it performs.

## Data Layer Structure

- Put all DTOs in the feature data module's `.dto` package.
- Put DTO↔domain and domain↔DTO mappers in the `.mapper` package; cache/entity↔domain mappers in
  `.storage.mapper`. Do not keep DTO→domain mapping inline in repositories.
- Serializable cache-envelope types and their `toDomain()` live in a `.mapper` (or `.dto`) package,
  not inside a repository.
- Repositories orchestrate (network + cache + storage) and call mappers; they do not define them.

## Compose UI Feature Structure

- Keep `XxxMobileScreen.kt` and `XxxTvScreen.kt` as public screen entrypoints:
  `@Composable fun XxxScreen(state, effect, onEvent)`.
- Screen files must contain the real top-level screen assembly and event wiring; they must not be
  empty pass-through wrappers to `XxxContent`.
- Do not create generic `XxxContent` as a duplicated screen layer. Use a `Content` suffix only for a
  real domain UI component, not as a default screen body.
- Put child composable UI pieces in the feature UI module's `.view` package.
- Prefer one significant composable component per file in `.view`; small local lambdas inside a
  component are fine.
- Put Handler classes next to the ViewModel in the feature module's `.handler` package.
- `XxxState.kt` must contain only the `State`/`Event`/`Effect` declarations (plus types nested
  strictly inside `State`, e.g. an enum that only makes sense as part of one screen's state). Any
  other top-level `data class`/`enum class`/`sealed interface` declared alongside them — anything
  referenced from a handler, the ViewModel, or UI — belongs in `.model` instead, one file per type,
  file name matching the type name.
- Put UI-only models in `.model`.
- Put Domain/Data model → UI model mapper functions in a feature-specific `.mapper` package
  (module-root-level, sibling to `.utils`/`.handler`/`.model`), one file per mapper, named
  `XxxMapper.kt`. Do not define mapper functions inside ViewModel files — ViewModels only call them.
- Put formatters and extension helpers in `.utils`.
- Keep reusable formatting and error extensions out of Screen and `.view` files; place them in a
  feature-specific `XxxUtils.kt` under `.utils`.
- Keep reusable parsers, normalizers, and stateless extension helpers out of use cases, handlers,
  and their companion objects; place them in a feature-specific `XxxUtils.kt` under `.utils`.
- Import extension receiver types normally; do not use fully qualified receiver types in function
  declarations.
- Keep stateless private entity mappers used by only one class in that class's companion object;
  move shared mappers to a dedicated mapper package.
- Keep user-facing strings in Android resources and read them with `stringResource`.
- Do not move business logic into UI components.
