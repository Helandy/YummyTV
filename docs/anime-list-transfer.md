# Импорт и экспорт списка аниме (Shikimori / MyAnimeList)

Перенос списка аниме между YummyTV, Shikimori и MyAnimeList работает целиком через локальные файлы
(SAF `OpenDocument` / `ACTION_CREATE_DOCUMENT` и `ACTION_SEND`), без OAuth и API-токенов сторонних
сервисов и без обращений к `/anime/{id}/videos`.

Точка входа — Настройки → Хранилище → «Импорт и экспорт списка» (`LibraryListTransferDestination`,
экран `ListTransferMobileScreen` / `ListTransferTvScreen`, `ListTransferViewModel`).

## Форматы

- **MAL / Shikimori XML** (`.xml`, при импорте также прозрачно распаковывается `.xml.gz` по
  магическим байтам `1f 8b`): `MalXmlTransferUtils` (`:feature:library:domain`).
  Экспорт выставляет `<user_export_type>1</user_export_type>` и `<update_on_import>1</update_on_import>`
  (без него импортёр MyAnimeList игнорирует записи), оборачивает название в CDATA и поддерживает при
  импорте как текстовые, так и числовые `<my_status>` (`1/2/3/4/6`) и `<shiki_status>`.
- **Shikimori JSON** (`*_animes.json`): `ShikimoriJsonTransferUtils` (`:feature:library:data`).
  Массив объектов `{target_title, target_id, target_type, score, status, rewatches, episodes, text}`.
  Записи с `target_type != "Anime"` (манга/ранобэ) отфильтровываются с отдельным счётчиком; при
  импорте поддерживается и вложенный объект `anime` из API `/api/v2/user_rates`.

Имя экспортируемого файла (`buildExportFileName`) включает UTC-штамп даты и времени
(`yyyyMMdd-HHmmss`), чтобы повторные выгрузки в `Downloads` на Android 10+ не порождали суффиксы
`file (1).xml`. Для «Поделиться» переиспользуется общий `FileProvider` приложения
(`${applicationId}.logs`, подкаталог `list_exports/` в `app/src/main/res/xml/log_file_paths.xml`).

Маппинг статусов (`ExternalListStatusUtils`):

| `UserAnimeList` | Shikimori JSON             | MAL XML              |
|-----------------|----------------------------|----------------------|
| `WATCHING (0)`  | `watching` (+`rewatching`) | `Watching` (`1`)     |
| `PLANNED (1)`   | `planned`                  | `Plan to Watch` (`6`)|
| `COMPLETED (2)` | `completed`                | `Completed` (`2`)    |
| `POSTPONED (5)` | `on_hold`                  | `On-Hold` (`3`)      |
| `DROPPED (3)`   | `dropped`                  | `Dropped` (`4`)      |

Избранные без списка в файл не попадают и учитываются в `skippedFavoriteOnlyCount`.

## Сетевой контракт `api.yani.tv`

`YaniListTransferApi` (`:feature:library:data`):

- **Экспорт (авторизован)**: один `GET /users/{userId}/lists` — ответ уже содержит
  `remote_ids.shikimori_id`, `remote_ids.myanimelist_id`, `user.list`, `user.rating` и `type.alias`.
  Гость: локальная `LibraryStorage` + пакетный `GET /anime?ids=...&limit=100` чанками по 50 ID.
  Экспорт берёт строго ID выбранного формата (`myanimelist_id` для XML, `shikimori_id` для JSON)
  без кросс-fallback; тайтлы без нужного ID учитываются в `skippedWithoutExternalIdCount`.
- **Экспорт просмотренных серий**: читается только локальная таблица `watch_progress`
  (`LocalWatchedCountUtils`, без дробных спецвыпусков). Для `PLANNED` всегда `0`, для `COMPLETED`
  без локальной истории — `1` у `movie` и `0` у сериалов (валидно и в MAL, и в Shikimori).
- **Импорт — сопоставление ID**: `GET /anime?shikimori_ids=...&limit=100` или
  `GET /anime?mal_ids=...&limit=100` чанками по 50 ID с `offset`-пагинацией, если страница вернула
  100 записей (`limit > 100` возвращает HTTP 400). Сопоставление выполняется строго по ID выбранного
  формата (`myanimelist_id` для MAL XML, `shikimori_id` для Shikimori JSON) без кросс-fallback, чтобы
  `shikimori_id` чужого тайтла не мог совпасть с запрошенным `mal_id` и наоборот. Если одному
  внешнему ID соответствует несколько `anime_id`, `selectPrimaryCandidate` выбирает уже имеющийся в
  библиотеке тайтл, затем обычную версию (не режиссёрскую), затем минимальный `anime_id`.
- **Импорт — применение дельты** (`ImportAnimeListUseCase`): текущее состояние сверяется с файлом
  (`SKIP_EXISTING` по умолчанию или `OVERWRITE`), мутации `setAnimeList` и `setRating` выполняются
  только для реально изменившихся полей последовательно с паузой и повторами по `Retry-After` /
  экспоненциальному backoff на `429` и транзиентных ошибках. Локальные флаги `isFavorite` и даты не
  затираются; повторный запуск после отмены или сбоя идемпотентен.
- **Оценка `0` при импорте**: и в MAL XML (`<my_score>0</my_score>`), и в Shikimori JSON
  (`"score": 0`) ноль означает «оценка не выставлена» и неотличим от снятой оценки, поэтому
  сознательно нормализуется в `null` (`normalizedExternalScore`). Снять уже стоящую на YummyAnime
  оценку через импорт нельзя — переносятся и обновляются только значения `1..10`, а неоценённые
  записи файла не затирают существующие оценки даже в режиме `OVERWRITE`.
- **Обработка сетевых сбоев и повтор**: если при импорте часть чанков `resolveByExternalIds` или
  отдельные мутации падают после повторов, успешно сопоставленные записи всё равно применяются
  (частичный результат сохраняется), ID из упавших чанков не попадают в «ненайденные», а в сводке
  отдельно показывается число сбоев и кнопка «Повторить неудавшиеся» только для этих записей. При
  полном отказе сопоставления или сбое `loadExportableLibrary` выбрасывается
  `AnimeListTransferStageException` с указанием конкретного этапа (и прогресса по пачкам для гостя),
  а баннер ошибки даёт повторить операцию в одно нажатие.
- **Локальные серии при импорте**: опция помечает серии `1..N` в `watch_progress` с `videoId = 0` и
  пустым `episodeUrl` только для незавершённых списков (`WATCHING` / `POSTPONED` / `DROPPED`), не
  перезаписывая существующие строки. `allMeaningfulVideoProgress` фильтрует `videoId > 0`, поэтому
  синтетические отметки не отправляются в `POST /video` и не попадают в «Продолжить просмотр».

## Жизненный цикл долгого импорта

Импорт выполняется в `viewModelScope` `ListTransferViewModel` (на крупном списке ~1000 тайтлов с
троттлингом 350 мс прогон занимает несколько минут):

- **Сон экрана (телефон и ТВ)**: `KeepTransferScreenOnEffect` удерживает `View.keepScreenOn = true`
  на всё время активного импорта и экспорта и возвращает исходный флаг по завершении или уходе с
  экрана, поэтому ТВ не уходит в заставку/сон посреди синхронизации.
- **Уход с экрана**: системная кнопка/жест «Назад» (через `BackHandler`), пульт ТВ и стрелка в
  верхней панели во время импорта не закрывают экран сразу, а открывают диалог подтверждения
  («Продолжить» / «Прервать и выйти»). При подтверждении корутина импорта отменяется, а уже
  отправленные записи остаются в библиотеке.
- **Поворот экрана и смена конфигурации**: `ListTransferViewModel` сохраняется в `ViewModelStore`
  навигационной записи, поэтому `viewModelScope`, разобранный файл и прогресс импорта переживают
  поворот без перезапуска.
- **Сворачивание приложения / выгрузка процесса**: пока ОС держит процесс живым, корутина продолжает
  работу; если система выгрузит процесс в фоне, уже применённые тайтлы остаются сохранены, а
  повторный запуск импорта (в `SKIP_EXISTING` или `OVERWRITE`) дошлёт только оставшуюся дельту.

## Особенности ТВ

На прошивках Android TV без системного `DocumentsUI` отказ `OpenDocument` показывает понятную
ошибку, а отказ `ACTION_CREATE_DOCUMENT` автоматически сохраняет файл экспорта в `Downloads` (как
выгрузка логов) и выводит путь в итоговой карточке.
