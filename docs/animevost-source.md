# Источник «AnimeVost»

Балансер animevost.org (озвучка студии AnimeVost). Модули `source/animevost/{domain,data}` необязательные, подключаются только
вместе с `source/availability` (см. `settings.gradle.kts`, в `app/build.gradle.kts` нужны `:source:availability:data` и
`:source:animevost:data`). Серверная часть и расписание обхода: `YummyTV_API/docs/animevost.md`.

## Как работает

- `AnimevostExtraVideoRepository` берёт наличие через `FindSourceAvailabilityUseCase("animevost", …)`: `key` = `{id}-{slug}`,
  `url` = `https://animevost.org/tip/{раздел}/{key}.html`. Видео `1..episodesAvailable` с
  `iframeUrl = source://animevost/{раздел}/{slug}/{episode}`, озвучкой и плеером «AnimeVost», отрицательным id (прогресс локальный).
- `AnimevostRepositoryImpl` разрешает ссылку при воспроизведении:
    1. `GET /tip/{раздел}/{slug}.html` → `var data = {"1 серия":"<id>",…}`; серия ищется по числу в начале метки.
    2. `GET /frame5.php?play=<id>&old=1` → ссылки `<a download="invoice" href="…mp4…">480p|720p`.
    3. Плееру уходит карта качеств по возрастанию (лучшее по умолчанию — 720p).
- Заголовки не нужны: страницы и mp4 проверены без `Referer` и с произвольным `User-Agent`. Ссылки подписаны (`md5`, `time`),
  поэтому читаются в момент воспроизведения.
- Нет страницы, серии или ссылок — `Unavailable` с локализованным текстом (en/ru/uk), сетевой сбой — `Failed`.

## Ограничения

- `/frame5.php` закрыт для краулеров в robots.txt: сервер YummyTV его не читает (только индекс страниц), плеер открывает
  клиент по действию пользователя.
- Серии сопоставляются по номеру в метке; метки без «N серия» (например «Анонс») пропускаются. Если нумерация на сайте сдвинута,
  серия покажется недоступной.
- Озвучка одна («AnimeVost»), варианты озвучек сервер не отдаёт.
- Хост `animevost.org` — константа в `AnimevostUtils.kt`; сайт ходит через зеркала, но ссылки и mp4 живут на CDN из ответа.
