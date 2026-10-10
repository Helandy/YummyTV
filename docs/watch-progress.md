# Прогресс просмотра

Как позиция в серии становится «Продолжить просмотр», отметкой на сервере и карточкой Watch Next на
ТВ. Правила выбора карточек — [continue-watching.md](continue-watching.md), контракт `PUT /video` —
[yani-api.md](yani-api.md), очередь мутаций — [storage-and-cache.md](storage-and-cache.md).

## Путь данных

```
ExoPlayer (позиция)
  PlayerProgressPollingEffect          каждую секунду уведомляет, раз в 10 с сохраняет
  → PlayerState.Event.PlaybackPositionChanged
  → PlayerPlaybackProgressHandler      готовит запрос на сохранение
  → PlayerProgressHandler              @ViewModelScoped
      ├─ SaveWatchProgressUseCase      → WatchProgressStore (Room, таблица watch_progress)
      └─ syncRemoteProgress            → SaveVideoWatchProgressUseCase → PUT /video/{id}
  при уходе с экрана: PlayerNavigationDelegate (ioScope), финальная отправка

WatchProgressStore.observeContinueWatching()
  ├─ Главная и «Библиотека» (Continue Watching)
  ├─ ТВ: WatchNextManager → системный ряд Watch Next
  └─ запуск из карточки: ResolveContinueWatchingLaunchUseCase
```

## Две копии прогресса

| Хранилище                       | Что                                                     | Зачем                                              |
|---------------------------------|---------------------------------------------------------|----------------------------------------------------|
| Локальная Room `watch_progress` | Позиция и длительность серии, метаданные для карточки   | «Продолжить просмотр», работает офлайн и для гостя |
| Сервер yani                     | Секунды (`times`), позиция `time`, статус «просмотрено» | Синхронизация между устройствами, статистика       |

Они независимы: локальная запись пишется всегда, серверная только авторизованному пользователю
(`yaniUserId > 0`) и только если прогресс «осмысленный».

## Локальная запись

Таблица `watch_progress`, ключ `(animeId, episode)`, индекс по `updatedAt`. Поля:
`videoId`, `episodeUrl`, `positionMs`, `durationMs`, `updatedAt`, метаданные карточки (`animeTitle`,
`posterUrl`, `playerName`, `dubbing`, `screenshotUrl`).

### Виды записи

Смысл определяют `positionMs` и `durationMs` (`core/model/.../AnimeWatchProgress.kt`):

| Вид             | Условие                                                                      |
|-----------------|------------------------------------------------------------------------------|
| meaningful      | `durationMs > 0` и `positionMs >= 30 с`                                      |
| watched         | meaningful и до конца осталось не больше порога (`WatchedThresholds`)        |
| unresolved      | `durationMs == 0` и `positionMs >= 30 с` (длительность не определили)        |
| continue target | `positionMs == 0` и `durationMs == 0`, при непустых `episode` и `episodeUrl` |

### `WatchProgressStore.save`

Запись сериализована `Mutex`'ом: параллельные события плеера не должны перетирать друг друга.

| Условие                                     | Результат                                                                      |
|---------------------------------------------|--------------------------------------------------------------------------------|
| `positionMs < 30 с`, есть continue target   | Обновляются метаданные, цель сохраняется                                       |
| `positionMs < 30 с`, есть meaningful запись | Ничего: временный ноль от lifecycle/Media3 не должен стирать Continue Watching |
| `positionMs < 30 с`, иных записей нет       | Запись удаляется                                                               |
| `positionMs >= 30 с`                        | Запись создаётся/заменяется; пустые поля берутся из прежней записи             |

Каждое сохранение прогресса снимает suppression (скрытие карточки) для `animeId`: новая активность
возвращает тайтл в список.

### Continue target

`saveContinueTarget` делает серию целью «следующая»: если у неё есть недосмотренный прогресс, он
сохраняется (меняются только источник и метаданные), иначе пишется цель `0:00`. Требует
`animeId > 0`, непустые `episode` и `episodeUrl`.

Вызывается из `PlayerViewModel` в двух местах:

- при ручном переходе на следующую серию (`Event.NextEpisode`): цель ставится на открываемую серию;
- при завершении серии (`saveWatchedProgressIfNeeded`): если включена настройка «Следующая серия»
  (`suggestNextEpisodeOnWatched`) и следующая серия есть, цель ставится на неё, иначе
  `suppressContinueWatchingDisplay` скрывает тайтл. Ручное удаление карточки скрывает так же.

### Порог «просмотрено»

Настраиваемый (Настройки → «Прогресс просмотра»), зависит от длины серии; таблица и значения по
умолчанию — в [continue-watching.md](continue-watching.md). KDoc `WatchedEpisodeRule`: правило
вызывается синхронно из storage/presentation, поэтому настройки не протаскиваются параметром, а
синхронизируются в процесс из DataStore при старте приложения (`WatchedEpisodeRule.update`,
`WatchedEpisodeRuleSync`).

### Нормализация при сохранении

Если снимок «просмотрен», плеер нормализует его до полного просмотра (`positionMs = durationMs`,
`withFullTimingIfWatched`). После этого `ContinueWatchingMerge.filterDisplayable()` убирает такую
запись из списка и скрывает более ранние записи того же тайтла.

## Серверная отправка

Реализует `PlayerProgressHandler.syncRemoteProgress` (контракт — [yani-api.md](yani-api.md)):

- условия: `videoId > 0`, прогресс meaningful, пользователь авторизован;
- `recordWatchedSecond` (тик ~1 с) копит уникальные секунды по `videoId` в
  `watchedSecondsByVideoId`; позиция зажимается в `[0, duration - 10 с]`;
- отправляется только дельта: `watched − synced`, потому что сервер суммирует длины `times`;
- плановая отправка не чаще раза в 10 с и не встаёт в очередь (`tryLock`), финальная при уходе ждёт
  замок;
- секунды помечаются отправленными только при успешном ответе;
- на достигнутом «просмотрено» отправка форсируется один раз (`completionAttemptedVideoIds`), после
  успеха (`completedRemoteVideoIds`) повторов нет.

KDoc `PlayerProgressHandler`: один экземпляр на ViewModel (`@ViewModelScoped`); секунды копит
ViewModel, а финальную отправку при уходе делает `PlayerNavigationDelegate`; «без общего скоупа у
него был свой пустой экземпляр и последние секунды перед выходом не уходили на сервер».

KDoc `PlayerNavigationDelegate`: прогресс сохраняется в `ioScope` (`@IoApplicationScope`), потому
что `nav.replace` уничтожает `NavEntry` плеера, а вместе с ним `viewModelScope`.

### Офлайн

Отметка «просмотрено» из деталей серии (`MARK_WATCHED`) при сетевой ошибке ставится в очередь
`PendingMutationOutbox` и дожимается `PendingMutationSyncWorker`, см.
[storage-and-cache.md](storage-and-cache.md). `PlayerProgressHandler` с очередью не работает: в его
конструкторе нет `PendingMutationOutbox`.

## Запуск из карточки и прогресс с других устройств

`ResolveContinueWatchingLaunchUseCase` (`feature/watching/domain`) загружает видео тайтла и вызывает
`resolveContinueWatchingLaunch`. Правила выбора источника — в
[continue-watching.md](continue-watching.md), здесь про выбор позиции:

- настройка «Свежий прогресс с других устройств» (`refreshContinueWatchingProgressOnLaunch`)
  включает и принудительное обновление списка видео, и учёт серверного прогресса
  (`useServerProgress`);
- серверный прогресс (`selectServerContinueProgress`) выигрывает, только если его `updatedAt` строго
  больше `updatedAt` локальной записи; тогда запуск идёт с серверной серии и позиции;
- иначе используется локальная запись, а при необходимости делается миграция записи на доверенный
  реальный `episode` (`progressMigration`);
- если серверная цель отличается от локальной (другое видео, iframe или серия, либо позиция
  расходится больше чем на 5 с), результат содержит `remoteProgressSwitch`, и `HomeViewModel` /
  `LibraryViewModel` показывают тост «продолжаем с серии N, время»
  (`home_remote_continue_progress_toast`);
- пока запуск идёт, `launchingContinueWatchingAnimeId` блокирует повторное нажатие.

## Источники отображения

| Где                         | Источник                                            | Особенности                                                                                    |
|-----------------------------|-----------------------------------------------------|------------------------------------------------------------------------------------------------|
| Главная (Continue Watching) | `observeContinueWatching()` поверх кэша ленты       | Local-записи подставляются поверх cached feed                                                  |
| Библиотека, «Продолжить»    | `ContinueWatchingGrid` в `ui-tv` библиотеки         | —                                                                                              |
| Библиотека, история         | `WatchHistoryEntry` (`feature/library`, `YaniWatchHistoryApi`) | Ключ элемента на ТВ составной, см. [image-loading-and-memory.md](image-loading-and-memory.md) |
| Детали, прогресс серий      | `watchProgressStore.observeByAnimeId` в `YaniAnimeRepository` | `DetailsWatchProgressIndex` в UI                                     |
| ТВ Watch Next               | `WatchNextManager.sync(entries)`                    | Полная пересборка ряда; включается настройкой `watchNextEnabled`, при выключении список пустой |

### Watch Next (ТВ)

`WatchNextManager` (`core:tv`) пишет в `TvContractCompat.WatchNextPrograms` через `ContentResolver`:
сначала удаляет все свои записи, затем вставляет по одной на тайтл (`latestByAnime`).

- Тип `WATCH_NEXT_TYPE_CONTINUE`, `TYPE_TV_SERIES`, позиция и длительность, `internal_provider_id` =
  `episodeUrl`.
- Картинка: превью серии Kodik, иначе скриншот, иначе постер.
- Интент: `yummytv://details/{animeId}`, то есть открывается экран деталей, а не плеер
  (см. [navigation.md](navigation.md), диплинки).
- `ITvIntegration` защищён `DeviceAwareTvIntegration`: на телефоне всё no-op, потому что
  TvProvider/TvContract вне TV недоступны. Новый метод в интерфейсе нужно продублировать в guard.
