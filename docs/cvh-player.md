# Плеер CVH

CVH (CdnVideoHub) отдаёт прямые mp4-ссылки на CDN `ok.ru` (`*.okcdn.ru`): два JSON-запроса к API и
готовая лестница качеств. Сессии, прокси и WebView, как у Alloha, нет. Экстрактор — один файл.

Две особенности ссылок определяют всю логику ниже:

- подпись не зависит от узла: тот же файл с той же подписью доступен на резервном узле;
- узел может перестать принимать новые соединения при живой подписи и отвечать `400`; уже открытое
  соединение при этом докачивает файл до конца.

Цифры замеров — в конце документа.

## 1. Схема

```
PlayerViewModel → PlayerStreamHandler.resolve() → ResolvePlayerStreamUseCase
  → DefaultPlayerStreamRepository.resolve()      кэш 3 мин по "iframeUrl|autoQualityLabel"
    → CvhExtractor.extract()
        GET plapi…/player/sv/playlist?pub=745&id=<animeId>&aggr=mali   → selectCvhItem() → vkId
        GET plapi…/player/sv/video/<vkId>                              → sources.mpeg*Url, failoverHost
        → Stream(url, headers, qualities, failoverHost)

Media3: OkHttpDataSource → CacheDataSource (50 МБ) → okcdn.ru
```

iframe-URL балансера:

```
//ru.yummyani.me/iframeCVH.html?dubbing_code=AniLibria&anime_id=61316&episode=1&dubbing=…
```

`anime_id` здесь — внутренний id CdnVideoHub, не id аниме в yani.

## 2. Файлы

| Файл                                                      | Назначение                                                         |
|-----------------------------------------------------------|--------------------------------------------------------------------|
| `data/.../extractor/cvh/CvhExtractor.kt`                  | Запросы к API, выбор элемента плейлиста, лестница качеств          |
| `data/.../extractor/cvh/CvhPlaylistItem.kt`               | Элемент плейлиста и `selectCvhItem`                                |
| `core/common/.../utils/player/PlayerCdnHostUtils.kt`      | `withCdnHost`, `isOkCdnUrl`, `isIpv4Host`                          |
| `core/utils/.../network/BrowserUserAgent.kt`              | `BrowserUserAgentProvider`: UA из настроек, профиль по умолчанию   |
| `core/model/.../settings/BrowserUserAgentProfile.kt`      | Закрытый список проверенных UA                                     |
| `presentation/.../utils/PlayerStreamFailoverUtils.kt`     | `availableFailoverHost()`, `withFailoverHost()`                    |
| `presentation/.../behavior/DefaultSourceBehavior.kt`      | Решение: переезд / тихий перерезолв / ошибка                       |
| `ui-common/.../service/PlayerLoadErrorHandlingPolicy.kt`  | Быстрый фатал на `400`                                             |
| `ui-common/.../utils/PlayerStreamingCacheKeyUtils.kt`     | Стабильный ключ онлайн-кэша для подписанных ссылок                 |
| `ui-common/.../service/PlayerStreamingCacheKeyFactory.kt` | Подключение ключа; всё не-okcdn уходит в `CacheKeyFactory.DEFAULT` |
| `video-download/data/.../strategy/CvhDownloadStrategy.kt` | Загрузки, `skipRefererOrigin = true`                               |

## 3. Подписанная ссылка

```
https://vd752.okcdn.ru/?expires=1791393814371&srcIp=178.22.51.56&pr=90&srcAg=CHROME
  &ms=95.163.35.29&type=5&sig=9xvgvMx8TFw&ct=0&urls=178.237.29.10&clientType=46&id=13799800310512
```

| Параметр  | Смысл                                                               |
|-----------|---------------------------------------------------------------------|
| `id`      | Идентификатор видео, одинаков у всех качеств                        |
| `type`    | Вариант качества: `0`=240p, `1`=360p, `2`=480p, `3`=720p, `5`=1080p |
| `expires` | Срок действия, мс; +24 часа от выдачи                               |
| `sig`     | Подпись                                                             |
| `srcAg`   | Класс клиента, под который подписана ссылка (`CHROME`)              |
| `srcIp`   | IP клиента на момент выдачи                                         |
| `ms`      | Адрес origin-сервера                                                |

Ограничения:

- `type` коллизирует между форматами: у `hlsUrl` и 480p это `type=2`, у `dashUrl` и 360p — `type=1`.
  Различает их только путь (`/video.m3u8` против `/`). Идентичность из `id` + `type` без пути склеит
  HLS с mp4. Плеер берёт только mp4, но ключ кэша путь учитывает.
- HLS и DASH из `sources` не используются. В HLS-манифестах лежат сегментные URL с сырыми IP CDN,
  они не переписываются на `failoverHost`, что ломает и загрузки, и переезд. Поэтому у CVH нет
  Auto-качества.

## 4. User-Agent

Подпись выдаётся под класс клиента (`srcAg=CHROME`), но проверка шире, чем «похоже на Chrome».
Принимаются Chrome на Windows и Linux, Chrome на Android с `Mobile`, Chromecast, `ExoPlayerLib`.
Отклоняются (`400`) Chrome на macOS, Chrome на Android без `Mobile`, Safari, Firefox, `okhttp`,
пустой и произвольный UA. Номер версии Chrome значения не имеет. Таблица — в разделе «Замеры».

Следствия:

- User-Agent должен стоять на каждом пути, который трогает эти ссылки: воспроизведение и загрузки.
  Исключение — Cast, там UA ставит сам Chromecast (§7).
- `Referer` не нужен, чужой `Referer`/`Origin` даёт `400`. Поэтому `CvhDownloadStrategy` ставит
  `skipRefererOrigin = true`.
- Отклонённый UA мог испортить ссылку. Рассчитывать на обратное нельзя.

### Настройка

UA выбирает пользователь: Настройки → Плеер → «User-Agent балансеров» (TV и mobile). Список
закрытый,
`BrowserUserAgentProfile`: Windows·Chrome (по умолчанию), Linux·Chrome, Android·Chrome. Свободного
текста нет намеренно, произвольная строка тихо ломает CVH.

- `BrowserUserAgentProvider` — контракт, реализация `DataStoreBrowserUserAgentProvider` в
  `core:preferences` держит профиль в `StateFlow` и отдаёт строку синхронно. Потребители
  (экстракторы CVH, Kodik, VK, Rutube, Sibnet, Aksor, Zedfilm, превью Kodik) читают её в момент
  запроса.
- Новое значение применяется к следующему резолву: играющий поток держит старые заголовки, резолв
  кэшируется на 3 минуты.
- До первого чтения DataStore отдаётся профиль по умолчанию.
- Загрузки берут UA из заголовков, которые вернул экстрактор. `BROWSER_USER_AGENT` подставляется,
  только если экстрактор UA не передал.
- Alloha в настройку не входит: её UA случаен per-process и относится к другому CDN.

Чтобы добавить профиль, его сначала проверяют на живой ссылке: свежая ссылка, три запроса, контроль
независимым UA до и после. Профиль, на который okcdn отвечает `400`, добавлять нельзя.

## 5. Переезд на резервный узел

```
okcdn отдал 400
  → PlayerLoadErrorHandlingPolicy: быстрый фатал, один запрос
  → PlaybackException.toPlaybackErrorEvent(): httpStatusCode() = 400
  → DefaultSourceBehavior.onPlaybackError()
      400 ∈ FAILOVER_HTTP_CODES, state.availableFailoverHost() = "vd591.okcdn.ru"
      → applyHostFailover(): host.update { withFailoverHost(host) } + host.invalidateStreamCache()
  → PlayerMediaItemUpdater: url изменился → setMediaItem + prepare
```

- Запросов к `plapi` нет: хост подменяется в ссылках, которые уже лежат в состоянии. Полный
  перерезолв ради смены имени хоста давал видимую заминку после паузы, перемотки или смены качества.
- Подменяется вся карта качеств, а не только `streamUrl`: фактический URL плеера берётся из
  `streamQualityMap` по метке качества (`PlayerPlaybackUiStateMapper`), иначе смена качества вернёт
  на отказавший узел.
- Бюджет тихих повторов (`MAX_ATTEMPTS = 5`) не тратится. Ветка стоит выше `retry.canRetry()`.
- `retryKey` не бампается: `PlayerMediaItemUpdater` пересоздаёт media item при `currentUri !=
  config.url`, а переезд меняет URL.
- Защиты от зацикливания отдельным флагом нет: после подмены хост ссылки равен `streamFailoverHost`,
  `availableFailoverHost()` возвращает `null`.
- `recovering` не выставляется, выставляется `isPlaybackRecovering`: последний кадр остаётся
  (`keepContentOnReset`), оверлея нет, флаг снимается по `PlaybackReady`.
- Если и резервный узел ответил `400`, управление падает в `scheduleRetryAttempt()` с
  `forceRefresh = true`: полный перерезолв, где `CvhExtractor` переводит все качества на
  `failoverHost`.

`FAILOVER_HTTP_CODES = {400, 403}`. `404` и `410` сюда не входят: файла нет, нужен перерезолв, а не
другой узел.

`FATAL_RESPONSE_CODES` в `PlayerLoadErrorHandlingPolicy` — контракт. Быстрый фатал на `400` доносит
отказ до слушателя плеера за один запрос, без 20 фоновых ретраев (`SILENT_RETRY_COUNT`). Менять
набор кодов, не посмотрев на `DefaultSourceBehavior.FAILOVER_HTTP_CODES`, нельзя: переезд перестанет
срабатывать.

В `CvhExtractor` есть и исходный механизм: сырые IPv4-хосты в `sources` переписываются на
`failoverHost` всегда, именованный узел — только при `forceRefresh`. Поле `failoverHost` в `Stream`
пустое, если ссылки уже выданы на этом узле.

## 6. Кэши

Кэш резолва — `DefaultPlayerStreamRepository`, in-memory, TTL 3 минуты, 8 записей, ключ
`"iframeUrl|autoQualityLabel"`. После переезда сбрасывается через
`InvalidatePlayerStreamCacheUseCase`, иначе в пределах TTL вернулись бы ссылки на отказавший узел.

Онлайн-кэш воспроизведения — `SimpleCache` на 50 МБ, `PlayerStreamingCacheProvider`. Ключ по
умолчанию — полный URI, у подписанных ссылок он меняется при каждом перерезолве. Для okcdn ключ
нормализуется до идентичности файла (`id` + `type` + путь):

```
okcdn|13799800310512|5|
```

Без этого скачанный буфер после смены ссылки недостижим, а переезд начинает загрузку с нуля.

- Нормализация только для okcdn-хоста, остальное — `CacheKeyFactory.DEFAULT`. У произвольного CDN
  срезать подпись нельзя: можно склеить разные дорожки одного манифеста.
- Фабрики ключей офлайн-загрузок (`DownloadCacheKeyFactory`, `RotatingHlsCacheKeyFactory`) не
  трогать: от них зависит идентичность уже скачанных байтов.

## 7. Cast

Используется штатный ресивер (`DEFAULT_MEDIA_RECEIVER_APPLICATION_ID`). Он ходит за медиа
собственным стеком Chromecast и своих заголовков не добавляет. UA Chromecast okcdn принимает (§4).

Проверено только `curl` по синтетической строке с `CrKey/1.56`. На реальном Chromecast Cast для CVH
не запускался. Настоящий UA можно узнать, подняв в той же сети HTTP-сервер, который печатает
заголовок, и скастив на него тестовую mp4-ссылку. Если на устройстве всё же `400`, решением будет
прокси в LAN, а не свой ресивер.

Кнопка Cast для okcdn не скрывается. Контейнер у ссылок без расширения определяется через
`video/mp4` в `YummyTvCastMediaItemConverter`.

Собственный Web Receiver не подходит:

- нужны регистрация в Cast Developer Console (разовый взнос) и HTTPS-хостинг;
- заголовки в нём ставятся только на XHR адаптивных потоков; `User-Agent`, `Referer`, `Origin`,
  `Sec-*` из JS не ставятся, а запросы `<video>` для прогрессивного mp4 не настраиваются вовсе;
- Alloha он не помогает: ей нужны запрещённые для JS заголовки сессии, а её прокси слушает
  `127.0.0.1`, недоступный Chromecast. Cast Alloha возможен только через прокси в LAN с токенами и
  белым списком клиента по IP; не делалось.

Проброс заголовков через `MediaInfo.customData` отвергнут: в `play-services-cast 22.3.1` нет
copy-конструктора `MediaInfo.Builder(MediaInfo)`, а штатный ресивер эти данные игнорирует.

## 8. Отладка

Логи идут через `AnalyticsTracker.log`: в debug в logcat, в release теги `PlayerViewModel`,
`CvhExtractor`, `PlayerBuffering`, `PlayerExtractor` пишутся в файл логов приложения (Настройки → О
приложении → Поделиться логами, при включённой «Настройки → Общие → Запись логов»).

```bash
adb logcat -c && adb logcat -v time PlayerViewModel:D CvhExtractor:D Analytics:D *:S
```

| Маркер                                         | Значение                                                          |
|------------------------------------------------|-------------------------------------------------------------------|
| `Resolved vkId=… qualities=[…] failoverHost=…` | Резолв прошёл. Пустой `failoverHost` — переезжать некуда          |
| `useFailoverHost=true`                         | Перерезолв после сбоя, все качества переведены на резервный узел  |
| `Playback error … http=400`                    | Узел отказал. Нет `CDN host failover` дальше — резервного не было |
| `CDN host failover -> vdNNN.okcdn.ru`          | Переезд выполнен, запросов к `plapi` не было                      |
| `Silent playback retry attempt=N/5`            | Переезд недоступен или израсходован, идёт обычный перерезолв      |
| `Silent playback retry with /videos refresh`   | Повреждённый контейнер, перезапрос `/videos` за свежим iframe     |
| `host_failovers` в отчёте об ошибке            | Сколько раз за сеанс переезжали; часть `400` гасится до фатала    |

Ручная проверка:

```bash
UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"
# 1. плейлист: найти vkId нужной серии и озвучки
curl -s "https://plapi.cdnvideohub.com/api/v1/player/sv/playlist?pub=745&id=61316&aggr=mali" \
  -H "Referer: https://ru.yummyani.me/" -A "$UA" | python3 -m json.tool | head -40
# 2. ссылки и резервный узел
curl -s "https://plapi.cdnvideohub.com/api/v1/player/sv/video/13352552323824" \
  -H "Referer: https://ru.yummyani.me/" -A "$UA" | python3 -m json.tool
# 3. живость узла (206 — жив, 400 — отказал)
curl -s -o /dev/null -A "$UA" -H "Range: bytes=0-65535" "<mpegFullHdUrl>" -w "%{http_code}\n"
# 4. та же подпись на резервном узле
curl -s -o /dev/null -A "$UA" -H "Range: bytes=0-65535" \
  "$(echo '<mpegFullHdUrl>' | sed 's|vd752\.okcdn\.ru|vd591.okcdn.ru|')" -w "%{http_code}\n"
```

Момент отказа узла непредсказуем (6-я минута, 38-я, а на 35-й ещё жив), по одному прогону выводов
делать нельзя. Чтобы получить отказ на устройстве, нужно поставить серию на паузу на несколько минут
и снять с паузы: плеер переоткроет соединение.

## 9. Выбор элемента плейлиста

`selectCvhItem` (`CvhPlaylistItem.kt`, тест `CvhPlaylistSelectionTest`) работает с плоским
плейлистом: все озвучки всех серий одним списком.

- `isSerial = false` (фильм): фильтр по номеру серии не применяется, `episode` может быть `null`.
- Озвучка ищется в три ступени, потому что `dubbing_code` не всегда равен `voiceStudio` (студия
  приезжает транслитом, `ort` против `ОРТ`, а в плейлисте её пишет только метка `dubbing`, например
  «Озвучка Дубляж ОРТ», префикс `Озвучка ` срезается):
  `voiceStudio == dubbing_code` → `"voiceType voiceStudio" == label` → `voiceStudio == label`.
- Если ничего не подошло, берётся первый кандидат по серии: чужая озвучка лучше пустого экрана.
- `voiceStudio` у субтитровых дорожек приходит как JSON `null`, `optString` превратил бы его в
  строку `"null"`, поэтому используется `optStringOrEmpty`.

Оба запроса идут через `fetchJsonWithRetry`: один повтор через 700 мс. HTTP-ошибка бросает
исключение и не маскируется под `JSONException` на HTML-теле.

## Замеры

### Отказ узла (06.10.2026)

Вне приложения, тем же путём, что у плеера (`plapi` → playlist → video → mp4 с Chrome-UA). Тайтл
`anime_id=61316`, `vkId=13352552323824`, 1080p: 160 МБ / 1424 с.

Что обрывов не даёт:

| Проверка                                                      | Результат                      |
|---------------------------------------------------------------|--------------------------------|
| Полный слив файла                                             | 160 МБ за 15 с, без ошибок     |
| Три слива по одной подписи                                    | 480 МБ, без ошибок             |
| Чтение пачками с простоем сокета 45 с (буфер SMALL)           | 100 %, 15 минут, без обрывов   |
| Чтение пачками с простоем сокета 120 с (буфер LARGE)          | 100 %, 52 минуты, без обрывов  |
| 26 переоткрытий соединения за 25 минут (как после паузы/seek) | все `206`                      |
| 40 запросов подряд по одной подписи                           | все `206`, лимита по числу нет |
| HTTP/2                                                        | ALPN отдаёт только `http/1.1`  |

Что происходит: ссылка перестаёт принимать новые соединения и отвечает

```
HTTP/1.1 400 Bad Request
Content-Length: 1
Connection: close
```

- Ловилось на ~6-й и ~38-й минуте жизни ссылки, третья в 35 минут была жива. Закономерности по
  возрасту нет, `expires` стоит на +24 часа.
- Открытое соединение докачивает файл до конца.
- Та же подпись на `failoverHost` отдаёт `206` (воспроизведено на обеих умерших ссылках): умирает
  узел, а не подпись.

### User-Agent (07.10.2026)

На каждый UA свежая ссылка, по три запроса, контрольный запрос эталонным UA до и после (строки с
мёртвым контролем отброшены, узел в тот момент мигал).

| User-Agent запроса                                 | Ответ |
|----------------------------------------------------|-------|
| Chrome 146–149 на Windows                          | `206` |
| Chrome 146–149 на Linux (`X11`)                    | `206` |
| Chrome 125 на Android с `Mobile` (Pixel)           | `206` |
| Chromecast / Google TV (`… CrKey/1.56`, синтетика) | `206` |
| `ExoPlayerLib/…`                                   | `206` |
| Chrome 146–149 на macOS                            | `400` |
| Chrome на Android без `Mobile`                     | `400` |
| Safari, Firefox, `okhttp/4.12.0`, пустой, `abc`    | `400` |

Раньше UA выбирался `random()` из пула на 12 строк, треть которых были macOS: примерно каждый третий
запуск не мог играть CVH до перезапуска.
