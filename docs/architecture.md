# Архитектура и модули

Карта проекта: из каких модулей он состоит, кто на кого может ссылаться и как экран попадает в
навигацию. Это точка входа в остальные доки. Правила структуры файлов внутри модулей (пакеты
`.domain.model`, `.view`, `.handler` и т. п.) лежат в `AGENTS.md` и здесь не повторяются.

## Одно приложение, два интерфейса

В одном APK собраны оба интерфейса: телефонный и ТВ. KDoc `InterfaceRouterActivity`: при первом
запуске предлагается выбрать интерфейс, после выбора исходный Intent всегда направляется в
`MobileActivity` или `TvActivity`, «не полагаясь на тип устройства из Configuration»; выбор хранит
`AppInterfaceModePreferences`.

```
InterfaceRouterActivity (трамплин, диалог выбора при первом запуске)
  ├─ MobileActivity → граф телефона (MobileNavigationHolder, feature/main/ui-mobile)
  └─ TvActivity     → граф ТВ      (TvNavigationHolder,     feature/main/ui-tv)
```

Следствия:

- Модули `ui-mobile` и `ui-tv` каждой фичи компилируются в один APK; детали — в
  [mobile-tv-ui-sharing.md](mobile-tv-ui-sharing.md).
- ViewModel, `State` и handler'ы лежат в `presentation` и используются обоими интерфейсами.
- DI — Hilt (`yummytv.android.hilt`, KSP).

## Слои фичи

Фича `feature:<name>` состоит из модулей по слоям:

| Модуль         | Тип                       | Что внутри                                                                     | Может зависеть от                               |
|----------------|---------------------------|--------------------------------------------------------------------------------|-------------------------------------------------|
| `api`          | Android library           | `NavKey`-destination'ы, контракты `I*Navigator`, `ITv*Entry` / `IMobile*Entry` | `core:navigation`                               |
| `domain`       | чистый JVM (`kotlin.jvm`) | `.model`, `.repository` (интерфейсы), `.usecase`                               | `core:model`, coroutines, `javax.inject`        |
| `data`         | Android library           | Ktor/Room/DataStore, DTO, мапперы, реализации репозиториев                     | `core:*`, свой `domain`                         |
| `presentation` | Android library + Compose | ViewModel, `State/Event/Effect`, `handler`, UI-модели, мапперы                 | свой `domain`, `core:mvi`, чужие `api`/`domain` |
| `ui-tv`        | Android library + Compose | ТВ-экраны, `view`, регистратор навигации                                       | `presentation`, `core:designsystem`             |
| `ui-mobile`    | Android library + Compose | Экраны телефона, то же самое                                                   | `presentation`, `core:designsystem`             |
| `ui-common`    | Android library + Compose | Общее для `ui-tv` и `ui-mobile` (есть у `player`)                              | `presentation`                                  |

Не у каждой фичи есть все модули. У `faq`, `pages`, `video-download`, `watch-later`, `messages`
только мобильный UI, у `watching` только `domain`, у `search` есть `android` (поисковый провайдер).

Что видно из `build.gradle.kts` модулей:

- `domain` — `kotlin.jvm` без Android; зависит от `core:model`, coroutines и `javax.inject`.
- Ни один `data` не зависит от `presentation` или UI, ни одна `presentation` не зависит от `data`.
- Зависимости между фичами: на `api` (116 ссылок) и на `domain` (45 ссылок). Ссылки на чужие
  `ui-mobile` / `ui-tv` есть только у `feature:main:ui-mobile` и `feature:main:ui-tv` (сборка графов).
  Пример: `details:presentation` зависит от `feature:account:domain`, `library:domain`,
  `player:domain` и от `api` других фич.
- `ui-tv` и `ui-mobile` одной фичи друг от друга не зависят.

### Что лежит в `core:model`

Общие модели предметной области (аниме, сезоны, настройки, `BrowserUserAgentProfile`,
`PlayerBufferProfile` и т. п.). Это чистый JVM-модуль (`kotlin.jvm` + serialization): shared kernel,
на который ссылаются `domain`-модули фич (например, `feature:details:domain`: `api(project(":core:model"))`).

## Core-модули

| Модуль                   | Тип               | Назначение                                                                    |
|--------------------------|-------------------|-------------------------------------------------------------------------------|
| `core:model`             | JVM               | Shared kernel: общие модели и enum'ы                                          |
| `core:common`            | JVM               | Чистые утилиты без Android (`anime`, `episode`, `coroutines`, `player`)       |
| `core:utils`             | Android           | Утилиты: `BrowserUserAgentProvider`, превью Kodik, файловые логи (`logging/`), `CastSupport`, `lazyKey`, пагинация |
| `core:network`           | Android           | HTTP-клиент yani (`buildYaniHttpClient`), `YaniApiJson`, `YANI_BASE_URL`      |
| `core:preferences`       | Android           | DataStore и защищённые настройки (токен, режим интерфейса, UA)                |
| `core:storage`           | Android           | Локальное хранилище (прогресс просмотра, «Позже», кэши)                       |
| `core:featuretoggle`     | Android           | Feature toggles (Varioqub), `FeatureToggleVersionSupportChecker`              |
| `core:analytics`         | Android           | `AnalyticsTracker`, `PersistedLogTags`, AppMetrica                            |
| `core:error` / `:api`    | Android           | Единая модель ошибок и её контракт                                            |
| `core:mvi`               | Android           | База ViewModel и контракты `State/Event/Effect`, без Compose                  |
| `core:designsystem`      | Android + Compose | Тема, компоненты, `CompositionLocal`'ы, фокус ТВ (`focus/`)                   |
| `core:navigation`        | Android + Compose | `NavigationManager`, `NavRegistrar`, `RootTab`, scene-стратегии, `AppNavHost` |
| `core:deeplink` / `:api` | Android           | Разбор и маршрутизация диплинков                                              |
| `core:tv`                | Android           | Интеграции Android TV: Watch Next, preview-каналы, воркеры обновления ленты   |
| `core:testing`           | JVM               | `BaseUnitTest` и утилиты тестов                                               |

Правило слоёв проверяет конвенционный плагин: модуль `:core:*` не может иметь зависимость на
`:feature:*`, сборка падает с «Нарушение слоёв». Временно понизить до предупреждения можно
`-PstrictLayering=false`. Если core нужен контракт из фичи, порт объявляется в `core`, а реализация
живёт в фиче (как `NavRegistrar`).

`core:mvi` — Android library без Compose; зависимости: `core:error:api`, `lifecycle-viewmodel`, `coroutines-android`.

## Конвенционные плагины

`build-logic` (`ConventionPlugins.kt`):

| Плагин id                                                                       | Для чего                                                             |
|---------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `yummytv.android.library`                                                       | Android library: compileSdk/minSdk из каталога, Java 21, desugaring  |
| `yummytv.android.library.compose`                                               | + Compose compiler, BOM, `kotlinx.collections.immutable` (как `api`) |
| `yummytv.android.application`                                                   | Приложение: Compose, BOM, Java 21                                    |
| `yummytv.android.hilt`                                                          | Hilt + KSP                                                           |
| `yummytv.android.application.baselineprofile` / `yummytv.baselineprofile.tasks` | Baseline profile, см. [baseline-profile.md](baseline-profile.md)     |

Версии Compose задаёт только BOM. Отчёты стабильности Compose включаются
`-PenableComposeCompilerReports=true`.

## Навигация в двух словах

Схема:

1. Экран объявляется как `@Serializable data object/class XxxDestination : NavKey` в `api` фичи.
2. Регистратор в `ui-tv` / `ui-mobile` реализует `ITvXxxEntry` / `IMobileXxxEntry`
   (наследники `NavRegistrar`) и вызывает `entry<XxxDestination> { … }`, внутри `hiltViewModel()` и
   экран.
3. Hilt-модуль биндит регистратор (`@Binds ... : ITvXxxEntry`).
4. `TvNavigationHolder` / `MobileNavigationHolder` в `feature:main` явно собирают регистраторы всех
   фич.
5. Между фичами переходят через `I*Navigator` из `api` и `INavigationManager`, а не по ссылкам на
   чужие экраны.

## Куда смотреть дальше

| Тема                                  | Документ                                                                                                                                                                 |
|---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Плеер Alloha, CVH, буфер              | [alloha-player.md](alloha-player.md), [cvh-player.md](cvh-player.md), [player-buffering.md](player-buffering.md)                                                         |
| Фокус и скролл на ТВ                  | [tv-focus.md](tv-focus.md)                                                                                                                                               |
| API yani                              | [yani-api.md](yani-api.md)                                                                                                                                               |
| Вход, подписки, восстановление сессии | [local-auth-session-transfer.md](local-auth-session-transfer.md), [subscriptions.md](subscriptions.md), [block-store-session-restore.md](block-store-session-restore.md) |
| Релизы и бета                         | [beta-updates.md](beta-updates.md)                                                                                                                                       |
| Unit-тесты                            | [unit.md](unit.md)                                                                                                                                                       |
