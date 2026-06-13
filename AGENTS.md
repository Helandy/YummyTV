# Project Instructions

- Stick to clean architecture.
- Do not write tests unless explicitly asked.

## Compose UI Feature Structure

- Keep `XxxMobileScreen.kt` and `XxxTvScreen.kt` as public screen entrypoints:
  `@Composable fun XxxScreen(state, effect, onEvent)`.
- Screen files must contain the real top-level screen assembly and event wiring; they must not be empty pass-through wrappers to `XxxContent`.
- Do not create generic `XxxContent` as a duplicated screen layer. Use a `Content` suffix only for a real domain UI component, not as a default screen body.
- Put child composable UI pieces in the feature UI module's `.view` package.
- Prefer one significant composable component per file in `.view`; small local lambdas inside a component are fine.
- Put Handler classes next to the ViewModel in the feature module's `.handler` package.
- Put UI-only models in `.model`.
- Put UI mappers, formatters, and extension helpers in `.utils`.
- Keep user-facing strings in Android resources and read them with `stringResource`.
- Do not move business logic into UI components.
