# Плеер Alloha

Alloha не отдаёт прямую ссылку на поток. Он отдаёт живую подписанную сессию: набор анти-бот
заголовков и master-URL с токеном в пути и TTL. Сессию нужно добыть, поддерживать и проксировать,
поэтому в `feature/player/data/.../extractor/alloha/` несколько файлов, а не один экстрактор, как у
Kodik.

Пункты «Нельзя» — инварианты: места, где очевидная оптимизация ломает воспроизведение. Все
подтверждены замерами на устройстве.

Референс для сверки: https://github.com/emilumiq/alloha-parser-kotlin

Большое спасибо за отличный код и пример работы с alloha <3

## 1. Схема

```
PlayerViewModel → PlayerStreamHandler.resolve() → OpenAllohaStreamSessionUseCase
  → DefaultPlayerStreamRepository.openAllohaSession()
      AllohaPlaybackSessionManager.find(iframeUrl)    переиспользование живой сессии
      AllohaExtractor.openSession()
        AllohaWebViewPool.acquire()                   старт Chromium, самый дорогой шаг
        wrapperHtml(iframeUrl) → loadDataWithBaseURL  JS перехватывает XHR/fetch/WebSocket
        parseSources(bnsi)                            озвучки, качества, субтитры
        LiveAllohaStreamSession                       состояние и ротация
          AllohaStreamProxy                           loopback HLS-прокси

Media3 играет с http://127.0.0.1:PORT/master.m3u8?token=…
```

Страница плеера живёт в скрытом WebView, её сетевой стек перехватывается из JS, добытые URL и
заголовки отдаются локальному прокси, через который играет Media3. CDN отдаёт сегменты только с
анти-бот заголовками сессии, а Media3 их не поставит, и они меняются при ротации. Поэтому все URL в
плейлисте переписываются на loopback, а заголовки подставляет прокси.

## 2. Файлы

| Файл                               | Назначение                                                             |
|------------------------------------|------------------------------------------------------------------------|
| `AllohaExtractor`                  | Оркестратор: WebView, JS-мост, ожидание сигналов, таймауты             |
| `AllohaWrapperScript`              | HTML/JS-обёртка, перехват XHR/fetch/WebSocket                          |
| `AllohaResponseParsing`            | Разбор `bnsi`: озвучки, лестница качеств, субтитры                     |
| `LiveAllohaStreamSession`          | Состояние живой сессии, поколения, атомарная ротация                   |
| `AllohaStreamProxy`                | Loopback HLS-прокси: заголовки, кэш, префетч, восстановление после 403 |
| `AllohaWebViewPool`                | Один прогретый WebView                                                 |
| `AllohaAssPositionFix`             | Обход бага Media3 с `MarginV/L/R` в ASS/SSA                            |
| `AllohaSourceUnavailableException` | Сессия открылась, но источника для этой озвучки/серии нет              |

## 3. Формат `bnsi`

Страница плеера запрашивает `…/bnsi/…` и получает JSON. Это источник правды о том, какие озвучки
есть у серии: каталог сайта может ошибаться.

```jsonc
{
  "hlsSource": [
    {
      "audioId": "10",
      "label": "(Russian) AniLibria",
      "default": true,
      "quality": {
        "1080": "https://cdn-a/…/master.m3u8 or https://cdn-b/…/master.m3u8",
        "720":  "…"
      }
    }
  ],
  "tracks": [
    { "kind": "captions", "label": "Русский", "language": "rus", "src": "//…/sub.vtt" }
  ]
}
```

`parseSources()`:

- `hlsSource[]` даёт список озвучек, у каждой своя лестница качеств.
- У качества может быть два CDN через ` or `. Берётся первый, второй wrapper-JS использует как
  браузерный fallback при 403/500/503.
- Метки качеств нормализуются (`1080` → `1080p`), сортировка по числу в метке.
- Если `audioId` нет, id равен индексу, чтобы дорожка осталась выбираемой.
- `//host/path` → `https://host/path` (`normalizeStreamUrl`).
- `tracks[]` с `kind == "captions"` — субтитры, формат по расширению `src`.
- Пустой `hlsSource` или ноль качеств → `AllohaSourceUnavailableException`: у этой озвучки нет
  источника, пользователю предлагается выбрать другую.

## 4. Заголовки сессии

Копятся в JS из `setRequestHeader` и `fetch(init.headers)` страницы, часть синтезируется.

| Заголовок                  | Откуда                                             | Роль                                               |
|----------------------------|----------------------------------------------------|----------------------------------------------------|
| `authorizations`           | `setRequestHeader` страницы                        | Токен на фильм, стабилен в пределах сессии         |
| `accepts-controls`         | `setRequestHeader`, обновляется из `config_update` | edge_hash, по нему CDN расшифровывает токен в пути |
| `borth`                    | `setRequestHeader`                                 | Одноразовый nonce на конкретный запрос             |
| `origin`, `referer`        | синтез из `w.location.origin`                      | Обязательны                                        |
| `user-agent`               | настройки WebView                                  | Десктопный Chrome, стабилен на процесс             |
| `sec-fetch-dest/mode/site` | синтез                                             | Как у браузера                                     |

Прокси не пересылает (`BLOCKED_FORWARD_HEADERS`):

- `host`, `connection`, `content-length`, `transfer-encoding`, `accept-encoding` — ими владеет
  OkHttp;
- `cookie` — ставится отдельно из `CookieManager`;
- `x-requested-with`, `content-type` — не нужны;
- `borth` — одноразовый, повтор даёт периодический 403 `token_decrypt`.

Нельзя полагаться только на перехваченные заголовки. Хуки видят лишь то, что страница ставит явно, а
`Accept-Language` и client hints (`sec-ch-ua`, `-mobile`, `-platform`) браузер добавляет сам. Без
них CDN видит Chrome-UA без единого client hint и отвечает 403 `client_blocked`, хотя запрос плеера
на тот же master проходит. Прокси добавляет их явно и выводит из предъявляемого UA
(`DEFAULT_ACCEPT_LANGUAGE`, `CHROME_VERSION`, `platformHintFor`).

## 5. Добыча сессии

### 5.1 Загрузка обёртки

`loadDataWithBaseURL(baseUrl = "https://alloha.yani.tv/", wrapperHtml, …)`.

- `baseUrl` обязан быть хостом Alloha: страница-обёртка становится same-origin с iframe, иначе
  `iframe.contentWindow.document` недоступен и перехват невозможен.
- `webView.getUrl()` возвращает `about:blank`, это нормально: он отдаёт `historyUrl`, пятый аргумент
  `loadDataWithBaseURL`, а мы передаём `null`. По нему нельзя диагностировать «страницу затёрли».

### 5.2 Установка хуков

- Хуки ставятся опросом `contentWindow` каждые 20 мс, а не на `iframe.onload`. К `load` скрипты
  страницы уже отработали и её запрос `/bnsi/` мог завершиться, хук его не увидит и извлечение
  зависнет на весь таймаут. Опрос ограничен 8 секундами и останавливается после первой успешной
  установки, `onload` остаётся страховкой для повторной навигации.
- Метка защиты от повторной установки стоит на самой обёрнутой функции
  (`w.XMLHttpRequest.prototype.open.__alloha`), а не на объекте окна: expando на window не
  удерживается, и опрос оборачивает хуки каскадом. Свежий документ после навигации приносит нативный
  `open()` без метки, поэтому одна метка и защищает от дублей, и переустанавливает хуки после
  навигации.
- Опрос пропускает окно с `location.href === 'about:blank'`: это промежуточное состояние iframe.

### 5.3 Хуки обёртки

| Хук                                                          | Что делает                                                                                                             |
|--------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `XMLHttpRequest.prototype.open`                              | На `load`: `/bnsi/` → `onReady`; `master.m3u8` из `responseURL` → `onM3u8Refreshed`. `loadend` — запасной перехват URL |
| `XMLHttpRequest.prototype.setRequestHeader`                  | Копит анти-бот заголовки                                                                                               |
| `fetch`                                                      | То же, плюс браузерный fallback: при 403/500/503 повтор на втором CDN из `"url1 or url2"`                              |
| `WebSocket`                                                  | `hookSocket()` читает `config_update` (edge_hash + ttl), шлёт heartbeat `{type:'playing'}` раз в 25 с                  |
| `Object.defineProperty(document,'visibilityState'/'hidden')` | Подделка видимости на обоих документах: WebView не прикреплён к окну                                                   |
| Опрос 1500 мс                                                | Держит плеер играющим, жмёт play, только если на корне `.allplay` нет класса `allplay--playing`                        |
| Скан текста                                                  | `/озвучка\s*недоступна/i` → `onDubbingUnavailable`                                                                     |

Нельзя блокировать запрос master у плеера страницы. Токен в пути не одноразовый: прокси
перезапрашивает тот же URL, и это работает. Блокировка оставляла плеер страницы без медиа
(`readyState=0`), он рвал свой WebSocket через ~2.5 с, `config_update` не приходил, и каждая ротация
упиралась в 8-секундный таймаут.

Кнопка play: `button.allplay__controls__item.allplay__control`, в играющем состоянии с
`aria-label="Пауза"`. Селектора `.allplay__play-btn` в текущей вёрстке нет. Клик по реальной кнопке
во время воспроизведения ставит на паузу, поэтому он под условием отсутствия `allplay--playing`.

### 5.4 Колбэки моста

`onReady(bnsi, headers)`, `onM3u8Refreshed(url, headers)`, `onConfigUpdate(edgeHash, ttl, headers)`,
`onStreamHeaders(headers)`, `onDubbingUnavailable()`, `onLog(message)`.

### 5.5 Таймауты

| Константа                | Значение | Смысл                                                                         |
|--------------------------|----------|-------------------------------------------------------------------------------|
| `NO_SIGNAL_TIMEOUT_MS`   | 10 с     | Не пришло ничего → перезагрузить обёртку. В норме bnsi приходит за 0.15–0.9 с |
| `MAX_WRAPPER_RELOADS`    | 2        | Итого три попытки внутри общего потолка                                       |
| `MASTER_WAIT_TIMEOUT_MS` | 6 с      | bnsi есть, подписанного master нет → отдать сессию на захваченном             |
| `TIMEOUT_MS`             | 30 с     | Общий потолок, дальше `Failed`                                                |

Сессия отдаётся, когда есть и разобранные источники, и подписанный master (`deliverWhenReady`).

## 6. Жизнь сессии

`LiveAllohaStreamSession` хранит заголовки, master-URL, TTL и `generation` — счётчик, по которому
прокси узнаёт, что сессия сменилась.

Расписание обновления (`PlayerAllohaSessionHandler`): сон до `expiresAt − SESSION_REFRESH_LEAD_MS`
(20 с), `refresh()`, затем опрос нового TTL каждые 500 мс до `SESSION_ROTATION_WAIT_MS` (15 с).

Ротация (`refresh()`) перезагружает обёртку, новые сигналы складываются в `staging`, а живое
состояние продолжает обслуживать запросы старым, ещё валидным токеном. Коммит атомарный, иначе
прокси часть окна работал бы на полуобновлённой сессии, что Media3 показывает как буферизацию.
Форсированный коммит — `STAGED_COMMIT_TIMEOUT_MS` (8 с).

`config_update` через сокет страницы не приходит. Сокет страницы живёт ~2.5 с и закрывается с
`code=1005`, heartbeat раз в 25 с не успевает уйти. Это не зависит от наличия медиа: сокет рвётся и
при реально играющем плеере. Поэтому `accepts-controls` не ротируется, а TTL равен угаданному
fallback в 120 с. Флаг `sawConfigUpdate` не даёт армировать ожидания на этот сигнал, пока его не
было ни разу: ни `STAGED_COMMIT_TIMEOUT_MS` (8 с на ротацию), ни `HOST_CHANGE_CONFIG_WAIT_MS` (10 с
при смене хоста CDN).

### Выбор дорожки

`selectAudioTrack()` нельзя звать до старта воспроизведения. Он перезаписывает `masterUrl` значением
из `masterFor()` — сырым bnsi-URL — и затирает правильно подписанный master, что даёт 403
`token_decrypt` на части CDN. Поэтому восстановление сохранённой дорожки живёт в `PlayerViewModel`
(после старта), а не в `PlayerStreamHandler` (до публикации URL).

### Переиспользование

`AllohaPlaybackSessionManager.find(iframeUrl)` отдаёт живую сессию, `release(immediately = false)`
откладывает закрытие на `CONFIGURATION_CHANGE_GRACE_MS` (10 с), чтобы поворот экрана или быстрый
перезаход не пересоздавали WebView.

## 7. Прокси

URL, которые он выдаёт:

```
http://127.0.0.1:PORT/master.m3u8?token=<uuid>[&audio=<id>][&quality=<label>]
http://127.0.0.1:PORT/proxy?url=<base64url>&token=<uuid>
```

`token` — секрет на сессию: loopback-порты не изолированы между приложениями одного пользователя.
Озвучка и качество передаются в URL, поэтому переключение — это подмена MediaItem на той же сессии.

Нельзя:

- Биндить на `getLoopbackAddress()`. Он может вернуть `::1`, и на `127.0.0.1`, куда указывают
  выдаваемые URL, никто не слушает: Media3 получает `ConnectException` на каждый запрос. Бинд идёт
  на
  `InetAddress.getByName("127.0.0.1")`. Проверка: `cat /proc/net/tcp6` показывает LISTEN, а
  `/proc/net/tcp` пуст.
- Резолвить имена и отдавать `404` на сетевых путях. `404` входит в `FATAL_RESPONSE_CODES`
  (`400, 401, 403, 404, 410`) в `PlayerLoadErrorHandlingPolicy`: Media3 его не ретраит и сразу валит
  воспроизведение. Всё сетевое проходит через `fetchWithRecovery` и возвращается ретраибельным
  `503`.
  `404` — только на заведомо кривой запрос.

Обработка запроса:

1. Разбор query одним проходом, сверка `token`.
2. `/master.m3u8` → `masterProvider(audio, quality)`; иначе base64-декод `url=`.
3. Плейлист → `servePlaylist` (переписывание URL на loopback); сегмент → `serveSegment`.
4. Кэш сегментов `CACHE_CAPACITY = 4`, префетч `PREFETCH_COUNT = 2`, дедупликация параллельных
   запросов через `computeIfAbsent`. Range-запросы кэш обходят.

### Восстановление запроса

| Шаг | Что делает                                                                                   |
|-----|----------------------------------------------------------------------------------------------|
| 1   | Точный повтор на свежем соединении при 403 (`Connection: close`, `evictAll`)                 |
| 2   | Перезапись пути под текущий master (`rewriteToCurrentPath`) для сегментов прошлого поколения |
| 3   | Эскалация к `refresh()` сессии, single-flight                                                |
| 4   | Удержание запроса до `SEGMENT_HOLD_FOR_REFRESH_MS` (14 с) и повтор на новом поколении        |
| 5   | Для `-a1.ts`/`-a2.ts` пустой 188-байтовый TS-пакет, для прочих `503`                         |

Маркеры отказа в `X-VD`: `session_blocked`, `token_decrypt`, `client_blocked`. При `token_decrypt`
удержание пропускается: `refresh()` перезагружает тот же WebView и возвращает тот же edge_hash,
ожидание бесполезно, лучше сразу эскалировать к свежей сессии.

`onSessionRotated` пересобирает `OkHttpClient` и `ConnectionPool`, а не просто зовёт `evictAll()`:
evict закрывает только простаивающие соединения, занятое вернётся в тот же пул и достанется новой
сессии. Кэш сегментов и запросы в полёте сохраняются: они авторизованы прошлым токеном и валидны, а
их сброс виден как стоп прямо на ротации.

### Удержание и буфер

Пока запрос висит, ExoPlayer доигрывает буфер, поэтому запас в этот момент должен быть больше 14 с.
Гарантированный минимум запаса — `min` профиля, а не `max` (см.
[player-buffering.md](player-buffering.md)). Удержание должно оставаться короче 16-секундного
read-таймаута `PlayerDataSourceFactory`.

## 8. Лестница отказа

Снизу вверх:

| Уровень   | Владелец                      | Что делает                                                                        |
|-----------|-------------------------------|-----------------------------------------------------------------------------------|
| Сегмент   | `AllohaStreamProxy`           | Ретрай 403 → rewrite пути → удержание 14 с → 503                                  |
| Сессия    | `LiveAllohaStreamSession`     | `refresh()`, staged-коммит, ожидание ≤ 20 с                                       |
| Поток     | `PlayerViewModel`             | `startAllohaPlaybackRecovery`: свежая сессия, задержка 1 с                        |
| Попытки   | `PlayerAllohaRecoveryHandler` | `MAX_ATTEMPTS = 4`, дальше ошибка с действиями                                    |
| Подсказка | `PlayerViewModel`             | Через `ALLOHA_RECOVERY_HINT_DELAY_MS` (15 с) предложить сменить плеер или озвучку |

У остальных источников другой путь: `PlayerPlaybackRetryHandler` с `MAX_ATTEMPTS = 5` и тихий
реконнект (`SILENT_RETRY_COUNT = 20` в `PlayerLoadErrorHandlingPolicy`). Для Alloha тихий реконнект
выключен, у неё своё восстановление.

## 9. Загрузки

Оффлайн-путь идёт через тот же прокси, но со своей сессией.

- `AllohaDownloadStrategy.openLiveSession()` открывает сессию с `reusePlaybackSession = false`,
  чтобы загрузка и просмотр не мешали друг другу. TTL fallback короче: 55 с против 120 с у
  воспроизведения.
- `VideoDownloadWorker` берёт `liveSession.initialStream.url` (loopback-URL прокси) и передаёт
  пустые заголовки (`downloadHeaders = emptyMap()`): заголовки сессии подставляет прокси. Воркер
  держит таймер обновления сессии и закрывает её в конце.
- `decorateHeaders(originOverride = "https://alloha.yani.tv")` применяется только там, где живой
  сессии нет, то есть для прогрессивных не-HLS файлов, которые качаются напрямую.
- Ключи кэша — `RotatingHlsCacheKeyFactory`: манифест перечитывается на каждой ротации (его URL
  эфемерный), сегменты кладутся под стабильное имя с префиксом загрузки, чтобы пережить смену
  подписи.
- `LiveAllohaStreamSession.directStream` (прямые CDN-URL без прокси) к загрузкам не относится, он
  нужен только разовому резолву в `AllohaExtractor.extract()`.

## 10. UI

- Панель и таб дорожек Alloha показываются только когда источник действительно Alloha
  (`usesAlloha && alloha.hasAudioChoice`). Без этого таб с подписью «Alloha» (строка
  `player_mobile_audio_track`) появлялся бы и у Kodik-потоков с выбираемыми дорожками. См.
  `TvExoPlayerView` и `MobileNativePlayer`.
- Списки дорожек чистятся в `PlayerSourceStreamHandler.preparingStreamLoad` и
  `preparingStreamResolve`, чтобы не пережить смену источника. Ветка `preserveCurrentStream = true`
  их сохраняет: это восстановление той же сессии на месте.

## 11. Отладка

Логи идут через `AnalyticsTracker.log`: в debug в logcat, в release теги `AllohaExtractor` и
`AllohaStreamProxy` (список — `PersistedLogTags`) пишутся в файл логов приложения (Настройки → О
приложении → Поделиться логами, при включённой «Настройки → Общие → Запись логов»). Остальные теги в
release no-op. Ссылки в строках усекаются до хоста (`AppLogDiagnosticSink`). Подробные логи прокси
по запросам (`verbose`) в release выключены.

```bash
adb shell setprop log.tag.AllohaExtractor DEBUG
adb shell setprop log.tag.AllohaStreamProxy DEBUG
```

| Маркер                            | Значение                                                                                             |
|-----------------------------------|------------------------------------------------------------------------------------------------------|
| `bnsi audioTracks=[…]`            | Источники разобраны                                                                                  |
| `master refreshed`                | Пойман подписанный master                                                                            |
| `ready headers=…`                 | Сессия собрана; поколение, host, ttl и отпечатки заголовков                                          |
| `Proxy started port=…`            | Готово, дальше играет Media3                                                                         |
| `WebSocket hooked`                | Страница открыла сокет. Не показывает, что JS запустился: вызывается из `hookSocket()`               |
| `no signal after 10000ms`         | Обёртка ничего не отдала, идёт перезагрузка                                                          |
| `session timed out after 30000ms` | Провал добычи; `streamReady`/`refreshedMaster` показывают, чего не хватило                           |
| `staged session commit timed out` | Ротация не собралась за 8 с; `ready/master/config` — что не пришло                                   |
| `CDN failure code=403 xVd=…`      | `client_blocked` → заголовки; `token_decrypt` → master протух; `session_blocked` → сессия отвергнута |
| `outgoing …`                      | Точный набор заголовков, ушедших на CDN (только под `verbose`)                                       |

## Замеры

- Хуки на `iframe.onload`: страница грузилась 46 раз из 46, а bnsi перехватывался в 8. Отсюда опрос
  `contentWindow` (§5.2).
- Метка на объекте окна: опрос обернул хуки 526 раз каскадом и убил плеер (§5.2).
- Запрос master из WebView проходил, а запрос прокси на тот же URL получал 403 `client_blocked`: не
  хватало `Accept-Language` и client hints (§4).
- CDN режет клиента по объёму открытых сессий. После нескольких десятков запусков подряд с одного IP
  начинают сыпаться `client_blocked`/`session_blocked`, и статистика меряет блокировку, а не код.
  Между сериями нужны паузы, прогоны с ненулевым `blocked=`/`tokdec=` отбрасываются. Разброс частоты
  отказов между сериями на одном коде доходил до 42–100 %, поэтому выводы по 10–12 прогонам
  ненадёжны.
