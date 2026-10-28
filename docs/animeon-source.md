# Источник «AnimeON»

Балансер animeon.club (украинская озвучка и субтитры). Устроен как [AniDub](anidub-source.md):
модули
`source/animeon/{domain,data}` необязательные, подключаются только вместе с `source/availability`
(см.
`settings.gradle.kts`, в `app/build.gradle.kts` нужны `:source:availability:data` и
`:source:animeon:data`). Серверная часть и расписание обхода: `YummyTV_API/docs/animeon.md`.

## Как работает

- `AnimeOnExtraVideoRepository` берёт наличие через `FindSourceAvailabilityUseCase("animeon", …)`.
  `key` тайтла — id на сайте, вариант — озвучка в одном плеере: `key` =
  `{translationId}:{playerId}`, `title` = «озвучка (плеер)». В список попадают **только варианты
  плеера Ashdi** (по суффиксу ` (Ashdi)`); Moon и Fenix пропускаются, пока для них нет разбора.
  Видео получают `iframeUrl = source://animeon/{title}/{episode}/{translation}/{player}`,
  отрицательный id (прогресс локальный).
- `AnimeOnRepositoryImpl` разрешает ссылку при воспроизведении:
    1. `GET /api/player/{title}/episodes?take=1&skip={episode-1}&playerId=…&translationId=…` → id
       серии (номер проверяется: если страница началась с другой серии, серии нет).
    2. `GET /api/player/{episodeId}/episode` →
       `{"videoUrl":"https://ashdi.vip/vod/N?player=animeon.club"}`.
    3. Если это `https://fenixplay.xyz/embed/…?ep=N&edition=tv`, страницу разбирает общий модуль `source/fenix` (см. ниже),
       озвучка берётся из `selection.track` ответа сайта. Иначе страницу плеера разбирает общий модуль `source/ashdi` (он же у [Mikai](mikai-source.md)):
       принимается только
       `https://ashdi.vip/vod/…`; `GET` страницы → `new Playerjs({ file:'…/index.m3u8' })`; `GET`
       HLS-мастера → варианты
       `1080/720/480` (каждый со звуком) → карта качеств по возрастанию.
- Заголовки не нужны: мастер, плейлисты вариантов и сегменты проверены без `Referer`/`User-Agent`,
  ссылки не подписаны.
- Нет серии или `file:` — `Unavailable` с локализованным текстом (en/ru/uk), сетевой сбой —
  `Failed`.

## Ограничения

- **Гео-блок Ashdi**: из РФ и Беларуси страница `vod` отдаёт заглушку «Плеєр недоступний в цій
  країні», `file:` на ней нет, серия считается недоступной в регионе: пользователь видит отдельное
  сообщение «недоступно в вашем регионе, включите VPN или выберите другой источник»
  (`RegionBlocked`). Проверено вживую с польского IP; из РФ без VPN серии не играются.
- Moon (`moonanime.art/iframe/…`) не поддержан: с нашего IP страница отвечает 400.
- **Fenix** (`fenixplay.xyz`): гео-блока нет, из РФ играет без VPN. Страница `/embed/` содержит `window.FENIX_PLAYLIST` (все серии,
  у каждой озвучки со своим HLS-мастером) и `window.FENIX_MEDIA_ACCESS` (токен доступа к медиа-хосту, живёт сутки). Серия выбирается по
  `ep` и `edition`, озвучка — по названию из `selection.track` без учёта регистра, без совпадения играет оригинальная дорожка.
  К мастеру дописывается `?token=…` (без него 403); ссылки плейлистов и сегментов подписаны сами. Звук у Fenix отдельной дорожкой
  мастера, поэтому карты качеств нет: мастер играет адаптивно. Субтитры Fenix (`.vtt` из `subtitles`) приложением не подключены.
- Субтитры (`kind = sub`) приходят тем же списком вариантов; клиент различает их только по названию
  («Субтитри …»).
- Плеер определяется по суффиксу названия варианта, который собирает сервер
  (`AnimeOnAvailabilitySource`).
