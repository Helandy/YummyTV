# Хранилище и кэши

Что где лежит на устройстве, как читаются данные (offline-first, TTL) и что очищается при выходе,
обновлении и старте. Прогресс просмотра — [watch-progress.md](watch-progress.md), токены —
[network-and-auth.md](network-and-auth.md), кэши плеера — [player-buffering.md](player-buffering.md),
загрузки — [video-download.md](video-download.md), картинки —
[image-loading-and-memory.md](image-loading-and-memory.md).

## Карта хранилищ

| Хранилище                        | Технология            | Что лежит                                                                                          | Модуль                   |
|----------------------------------|-----------------------|----------------------------------------------------------------------------------------------------|--------------------------|
| `yummy_cache.db` (`AppDatabase`) | Room, версия 54       | Кэши ответов API, прогресс просмотра, загрузки, «Позже», очередь мутаций, настройки Alloha-дорожек | `core:storage`           |
| `app_settings`                   | DataStore Preferences | Пользовательские настройки (внешний вид, плеер, язык, аккаунт)                                     | `core:preferences`       |
| `yani_auth_secure_preferences`   | SharedPreferences     | Зашифрованный refresh-токен и режим хранения                                                       | `core:preferences`       |
| `files/video_download_cache`     | Media3 `SimpleCache`  | Скачанные серии (без вытеснения)                                                                   | `feature:video-download` |
| Стриминговый кэш плеера          | Media3 `SimpleCache`  | `LeastRecentlyUsedCacheEvictor(50 МБ)`, `PlayerStreamingCacheProvider`                             | `feature:player`         |
| `cache/image_cache`              | Coil `DiskCache`      | Картинки, размер из настройки                                                                      | `app` (Coil)             |
| Кэш превью Kodik                 | Coil `DiskCache`      | Превью серий                                                                                       | `core:utils`             |
| Block Store                      | Google Play Services  | Копия refresh-токена для восстановления входа                                                      | `feature:account`        |

## Room

`StorageModule` собирает базу: `yummy_cache.db`, `addMigrations(*ALL_MIGRATIONS)` (цепочка от 7 к
текущей версии), `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1..6)`. То есть версии
1–6 уничтожаются целиком, начиная с 7 миграции пишутся руками.

Сущности сгруппированы по доменам: библиотека, прогресс, аниме (детали, видео, рекомендации,
трейлеры), лента, топ, расписание, поиск, коллекции, аккаунт (профили, списки, оценки, подписки,
уведомления, статистика), комментарии, документы, загрузки, дорожки Alloha, «Позже», очередь
мутаций.

`exportSchema = false`. Миграции описаны в `core/storage/.../db/migrations/` и перечислены в
`ALL_MIGRATIONS`. `StorageCleanup.purgeStaleCaches` (`StorageCleanupStore`) удаляет кэши старше
7 суток (`CACHE_RETENTION_MS`) вместе с «осиротевшими» дочерними строками.

### Слои доступа

Для каждого домена свой набор: `Entity` + `Dao` + `XxxStorage` (интерфейс) + `XxxStore`
(реализация). Репозитории зависят от интерфейса `XxxStorage`, `Dao` наружу не торчит.

## Паттерны чтения

### offline-first по типизированной таблице

`offlineFirstCache(...)` (`core/storage/.../offlinefirst/OfflineFirstCache.kt`):

```
stored = forceRefresh ? null : read()
если stored есть и (isFresh(stored) или !isOnline()) → вернуть toDomain(stored)
иначе fetchAndSave(); при ошибке сети вернуть устаревший кэш, если он есть, иначе onMissing(error)
```

- Каждый домен держит свою типизированную таблицу и свой `isFresh(ttlMs)`.
- Сетевая ошибка не показывается пользователю, если есть любой кэш: сначала устаревшие данные.
- Одновременные запросы не дедуплицируются (нет мьютекса по ключу): так же вели себя репозитории,
  которые этот хелпер заменил.
- `Domain` может быть nullable, поэтому «есть кэш с null» и «кэша нет» различаются явной проверкой.

### JSON-документ

`DocumentCacheStorage.getOrFetch(cacheKey, ttlMs, forceRefresh, decode, encode, fetch)`
(`DocumentCacheStore`): таблица `DocumentCacheEntry(cacheKey, payload, cachedAt)`.

- Запросы по одному ключу сериализованы `Mutex`'ом (`requestLocks`): параллельные вызовы ждут
  первого и берут его результат.
- Ошибка `fetch` при наличии декодируемого кэша отдаёт кэш, иначе пробрасывается.
- `invalidationVersion`: счётчик, увеличивается при `delete`, `deleteByPrefix`,
  `deleteUserNamespace`. Если за время `fetch` инвалидация произошла, результат не записывается:
  иначе запрос, начатый до выхода из аккаунта, сохранил бы чужие данные после чистки.

`UserScopedCache` строит ключ `user:$userId:$namespace:$language:$key` (гость — `user:0:…`) и (де)
сериализует значения через `DocumentCacheJson`. Выход пользователя чистит
`user:$userId:` по префиксу.

## TTL

Значения лежат рядом с репозиториями (`private const val ..._TTL_MS`).

| Данные                                              | TTL                  | Где                              |
|-----------------------------------------------------|----------------------|----------------------------------|
| Детали аниме                                        | 24 ч                 | `YaniAnimeRepository`            |
| Список видео (серии, озвучки)                       | 5 мин                | `YaniAnimeRepository`            |
| Связанные, эпизоды (`episodeInfo`)                  | 24 ч / 7 суток       | `YaniAnimeRepository`            |
| Дополнительное публичное, персональные рекомендации | 6 ч / 5 мин          | `YaniAnimeRepository`            |
| Топ                                                 | 6 ч                  | `YaniAnimeTopRepository`         |
| Лента главной                                       | 1 мин                | `YaniHomeFeedRepository`         |
| Расписание                                          | 1 ч                  | `YaniScheduleRepository`         |
| Поиск: результаты / фильтры                         | 10 мин / 24 ч        | `YaniSearchRepository`           |
| Коллекции (детали, каталог)                         | 1 мин                | `YaniCollectionDetailRepository` |
| Комментарии                                         | 5 мин                | `YaniCommentsRepository`         |
| Посты (лента / деталь / категории)                  | 2 / 5 мин / 6 ч      | `YaniPostsRepository`            |
| Блогеры (лента / деталь / справочник)               | 2 / 5 мин / 6 ч      | `YaniBloggerVideosRepository`    |
| Аккаунт: короткий / средний / длинный               | 5 мин / 30 мин / 6 ч | `AccountStorageTtl.kt`           |
| Уведомления                                         | 2 мин                | `AccountStorageTtl.kt`           |
| Состояние списка аниме                              | 24 ч                 | `AccountStorageTtl.kt`           |
| Страницы сайта                                      | 24 ч                 | `YaniSitePagesRepository`        |
| Кэш резолва потока плеера                           | 3 мин                | `DefaultPlayerStreamRepository`  |

TTL — свежесть для чтения, а не срок удаления. Удаляет `StorageCleanup` по возрасту 7 суток.

## Очистка

| Когда                                          | Что чистится                                                                                                                              |
|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Холодный старт (`AppStartupMaintenanceRunner`) | `purgeStaleCaches`: кэши старше 7 суток и осиротевшие дочерние строки; `LegacyStreamingCachePruner`; старый `update.apk` при смене версии |
| Выход (`logout`)                               | Документ-кэш по `user:$id:`, user-scoped аккаунт, `expireAllVideos()` (`cachedAt = 0`), Block Store, токен, настройки аккаунта            |
| Смена пользователя при входе                   | Документ-кэш и user-scoped данные прошлого пользователя (до записи `setYaniAccount`)                                                      |
| Смена языка контента                           | Кэши ключуются по языку, чужой язык не читается                                                                                           |
| Удаление загрузки                              | Ресурсы по префиксам записи (`downloadResourcePrefixes`)                                                                                  |

`expireAllVideos()` не удаляет строки, а выполняет `UPDATE anime_video_caches SET cachedAt = 0`
(комментарий в `logout()`: в кэше видео лежат привязанные к пользователю `watched` и `subscribed`).
Таблица `watch_progress` в `logout()` не чистится.

## Очередь мутаций (outbox)

Мутации, которые не дошли до сервера из-за сети, не теряются: `PendingMutationOutbox` хранит их в
Room (`pending_mutations`: `id`, `type`, `payloadJson`, `createdAt`, `attemptCount`).

```
ViewModel/Handler  → сетевая ошибка → outbox.enqueue(type, payloadJson) → scheduleFlush()
PendingMutationSyncWorker (WorkManager, NetworkType.CONNECTED)
  для каждой записи: apply(entry)
    успех                → remove(id)
    сетевая ошибка       → оставить, Result.retry() (экспоненциальный backoff от 15 минут)
    отказ сервера        → remove(id): повторять бессмысленно
```

Типы (`PendingMutationTypes`, строки, чтобы БД переживала переименование enum'ов): `mark_watched`,
`remove_watched`, `set_list`, `remove_list`, `set_favorite`, `set_rating`, `delete_rating`,
`vote_review`.

- Воркер живёт в `:app`, а не в `core`: ему нужны use case'ы сразу нескольких фич.
- `schedule()` ещё взводит периодическую страховку (`PeriodicWork`, раз в 6 часов): мутация,
  поставленная в очередь, пока процесс убит, всё равно уйдёт.
- Типы объявлены в `PendingMutationTypes`, payload'ы (`encode`/`decode`) — в
  `PendingMutationPayloads.kt`, применяются в `PendingMutationSyncWorker.apply`. Ветка `else -> Unit`
  для неизвестного типа завершается без ошибки, и запись удаляется как успешная.

## Настройки (DataStore)

Единый `preferencesDataStore("app_settings")` в `SettingsDataStore.kt` (делегат допускает ровно один
экземпляр на файл). Интерфейсы по областям в `core:preferences/.../settings/`:
`AppearanceSettingsStore`, `PlayerSettingsStore`, `CacheSettingsStore`, `YaniAccountSettingsStore`,
`SearchSettingsStore`, `EpisodePushSettingsStore`, `VideoExportSettingsStore`,
`AppLifecycleSettingsStore`, общий `SettingsStore`.

- Реализации (`DataStore*SettingsStore`) объявлены `internal` в `core:preferences`, наружу видны интерфейсы.
- Значения, нужные синхронно (порог «просмотрено», UA, снапшот размера кэша), зеркалятся в процесс
  на старте (`WatchedEpisodeRuleSync`, `DataStoreBrowserUserAgentProvider`) или читаются блокирующе
  с таймаутом там, где API платформы синхронное (`PlayerExoPlayerFactory`,
  `CoilImageLoaderInstaller`).
- Токен лежит не в DataStore, а в SharedPreferences (KDoc `YaniAuthPreferences`: токен должен
  оставаться зашифрованным на диске).
