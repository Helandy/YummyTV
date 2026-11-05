# Alloha: блок `session_blocked` через 5-7 минут — журнал расследования

Рабочий журнал: что пробовали, откуда брали идеи, что помогло. Дописывать в конец, не переписывать
историю. Общая схема плеера — в [alloha-player.md](alloha-player.md).

## Симптом

Серия Alloha играет ~5-7 минут, затем CDN (`*.vkvideo.cloud`) отвечает `403`, заголовок `X-VD`:

| `X-VD`           | Когда                                                                                  |
|------------------|----------------------------------------------------------------------------------------|
| `session_blocked`| сессию отрезали посреди воспроизведения; свежие `bnsi`-сессии после этого тоже отбиваются |
| `client_blocked` | эскалация после серии блоков подряд; отбивается даже `master.m3u8`                     |

Блок приходит на 270-480 с воспроизведения, на сегменте ~50-230, не зависит от тайтла, серии, CDN-хоста.
Настоящий плеер сайта в Chrome на том же эмуляторе и том же IP серию доигрывает до конца.

## Как проверять (инструменты)

- Диплинк `yummytv://play/{animeId}?balancer=alloha&episode=N&position=секунды` (см.
  [deeplinks.md](deeplinks.md)). Озвучка у yani «AniLibria», не «AniLiberty». `position=0` не
  сбрасывает сохранённый прогресс, нужен `position=1`.
- Логи: `adb logcat 'AllohaExtractor:D' 'PlayerBuffering:D' '*:S'`; на каждом новом эмуляторе
  сначала `adb shell setprop log.tag.AllohaExtractor DEBUG` (и `PlayerBuffering`). В `PlayerBuffering`
  `load error` содержит `xVd=` и номер сегмента.
- Запись трафика настоящего плеера: Chrome на эмуляторе + DevTools (`adb forward tcp:9222
  localabstract:chrome_devtools_remote`), страница тайтла на ru.yummyani.me, в её iframe подставлен
  URL Alloha, `Target.setAutoAttach` (flatten) + `Network.enable` ловит запросы iframe с телами POST и
  кадрами WebSocket. Встроенный браузер/расширения iframe не видят.
- Тест с блоком эскалирует его: **между прогонами — пауза, не больше одного запуска подряд.**

## Источники идей (чужие репозитории)

| Репозиторий | Что там | Что взяли | Результат |
|---|---|---|---|
| [AsfhtgkDavid/davy-parsers](https://github.com/AsfhtgkDavid/davy-parsers) `AllohaParser.kt` | WebSocket сессии (`pnr`/`pnk` из `bnsi`), `edge_hash` → `Accepts-Controls`, `Authorizations: Bearer <ключ из app.js>`, пульс `playing` | сокет, живой `edge_hash`, ключ из `app.js`; потом буквально: Firefox UA, fingerprint `d2c8a63c…260b`, `playback_start`+`resumed`, пульс 15 с | не помогло: блок на 291 с |
| [saltek1995/YummyDroid](https://github.com/saltek1995/YummyDroid) `docs/alloha-long-playback-investigation.md` | то же расследование; сайт кроме WS шлёт `POST /events` и `POST /stat`; с ними (+WebView-сессия) серия 23:40 доиграна до `STATE_ENDED` | `/events` (heartbeat 10 с, пачка 30 с) и `/stat` без WebView | один частичный успех (см. ниже), потом не воспроизвелось |
| [queukat/YummyKodik](https://github.com/queukat/YummyKodik) `Alloha/*`, `scripts/inspect-alloha-tracks.ps1` | Jellyfin-прокси: одноразовый WS за `edge_hash`, `Authorizations`+`Accepts-Controls`, реальный 64-hex fingerprint в `Borth` | заголовки, fingerprint `9badb2c5…45d9` | не помогло; подтвердило набор заголовков |
| [Kirill9732/alpac_source](https://github.com/Kirill9732/alpac_source) `internal/litesrc/mirage_ws.go`, `alloha.go` | время в `playing` по номеру сегмента `(seg-25)*6`, `init`, пульс 30 с, один WS на `sid` (второй получает 4005), без `/events`; «без живого WS сессия живёт 6-9 мин» | heartbeat по сегментам, `lastSegment` из URL запроса | не помогло: блок на 379 с |
| [sunic007/lampa-samsung](https://github.com/sunic007/lampa-samsung) `src/playerguard.js` | сторож плеера (перезапуск, качество ниже, hls.js вместо AVPlay) | ничего: лечит симптом, у нас такое восстановление уже есть | неприменимо |

## Хронология прогонов (2026-11-05)

Серия Hotaru no Yomeiri (animeId 26053) AniLibria, если не указано иное. «Блок» — секунда сессии, на которой пришёл
первый `session_blocked`.

| Что менялось | Блок |
|---|---|
| WS (статичный `edge_hash`), время пульса = секунды с открытия | ~399 с |
| + реальная позиция плеера в пульсе | ~377 с |
| + живое обновление `edge_hash` в каждом запросе (интерцептор) | ~429 с |
| + `/events` и `/stat` | 480 с, серия с 15:00, сегмент 228 из ~229 (за 14 с до конца) |
| **+ `bytesLoadedTotal` в `/events` (счётчик в интерцепторе)** | **доиграла до конца, 508 с, 0 ошибок (серия с 15:00)** |
| то же, серия с нуля | 272 с, 434 с |
| + `Authorizations: Bearer <ключ app.js>` | 344 с |
| 480p вместо 1080p | 362 с |
| HTTP/1.1 вместо HTTP/2 | 452 с |
| Android Chrome 149 UA + client hints + полная стартовая последовательность `/events` | 435 с |
| `OPTIONS`-preflight перед каждым GET (как у браузера) | 397 с (серия 1) |
| heartbeat по сегментам (alpac_source), без `/events` | 379 с (серия 3) |
| другой тайтл (Akame ga Kill!, 1468) | 360 с |
| davy-parsers буквально (Firefox UA, его fingerprint, его пульс), тайтл 11568 | 291 с |
| Cronet (Chromium-стек, `Cronet/157`, TLS Chrome) для запросов к CDN, серия 1 | 326 с (сегмент 59) |
| Запуск владельца в плеере (23:01, коммит `fbf3bf6f4`: Cronet, Firefox UA, fingerprint davy, heartbeat davy, программный декодер) | 416 с (сегмент 74, хост `91-3f-55-r406`), `session_blocked` |
| **Chrome на эмуляторе, настоящий плеер сайта** | **доиграл: 301 сегмент, 1591 с, 0 ошибок** |

## Дополнение: запись настоящего плеера с cookie, рукопожатием WS и телом `bnsi` (22:12)

- **Cookie нет вообще**: ни `Set-Cookie`, ни `Cookie` на запросах к `alloha.yani.tv` и CDN. Гипотеза про CookieJar отпала.
- Рукопожатие WS обычное: `Origin`, UA, `permessage-deflate`, без cookie, ничего лишнего относительно нашего.
- WS по времени и составу сообщений совпадает с нашим (один сокет, `playing` раз в 30 с, смена `edge_hash` каждые 2-6 мин).
- 64-hex fingerprint в `Borth` **равен `Accepts-Controls` на первом запросе манифеста** (до `edge_hash`) и стабилен внутри браузера,
  но **разный у разных браузеров** (Chrome 149 на одном эмуляторе `291ae1e3…`, Chrome 145 на другом `0930515f…`): его считает JS плеера.
- Браузер на новом эмуляторе (Chrome 145) играл Alloha 733 с, 157 сегментов, без ошибок.

## Проверка H1: свой fingerprint в `Borth` и в `Accepts-Controls` на манифесте (22:27)

Случайный 64-hex на установку в `Borth` и как начальный `Accepts-Controls`, сокет без ожидания первого `edge_hash`:
**`client_blocked` на первом же `master.m3u8`** (три попытки подряд в цикле восстановления; приложение остановлено).
Это не блок IP: браузер на том же эмуляторе играл без ошибок. Значит, CDN проверяет значение в `Accepts-Controls`:
случайное не принимает. Не проверено: известное значение чужого проекта (davy `d2c8a63c…`, YummyKodik `9badb2c5…`) в начальном
`Accepts-Controls`. Эксперимент остановлен по решению владельца, код H1 не закоммичен.

## Откуда fingerprint и гипотеза H6: общий идентификатор клиента

В `app.js` fingerprint считает `Tw()`: SHA-256 от строки из признаков устройства (canvas 2D `toDataURL`, WebGL
vendor/renderer, аудио-отпечаток, `hardwareConcurrency`, `navigator.*`). Поэтому у каждого браузера он свой
и стабилен между сессиями. Мы же слали нули или константы чужих проектов (davy `d2c8…`, YummyKodik `9bad…`), то есть
**один общий идентификатор на всех клиентов**. Если CDN считает лимит сессии на этот идентификатор (его несёт подпись ссылки
из `bnsi`), то блок через ~6 минут и эскалация `session_blocked` -> `client_blocked` объясняются общим лимитом, а не нашим
поведением. Проверка H6: **уникальный fingerprint на установку только в `Borth`**, а `Accepts-Controls` как раньше (живой
`edge_hash`, не fingerprint: случайный fingerprint в `Accepts-Controls` CDN отбил `client_blocked`, см. H1).
Прогон: после паузы, один запуск, мониторинг раз в минуту.

### Результат H6 (22:35)

Уникальный случайный fingerprint на установку **только в `Borth`**, `Accepts-Controls` как раньше (`edge_hash`): `client_blocked` на первом
`master.m3u8`. Контроль в ту же минуту (22:36): настоящий плеер в Chrome на том же эмуляторе и IP получил 46 ответов CDN без единой
ошибки, значит, это не блок IP и не общий лимит. Выходит, CDN отбивает **неизвестный случайный fingerprint** (и в `Accepts-Controls`
по H1, и в `Borth` по H6), а нули и чужие константы (davy, YummyKodik) пропускает хотя бы на старте. Но и настоящий fingerprint
нового устройства (Chrome 145, ни разу не виденный) проходит. Значит, сервер проверяет не «виденность» значения, а, вероятно, его
**структуру или происхождение** (формат хэша из `Tw()`), которое случайный 64-hex не воспроизводит. Не проверено: что именно за хэш
`TP` в `Tw()` и нет ли в настоящем fingerprint проверяемой структуры.

## Как считается fingerprint (разбор `app.js` `Tw()` и чужих репозиториев)

`Tw()` в `app.js` совпадает с `fingerprint()` из пакета `alloha-bootstrap.js` у YummyDroid (там он выполняется в WebView).
Строка `values.join('||')`, затем SHA-256 (hex, 64 символа); значения по порядку:

1. `navigator.userAgent` с вырезанными группами цифр с точками (`replace(/\d+(?:[._]\d+)+/g, '')`), т.е. без версий;
2. часовой пояс (`Intl.DateTimeFormat().resolvedOptions().timeZone`);
3. размер экрана `max(width,height)x min(width,height)`;
4. `navigator.language`;
5. `navigator.deviceMemory`;
6. `navigator.hardwareConcurrency`;
7. canvas 2D `toDataURL()` после `fillText('fp', 2, 2)` (`16px sans-serif`, `textBaseline='top'`);
8. WebGL `UNMASKED_VENDOR|UNMASKED_RENDERER`;
9. длина буфера `OfflineAudioContext(1, 44100, 44100)` (таймаут 1,5 с; нет — пропускается).

Входов мало и они «классовые» (GPU, ОС, шрифт, язык), поэтому у одинаковых устройств fingerprint совпадает. Контрольной суммы в
настоящих значениях нет (проверено на трёх), то есть формально от случайного SHA-256 они не отличаются. Рабочая гипотеза H7: CDN
знает набор реально виденных fingerprint (классы устройств), поэтому случайный отбивается `client_blocked`, нули и значения
реальных браузеров (davy, YummyKodik, наш Chrome) проходят, а общая константа, которой пользуются многие клиенты, получает общий
короткий лимит (~6 мин). Настоящий fingerprint своего устройства получить нативно нельзя (canvas, WebGL, аудио) — YummyDroid
запускает пробу в WebView. У YummyDroid нет файла LICENSE: их код не копируем, пишем свою реализацию по алгоритму сайта.

### Результат H7 (22:46-22:58) и почему он неоднозначен

H7: настоящий fingerprint этого устройства (тот, что посчитал Chrome на этом эмуляторе) в `Borth`, остальное как в коммите.
- 22:46, серия 1 Hotaru (трогали раньше, много блоков): `client_blocked` на первом манифесте. Затёрто: серия 1 к тому моменту уже была «сожжена».
- 22:47, **свежая** серия 5 (Akame ga Kill!): манифест и сегменты прошли, но на этом AVD (`sdk_gphone64_arm64`) падает аппаратный
  `c2.goldfish.h264.decoder` (`DECODING_FAILED`), воспроизведения не было; 9 минут простоя WS с пульсом davy («играет» без скачивания).
- 22:58, **свежая** серия 6, уже с программным декодером: `client_blocked` на первом манифесте.
Серия 5 прошла, а серия 6 через 11 минут нет. Токен серии тут ни при чём. Правдоподобное объяснение: 9 минут «фантомного
воспроизведения» (WS говорит, что играем, а сегменты не качаются) приравняли нашу идентичность к ботовой, и CDN перешёл на
`client_blocked`. Это только объяснение, не проверено. Вывод: после такого прогона нужна пауза в часы, и **любой следующий прогон
должен быть реальным воспроизведением**, а не простоем (на эмуляторе — программный декодер).

Правило для тестов: одна попытка на свежую серию; не оставлять сессию с живым WS без скачивания сегментов.

## Что браузер делает во времени (разбор записи на 26 минут, `cdp2.ndjson`)

**Менять нечего: за всю серию (1591 с) браузер не меняет ни заголовков, ни параметров, ни fingerprint.**
- Все 302 GET к CDN с одним и тем же набором заголовков (UA, `Referer`, `Authorizations`, client hints); меняется только `Accepts-Controls` (живой
  `edge_hash`, через ~3,5 с после `config_update`).
- Пути ссылок без query, один хост на сессию; никакого повторного `bnsi`, перезагрузки страницы или обновления манифеста посреди серии.
  Кроме `/events` и `/stat` к `alloha.yani.tv` за серию только два `POST /errors` (через ~30 с после старта сессии).
- Fingerprint один и тот же в `Borth` на всех `bnsi`.

**Что отличается — рисунок скачивания (мы его ни разу не повторяли):**
- Стартовая пачка: ~14 запросов за первые 4,3 с (заполняет ~67 с буфера), дальше **ровно один сегмент каждые 6,0 с** (медиана 6,0, максимум 6,1,
  минимум 5,9) и **постоянный запас ~67 с** вперёд от позиции (`ahead_s = 67` на всём протяжении, 16-265 запросов).
- У нас буфер с гистерезисом (`PlayerBufferProfile`: SMALL 18/30 с, MEDIUM 25/60, LARGE 30/120; `min != max`): Media3 качает пачкой до `max`, потом
  простаивает, пока не упадёт ниже `min`, то есть всплесками, а запас вперёд 18-30 с вместо 67 с.
- Гипотеза H8: CDN считает «живой» только метрономное скачивание hls.js (≈1 сегмент за 6 с при постоянном запасе); всплески Media3 копят
  подозрение, и около 5-7 минуты сессию режут. У alpac_source есть throttle «пачка, потом по одному в интервал», у YummyDroid 90/90 пробовали
  без телеметрии. Проверка: `minBufferMs = maxBufferMs ≈ 67 с` для Alloha (Media3 тогда докачивает по сегменту сразу, как только запас стал меньше `max`)
  и сравнить интервалы запросов (счётчик).

### Результат H8 (23:38): метрономное скачивание как у браузера

Для эксперимента `minBufferMs = maxBufferMs = 67 с`, `targetBufferBytes` 96 МБ, лог времени каждого запроса к CDN. Рисунок получился **таким же, как у
браузера**: стартовая пачка 15 запросов за 2,3 с, дальше ровно один сегмент каждые 6,0 с (медиана, среднее, минимум и максимум 6,0, n=53), запас
~67 с (`bufferedMs=67294` в логе ошибки). Результат: **`session_blocked` на ~342 с (23:44:00, сегмент 69, 72-й запрос к CDN)** при полностью браузерном ритме.
Значит, рисунок и глубина скачивания не причина. Эксперимент откатил (буфер и лог).

Что теперь сходится с браузером и всё равно блокируется: заголовки, WS (состав, ритм, позиция), `edge_hash`, `Authorizations`, UA и client hints, TLS (Cronet),
HTTP/1.1, `/events` и `/stat`, preflight, fingerprint (нули/константы), рисунок и глубина скачивания. Остаётся только то, что делает сам JS плеера.

## Guard (`/sarn` -> `/vorf`) и плагин Aladin для Lampac (H9, 00:00-00:16)

Плагин Aladin (вставлен владельцем, файлы `AladinBorth`, `AladinGuard`, `AladinWsSession`, `AladinController`) и alpac_source делают одно и то же:
- **Guard**: `GET /sarn` -> `{challenge, nonce, uap}`; `proof = SHA-256(nonce || "||" || uap || "|||")` (parentOrigin пустой); `POST /vorf {challenge, proof, parentOrigin:"", referrer}`
  -> `{ok, token}` (238 символов). TTL 90 с, обновлять. Эндпоинты у `alloha.yani.tv` **живые** (проверено вручную: `/sarn` 200, `/vorf` 200 ok=true), но
  собственный JS страницы Alloha их не вызывает (`L1` в `app.js` сразу возвращает).
- В Aladin `Accepts-Controls` = **токен Guard в первую очередь**, `edge_hash` из WS только запасной (`GetLiveToken(...) ?? GetGlobalEdgeHash()`); `Authorizations`
  в заголовках потока вообще нет; fingerprint в `Borth` = `SHA-256("fingerprint-alloha")` (ещё одна общая константа); WS: `playback_start` + `init`, `playing` раз в 30 с с
  `current_time = 0` всегда, пинг 20 с, живёт до 4 часов; поток идёт через серверный прокси; запасное зеркало `reserve` (`file + " or " + reserve`) — в нашем `bnsi` его нет.

Проверка у нас (одна попытка на свежую серию Akame ga Kill!):
- **H9b**: Guard обновляется фоном каждые 90 с, `Accepts-Controls` как раньше (`edge_hash`): `session_blocked` на 469 с (00:06:34, сегмент 83).
- **H9a**: токен Guard вместо `edge_hash` в `Accepts-Controls` (как в Aladin): **CDN токен принимает** (манифест и сегменты пошли), но `session_blocked` на 505 с (00:16:24, сегмент 89).
Оба в обычном диапазоне 270-508 с. Guard блока не снимает.

## Успешные прогоны

1. **Настоящий плеер в Chrome** (эмулятор, тот же IP, 480p): 301 сегмент, 1591 с, `X-VD` не было ни разу.
   Что он шлёт, чего не было у нас:
   - к CDN: `Accepts-Controls` (живой `edge_hash`) **и** `Authorizations: Bearer <ключ из app.js>`, перед каждым GET
     CORS-`OPTIONS`, Android UA с client hints; соединение по HTTP/1.1;
   - `POST /events` пачками: стартовая серия `ad_block_start, view_start, startup, play, pause, playback_ready,
     ad_block_end, play, first_frame, file_load_complete`, затем `view_percent` и `heartbeat` раз в 10 с (`bufferLength`,
     `playedSecondsDelta`, `droppedFrames`, `bytesLoadedTotal`…);
   - `POST /stat` (`type=mgpo`, `percent` 0/10/30/60/97, `info[...]`);
   - WS: `playback_start` дважды, `resumed`, `paused`, `resumed`, пульс 30 с, переподключения.
2. **Единственный наш полный прогон** (1080p, десктопный Chrome UA, HTTP/2): WS + живой `edge_hash` + `/events` и
   `/stat` + `bytesLoadedTotal` из интерцептора, серия с 15:00 до конца, 508 с. Не воспроизвелось на серии с нуля:
   вероятно, просто не дошло до порога ~5-7 мин.
3. **YummyDroid** (по их документу): WebView-сессия + все три канала, серия 23:40 до `STATE_ENDED`.

Вывод по успешным прогонам: надёжно проходит только настоящий браузер (плеер сайта в Chrome или WebView-сессия).
Всё, что мы повторили на уровне запросов к CDN (заголовки, WS, отчёты, UA, TLS через Cronet), блок не снимает.
Не повторено только то, что делает сам JS плеера: WebSocket и HTTP-отчёты из его реальных счётчиков.

## Что не помогло (не повторять)

Живой WS; обновление `edge_hash` на лету; heartbeat по времени / по позиции / по сегментам / как у davy;
`/events` и `/stat` (с байтами и стартовыми событиями); `Authorizations`; Android Chrome и Firefox UA, client hints;
реальный fingerprint в `Borth`; 480p; HTTP/1.1; `OPTIONS`-preflight; другие серии, тайтлы и CDN-хосты.

## Открытое

- **Cronet** (Chromium-стек, TLS-отпечаток Chrome) для запросов к CDN проверен: `PlayerDataSourceFactory` берёт
  `CronetDataSource` (`play-services-cronet`) для запросов с `accepts-controls`, остальное на OkHttp. Движок
  поднимается (`Cronet/157.0.8103.14`), но блок тот же: `session_blocked` на 326 с. Значит, дело не в TLS-отпечатке
  запросов к CDN. На AVD `sdk_gphone64_arm64` (Android 17) H.264 не декодируется, проверки шли на `sdk_gphone16k_arm64`.
  WebSocket, страница плеера и `bnsi` при этом остаются на OkHttp (Cronet WebSocket не умеет).
- WebView-сессия (путь YummyDroid) — убрана сознательно, не пробовали.

## Состояние кода на момент записи

Экспериментальный код оставлен в дереве: `AllohaPlaybackSocket` (WS), `AllohaEdgeHash` (живой хеш, номер сегмента, байты),
`AllohaViewReporter` (`/events`, `/stat`; выключен `REPORT_VIEW = false`), `Authorizations` из `app.js` в
`AllohaHttpSessionOpener`, интерцептор (OkHttp) и `CronetDataSource` для Alloha в `PlayerDataSourceFactory`. Идентичность клиента на
последнем прогоне: Firefox 159 UA, fingerprint davy, пульс davy (`DAVY_HEARTBEAT = true`).

## H10: полный профиль Aladin (06.11.2026)

Раньше куски Aladin проверялись по одному. Теперь все сразу (флаги `ALADIN_PROFILE` в `AllohaExtractor`, `AllohaPlaybackSocket`, `AllohaBorth`):
- один Mac Chrome 128 UA на страницу, `bnsi`, Guard, WS и CDN (client hints выводятся из UA, платформа `macOS`);
- fingerprint в `Borth` = `SHA-256("fingerprint-alloha")` (`91ede163…d639`);
- без `Authorizations` (`SEND_AUTHORIZATIONS = false` в `AllohaHttpSessionOpener`);
- WS: `playback_start` + `init`, `playing` раз в 30 с, `current_time = 0`, без `seeked`;
- `Accepts-Controls`: токен Guard (обновление раз в 90 с), `edge_hash` только если токена нет.

Не повторено: серверный прокси потока и зеркало `reserve` (в нашем `bnsi` поля нет).

Результат (эмулятор `sdk_gphone64_arm64`, Akame ga Kill! 1468, серия 5, 08:56-09:04): старт чистый (`bnsi` 200, Guard `ok=true`, WS открыт, `edge_hash` пришёл, `client_blocked` нет), но `session_blocked` на **451 с** (09:04:06, сегмент 80). Полный профиль Aladin блока не снимает - те же 270-510 с.

## H11: путь `api/movies/{id}` из плагина nb557 `online_mod.js` (06.11.2026)

Провайдер `alloha` в плагине не использует WebSocket, Guard и `Accepts-Controls`: страница плеера уходит в зашифрованный скрипт (пароль у автора, не раскодировали),
который возвращает `domain`/`postdata`/`headers`, затем `GET/POST {domain}api/movies/{id}`, из `hlsSource.quality` берётся первая ссылка из `link1 or link2`, играет через прокси.
Проба скриптом с Mac (тот же Borth и заголовки, что в приложении), серия Akame ga Kill! 1468 e5:
- контроль `POST /bnsi/movies/{id}` с `Borth`: **200**, `hlsSource`, `pnr`, `pnk` (Borth в скрипте считается верно);
- `GET`/`POST /api/movies/{id}` на `alloha.yani.tv` с `Borth` и без: **404** (HTML-страница ошибки);
- тот же путь на `scalp-as.stloadi.live` и `api.apbugall.org`: **404**; `bnsi` на тех же хостах без тела: 405 (путь существует).

Вывод: на известных нам хостах `api/movies/{id}` не отвечает, гипотеза «старый путь без сессии» не подтвердилась. Домен и путь плагин берёт из своего зашифрованного скрипта, поэтому
другой хост или схема возможны, но проверить без чужого пароля нельзя (подбирать не будем).

## H12: клиент как YummyDroid, только HTTP-часть без WebView (06.11.2026)

Источник: [saltek1995/YummyDroid](https://github.com/saltek1995/YummyDroid) (`AllohaPlaybackSession`, `AllohaBootstrapResolver`). Флаги `YUMMYDROID_PROFILE` (`AllohaExtractor`), `REAL_STATE` (`AllohaPlaybackSocket`), `SEND_AUTHORIZATIONS` (`AllohaHttpSessionOpener`).
- UA = `WebSettings.getDefaultUserAgent` (строка системного WebView, сам WebView не создаётся), client hints Android, `X-Requested-With = пакет приложения` на запросах к CDN;
- CDN: `Accepts-Controls` = живой `edge_hash` из WS **и** `Authorizations: Bearer <ключ из app.js>`; Guard (`/sarn`, `/vorf`) выключен;
- WS: `playback_start` + `init`; `current_time` = реальная позиция плеера; `playing` раз в 30 с только пока позиция движется; `resumed`/`paused` при смене движения, `seeked` при скачке > 3 с (состояние плеера выводится из позиции, раз в секунду);
- HTTP-отчёты `/events` и `/stat` включены (`REPORT_VIEW`), окружение статичное.

Результат (эмулятор `sdk_gphone64_arm64`, Akame ga Kill! 1468, серия 5, 09:55:42-10:10): **без `session_blocked` на 840+ с** (WS `config_update` на 840 с, позиция 840 с, `/events` и `/stat` 200 весь прогон; прежний диапазон блока 270-510 с пройден). Первый прогон, где клиент на HTTP-пути прошёл порог.

Что отличает от прежних неудачных прогонов: комбинация (реальная позиция и состояние в WS + `edge_hash` с `Authorizations` + отчёты + WebView-UA и `X-Requested-With`). По отдельности эти куски блок не снимали. Какой из них решающий, не выяснено (разбирать по одному).

## H13: повтор после чистки кода (06.11.2026, 10:41-10:56)

Тот же профиль H12 после удаления флагов и мёртвых веток (Guard, davy, Aladin, пульс по сегментам). Akame ga Kill! 1468, **серия 6** (свежая):
без `session_blocked` и `client_blocked` за 840+ с (последний `config_update` на 840 с, позиция 839 с), 34 ответа 200 на `/events` и `/stat`. Второй подряд прогон выше порога 270-510 с,
уже на другой серии; поведение не отличается от H12.

## H14: синтетический fingerprint по алгоритму сайта (06.11.2026, 11:00-11:15)

Вместо константы Aladin в `Borth` подставлен SHA-256 от строки признаков по алгоритму `Tw()` (UA WebView без версий, `Europe/Moscow`, `2400x1080`, `ru-RU`, `deviceMemory=8`,
`hardwareConcurrency=8`, образец canvas `toDataURL`, WebGL `Google Inc. (Google)|ANGLE (… SwiftShader …)`, длина аудио-буфера 441): `c2322f01…9ebb`. Значение собрано нами, ни одно
устройство его не считало; код временный, откатан.
Akame ga Kill! 1468, серия 7 (свежая): `bnsi` 200, первый манифест без `client_blocked` (случайный 64-hex в H1/H6 отбивался на нём), **840+ с без `session_blocked`**, `/events` и `/stat` 200.
Вывод: сервер принимает fingerprint, собранный из правдоподобных признаков по алгоритму сайта, и в одном прогоне не отличает его от настоящего. Для генерации на клиента нужен генератор признаков
(реальные размеры экрана, память, ядра, образцы canvas/WebGL с живых устройств) и те же значения в `/stat` и `/events`. Не проверено: несколько разных синтетических fingerprint с одного IP,
сохранение между запусками, разные профили устройств.

## H15: fingerprint на установку, сгенерированный приложением (06.11.2026, 11:20-11:35)

`AllohaFingerprint`/`AllohaClientProfile`: алгоритм `Tw()` (SHA-256 от признаков через `||`), признаки с устройства (UA WebView без версий, часовой пояс, экран в CSS-пикселях, `ru-RU`,
`deviceMemory`, ядра, аудио 44100), canvas и WebGL по случайному `seed` установки (`SharedPreferences` `alloha_client`). Те же экран, `devicePixelRatio` и платформа идут в `/events` и `/stat`.
Akame ga Kill! 1468, серия 8 (свежая), эмулятор: первый манифест без `client_blocked`, **840+ с без `session_blocked`**, `/events` и `/stat` 200.
Это третий подряд результат выше порога 270-510 с и первый с fingerprint, который приложение генерирует само. В H14 аудио было 441 (опечатка в догадке), в коде 44100 как в `audioLength()` YummyDroid.
Не проверено: много разных fingerprint с одного IP, приставки, массовое использование. На эмуляторе в `alloha_client.xml` остался ключ `fingerprint` от эксперимента H6; код его не читает.

## H16: без Cronet, 20 минут (06.11.2026, 11:44-12:05)

Профиль H15 (свой fingerprint на установку), но `CronetDataSource` для CDN Alloha отключён (временная правка `PlayerDataSourceFactory`, откатана): сегменты идут через OkHttp, в логе нет строки `cronet engine`.
Akame ga Kill! 1468, серия 9 (свежая), эмулятор: первый манифест без `client_blocked`, **1200+ с (20 минут) без `session_blocked`** (последний `config_update` на 1200 с, позиция 1198 с),
47 ответов 200 на `/events` и `/stat`. Вывод: Cronet для прохождения порога 270-510 с не нужен; TLS-отпечаток сегментов роли не играет. Его можно удалить вместе с `media3-datasource-cronet` и
`play-services-cronet` (на приставках без Google Play Services он и так не поднимался).
