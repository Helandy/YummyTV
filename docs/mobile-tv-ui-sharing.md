# Общий код мобильного и ТВ-интерфейса

В одном APK собраны оба интерфейса: модули `ui-mobile` и `ui-tv` каждой фичи компилируются в одно
приложение и используют одну тему, один `NavigationManager` и общие `presentation`-модули.
Структура модулей — [architecture.md](architecture.md), правила файлов — `AGENTS.md`. Описано только
то, что есть в коде.

## Что общее и что раздельное

| Общее                                                    | Раздельное                                                                 |
|----------------------------------------------------------|----------------------------------------------------------------------------|
| `presentation`: ViewModel, `State`, handler'ы, UI-модели | `ui-mobile` / `ui-tv`: экраны (`XxxMobileScreen` / `XxxTvScreen`) и `view` |
| Тема `YummyTvTheme` (Material3)                          | Регистраторы навигации (`IMobile*Entry`, `ITv*Entry`)                      |
| `NavigationManager`, `RootTab`, стеки                    | Графы `MobileMainGraph` / `TvMainGraph`                                    |
| `core:designsystem`: компоненты, locals                  | `core:designsystem/mobile/`, `core:designsystem/tv/`                       |

`TvNavigationHolder` и `MobileNavigationHolder` в `feature:main` собирают регистраторы своей
платформы; общий стек может содержать ключ, зарегистрированный только в другой платформе
(fallback в `AppNavHost` вызывает `back()`), см. [navigation.md](navigation.md).

## Пространства имён

| Модуль      | `namespace` (пример `reviews`)           | Пакет Kotlin-файлов (пример)                  |
|-------------|------------------------------------------|-----------------------------------------------|
| `ui-mobile` | `su.afk.yummy.tv.feature.reviews.mobile` | `su.afk.yummy.tv.feature.reviews.mobile.view` |
| `ui-tv`     | `su.afk.yummy.tv.feature.reviews.tv`     | `su.afk.yummy.tv.feature.reviews.view`        |

Пакеты Kotlin-файлов `ui-mobile` и `ui-tv` в `feature/*` не пересекаются. Парные компоненты
называются с префиксом платформы: `ReviewMobileCard` и `ReviewTvCard`.

## Строковые ресурсы

Ресурсы `res/values` всех модулей входят в один APK, имена строк общие для платформ. В нескольких
фичах `ui-mobile` и `ui-tv` содержат в `values/strings.xml` строки с одинаковыми именами:

| Фича       | Одноимённых строк | У скольких из них значения различаются |
|------------|------------------:|---------------------------------------:|
| `account`  |                80 |                                      2 |
| `settings` |               202 |                                      3 |
| `comments` |                35 |                                      1 |
| `bloggers` |                23 |                                      2 |
| `reviews`  |                14 |                                      2 |
| `posts`    |                12 |                                      2 |

В `ui-tv` у `posts` рядом с `posts_views` (`%1$d`) лежит `posts_views_short` (`%1$s`).

Строки читаются через `stringResource` (правило `AGENTS.md`).

## Тема

`YummyTvTheme(appTheme, backgroundStyle, isTelevision: Boolean? = null, content)`:

- одна Material3-тема (`MaterialTheme`), не `androidx.tv.material3`;
- `isTelevision` управляет типографикой; если не задан, берётся
  `uiMode and UI_MODE_TYPE_MASK == UI_MODE_TYPE_TELEVISION`;
- `appTheme` (по умолчанию `AppTheme.WARM_AMBER`; `AppTheme.DYNAMIC` при поддержке dynamic color) и
  `backgroundStyle` (`SYSTEM`, `LIGHT`, `DARK`).

`BaseScreen` используется в `ui-mobile` (45 файлов), в `ui-tv` вызовов нет.

## `core:designsystem`: общие компоненты

| Группа                   | Что                                                                                                                                         |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Состояния (мобилка)      | `mobile/state/MobileStateContent`, `MobileAppendError`                                                                                      |
| Состояния (ТВ)           | `tv/TvStateContent`, `TvStateMessage`, `TvAppendErrorFooter`, `TvLoadingFooter`, `TvLoadingScreen`                                          |
| Общие                    | `components/StateMessage`, `CachedAsyncImage`, `GlobalToastOverlay`, `OfflineBanner`, `RatingBadge`, `MarqueeTitleText`                     |
| Фокус ТВ                 | `focus/` ([tv-focus.md](tv-focus.md))                                                                                                       |
| Каркас мобильных экранов | `baseScreen/`: `BaseScreen`, `BaseBottomSheet`, `BaseBottomSheetCustom`, `ScreenNavigator` ([insets-and-sheets.md](insets-and-sheets.md))   |
| Мобильные блоки          | `mobile/`: `MobileSectionHeader`, `MobileMetaRow`, `MobileSwipeableTabsPager`, `bar/`, `cards/`                                             |
| Разрешения               | `permissions/LocalNetworkPermissions.kt`                                                                                                    |

## CompositionLocal

В `core:designsystem/locals/` объявлены `LocalIsOffline`, `LocalMainMenuFocusRequester`,
`LocalPosterCardSize`, `LocalPosterQuality`, `LocalPreferredContentFocusRequester`,
`LocalResolveKodikThumbnailUrl`.

| Local                                                                | Где задаётся                                                                              |
|----------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| `LocalPosterQuality`, `LocalPosterCardSize`, `LocalIsOffline`        | `MobileMainGraph` и `TvMainGraph` из состояния `MainState`                                |
| `LocalResolveKodikThumbnailUrl`                                      | `MobileMainGraph`: `resolveKodikThumbnailUrl::invoke` (`ResolveKodikThumbnailUrlUseCase`) |
| `LocalMainMenuFocusRequester`, `LocalPreferredContentFocusRequester` | `TvMainScaffold`                                                                          |
| `LocalMobileNavigationLayout` (`core/designsystem/mobile/bar`)       | `MobileMainScaffold`                                                                      |

`LocalPosterQuality` имеет значение по умолчанию `PosterQuality.STANDARD`.

## Стабильные коллекции в состоянии

`State` общий для обеих платформ; его коллекции — `kotlinx.collections.immutable`
([image-loading-and-memory.md](image-loading-and-memory.md)). Модели `domain`-модулей, которые нельзя
пометить `@Immutable` (чистый JVM без Compose), перечислены в `config/compose-stability.conf`,
который `configureComposeCompiler` добавляет в `stabilityConfigurationFiles` каждого Compose-модуля.
