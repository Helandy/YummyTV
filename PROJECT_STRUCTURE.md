# YummyTV Project Structure

YummyTV — TV-only Android приложение. Единственный app-модуль — `:tvApp`. Фичи организованы вертикальными Clean Architecture срезами.

## Module Map

```text
:tvApp

:core:di
:core:navigation
:core:utils
:core:designsystem
:core:network
:core:database
:core:common

:feature:home:domain
:feature:home:data
:feature:home:presentation
:feature:home:ui-tv

:feature:details:domain
:feature:details:data
:feature:details:presentation
:feature:details:ui-tv

:feature:search:presentation
:feature:search:ui-tv

:feature:top100:presentation
:feature:top100:ui-tv

:feature:player:presentation
:feature:player:ui-tv

:feature:settings:presentation
:feature:settings:ui-tv

:feature:library:ui-tv
```

## Responsibilities

- `tvApp` — app shell, Android TV манифест, навигационный хост, wiring экранов.
- `core:di` — Koin-модули для всех фич; единственное место, где собирается граф зависимостей.
- `core:navigation` — sealed class `Screen`, `LocalNavStack`, `NavigationHost`, функции `push`/`pop`.
- `core:utils` — небольшие утилиты и расширения.
- `core:designsystem` — TV-тема, базовые MVI классы (`BaseViewModelNew`, `ScreenNavigator`), Composable-примитивы.
- `core:network`, `core:database`, `core:common` — инфраструктура и общие примитивы.
- `feature:*:domain` — модели, контракты репозиториев, use cases. Никаких UI/framework зависимостей.
- `feature:*:data` — реализации репозиториев, DTO, маперы.
- `feature:*:presentation` — ViewModel, UiState (XxxState.State), Event, Effect по MVI паттерну.
- `feature:*:ui-tv` — Compose-экраны для TV; роль NavigatorRegister: собирает ViewModel, слушает Effect, вызывает навигационные коллбэки.

## Clean Architecture Direction

Зависимости направлены внутрь:

```text
tvApp → feature:*:ui-tv → feature:*:presentation → feature:*:domain
core:di → feature:*:data → feature:*:domain
ui-tv → core:designsystem
feature:*:data → core:network / core:database / core:common
```

## MVI Feature Pattern

Каждая фича — 4 файла (образец в `/example/`):

```
XxxState.kt        — State / Event / Effect (presentation)
XxxViewModel.kt    — BaseViewModelNew<S,E,F> (presentation)
XxxTvScreen.kt     — Composable-экран + навигация (ui-tv)
```

Базовые классы `BaseViewModelNew`, `ScreenNavigator` живут в `core:designsystem`.

## Navigation

Навигация — простой стек (`mutableStateListOf`) через `CompositionLocal`:

```kotlin
// push
val nav = LocalNavStack.current
nav.push(Screen.SeriesDetails(id))

// pop
nav.pop()
```

Все экраны (`Screen`) — sealed class в `core:navigation`. Роутинг — `when (screen)` в `tvApp`.

## Verification

```bash
./gradlew :tvApp:assembleDebug
./gradlew :feature:home:data:testDebugUnitTest
./gradlew :feature:details:data:testDebugUnitTest
```
