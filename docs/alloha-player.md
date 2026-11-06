# Плеер Alloha

Alloha открывается **обычным HTTP, без WebView, прокси и сессионного объекта**: страница плеера не
запускается, список озвучек с лесенками качеств достаётся из `POST /bnsi`. Но одних ссылок мало: CDN
отрезает клиента через 5-8 минут (`403`, `X-VD: session_blocked`), если рядом с воспроизведением не
идёт **«живая сессия»**, которую сайт держит у себя: WebSocket, обновляющий `edge_hash`, и
HTTP-отчёты
`/events` и `/stat`. Эту сессию экстрактор держит сам, по образцу YummyDroid (§7).

Журнал расследования (что пробовали, цифры прогонов, чужие репозитории) вынесен в
[alloha-session-investigation.md](alloha-session-investigation.md). Здесь только то, как оно
устроено
сейчас.

Референсы для
сверки: [emilumiq/alloha-parser-kotlin](https://github.com/emilumiq/alloha-parser-kotlin),
[HnDK0/external-sources](https://github.com/HnDK0/external-sources) (`ru/yummyanime.lua`,
`docs/hoster-implementations.md`: перестановка
`Borth`), [AsfhtgkDavid/davy-parsers](https://github.com/AsfhtgkDavid/davy-parsers),
[saltek1995/YummyDroid](https://github.com/saltek1995/YummyDroid) (`AllohaPlaybackSession`,
`AllohaBootstrapResolver`), [queukat/YummyKodik](https://github.com/queukat/YummyKodik),
[Kirill9732/alpac_source](https://github.com/Kirill9732/alpac_source). У YummyDroid нет файла
LICENSE:
их код не копируем, реализация своя по наблюдаемому поведению.

## Статус (06.11.2026)

- Профиль «как YummyDroid, без WebView» (§7): один прогон на эмуляторе, серия 5 тайтла 1468, **14
  минут (840+ с) без `session_blocked`**, весь прогон `/events` и `/stat` отвечали 200. Прежний
  диапазон блока 270-510 с пройден. Коммит `be4f31527`.
- Это одно наблюдение, не гарантия: на других тайтлах, CDN-хостах и приставках не проверено. Какой
  из
  элементов профиля решающий, не выяснено: по отдельности они блок не снимали.
- Не проверено: приставки с кастомными прошивками, другие CDN (раньше часть отвечала 403
  `token_decrypt` на сыром URL), долгие серии (больше 14 минут), возобновление после паузы или
  перемотки.

## 1. Схема

```
PlayerViewModel → PlayerStreamHandler.resolve() → ResolvePlayerStreamUseCase
  → DefaultPlayerStreamRepository.resolve()          общий кэш resolve, как у CVH/Kodik
      AllohaExtractor.extract()
        AllohaHttpSessionOpener.open()               GET страница → Borth → POST bnsi (+ ключ Authorizations из app.js)
        parseSources(bnsi)                           озвучки с лесенками, субтитры, зеркала
        withSocket()                                 открывает WS (pnr/pnk), ждёт первый edge_hash
            AllohaPlaybackSocket                     playback_start/init, resumed/paused/seeked/playing, config_update → edge_hash
            AllohaViewReporter                       POST /events и /stat
        buildAllohaStream()                          Stream со всеми озвучками и заголовками CDN

Media3 играет прямую CDN-ссылку; PlayerDataSourceFactory на каждом запросе подставляет свежий
Accepts-Controls (AllohaEdgeHash.current) и считает номер сегмента и байты
```

В отличие от остальных источников у Alloha есть живое состояние: WS и репортёр работают, пока играет
серия. Объект один на процесс (`AllohaExtractor` синглтон, поле `activeSocket`/`activeReporter`):
открытие новой сессии закрывает предыдущую (`withSocket` вызывает `close()` у старых).
Воспроизведение
одно за раз. Лесенки всех озвучек лежат в `Stream` (`AllohaAudioTrack.qualities`), поэтому смена
озвучки это подмена ссылки в состоянии плеера (`withAllohaAudio`), без нового `bnsi` и без новой
сессии. WS при этом не получает нового `playback_start` с другим `track_id` (в нём остаётся озвучка
на момент открытия); у YummyDroid такое сообщение уходит при смене озвучки или качества. Не
проверено,
влияет ли это на сессию (см. «Открытое»).

## 2. Файлы

Все в `feature/player/data/.../extractor/alloha/`, если не сказано иное.

| Файл                                                   | Назначение                                                                                                 |
|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `AllohaExtractor`                                      | Оркестратор: открыть сессию с повтором, поднять WS и репортёр, собрать поток; флаги профиля, выбор UA      |
| `AllohaHttpSessionOpener`                              | Один HTTP-цикл: страница плеера, ключ `Authorizations`, `Borth`, `POST /bnsi/movies/{id}`, проверка зеркал |
| `AllohaBorth`                                          | `viewporti` → заголовок `Borth` (fingerprint + три перестановки символов)                                  |
| `AllohaFingerprint` | Алгоритм fingerprint сайта: SHA-256 от признаков, canvas и WebGL по `seed` |
| `AllohaClientProfile` | Профиль установки: fingerprint, экран, платформа; `seed` в `SharedPreferences` |
| `AllohaCdnHeaders`                                     | Заголовки CDN: Origin/Referer, UA, Accept-Language, `sec-fetch-*`, client hints                            |
| `AllohaResponseParsing`                                | Разбор `bnsi`: озвучки с лесенками, субтитры, зеркала                                                      |
| `AllohaMirrorPicker`                                   | Какое из зеркал `url1 or url2` играть (проба первого, кэш вердикта по хосту)                               |
| `AllohaPlaybackSocket`                                 | WebSocket сессии: рукопожатие, состояние плеера из позиции, пульс, `edge_hash`                             |
| `AllohaViewReporter`                                   | `POST /events` (стартовая пачка, heartbeat) и `POST /stat` (вехи просмотра)                                |
| `AllohaStreamFactory`                                  | `Stream` из открытой сессии: озвучка по умолчанию, её качество                                             |
| `AllohaSourceUnavailableException`                     | Открылось, но источника для этой озвучки/серии нет                                                         |
| `core/common/.../AllohaEdgeHash`                       | Глобальное состояние одного воспроизведения: живой `edge_hash`, номер сегмента, байты                      |
| `feature/player/ui-common/.../PlayerDataSourceFactory` | Подмена `Accepts-Controls` на каждом запросе, счётчик байтов                        |

В presentation: `AllohaSourceBehavior` (восстановление, сохранённые дорожки),
`PlayerAllohaRecoveryHandler`, `PlayerAllohaTrackPreferenceHandler`, `PlayerAllohaAudioUtils`
(`withAllohaAudio`).

Тесты: `AllohaBorthTest` (перестановка `Borth` против независимой реализации), тесты разбора ответа.

## 3. Открытие

`AllohaHttpSessionOpener.open(iframeUrl)`, блокирующий, вызывается с IO-диспетчера:

1. `GET iframeUrl` с `Referer: https://ru.yummyani.me/`, UA сессии и `Accept-Language`. Страница
   выдаётся только тому, кто пришёл с сайта, который её встраивает.
2. Из HTML регулярками:
    - `<meta name="viewporti" content="…">` — зерно этой загрузки страницы;
    - `const fileList = JSON.parse("…")` → `active.id` — числовой id для пути `bnsi`;
    - `<script src=".../app.<хеш>.js">` — скрипт плеера, из него берётся ключ `Authorizations` (§5);
    - `userParam = {token:'…', domain:'…'}` — токен и домен для `/events` и `/stat`;
    - `token` для `bnsi` берётся из query самой iframe-ссылки.
3. `Borth = <fingerprint>|<перестановка viewporti>` (`allohaBorthHeader`, §4).
4. `POST https://alloha.yani.tv/bnsi/movies/{activeId}`, форма `token`, `av1=false`, `autoplay=0`,
   `audio=`, `subtitle=`; заголовки `Borth`, `Referer` (iframe), `Origin`, `X-Requested-With:
   XMLHttpRequest`, `Accept-Language`, UA. `av1=false`: AV1 на приставках не декодируется. Пустой
   `audio` значит «все озвучки»: сервер отдаёт лесенки всех.
5. Ответ в `parseSources()`; для каждого качества с двумя зеркалами `AllohaMirrorPicker` проверяет
   первое запросом к плейлисту (статус, таймаут 4 с) и при отказе берёт последнее.
6. Из ответа выделяется `AllohaSocketTicket(pnr, pnk)` для WS и `AllohaViewTicket(token, domain,
   fileId)` для репортёра.

Сбой сети повторяется один раз (`MAX_ATTEMPTS = 2`, пауза 500 мс). Отсутствие источника
(`AllohaSourceUnavailableException`) не повторяется.

## 4. Borth и fingerprint

`Borth` — подпись запроса `bnsi`: `<64-hex fingerprint>|<viewporti после трёх перестановок>`.

Перестановка не шифрование, а смена порядка символов тремя проходами (`zy` → `zZ` → `z9` в
`app.js`):

1. по длине двоичной записи индекса (группы собираются от большей к меньшей);
2. по числу нулей в конце двоичной записи индекса (от меньшей к большей, индекс 0 в верхней группе);
3. шагом 2 по модулю первого простого числа больше длины.

Она должна совпасть символ в символ: `AllohaBorthTest` сверяет с независимой реализацией, и ту же
перестановку держит скрипт `alloha-bootstrap.js` у YummyDroid. Контрольный Python-скрипт на Mac (в
`scratchpad`, не в проекте) получал по ней 200 от `bnsi`.

**Fingerprint.** Настоящий считает `Tw()` в `app.js`: SHA-256 от строки `values.join('||')`, в
которой
по порядку: UA без версий, часовой пояс, размер экрана `max x min`, язык, `deviceMemory`,
`hardwareConcurrency`, canvas 2D `toDataURL()` (текст `fp`), WebGL vendor|renderer, длина буфера
`OfflineAudioContext`. Нативно его получить нельзя (canvas, WebGL, аудио). Что видел сервер:

| Значение в `Borth`                                  | Реакция                                         |
|-----------------------------------------------------|-------------------------------------------------|
| нули, константы davy/YummyKodik/Aladin              | `bnsi` 200, ссылки играют на старте             |
| случайный 64-hex (в `Borth` или `Accepts-Controls`) | `client_blocked` на первом `master.m3u8`        |
| настоящий fingerprint устройства                    | проходит на старте, но блок через ~6 минут тоже |

**Свой fingerprint на установку** (`AllohaFingerprint.kt`, `AllohaClientProfile.kt`). Приложение считает его
тем же алгоритмом, что сайт, только признаки берёт с устройства: UA WebView без версий, часовой пояс,
размер экрана в CSS-пикселях (`max x min`, из `displayMetrics`), `ru-RU`, `deviceMemory` (RAM, округлённая
вниз до степени двойки, максимум 8), число ядер, длина аудио-буфера 44100. Canvas и WebGL взять нельзя, поэтому
они подставляются: canvas это псевдослучайная строка вида `data:image/png;base64,iVBORw0KGgo…` по `seed`,
WebGL-рендерер выбирается по `seed` из списка реальных (Mali, Adreno, PowerVR). `seed` случайное число,
создаётся при первом запуске и хранится в `SharedPreferences` (`alloha_client`/`seed`), поэтому fingerprint
стабилен между запусками и различается между установками. Те же размеры экрана, `devicePixelRatio` и
платформа (`Linux armv81`…) идут в `/events` и `/stat` (`AllohaClientProfile`), чтобы данные сходились.

Проверка (H14/H15): хеш, собранный по алгоритму из правдоподобных признаков, принят: первый манифест без
`client_blocked`, 840+ с без блока; так же с хешем, который приложение сгенерировало само (серия 8,
эмулятор). Чисто случайный 64-hex по-прежнему отбивается. Не проверено: много разных fingerprint с одного
IP, разные устройства и приставки.

## 5. Формат `bnsi` и ключ `Authorizations`

JSON — источник правды о том, какие озвучки есть у серии: каталог сайта может ошибаться.

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
  ],
  "pnr": "wss://alloha.yani.tv/ws/", "pnk": "…",
  "time": 1730000000000
}
```

`parseSources()`:

- `hlsSource[]` даёт список озвучек, у каждой своя лесенка качеств.
- У качества может быть два CDN через ` or `; выбор — `AllohaMirrorPicker`.
- Метки качеств нормализуются (`1080` → `1080p`), сортировка по числу в метке.
- Если `audioId` нет, id равен индексу, чтобы дорожка осталась выбираемой.
- `//host/path` → `https://host/path` (`normalizeStreamUrl`).
- `tracks[]` с `kind == "captions"` — субтитры, формат по расширению `src`.
- Пустой `hlsSource` или ноль качеств → `AllohaSourceUnavailableException`: у этой озвучки нет
  источника, пользователю предлагается выбрать другую.

`pnr`/`pnk` — адрес и ключ WebSocket сессии (§6). `time` — подсказка срока сессии; клиент её не
использует (рабочее соединение живёт дольше).

**Ключ `Authorizations`.** В `app.js` лежит длинная константа (340-420 символов `[a-zA-Z0-9]`; у
YummyDroid она вынимается из функции `Xk` и проверяется по хешу нормализованного бандла), которую
сайт шлёт к CDN как `Authorizations: Bearer <ключ>` рядом с `Accepts-Controls`. Имя скрипта несёт
хеш содержимого, поэтому ключ читается один раз на скрипт и кэшируется (`apiKeyCache`).

## 6. WebSocket сессии и `edge_hash`

Сайт держит открытым WS, пока играет серия. Сервер присылает `config_update` с `edge_hash`, который
плеер кладёт в заголовок `Accepts-Controls` каждого запроса к CDN. Хеш меняется каждые 2-6 минут;
устаревший CDN отклоняет `403 session_blocked`.

`AllohaPlaybackSocket(client, userAgent, origin, analytics, positionMs, guard, guardAsAcceptsControls)`:

- URL: `{pnr}?sid={pnk}&v=2.1&t={ms}`; заголовки `User-Agent`, `Origin`, `Accept-Language`; пинг 20
  с.
- Рукопожатие: `playback_start`, затем `init`. Схема сообщения:
  `{"type":…, "current_time": секунды, "resolution":"1080", "track_id":"<audioId>", "speed":1, "subtitle":-1, "ts": мс}`.
- Первый `edge_hash` ждётся до `FIRST_HASH_TIMEOUT_MS = 4 с`; без него поток всё равно отдаётся
  (сырые
  ссылки играли и без него, пока сессия не блокировалась).
- Каждый `config_update` с `edge_hash` пишется в `AllohaEdgeHash.current`; запросы к CDN читают его
  на
  лету, не из карты заголовков, замороженной при резолве.
- Переподключение: до 30 попыток, задержка `1.5^n` секунд, максимум 15 с.

**Состояние плеера выводится из позиции** . Приложение не передаёт экстрактору
«играет/пауза»: у запроса есть только `positionMs()`. Раз в секунду (`STATE_POLL_MS`) сокет
сравнивает
позицию с прошлой:

| Что видно                                  | Что отправляется                    |
|--------------------------------------------|-------------------------------------|
| позиция сдвинулась после стоянки           | `resumed`                           |
| позиция встала                             | `paused`                            |
| скачок больше 3 с (`SEEK_JUMP_MS`)         | `seeked` с новой позицией           |
| раз в 30 с (`HEARTBEAT_MS`), пока движется | `playing` с реальным `current_time` |

`current_time` везде реальная позиция в секундах. Это то, что делает сайт и YummyDroid
(`AllohaPlaybackSession.send`): `resumed`/`paused` при смене `playWhenReady`, `seeked`, `ended`,
`playing`
раз в 30 с только при воспроизведении. Прежние варианты считали время иначе (секунды с открытия,
номер сегмента `(seg-25)*6`, нули) и блока не снимали.

**Номер сегмента и байты.** `PlayerDataSourceFactory` на каждом запросе к CDN Alloha: подставляет `AllohaEdgeHash.current`, на успешном ответе добавляет
`Content-Length` в `AllohaEdgeHash.bytesLoaded`. Счётчик `bytesLoaded` нужен
репортёру (`bytesLoadedTotal`). Всё, что ходит к Alloha, использует `@AllohaOkHttp`, а сегменты `@StreamingOkHttp` (см. [network-and-auth.md](network-and-auth.md)); WS тоже на OkHttp. Cronet (TLS Chrome) раньше
использовался для CDN Alloha, но прогон на 20 минут без него прошёл без блока (H16), и он удалён
вместе с зависимостями `media3-datasource-cronet` и `play-services-cronet`.

## 7. Профиль клиента «как YummyDroid» (текущий)

Флагов больше нет: это единственный режим, ветки прежних экспериментов (davy, Aladin, Guard, пульс по
сегментам) удалены вместе с кодом.

Что получается в итоге:

- **UA** = `WebSettings.getDefaultUserAgent(context)`: строка UA системного WebView (Android Chrome
  Mobile), сам WebView не создаётся и JS не запускается. Выбирается один раз на процесс (ленивое
  поле
  `sessionUserAgent`; новый UA на каждый запуск выглядел бы с одного адреса как ферма ботов). Client
  hints (`sec-ch-ua` в порядке Android: Google Chrome, Chromium, Not)A;Brand) выводятся из версии и
  ОС в UA.
- **`X-Requested-With`** = имя пакета приложения на запросах к CDN: так делает WebView.
- **Заголовки CDN** (`allohaCdnHeaders`): `origin`, `referer`, `user-agent`, `accept-language`,
  `sec-fetch-dest/mode/site` (`empty`/`cors`/`cross-site`), `sec-ch-ua*`, плюс `authorizations` и
  `x-requested-with`, плюс живой `accepts-controls`. Без Origin CDN даёт 403; Chrome-UA без
  `Accept-Language` и client hints даёт 403 `client_blocked`.
- **`Accepts-Controls`** = `edge_hash` из WS.
- **Отчёты** `/events` и `/stat` включены.
- **Cookie** не нужны: на запись настоящего плеера ни `Set-Cookie`, ни `Cookie` не было.

### Что из YummyDroid не перенесено

У YummyDroid перед стартом отрабатывает скрытый «bootstrap» на WebView: документ с их
`alloha-bootstrap.js` считает настоящий fingerprint (canvas, WebGL, аудио), окружение `env`
(тип соединения, размеры экрана и окна, `devicePixelRatio`…) и результаты проверок возможностей
(`statType`, `statInfo`), а затем нативная часть продолжает уже без WebView. Мы **WebView не
используем** (решение владельца), поэтому размеры экрана, `devicePixelRatio` и платформа берутся из
`displayMetrics` и `Build` (`AllohaClientProfile`), а canvas, WebGL и результаты проверок возможностей
синтетические (§4). Если блок вернётся, первое подозрение: настоящий `env` и fingerprint.
Вернуться к WebView-bootstrap можно только решением владельца.

## 8. HTTP-отчёты `/events` и `/stat`

Сайт кроме WS шлёт отчёт о просмотре. `AllohaViewReporter` (запускается в `withSocket`, если у
страницы
есть `userParam`; закрывается вместе со следующей сессией и на `close()`):

**`POST /events`** (форма: `token` и `payload`, JSON):

- стартовая пачка по записи настоящего плеера: `ad_block_start`, `view_start` (`resumeFrom`,
  `startupQuality`, `startupAudio`), `startup`, `play`, `pause`, `playback_ready`, `ad_block_end`,
  `play`, `first_frame`, `file_load_complete`;
- затем `heartbeat` каждые 10 с (`bytesLoadedTotal`, `bytesLoadedDelta`, `playedSecondsDelta` из
  позиции, `isBuffering`, `isSeeking`, `playbackRate`), отправка пачкой раз в 30 с (каждый 3-й тик);
- вехи `view_percent` при 10/30/60/97 % длительности;
- при закрытии `pause` и `view_finish` (`watchTimeSec`, `completed`).

Конверт: `token`, `domain`, `isTrailer`, `dropped`, `playerInitAt`, `clientSessionId`,
`clientRequestId` (две случайные 32-символьные строки на сессию), `env`, `events[]`. Каждое событие
несёт `manifestUrl`, `currentTime`, `duration`, `paused`, `audioTrack`, `quality`, `event_type`,
`at`,
`since`. Отправляется только то, что приложение знает: позиция, длительность, поток; остальное
`null`.

**`POST /stat`**: при старте без `percent` и с `percent=0`, затем на вехах. Поля: `id` (файл),
`token`,
`domain`, `url` (iframe), `type=mgpo`, `ab=false`, `percent`, `info[wasm]`, `info[sw]`,
`info[resolution][…]`, `info[webSocket]`, `info[platform]`.

Заголовки: UA, `Origin`, `Referer` (iframe), `Accept-Language`. Если сервер ответил `403`/`429`,
репортёр прекращает отправку (`restricted`).

## 9. Guard (`/sarn` → `/vorf`): не используется

Проверка браузера, которую знают плагин Aladin и alpac_source: `GET /sarn` → `{challenge, nonce, uap}`,
`proof = SHA-256(nonce || "||" || uap || "|||")`, `POST /vorf {challenge, proof, parentOrigin:"",
referrer}` → `{ok, token}` (238 символов, TTL 90 с). Эндпоинты живые, но собственный JS Alloha их не
вызывает (`L1` в `app.js` сразу возвращает). Как `Accepts-Controls` CDN токен принимал, но блок не
снимал (505 с), у YummyDroid этого рукопожатия нет. Клиент `AllohaGuard` удалён (есть в истории git
до коммита чистки).

## 10. Выбор озвучки и качества

`Stream` собирается один раз (`buildAllohaStream`), дальше выбор пользователя хранится в состоянии
плеера. `withAllohaAudio(audioId)` берёт лесенку выбранной озвучки из `allohaAudioTracks`, делает её
`streamQualityMap` и подбирает ссылку: выбранное качество, если оно есть у новой озвучки, иначе
лучшее.
Плеер применяет результат заменой `MediaItem` без повторного резолва. Сохранённая озвучка/субтитры
тайтла применяются после загрузки потока (`AllohaSourceBehavior.restoreTrackPreference`).

Резолв кэшируется общим кэшем `DefaultPlayerStreamRepository` (3 минуты, ключ
`"$url|$autoQualityLabel"`), как у CVH и Kodik, поэтому повторный вход и смена качества не делают
новых запросов. Восстановление после ошибки идёт с `forceRefresh = true`, иначе из кэша вернулась бы
та же нерабочая ссылка.

## 11. Ошибки и восстановление

Своего сегментного восстановления нет: сегменты играет Media3, как у остальных источников.

| Уровень   | Владелец                      | Что делает                                                                        |
|-----------|-------------------------------|-----------------------------------------------------------------------------------|
| Открытие  | `AllohaExtractor`             | Один повтор при сбое сети; отсутствие источника не повторяется                    |
| Поток     | `AllohaSourceBehavior`        | Свежий резолв (`forceRefresh`), задержка 1 с, ожидание сети                       |
| Попытки   | `PlayerAllohaRecoveryHandler` | `MAX_ATTEMPTS = 4`, дальше ошибка с действиями                                    |
| Подсказка | `AllohaSourceBehavior`        | Через `ALLOHA_RECOVERY_HINT_DELAY_MS` (15 с) предложить сменить плеер или озвучку |

У остальных источников другой путь: `PlayerPlaybackRetryHandler` с `MAX_ATTEMPTS = 5` и тихий
реконнект (`SILENT_RETRY_COUNT = 20` в `PlayerLoadErrorHandlingPolicy`). Для Alloha тихий реконнект
выключен, у неё своё восстановление.

Признаки блока на стороне CDN (заголовок `X-VD`, виден в `PlayerBuffering` как `xVd=`):

| `X-VD`            | Когда                                                                                          |
|-------------------|------------------------------------------------------------------------------------------------|
| `session_blocked` | сессию отрезали посреди воспроизведения; свежие `bnsi`-сессии после этого тоже отбиваются      |
| `client_blocked`  | эскалация после серии блоков подряд или неизвестный fingerprint; отбивается даже `master.m3u8` |

Свежий резолв после `session_blocked` открывает новую сессию, но если блок накопился, её тоже
отбивают: пауза в часы помогает больше, чем повторы.

## 12. Загрузки

Alloha качается как любой HLS-источник: ссылка и заголовки берутся из `extract()`
(`AllohaExtractor.extract`), `AllohaDownloadStrategy` лишь подменяет `Origin` (`decorateHeaders`) и
хранит ключи кэша по стабильному имени сегмента (`RotatingHlsCacheKeyFactory`: старые загрузки
ключуются так же). Скорость ограничена (`DEFAULT_DOWNLOAD_BYTES_PER_SECOND`): CDN режет сессию,
если тянуть быстрее воспроизведения. Просроченная ссылка обновляется через
`DefaultVideoDownloadStreamRefresher` до повторной попытки. Живой WS для загрузок не открывается:
они идут с заголовками, как их отдал резолв, и рассчитаны на короткие окна.

## 13. UI

- Панель и таб дорожек Alloha показываются только когда источник действительно Alloha
  (`usesAlloha && alloha.hasAudioChoice`). Без этого таб с подписью «Alloha» (строка
  `player_mobile_audio_track`) появлялся бы и у Kodik-потоков с выбираемыми дорожками. См.
  `TvExoPlayerView` и `MobileNativePlayer`.
- Списки дорожек чистятся в `PlayerSourceStreamHandler.preparingStreamLoad` и
  `preparingStreamResolve`, чтобы не пережить смену источника. Ветка `preserveCurrentStream = true`
  их сохраняет: это восстановление того же источника на месте.

## 14. Отладка и проверка

Логи идут через `AnalyticsTracker.log`: в debug в logcat, в release тег `AllohaExtractor` (список —
`PersistedLogTags`) пишется в файл логов приложения (Настройки → О приложении → Поделиться логами,
при включённой «Настройки → Общие → Запись логов»). Остальные теги в release no-op. Ссылки в строках
усекаются до хоста (`AppLogDiagnosticSink`).

```bash
adb shell setprop log.tag.AllohaExtractor DEBUG
adb shell setprop log.tag.PlayerBuffering DEBUG
adb logcat 'AllohaExtractor:D' 'PlayerBuffering:D' '*:S'
```

Тестовый прогон (диплинк, см. [deeplinks.md](deeplinks.md)):

```bash
adb shell am start -a android.intent.action.VIEW -d "yummytv://play/1468?balancer=alloha\&episode=5\&position=1" su.afk.yummy.tv.debug
```

`position=0` не сбрасывает сохранённый прогресс, нужен `position=1`. Озвучка у yani «AniLibria», не
«AniLiberty».

| Маркер                                  | Значение                                              |
|-----------------------------------------|-------------------------------------------------------|
| `bnsi audioTracks=[…]`                  | Источники разобраны                                   |
| `mirrors=[…] picked=…`                  | Какое зеркало выбрано                                 |
| `authorization key present=true`        | Ключ `Authorizations` вынут из `app.js`               |
| `ws open at Ns` / `ws first edge_hash=` | Сокет открылся, первый хеш получен                    |
| `ws config_update at Ns pos=… hash=…`   | Хеш обновился (раз в 2-6 минут)                       |
| `ws failure …` / `ws closed code=…`     | Сокет упал, пойдёт переподключение                    |
| `view report /events http=…`            | Ответ на отчёт; `403`/`429` выключают репортёр        |
| `open attempt N/2 failed: …`            | Сбой сети или страницы при открытии; причина в хвосте |
| `audio track selected id=…`             | Пользователь сменил озвучку                           |
| `load error … xVd=session_blocked`      | Блок CDN; в `PlayerBuffering` есть сегмент и позиция  |

**Правила прогонов** (иначе меряется блокировка, а не код):

- один запуск на свежую серию, между запусками пауза, не больше одного запуска подряд: блок
  накапливается и переходит в `client_blocked`;
- не оставлять сессию с живым WS без скачивания сегментов (простой с «играющим» WS приравнивают к
  боту);
- на эмуляторе `sdk_gphone64_arm64` аппаратный `c2.goldfish.h264.decoder` падает; плеер берёт
  программные декодеры первыми (коммит `fbf3bf6f4`); не запускать AVD `Pixel_10_36_MSB`;
- если картинки пустые или TLS ломается, сверить `date` на эмуляторе (часы отстают →
  `tv-emulator-stale-clock`);
- держать воспроизведение дольше 10 минут: порог блока был 270-510 с.

Запись трафика настоящего плеера: Chrome на эмуляторе + DevTools (`adb forward tcp:9222
localabstract:chrome_devtools_remote`), страница тайтла на ru.yummyani.me, в её iframe подставлен
URL
Alloha, `Target.setAutoAttach` (flatten) + `Network.enable` ловит запросы iframe с телами POST и
кадрами WebSocket. Встроенный браузер и расширения iframe не видят.

## 15. Что не помогало (не повторять по одному)

По отдельности блок не снимали (подробности и цифры в журнале): живой WS; обновление `edge_hash`
на лету; пульс по времени, по позиции, по сегментам, как у davy; `/events` и `/stat` со статичным
окружением (один раз дошли до конца 15-минутной серии с 15:00, не воспроизвелось); `Authorizations`
отдельно; Android Chrome и Firefox UA; client hints; Mac Chrome UA (Aladin); реальный fingerprint
устройства; нули и чужие константы; 480p вместо 1080p; HTTP/1.1; `OPTIONS`-preflight; метрономное
скачивание 1 сегмент за 6 с с запасом 67 с (как hls.js); Cronet (TLS Chrome, удалён после H16); Guard (`/sarn`,
`/vorf`) как `Accepts-Controls`; полный профиль Aladin (451 с); путь `api/movies/{id}` из плагина
nb557 `online_mod.js` (404 на всех хостах). Вывод журнала: блок снимает **комбинация** «реальное
состояние в WS + `edge_hash` с `Authorizations` + отчёты + идентичность WebView», а не отдельный
элемент.

## 16. Замеры и ограничения

- CDN режет клиента по объёму открытых сессий. После нескольких десятков запусков подряд с одного IP
  начинают сыпаться `client_blocked`/`session_blocked`. Разброс частоты отказов на одном коде
  доходил до 100 %.
- Сырой URL из `bnsi` без `accepts-controls`, `Authorizations` и WebSocket играет несколько минут
  (замер 04.11.2026), затем блок: поэтому HTTP-цикл §3 один не годится.

## Открытое

1. Какой элемент §7 решающий: отключать по одному (UA, `X-Requested-With`, `Authorizations`,
   состояние в WS, `/events`+`/stat`) отдельными прогонами с паузами.
2. `playback_start` при смене озвучки или качества (как у YummyDroid) и `ended` в конце серии: у нас
   не
   отправляются.
3. Воспроизводимость: серия других тайтлов и долгие серии (больше 14 минут), пауза/перемотка/смена
   озвучки посреди серии.
4. Приставки на кастомных прошивках и их WebView-UA.
5. Настоящие `env` и fingerprint (§7): понадобятся, если блок вернётся; потребуют WebView-bootstrap,
   на что нужно решение владельца.

## История

До 04.11.2026 сессию открывал скрытый WebView: страница плеера грузилась в обёртке, JS перехватывал
XHR/fetch/WebSocket, из `config_update` брался `edge_hash` (`accepts-controls`), подписанный master
ловился из собственного запроса плеера. Он давал лишь то, что получается одним HTTP-циклом, а стоил
запуска Chromium и нестабильной ротации (перезагрузка страницы не отдавала сигналы за 8 с в половине
случаев). Убран вместе с `AllohaWebViewPool`, `AllohaWrapperScript`, идентичностью (cookie jar) и
таймаутами ожидания сигналов.

Затем убраны `AllohaStreamProxy` (loopback HLS-сервер на ~900 строк) и сам объект сессии. Прокси
существовал, чтобы подставлять заголовки сессии и незаметно менять ссылки при ротации подписи. С
HTTP-открытием заголовки передаются плееру обычной картой, а ссылки не ротируются. Вместе с ним
ушли кэш сегментов и префетч, цикл обновления сессии (плеер и загрузки), `AllohaStreamSession`,
`AllohaPlaybackSessionRepository` с менеджером (10 секунд отсрочки закрытия при повороте экрана),
`OpenAllohaStreamSessionUseCase`, `PlayerAllohaSessionHandler`, флаги `reusePlaybackSession` и
`forceFreshAllohaSession`, `AllohaAssPositionFix` (обход бага Media3 с `MarginV/L/R` в ASS: правил
байты субтитров в прокси) и поле запроса `sessionFallbackTtlSeconds`. Без прокси ASS-субтитры Alloha
идут как есть: если позиция снова уедет, вернуть обход как обёртку источника данных субтитров.

5-6.11.2026 вернули лёгкую «живую» часть без WebView и без прокси: WebSocket
(`AllohaPlaybackSocket`),
`edge_hash` на каждый запрос, отчёты (`AllohaViewReporter`), Guard (удалён позже). Серия
экспериментов против `session_blocked` (davy, Aladin, nb557, Cronet, метрономное скачивание)
закончилась
профилем §7, который прошёл 14 минут.
