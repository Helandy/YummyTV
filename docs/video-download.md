# Загрузки видео

Как работает офлайн-загрузка серии: от постановки в очередь до воспроизведения и экспорта в файл.
Код — `feature/video-download/` (только мобильный UI). Источники подробно:
[alloha-player.md](alloha-player.md)
§9, [cvh-player.md](cvh-player.md), [other-extractors.md](other-extractors.md).

## Схема

```
UI → EnqueueVideoDownloadUseCase → VideoDownloadRepository
       запись в хранилище (status = Queued) + OneTimeWork(VideoDownloadWorker)

VideoDownloadWorker (foreground, до 2 одновременно)
  стратегия = DownloadPlayerStrategyResolver.resolve(item)
  (при перезапуске) VideoDownloadStreamRefresher → новый iframe и поток
  VideoDownloadExecutor.download()
      live-сессия (только Alloha HLS) + таймер обновления
      upstream: OkHttp / DefaultHttpDataSource  →  RateLimitedDataSource (3 МБ/с)
      CacheDataSource на VideoDownloadCacheProvider (SimpleCache, NoOpCacheEvictor)
      HlsDownloader / DashDownloader / ProgressiveDownloader
  Downloaded → (по настройке) VideoExportWorker → mp4 в выбранную папку (SAF)

Воспроизведение: PlayerPlaybackConfig → CacheDataSource на том же кэше + downloadCacheKeyFactory
```

Статусы `VideoDownloadStatus`: `Idle`, `Resolving`, `Queued`, `Downloading`, `Paused`, `Downloaded`,
`Failed`, `Deleting`, `Deleted`.

## Модули

| Слой           | Содержимое                                                                            |
|----------------|---------------------------------------------------------------------------------------|
| `api`          | `IVideoDownloadNavigator`, `VideoDownloadPlaybackCache` (контракт для плеера)         |
| `domain`       | Модели, `VideoDownloadCacheKeyScheme`, use case'ы постановки, паузы, отмены, экспорта |
| `data`         | Воркеры, `VideoDownloadExecutor`, стратегии, кэш, уведомления, экспорт                |
| `presentation` | `VideoDownloadViewModel`, навигатор, резолвер диплинка `yummytv://downloads`          |

`VideoDownloadPlaybackCache` (`feature:video-download:api`) отдаёт `Cache` и фабрику ключей для
записи загрузки; `feature:player:ui-common` зависит от этого `api`-модуля.

## Воркер

`VideoDownloadWorker` — `@HiltWorker` `CoroutineWorker`.

- Уникальная работа по id записи. Ограничение `NetworkType.CONNECTED`: без сети воркер сжёг бы все
  ретраи на transient-ошибках и ушёл в `Failed`. Backoff линейный, 10 с
  (`DOWNLOAD_RETRY_BACKOFF_MS`).
- Параллельно качаются не более двух (`MAX_PARALLEL_DOWNLOADS = 2`, `Semaphore` на процесс). Слот
  ждут уже в foreground, иначе WorkManager остановил бы фоновую работу через 10 минут.
- Перед стартом поток перезапрашивается (`shouldRefreshBeforeStart`), если был прогресс, это не
  первая попытка, есть сообщение об ошибке или задан `KEY_FORCE_STREAM_REFRESH`.

### Лестница ретраев

| Ситуация                                            | Действие                                                                                |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------|
| 403 в первый раз                                    | Перезапрос `/videos`, новый iframe и поток, пауза 3 с, повтор (`retriedAfterForbidden`) |
| 403 снова                                           | `Queued` и `Result.retry()`, до `MAX_STREAM_REFRESH_WORK_RETRIES = 3`                   |
| Transient (таймаут сокета, 408/429/500/502/503/504) | Перезапрос потока и повтор до `MAX_TRANSIENT_DOWNLOAD_RETRIES = 3`, пауза 3 с           |
| Transient исчерпан                                  | `Queued` и `Result.retry()` до трёх попыток воркера                                     |
| Перезапрос потока не удался                         | То же: retry воркера, затем `Failed`                                                    |
| Остановка (`isStopped`)                             | `Result.failure()`                                                                      |
| Всё остальное                                       | `Failed` с описанием (`errorMessage`) и отчётом в аналитику                             |

После успеха: `Downloaded`, `progress = 1`, при включённой настройке — автоэкспорт.

### Перезапрос потока

`DefaultVideoDownloadStreamRefresher`:

1. `getSources(animeId, forceRefreshVideos = true)` и поиск того же эпизода по `episode`, `player`,
   `dubbing`; предпочтение тому же `videoId`, затем тому же `playerId`, затем первому кандидату.
2. Резолв потока (`ResolvePlayerStreamUseCase`) с прежним качеством, если оно числовое, иначе с
   меткой авто-качества.
3. Выбор качества: то же по метке, то же по числу, а для Alloha — самое высокое из доступных
   (`allowsQualityFallbackToHighest`).
4. Результат — `VideoDownloadRestartStream` с новым URL, меткой качества и заголовками. Для Alloha
   (`reusesHeadersOnRefresh`) к свежим заголовкам добавляются те «переиспользуемые» заголовки
   записи, которых в новом наборе нет (`withReusableFallbackHeaders`).

## Стратегии источников

`DownloadPlayerStrategy` собирает всё, что различается между источниками, чтобы воркер не сравнивал
имена плееров. Выбор — `DownloadPlayerStrategyResolver`: Alloha по iframe или имени плеера, CVH по
iframe, остальное `DefaultDownloadStrategy`.

| Параметр                                           | Default (Kodik, Aksor, VK, Rutube, Sibnet, Zedfilm) | CVH                               | Alloha                                                 |
|----------------------------------------------------|-----------------------------------------------------|-----------------------------------|--------------------------------------------------------|
| Живая сессия                                       | нет                                                 | нет                               | да, для HLS (`reusePlaybackSession = false`, TTL 55 с) |
| Ротация URL сегментов                              | нет                                                 | нет                               | да, для HLS                                            |
| Upstream                                           | `DefaultHttpDataSource`                             | как Default                       | OkHttp для адаптивных потоков                          |
| Заголовки                                          | `withDownloadRequestHeaders(iframe)`                | то же, `skipRefererOrigin = true` | то же, `Origin = https://alloha.yani.tv`               |
| Только числовые качества                           | нет                                                 | нет                               | да                                                     |
| Перезапуск использует заголовки сохранённой записи | нет                                                 | нет                               | да (`reusesHeadersOnRefresh`)                          |

Лимит скорости по умолчанию — 3 МБ/с на любую загрузку (`DEFAULT_DOWNLOAD_BYTES_PER_SECOND`).
`RateLimitedDataSource` держит общий бюджет на все параллельные сегменты, а не на каждое соединение
отдельно. Причина — CDN Alloha блокирует сессию (`403 session_blocked`), когда выкачивание обгоняет
реальное время просмотра; быстрый загрузчик упирается в этот порог за секунды.

### Alloha

`usesLiveSession(Hls) = true`. `VideoDownloadExecutor` открывает отдельную сессию
(`openLiveSession`), качает через её loopback-URL с пустыми заголовками (их подставляет прокси) и
держит таймер: за 20 с до `expiresAt` зовёт `session.refresh()`. Сессию закрывает в `finally`.

## Кэш и ключи

Один `SimpleCache` в `filesDir/video_download_cache` с `NoOpCacheEvictor` (вытеснения нет), общий
для загрузки, воспроизведения и экспорта.

### Схема ключей

Проблема: `HlsDownloader` и `DashDownloader` строят `DataSpec` из сырых URI и не применяют
`MediaItem.customCacheKey` (его учитывает только `ProgressiveDownloader`). Без своих ключей данные
HLS-загрузки нельзя привязать к записи, удалить точечно или найти при воспроизведении.

`VideoDownloadCacheKeyScheme` хранится в записи (`storageValue`) и фиксируется один раз при
постановке в очередь:

| Схема                    | Что значит                                                                                                        |
|--------------------------|-------------------------------------------------------------------------------------------------------------------|
| `Legacy` (0)             | Скачано до неймспейсинга. HLS/DASH-сегменты лежат под сырыми URL, атрибутировать их нельзя                        |
| `Namespaced` (1)         | Все ресурсы под префиксом `<cacheKey>\|dl-res\|` и идентичностью = полный URI (`DownloadCacheKeyFactory`)         |
| `NamespacedRotating` (2) | То же, но для медиафайлов идентичность — имя файла (стабильное расширение HLS): сегменты переживают смену подписи |

`downloadCacheKeyFactoryFor(scheme, cacheKey, manifestUri, legacyRotating)` — единственная точка,
где схема превращается в фабрику. Загрузка, экспорт и воспроизведение обязаны спрашивать её же.

Что для Legacy: если `legacyRotating` (Alloha HLS) — `RotatingHlsCacheKeyFactory` (префикс
`|alloha-segment|`), иначе фабрики нет и сегменты ищутся под сырыми URL.

Инварианты:

- Верхнеуровневый манифест кэшируется под самим `downloadCacheKey` (`manifestUri` — тот URI, которым
  его забирает загрузчик). Благодаря этому `manifestKeyToEvictOnRefresh` выселяет протухший
  плейлист, а воспроизведение находит его по `customCacheKey`.
- Вложенные плейлисты Alloha идут под `CacheKeyFactory.DEFAULT`: их URI ротируется вместе с
  подписью, и каждый рефреш перечитывает свежий список сегментов.
- Имя файла как идентичность допустимо только при ротации: в обычном multivariant-потоке сегменты
  разных дорожек могут называться одинаково и склеились бы в один ресурс.

Из KDoc:

- `VideoDownloadCacheKeyScheme`: схема фиксируется в момент постановки в очередь и персистится; и
  загрузка, и воспроизведение, и удаление обязаны читать один и тот же вариант;
  `DownloadPlayerStrategyResolver.cacheKeyScheme`: «пересчёт посреди загрузки осиротил бы уже
  скачанные сегменты»;
- `LegacyStreamingCachePruner.pruneOrphanedEntries()` работает только когда нет активных записей
  схемы `Legacy`: их HLS/DASH-сегменты лежат под сырыми URL и неотличимы от чужих, поэтому «попытка
  прибраться сносила данные всех остальных серий».

## Воспроизведение скачанного

Плеер получает офлайн-признаки в `PlayerDestination` (`downloadId`, `localFileUri`) и берёт ветку
«офлайн-загрузка» в `DefaultPlayerPlaybackConfig.dataSourceFactory()`:
`CacheDataSource` на кэше загрузок, фабрика ключей из `VideoDownloadPlaybackCache`,
`FLAG_BLOCK_ON_CACHE`. Сетевые ретраи и резолв source-graph в этом
режиме отключены, см. [player-architecture.md](player-architecture.md).

## Экспорт

`VideoExportWorker` собирает автономный mp4 из кэша в выбранную пользователем папку (SAF,
`DocumentsContract`).

- Только для записей в статусе `Downloaded` и при непустом `destinationUri`.
- Один экспорт одновременно (`exportMutex`), foreground-уведомление.
- Порядок: проверка записи в папку → проверка места → создание документа → перекодирование Media3
  `Transformer` во временный файл `cacheDir/<id>.mp4` → копирование в документ. Доступ и документ
  проверяются до перекодирования: иначе пользователь ждал бы минуты ради `SecurityException`.
- При ошибке созданный документ удаляется (`deleteDocument`).
- Состояния — `VideoExportStatus`, `VideoExportSource`, `VideoExportDestination`.
