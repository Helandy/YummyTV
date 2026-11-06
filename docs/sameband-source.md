# Источник «Sameband»

Балансер sameband.studio (DLE-сайт, дубляж аниме, прямой HLS 480/720/1080 без авторизации). Устроен как
[AniDub](anidub-source.md): модули `source/sameband/{domain,data}` необязательные, подключаются только вместе с
`source/availability` (см. `settings.gradle.kts`; в `app/build.gradle.kts` нужны `:source:availability:data` и
`:source:sameband:data`).

## Как работает

- `SamebandExtraVideoRepository` берёт наличие через `FindSourceAvailabilityUseCase("sameband", …)`. Сервер отдаёт URL
  страницы `https://sameband.studio/anime/{id}-{slug}.html`, а число серий может быть `null` (клиентский маппер превращает его в 0),
  поэтому серии строятся **из плейлиста сайта**, а не из `episodesAvailable`: `iframeUrl = source://sameband/{slug}/{episode}`,
  озвучка «Студийная Банда» (сливается с одноимённой дорожкой yani, значок `dub_studioband`), плеер «Sameband», отрицательный id (прогресс локальный). Любой сбой чтения плейлиста — балансера нет.
- `SamebandExtraStreamRepository` разрешает ссылку в момент воспроизведения, `SamebandRepositoryImpl`:
    1. `GET /anime/{slug}.html` → `<iframe src="/v/play/X.html">`;
    2. `GET /v/play/X.html` (Referer — страница) → Playerjs `file: "/v/list/X_list.txt"`;
    3. `GET /v/list/X_list.txt` (Referer — плеер) → JSON-массив: `title` (HTML с «Серия NN»), `file` вида
       `[480p]/v/…m3u8,[720p]…,[1080p]…` (пути относительные к хосту сайта).
  Номер серии — из `title` (запасной вариант — порядковый номер); элементы без `file` пропускаются. Плееру уходит карта качеств
  по возрастанию высоты (лучшее по умолчанию — последнее) и заголовки `Referer: https://sameband.studio/`, `User-Agent`
  из `BrowserUserAgentProvider`.
- Нет тайтла, плеера или серии — `Unavailable` с локализованным текстом (en/ru/uk), сетевой сбой — `Failed`.

## Ограничения

- **robots.txt сайта: `Disallow: /v/`**. Поэтому серверный краулер читает только `/anime/…`, а `/v/list` и HLS запрашивает клиент
  по действию пользователя.
- Хост сайта (`https://sameband.studio`) — константа в `SamebandPlaylistUtils.kt`.
- Мастер-плейлиста нет: «Авто» (адаптивное) не предлагается, только три фиксированных качества.
- Формат разметки сайта выведен из разведки; при смене вёрстки правятся регулярки в `SamebandPlaylistUtils.kt`.
