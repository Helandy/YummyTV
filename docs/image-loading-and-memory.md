# Картинки и списки

Как приложение грузит картинки через Coil, как строятся ключи элементов списков и как устроена
пагинация. Только то, что есть в коде.

## Coil

Один `ImageLoader` на процесс, собирает `CoilImageLoaderInstaller` (`app/.../startup/`). Лоадер
создаётся лениво, на первой картинке: к этому моменту интерфейс уже выбран.

| Параметр                  | Значение                                                                            |
|---------------------------|-------------------------------------------------------------------------------------|
| Memory cache              | `maxSizePercent`: `0.15` доступной памяти приложения, `0.10` при `isLowRamDevice`   |
| Disk cache                | `cacheDir/image_cache`, размер из настройки (`CacheSettingsStore.previewCacheSize`) |
| Crossfade                 | Включён на мобильном интерфейсе, выключен на ТВ                                     |
| Сеть                      | `KtorNetworkFetcherFactory` поверх `okHttpClient.newBuilder()` (общий пул соединений и диспетчер с API-клиентом, независимая конфигурация) |
| Дополнительные компоненты | `KodikThumbnailKeyer`, `KodikThumbnailFetcher.Factory`, `HistoryEpisodeThumbnailKeyer`, `HistoryEpisodeThumbnailFetcher.Factory` |

Из комментариев в коде:

- Размер диска читается блокирующе (`runBlocking`, таймаут 1 с): Coil создаёт дисковый кэш лениво и
  не на main, а снапшот `currentPreviewCacheSize` на холодном старте ещё не заполнен и отдал бы
  значение по умолчанию вместо выбранного пользователем.
- Crossfade на ТВ выключен: анимация на каждой карточке грида стоит кадров на слабых приставках, а
  при DPAD-скролле её всё равно не видно.

### Ключ memory-кэша

`CachedAsyncImage` и `rememberCachedImageRequest(url)` задают `memoryCacheKey(url)` и
`placeholderMemoryCacheKey(url)`. По умолчанию Coil добавляет в ключ размер таргета, поэтому одна и
та же картинка в списке и на экране деталей имеет два ключа и грузится дважды. С общим ключом экран
деталей сразу показывает уже загруженное превью из списка как placeholder, пока подтягивается версия
в полном разрешении (KDoc `CachedAsyncImage`).

Картинки, которые рисует не `CachedAsyncImage`, берут запрос из `rememberCachedImageRequest`.

### Превью серий

`KodikThumbnail(iframeUrl)` с ключом кэша `kodik_thumb:<нормализованный iframe>` читается и пишется
не в общий дисковый кэш, а в собственный `DiskCache` (`KodikThumbnailCacheIO`,
`@KodikThumbnailDiskCache`). Код лежит в `core:utils` (пакет `kodik`), см.
[other-extractors.md](other-extractors.md).

### Качество постеров

`PosterQuality`: `LOW`, `STANDARD`, `MEGA`, `HIGH`.

- Значение по умолчанию (`DataStoreAppearanceSettingsStore.defaultPosterQuality`): на Android 12+
  (`Build.VERSION_CODES.S`) `MEGA`, ниже `STANDARD`; в KDoc: на более старых устройствах стандартное,
  «чтобы не упираться в лимит bitmap-кэша».
- В UI значение приходит через `LocalPosterQuality` (его задают `MobileMainGraph` / `TvMainGraph` из
  состояния).
- URL постера выбирается функциями вида `posterUrl(quality)` с цепочкой запасных размеров
  (`LibraryTvMappers.kt`): для `LOW` порядок `medium → big → fullsize → small`, для `STANDARD`
  `big → medium → fullsize → small`, для `MEGA` `mega → big → medium → fullsize → small`, для `HIGH`
  `fullsize → mega → big → medium → small`.

## Память

От `ActivityManager.isLowRamDevice` в коде зависят две вещи: доля memory cache Coil (`0.10` вместо
`0.15`) и `setForceHighestSupportedBitrate(!isLowRamDevice)` у трек-селектора плеера
(`PlayerExoPlayerFactory`). Буфер плеера определяется настройкой `PlayerBufferProfile`, а не типом
устройства ([player-buffering.md](player-buffering.md)). Других классификаций устройств по памяти
в коде нет.

## Пагинация

`pagingFlow` / `pagingSource` (`core/utils/.../paging/OffsetPagingSource.kt`):

- `PagingConfig(pageSize, initialLoadSize = pageSize, enablePlaceholders = false)`;
- `OffsetPagingSource` отдаёт `prevKey = null`, то есть догрузка вверх (PREPEND) невозможна;
- `itemKey` дедуплицирует элементы в пределах одного источника: повторы отбрасываются, серверные
  `nextOffset` и признак конца страницы сохраняются;
- пустая, но непоследняя страница (`items.isEmpty() && canLoadMore`) не завершает пагинацию:
  источник переходит на `nextOffset` и грузит дальше;
- `LoadResult.Page` строится без `itemsBefore` / `itemsAfter`;
- `getRefreshKey` сдвигает ключ на `initialLoadSize / 2` от якоря, не ниже `initialOffset`.

Параметр `maxSize` в `PagingConfig` нигде в проекте не задаётся.

## Ключи lazy-элементов

`lazyKey(prefix, id, index)` (`core/utils/.../lazylist/LazyKeys.kt`) возвращает `"$prefix:$id"`, а
для `id == null` (плейсхолдер пагинации) `"$prefix:placeholder:$index"`. KDoc функции:

- `Key "..." was already used` в Compose возникает, если в одном lazy-layout два элемента получили
  одинаковый ключ;
- сырой числовой id опасен: он может совпасть с id соседней секции того же `LazyColumn` и живёт в
  одном пространстве с индексами-плейсхолдерами (`items[index]?.id ?: index`);
- ключ обязан быть saveable-типом, поэтому `String`.

Правила, действующие в коде:

- `itemKey` передаётся в `pagingFlow` / `pagingSource` (`Pager` в `TopViewModel` тоже:
  `OffsetPagingSource(itemKey = { it.id })`).
- Дедупликация плоских списков делается не в UI, а в use case'ах, мапперах и источниках через
  `distinctBy` (например, `GetRecentlyAiredScheduleUseCase`, `CollectionStorageMapper`, `PostsMapper`).
- В `LibraryTvHistoryPage` ключ элемента берётся из `entries.historyFocusKeys()`, для плейсхолдера
  `lazyKey("history", null, index)`. KDoc `historyFocusKeys`: ключ намеренно без
  `watchedAtSeconds`, чтобы после просмотра серии, когда запись уезжает вверх, фокус ехал за ней;
  пара `animeId` + серия в истории не уникальна (пересмотр, разные озвучки), поэтому к повторам
  дописывается порядковый номер `#N`, иначе строки делили бы один `FocusRequester`. Этот же ключ
  использует `launchTvLazyListKeyFocusRestore` ([tv-focus.md](tv-focus.md)).

## Коллекции в состоянии

В `*State` применяются типы `kotlinx.collections.immutable`: `ImmutableList`, `ImmutableMap`,
`PersistentSet`, `PersistentMap` (например, `EpisodesState`). Зависимость подключает конвенционный
плагин `yummytv.android.library.compose` как `api`.

Где поле изменяется операторами `+` / `-` (`watchLaterEpisodes`, `resolvingDownloadKeys` в
`EpisodesState`), оно объявлено как `PersistentSet` / `PersistentMap`, а не `ImmutableSet` /
`ImmutableMap`: операторы `plus` / `minus` библиотеки определены на `Persistent*`. Файлы, которые их
используют, импортируют `kotlinx.collections.immutable.plus` / `minus` явно.
