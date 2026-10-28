# Источник «AiLiberty» и общий модуль наличия

Балансер ailiberty.top (нейро-озвучка AniLiberty) и модуль `source/availability`, через который источники спрашивают сервер YummyTV
«есть ли тайтл». Оба необязательные (как `source/anime4k`): нет каталога — приложение собирается без них. `source/ailiberty`
подключается, только если есть и `source/availability` (см. `settings.gradle.kts`); в `app/build.gradle.kts` нужны оба `:data`.

## Идея
Клиент не ищет по сайтам. Сервер заранее обходит ailiberty.top и хранит индекс «релиз → страница» (подробности и расписание обхода:
`YummyTV_API/docs/ailiberty.md`). Клиент одним запросом спрашивает, на каких балансерах есть тайтл, и **сам** забирает плейлист со
страницы релиза в момент воспроизведения. Подписанные ссылки на потоки живут недолго, поэтому ни сервер, ни приложение их не хранят.

## `source/availability`
- `FindSourceAvailabilityUseCase(sourceId, lookup)` → `SourceAvailability?` (`key`, `url`, `episodesAvailable`, `episodesTotal`,
  `ongoing`, `qualities`). `null` — у балансера тайтла нет. Если сервер не смог опросить сам балансер (`failed` в ответе), это
  `IOException`, а не «нет тайтла»: источник не пропадёт из кэша выдачи.
- `AvailabilityApi`: подписанный `POST {baseUrl}/availability`, тело — названия (латинский оригинал первым), `malId`, год, тип, число серий.
- `AvailabilityRepositoryImpl`: **один запрос на тайтл для всех балансеров**. Параллельные вызовы по одному тайтлу ждут под `Mutex`
  и берут готовый ответ; кэш 5 минут по `malId` (или названию и году). Ответ с непустым `failed` и сетевой сбой не кэшируются.
- Новый балансер подключается так: свой `ExtraVideoRepository`, который зовёт `FindSourceAvailabilityUseCase("<sourceId>", lookup)`.
  На общую ручку уже переведён и AniLibria (см. [anilibria-source.md](anilibria-source.md)): он берёт `variants` — релизы озвучки и
  нового дубляжа с сериями. Amedia тоже берёт тайтл и серии из общей ручки (см. ниже). Остальные источники (anime4k, aniboom, anistar) пока ходят на свои `/title`.

## `source/ailiberty`
- `AiLibertyExtraVideoRepository` берёт наличие через общий модуль и строит видео `1..episodesAvailable` с
  `iframeUrl = source://ailiberty/{slug}/{episode}`, озвучкой и плеером «AiLiberty», отрицательным id (прогресс локальный).
  `requiresMatchingDubbing = false`: озвучка собственная.
- `AiLibertyExtraStreamRepository` превращает ссылку в потоки: `GET https://ailiberty.top/releases/{slug}`, в скрипте Playerjs находит
  `file: [...]`, серию выбирает по числу в названии («Эпизод 3»), без числа — по порядку; из строки
  `[480p]/video/….m3u8,[720p]…` собирает карту качеств по возрастанию (лучшее по умолчанию — последнее), относительные адреса
  дополняет хостом сайта. Каждое качество — отдельный плейлист, мастера нет. Заголовков плееру не нужно (проверено: Referer не требуется).
- Нет релиза или серии — `Unavailable` с локализованным текстом (en/ru/uk), сетевой сбой — `Failed`.

## Ограничения
- Хост сайта (`https://ailiberty.top`) — константа в `AiLibertyPlaylistUtils.kt`; адрес из ответа сервера (`url`) пока только для справки.
- Нужен задеплоенный `POST /api/availability` на сервере; без него источник просто не появится (запрос не кэшируется).
- Поиск идёт по названиям: у сайта нет внешних id, `malId` используется только как ключ кэша.

## Amedia (amd.online) через общий модуль
`source/amedia` больше не ходит на `POST /amedia/title`: `AmediaExtraVideoRepository` спрашивает `FindSourceAvailabilityUseCase("amedia", lookup)`, как AiLiberty и AniLibria, поэтому тайтл ищется одним запросом `/availability` на все балансеры. Модуль требует `source/availability` (в `settings.gradle.kts` подключается только вместе с ним).
- Серии лежат в единственном варианте ответа: `SourceEpisode.id` — `vodId` плеера `aser.pro/vod/{id}` (серия играет адресом `source://amedia/{vodId}`), `SourceEpisode.url` — Rutube-embed, который сайт вставил вместо своего плеера (штатный Rutube-экстрактор). Чужой хост страницы, нечисловой или неположительный `id`, Rutube-ссылки не с `https://rutube.ru/play/embed/`, серии без номера отбрасываются в `AmediaMapper`.
- Если сервер нашёл страницу, но не прочитал серии, источник приходит в `failed`: use case бросает `IOException`, пустой список не кэшируется и прежний кэш выдачи остаётся.
- Без MAL id источника по-прежнему нет. Удалены мёртвые `AmediaApi.findTitle`, `FindAmediaTitleUseCase`, `AmediaLookup`, DTO запроса и ответа `/amedia/title`. Поток (страница `aser.pro` + мастер-плейлист) не менялся.
