# Диплинки

Сводная таблица всех внешних ссылок и intent'ов, которые открывают экраны приложения. Как устроена
навигация в целом — в [navigation.md](navigation.md).

## Как обрабатывается ссылка

```
внешний Intent
  → InterfaceRouterActivity            единственная exported-точка входа, пробрасывает Intent
  → MobileActivity / TvActivity        onCreate и onNewIntent → handleIncomingIntent
      → SystemSearchIntentHandler      ACTION_SEARCH
      → DeepLinkHandlerImpl            resolvers.firstNotNullOfOrNull { it.resolve(link) }
          → navManager.navigate(key)
```

- Разделения на мобилку и ТВ нет: один APK, один набор резолверов. Платформы различаются только
  регистрацией финального экрана в `NavRegistrar` модулей `ui-mobile` / `ui-tv`.
- Если числовой id отсутствует или не число, резолвер возвращает `null` и ничего не происходит.
- `intent-filter` лежат в `app/src/main/AndroidManifest.xml` (строки 123-217), без `pathPattern`.

## Схема `yummytv://`

| URI                            | Параметры                  | Резолвер                          | Что открывает                                                              |
|--------------------------------|----------------------------|-----------------------------------|----------------------------------------------------------------------------|
| `yummytv://home`               | нет                        | `RootTabDeepLinkResolver`         | вкладка «Главная»                                                          |
| `yummytv://details/{animeId}`  | `animeId` — Int            | `DetailsDeepLinkResolver`         | детали тайтла                                                              |
| `yummytv://episodes/{animeId}` | `animeId` — Int            | `EpisodesDeepLinkResolver`        | экран выбора серии тайтла                                                  |
| `yummytv://collection/{id}`    | `id` — Int                 | `CollectionDeepLinkResolver`      | подборка                                                                   |
| `yummytv://downloads`          | нет                        | `VideoDownloadDeepLinkResolver`   | экран загрузок                                                             |
| `yummytv://continue/{animeId}` | `animeId` — Int            | `ContinueWatchingDeepLinkResolver`| «Продолжить»: плеер на записи просмотра, без записи или цели — детали      |

Параметр всегда берётся из последнего сегмента пути.

## Ссылки сайта

Без `autoVerify` (нет `assetlinks.json`), поэтому система показывает выбор приложения.
Хосты: `yummyani.me`, `ru.yummyani.me`, `www.yummyani.me`, `yani.tv`, `www.yani.tv`.

| URL                               | Параметры       | Резолвер                       | Что открывает                          |
|-----------------------------------|-----------------|--------------------------------|----------------------------------------|
| `https://{host}/catalog/item/{slug}`       | `slug` — 3-й сегмент | `AnimeSiteLinkDeepLinkResolver` | детали тайтла (`DetailsSiteLinkViewModel` резолвит slug) |
| `https://{host}/catalog/collection/{id}`   | `id` — Int           | `CollectionDeepLinkResolver`    | подборка                               |

`toAnimeSiteSlug()` принимает и `http`. Ветка подборки в `CollectionDeepLinkResolver` проверяет
только схему `https` и префикс пути, а список хостов ограничивает манифест.

## Прочие входы

| Вход                                              | Резолвер / обработчик         | Что открывает                                    |
|---------------------------------------------------|-------------------------------|--------------------------------------------------|
| `ACTION_VIEW` + `video/*`, схемы `content://` и `file://` | `LocalVideoDeepLinkResolver`  | плеер с локальным файлом (имя из `DISPLAY_NAME` или последнего сегмента) |
| `ACTION_SEARCH` (`SearchManager.QUERY`)           | `SystemSearchIntentHandler`   | каталог с введённым запросом                     |
| Подсказки поиска (`SystemSearchProvider`)         | `DetailsDeepLinkResolver`     | `yummytv://details/{id}`; от 2 символов, до 10 штук, таймаут 2,5 с |

## Кто порождает ссылки

| Источник                                   | Ссылка                                                                 | Платформа |
|--------------------------------------------|------------------------------------------------------------------------|-----------|
| Лаунчер (алиасы Default, Halloween, NewYear) | `MAIN` + `LAUNCHER` / `LEANBACK_LAUNCHER` → `InterfaceRouterActivity` | обе       |
| ТВ-ряд «Смотреть далее» (`WatchNextManager`) | `yummytv://details/{animeId}`                                        | ТВ        |
| Каналы превью (`PreviewChannelManager`)    | программы — `yummytv://details/{animeId}`, app-link канала — `yummytv://home` | ТВ  |
| Ярлыки (`AppShortcutsPublisher`)           | `yummytv://continue/{id}`, `yummytv://downloads`, `ACTION_SEARCH` с пустым запросом | мобилка |
| Виджет «Продолжить» (`ContinueWatchingWidget`) | `yummytv://continue/{id}` на карточке; шапка — просто открытие приложения | мобилка |
| Push новой серии (`NewEpisodePushNotificationService`) | тап и «Смотреть» — `yummytv://continue/{id}`, «Детали» — `yummytv://details/{id}`, без id — `yummytv://home` | обе |
| Уведомления загрузок и экспорта            | тап — `yummytv://downloads`; «Пауза» и «Продолжить» — broadcast, не диплинк | обе       |

## Не диплинк

`yummytv://pair?d=<deviceId>&c=<code>` — только QR-payload привязки ТВ (`LocalAuthPairingPayloadUtils`).
`intent-filter` на него нет, приложение его по ссылке не открывает.
См. [local-auth-session-transfer.md](local-auth-session-transfer.md).

## Как добавить диплинк

1. Резолвер `XxxDeepLinkResolver` в `feature/<name>/presentation/.../deeplink/`, регистрация
   `@Binds @IntoSet` в DI-модуле фичи. Он возвращает `NavKey` или `null`; порядок обхода не
   определён, поэтому резолверы не должны пересекаться.
2. `intent-filter` на `InterfaceRouterActivity` в манифесте.
3. Строка в таблицу выше.
