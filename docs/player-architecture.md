# Плеер: архитектура

Общая картина плеера: кто что делает от нажатия «Смотреть» до кадра на экране. Источники подробно
описаны отдельно: [other-extractors.md](other-extractors.md), [alloha-player.md](alloha-player.md),
[cvh-player.md](cvh-player.md); буфер и остановки — [player-buffering.md](player-buffering.md).

## Слои

```
feature/player/
  api              PlayerDestination (NavKey, FullscreenDestination), PLAYER_CONTENT_KEY
  domain           PlayerStreamResolveResult, репозитории, use case'ы
  data             экстракторы (extractor/*), DefaultPlayerStreamRepository
  presentation     PlayerViewModel, PlayerState, handler/, behavior/, delegate/
  ui-common        сервис медиа-сессии, Compose-эффекты, общие для ТВ и мобилки
  ui-tv            PlayerTvScreen, ТВ-контролы
  ui-mobile        PlayerMobileScreen, PiP, Cast-кнопка
```

Принцип: ExoPlayer живёт не в Activity и не в ViewModel, а в **сервисе**
(`PlayerMediaSessionService`). UI подключается к нему через `MediaController`. ViewModel про
ExoPlayer ничего не знает: она ведёт состояние (`PlayerState`) и решает, какой поток играть, а
Compose-эффекты в `ui-common` переносят это состояние в плеер и события плеера обратно в состояние.

## Поток данных

```
PlayerDestination ──► PlayerViewModel ──► PlayerState (url, headers, качества, позиция, …)
                          ▲  │
         PlayerState.Event│  │ state
                          │  ▼
          Compose-эффекты (ui-common)      PlayerMediaItemEffect ─► MediaItem + PlaybackConfig
          PlayerListenerEffect                                         │
          PlayerLifecycleEffect                                        ▼
                          ▲                              MediaController ──IPC── PlayerMediaSessionService
                          └────── Player.Listener ◄──────────────────────────── ExoPlayer / CastPlayer
```

1. Экран открывается с `PlayerDestination` (серия, `iframeUrl`, озвучка, позиция, офлайн-признаки).
   `PlayerDestinationStateMapper` собирает начальное `State`.
2. `PlayerViewModel` грузит граф источников (`PlayerSourceStreamHandler`, `PlayerSourceGraphMapper`)
   и резолвит поток через `PlayerStreamHandler` → use case → `DefaultPlayerStreamRepository`.
3. Готовый поток (`streamUrl`, `streamHeaders`, `streamQualityMap`) попадает в `State`.
4. `PlayerMediaItemEffect` строит `PlayerMediaItemConfig` и через `PlayerMediaItemUpdater` отдаёт
   ExoPlayer'у media item.
5. События плеера (`STATE_READY`, `STATE_ENDED`, ошибки, позиция) возвращаются в ViewModel как
   `PlayerState.Event`.

## PlayerViewModel и делегаты

`PlayerViewModel` (~830 строк) — оркестратор. Он держит jobs (`sourceGraphJob`, `extractionJob`,
`finalEpisodeActionJob`) и не содержит логики источников: она разнесена.

| Часть                                  | Назначение                                                                                                                                            |
|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PlayerStateHost` / `PlayerSourceHost` | Узкий доступ делегатов к состоянию: `state`, `scope`, `update`, плюс `loadStream`, `cancelStreamLoad`, `closeSourceSessions`, `invalidateStreamCache` |
| `PlayerSourceBehavior`                 | Поведение, зависящее от балансера: ошибки, ретраи, восстановление                                                                                     |
| `handler/*`                            | Классы с `@Inject`-конструктором: поток, прогресс, выбор источника, настройки, Alloha                                                                |
| `delegate/*`                           | Крупные сценарии: навигация, офлайн-источники, привязка настроек                                                                                      |
| `mapper/*`                             | `PlayerDestinationStateMapper`, `PlayerPlaybackUiStateMapper`, `PlayerSourceGraphMapper`                                                              |

### Поведения источников

`PlayerSourceBehavior` описывает, как переживать ошибки и что делать с результатом резолва.
Реализации:
`AllohaSourceBehavior` и `DefaultSourceBehavior` (Kodik, CVH и остальные).

- Балансер может смениться внутри одного экрана, поэтому ViewModel держит все поведения сразу.
- Решения (ошибка, ретрай, успешный старт) принимает первое, чей `handles(state)` подошёл. Порядок в
  списке важен: `[allohaSource, defaultSource]`.
- Хуки жизненного цикла (`reset`, `close`, `onPlaybackReady`, `onPlaybackPositionChanged`) получают
  все поведения: состояние старого источника должно закрыться и после смены балансера.

Ключевые методы: `onPlaybackError` (true — своё восстановление запущено), `onPlaybackStalled`,
`onRetryRequested`, `onStreamResolved`, `retriesFailedResolve`, `keepsStreamWhileResolving`,
`recoveryResumePositionMs`. `DefaultSourceBehavior` ещё ведёт переезд на резервный узел CDN
(`hostFailovers`), `AllohaSourceBehavior` — свежие сессии и подсказку смены плеера.

### Handler'ы

| Handler                              | За что отвечает                                                                                                                                                                      |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PlayerSourceStreamHandler`          | Граф источников и инструкция «как получить поток», не мутирует состояние                                                                                                             |
| `PlayerStreamHandler`                | Резолв активного iframe в поток и тексты ошибок                                                                                                                                      |
| `PlayerSourceSelectionHandler`       | Чистые функции над `State`: `previousEpisode`, `nextEpisode`, `nextEpisodeInOtherDubbing`, `selectDubbing`, `selectBalancer`, `resizeSettingsScope`                                  |
| `PlayerPlaybackProgressHandler`      | Запросы на сохранение прогресса и аналитику завершения                                                                                                                               |
| `PlayerProgressHandler`              | `@ViewModelScoped`: локальный прогресс и тихая отправка на сервер (`PUT /video`, дельта секунд, см. [yani-api.md](yani-api.md))                                                      |
| `PlayerPlaybackRetryHandler`         | Бюджет тихих повторов не-Alloha (5 попыток), сброс по `STATE_READY`                                                                                                                  |
| `PlayerAllohaRecoveryHandler`        | Попытки восстановления Alloha (`MAX_ATTEMPTS = 4`)                                                                                                                                   |
| `PlayerAllohaSessionHandler`         | Расписание обновления сессии Alloha                                                                                                                                                  |
| `PlayerAllohaTrackPreferenceHandler` | Запоминает и восстанавливает выбор аудиодорожки и субтитров Alloha по озвучке тайтла (`id` и `url` генерируются заново при каждом разборе bnsi, поэтому сравнивается по содержимому) |
| `PlayerFinalEpisodeActionHandler`    | `resolve(animeId): PlayerFinalEpisodeAction`: что показать после последней серии                                                                                                      |
| `PlayerArtworkHandler`               | `resolve(PlayerArtworkSource): String?`: URL обложки для медиа-сессии (`State.artworkUrl`)                                                                                           |
| `PlayerSettingsHandler`              | Потоки настроек плеера (автоскип, автоплей, задержки) и их сохранение                                                                                                                |
| `PlayerDisplaySettingsHandler`       | Размер/зум (ТВ) и transform (мобилка), хранятся на пару тайтл/плеер, с debounce                                                                                                      |

### Делегаты

- `PlayerNavigationDelegate` — уход с экрана. Навигация не ждёт сеть, прогресс сохраняется в
  `ioScope` (переживает `viewModelScope`), дочерние экраны открываются поверх деталей. Флаг «идёт
  переход на экран поверх плеера» не даёт уйти в детали при уходе в фон.
- `PlayerOfflineSourceLoader` — скачанная серия и локальный файл, открытый извне. Идут
  офлайн-маршрутом (`isOfflinePlayback`): без сетевых ретраев и резолва source-graph, граф из одного
  эпизода.
- `PlayerPreferencesBinder` — держит глобальные настройки плеера и одноразовые обучения в состоянии
  экрана (через `PlayerSettingsHandler`).

## Сервис медиа-сессии

`PlayerMediaSessionService` (`MediaSessionService` из Media3) создаёт ExoPlayer один раз на сервис.
Состав в `onCreate`:

1. `PlayerExoPlayerFactory.create(context)`: `DefaultTrackSelector`, `DefaultRenderersFactory` с
   `enableDecoderFallback`, `DefaultMediaSourceFactory` с `PlayerLoadErrorHandlingPolicy`,
   `PlayerLoadControlFactory` из профиля буфера, аудио-фокус (`USAGE_MEDIA`, `CONTENT_TYPE_MOVIE`),
   `handleAudioBecomingNoisy`.
2. Слушатели: `PlayerVolumeStabilization` (стабилизация громкости), `PlayerAllohaAudioOverride`
   (первая аудиогруппа и H.264 для Alloha), аналитика декодера и буферизации.
3. `PlayerCastPlayerFactory.createOrNull` оборачивает ExoPlayer в `CastPlayer`, если Cast
   поддерживается. `MediaSession` строится на `castPlayer ?: exoPlayer`.
4. `PlayerSessionCallback` принимает кастомную команду `STOP_SERVICE`.

### Конфигурация потока

`PlayerPlaybackConfig` (`DefaultPlayerPlaybackConfig`, синглтон) — «мост» между UI и сервисом. UI
пишет в него заголовки, офлайн-ключи, политику дорожек и флаг тихого переподключения
(`updateStream`), а `DataSource.Factory`, который читает ExoPlayer, берёт их оттуда при создании
источника. Три ветки `dataSourceFactory()`:

| Условие         | Источник данных                                                                                 |
|-----------------|-------------------------------------------------------------------------------------------------|
| `isLocalFile`   | `DefaultDataSource` (content/file/asset), без кэша                                              |
| офлайн-загрузка | `CacheDataSource` на кэше загрузок с ключами `downloadCacheKeyFactory`, `FLAG_BLOCK_ON_CACHE`   |
| онлайн          | `CacheDataSource` на `SimpleCache` стриминга, `PlayerStreamingCacheKeyFactory`, upstream OkHttp |

Поэтому `PlayerMediaItemUpdater.update` сначала зовёт `playbackConfig.updateStream`, а уже потом
`setMediaItem`: заголовки должны быть на месте до первого запроса.

### Media item

`PlayerMediaItemUpdater` различает три случая по двум ключам:

- URL или `playbackKey` изменились → `setMediaItem` + `prepare` (с позицией `resume`);
- изменился только `mediaItemKey` (метаданные: название, обложка, серия) → `replaceMediaItem`
  без переподготовки;
- ничего не изменилось → ничего.

`playbackKey` (`rememberPlayerPlaybackKey`) зависит от URL, заголовков, офлайн-ключа, `retryKey` и
выбранных субтитров. Поэтому бамп `retryKey` пересоздаёт media item при неизменном URL (повтор), а
смена субтитров Alloha пересобирает его, потому что side-loaded субтитры входят в `MediaItem`.

Позиция старта нового потока берётся из `State.playbackPositionMs`: события, пересобирающие поток
(качество, дорожка, субтитры), заранее кладут туда точную позицию.

## Подключение UI к сервису

`rememberPlayerPlaybackSessionClient()` собирает `MediaController` и отдаёт
`PlayerPlaybackSessionClient`.

Из комментариев и KDoc в коде:

- `MediaController` строится на `applicationContext`, не на Activity: `release()`
  отвязывается отложенно (до 30 с), а bind'ы Activity-контекста система снимает сама при её
  уничтожении, поэтому повторный `unbindService` падает с «Service not registered».
- сервис не гасится через `Context.stopService`: команда `STOP_SERVICE` едет по тому же IPC-каналу,
  что `pause` и `clearMediaItems`, поэтому порядок гарантирован, а завершение выполняет сам сервис
  через `pauseAllPlayersAndStopSelf()`; внешний снос foreground-сервиса система расценивает как
  `startForegroundService()` без `startForeground()` и убивает процесс; голый `stopSelf()` при
  ongoing playback роняет процесс `RemoteServiceException`;
- `onUpdateNotification` пропускается, пока `stopState.isStopping`: отложенное обновление
  внутреннего контроллера media3 ещё видит `playWhenReady = true` и снова зовёт
  `startForegroundService()`.

`PlayerServiceWarmupEffect` держит лишнее подключение, пока экран плеера виден. Сервис, ExoPlayer и
кэш поднимаются сразу при входе, одновременно с резолвом ссылки. При смене серии view плеера уходит
из композиции и шлёт `STOP_SERVICE`, но привязанный клиент не даёт системе уничтожить сервис:
следующая серия подключается к тому же экземпляру. На `ON_STOP` подключение снимается, чтобы
свёрнутое приложение не держало плеер в памяти.

`onTaskRemoved` останавливает сервис (комментарий: фонового воспроизведения без экрана нет). В
`onCreate` вызывается `setForegroundServiceTimeoutMs(0)`: комментарий в коде — «user engaged» окно
не нужно, с дефолтными 10 минутами media3 ещё долго после паузы держит сервис foreground-нужным, а
завершение сервиса в этом окне система расценивает как `startForegroundService()` без
`startForeground()`.

## Compose-эффекты (`ui-common`)

| Эффект / класс                | Что делает                                                                     |
|-------------------------------|--------------------------------------------------------------------------------|
| `PlayerMediaItemEffect`       | Отдаёт media item плееру, затем `playWhenReady`                                |
| `PlayerListenerEffect`        | `Player.Listener` → `PlaybackReady`, конец серии, `PlaybackError`, автоскрытие |
| `PlayerLifecycleEffect`       | Пауза и сохранение прогресса на `ON_PAUSE`, возобновление, выгрузка при уходе  |
| `PlayerStallWatchdogEffect`   | 15 с буферизации при запрошенном воспроизведении → `PlaybackStalled`           |
| `PlayerProgressPollingEffect` | Опрос позиции                                                                  |
| `PlayerKeepScreenOnEffect`    | Экран не гаснет во время воспроизведения                                       |
| `PlayerVolumeEffect`          | Громкость плеера и системы                                                     |

Порядок эффектов в `PlayerLifecycleEffect` важен: Compose освобождает эффекты в обратном порядке, а
выгрузка (`clearMediaItems`) при ещё подписанном слушателе выглядит для него как конец серии.
Возобновление решается флагом, снятым на `ON_PAUSE`, а не текущим `wantsPlay`: пауза из `ON_PAUSE`
сама его сбрасывает, и после короткого `ON_PAUSE → ON_RESUME` видео осталось бы на паузе.

## Различия ТВ и мобилки

| Аспект              | ТВ (`ui-tv`)                                                       | Мобилка (`ui-mobile`)                                                           |
|---------------------|--------------------------------------------------------------------|---------------------------------------------------------------------------------|
| Уход в фон          | `releaseOnStop = true`: фон = уход с плеера                        | PiP или Cast удерживают воспроизведение                                         |
| Пауза на `ON_PAUSE` | всегда                                                             | не ставится при PiP/Cast (`keepPlayingOnPause`)                                 |
| Выгрузка при уходе  | всегда                                                             | не выгружается в PiP (`keepPlayingOnLeave`)                                     |
| PiP                 | нет                                                                | `MobilePlayerPipController`, `onUserLeaveHint`                                  |
| Cast                | нет: `CastSupport.Status.TELEVISION`, кастовать с приставки некуда | `PlayerCastPlayerFactory`, если `CastSupport.isSupported` (GMS не старше 22.26) |
