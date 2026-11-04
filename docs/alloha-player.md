# Плеер Alloha

Ссылки из `bnsi` играют только с заголовками, похожими на браузерные (`Origin`, `Referer`, UA, client
hints), поэтому кроме самих ссылок экстрактор отдаёт и эти заголовки. Больше ничего нет: ни сессии, ни
прокси, ни WebView. Список озвучек с ладдерами достаётся **обычным HTTP**: страница плеера не
запускается. Раньше здесь были скрытый WebView, loopback-прокси и объект живой сессии; всё убрано
(см. «История»).

Референсы для сверки: https://github.com/emilumiq/alloha-parser-kotlin и
https://github.com/HnDK0/external-sources (`ru/yummyanime.lua` и `docs/hoster-implementations.md`:
перестановка `Borth` и нулевой fingerprint).

## 1. Схема

```
PlayerViewModel → PlayerStreamHandler.resolve() → ResolvePlayerStreamUseCase
  → DefaultPlayerStreamRepository.resolve()          общий кэш resolve, как у CVH/Kodik
      AllohaExtractor.extract()
        AllohaHttpSessionOpener.open()               GET страница → Borth → POST bnsi
        parseSources(bnsi)                           озвучки с ладдерами, субтитры
        buildAllohaStream()                          Stream со всеми озвучками

Media3 играет прямую CDN-ссылку с заголовками из Stream.headers
```

Alloha идёт тем же путём, что любой источник: объекта сессии нет. `Stream` несёт ладдеры всех озвучек
(`AllohaAudioTrack.qualities`), поэтому смена озвучки это подмена ссылки в состоянии плеера
(`withAllohaAudio`), без запроса. Ссылки из `bnsi` живут много минут (замер: десятиминутной давности
отдавала сегменты), поэтому ни прокси, ни обновления нет. Когда ссылка всё же перестаёт работать,
ошибка воспроизведения запускает свежий резолв (§7).

## 2. Файлы

| Файл                               | Назначение                                                             |
|------------------------------------|------------------------------------------------------------------------|
| `AllohaExtractor`                  | Оркестратор: открыть сессию с повтором, собрать поток                  |
| `AllohaHttpSessionOpener`          | Один HTTP-цикл: страница плеера, `Borth`, `POST /bnsi/movies/{id}`     |
| `AllohaBorth`                      | `viewporti` → заголовок `Borth` (три перестановки символов)            |
| `AllohaCdnHeaders`                 | Заголовки CDN: Origin/Referer, UA, Accept-Language, client hints       |
| `AllohaResponseParsing`            | Разбор `bnsi`: озвучки с ладдерами, субтитры                           |
| `AllohaStreamFactory`              | `Stream` из открытой сессии: озвучка по умолчанию, её качество         |
| `AllohaSourceUnavailableException` | Открылось, но источника для этой озвучки/серии нет                     |

В presentation: `AllohaSourceBehavior` (восстановление, сохранённые дорожки),
`PlayerAllohaRecoveryHandler`, `PlayerAllohaTrackPreferenceHandler`, `PlayerAllohaAudioUtils`
(`withAllohaAudio`).

## 3. Открытие

`AllohaHttpSessionOpener.open(iframeUrl)`, блокирующий, вызывается с IO-диспетчера:

1. `GET iframeUrl` с `Referer: https://ru.yummyani.me/`, десктопным UA и `Accept-Language`. Страница
   выдаётся только тому, кто пришёл с сайта, который её встраивает.
2. Из HTML регулярками:
   - `<meta name="viewporti" content="…">` — зерно этой загрузки страницы;
   - `const fileList = JSON.parse("…")` → `active.id` — числовой id для пути `bnsi`;
   - `token` берётся из query самой iframe-ссылки.
3. `Borth = <fingerprint>|<перестановка viewporti>` (`allohaBorthHeader`). Это не шифрование, а
   перестановка символов тремя проходами (по длине двоичной записи индекса, по числу нулей в конце
   индекса и шагом 2 по модулю простого числа). Она должна совпасть символ в символ с эталоном:
   `AllohaBorthTest` сверяет её с независимой реализацией. Fingerprint сервер не проверяет (нули и
   настоящий хеш дают одинаковый 200), поэтому он константа.
4. `POST https://alloha.yani.tv/bnsi/movies/{activeId}`, форма `token`, `av1=false`, `autoplay=0`,
   `audio=`, `subtitle=`; заголовки `Borth`, `Referer` (iframe), `Origin`, `X-Requested-With`.
   `av1=false`: AV1 на приставках не декодируется.
5. Ответ в `parseSources()`; CDN-заголовки (`allohaCdnHeaders`: `origin`, `referer`, `user-agent`,
   `accept-language`, `sec-fetch-*`, client hints) идут в `Stream.headers`.

Сбой сети повторяется один раз (`MAX_ATTEMPTS`). Отсутствие источника (`AllohaSourceUnavailableException`)
не повторяется.

## 4. Формат `bnsi`

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
  "pnr": "wss://alloha.yani.tv/ws/", "pnk": "…"
}
```

`parseSources()`:

- `hlsSource[]` даёт список озвучек, у каждой своя лестница качеств.
- У качества может быть два CDN через ` or `. Берётся первый.
- Метки качеств нормализуются (`1080` → `1080p`), сортировка по числу в метке.
- Если `audioId` нет, id равен индексу, чтобы дорожка осталась выбираемой.
- `//host/path` → `https://host/path` (`normalizeStreamUrl`).
- `tracks[]` с `kind == "captions"` — субтитры, формат по расширению `src`.
- Пустой `hlsSource` или ноль качеств → `AllohaSourceUnavailableException`: у этой озвучки нет
  источника, пользователю предлагается выбрать другую.

`pnr`/`pnk` (WebSocket сессии плеера) не используются: ссылки из `bnsi` играют без него.

## 5. Заголовки CDN

`allohaCdnHeaders(origin, userAgent)` кладёт в `Stream.headers` и `qualityHeaders`:

| Заголовок                  | Значение                                              |
|----------------------------|-------------------------------------------------------|
| `origin`, `referer`        | `https://<host iframe>` и `/` (без Origin CDN даёт 403) |
| `user-agent`               | десктопный Chrome, стабилен на процесс                |
| `accept-language`          | `ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7`                 |
| `sec-fetch-dest/mode/site` | `empty` / `cors` / `cross-site`                       |
| `sec-ch-ua`, `-mobile`, `-platform` | выводятся из версии и ОС в UA               |

Chrome-UA без `Accept-Language` и client hints CDN отвечает 403 `client_blocked`, а плеер их сам не
добавляет, поэтому они в карте заголовков. Субтитры тоже запрашиваются с этими заголовками (без них
403), плеер ставит их на тот же источник данных.

UA выбирается один раз на процесс. Новый UA на каждое открытие выглядел бы с одного адреса как
ферма ботов.

## 6. Выбор озвучки и качества

Ничего живого нет: `Stream` собирается один раз (`buildAllohaStream`), дальше выбор пользователя
хранится в состоянии плеера. `withAllohaAudio(audioId)` берёт ладдер выбранной озвучки из
`allohaAudioTracks`, делает его `streamQualityMap` и подбирает ссылку: выбранное качество, если оно
есть у новой озвучки, иначе лучшее. Плеер применяет результат заменой `MediaItem` без повторного
резолва. Сохранённая озвучка/субтитры тайтла применяются после загрузки потока
(`AllohaSourceBehavior.restoreTrackPreference`).

Резолв кэшируется общим кэшем `DefaultPlayerStreamRepository` (3 минуты, ключ `"$url|$autoQualityLabel"`),
как у CVH и Kodik, поэтому повторный вход и смена качества не делают новых запросов. Восстановление
после ошибки идёт с `forceRefresh = true`, иначе из кэша вернулась бы та же нерабочая ссылка.

## 7. Ошибки и восстановление

Своего сегментного восстановления нет: сегменты играет Media3, как у остальных источников.

| Уровень  | Владелец                      | Что делает                                                              |
|----------|-------------------------------|-------------------------------------------------------------------------|
| Открытие | `AllohaExtractor`             | Один повтор при сбое сети; отсутствие источника не повторяется          |
| Поток    | `AllohaSourceBehavior`        | Свежий резолв (`forceRefresh`), задержка 1 с, ожидание сети             |
| Попытки  | `PlayerAllohaRecoveryHandler` | `MAX_ATTEMPTS = 4`, дальше ошибка с действиями                          |
| Подсказка| `AllohaSourceBehavior`        | Через `ALLOHA_RECOVERY_HINT_DELAY_MS` (15 с) предложить сменить плеер или озвучку |

У остальных источников другой путь: `PlayerPlaybackRetryHandler` с `MAX_ATTEMPTS = 5` и тихий
реконнект (`SILENT_RETRY_COUNT = 20` в `PlayerLoadErrorHandlingPolicy`). Для Alloha тихий реконнект
выключен, у неё своё восстановление.

## 8. Загрузки

Alloha качается как любой HLS-источник: ссылка и заголовки берутся из `extract()`
(`AllohaExtractor.extract`), `AllohaDownloadStrategy` лишь подменяет
`Origin` (`decorateHeaders`) и хранит ключи кэша по стабильному имени сегмента
(`RotatingHlsCacheKeyFactory`: старые загрузки ключуются так же). Скорость ограничена
(`DEFAULT_DOWNLOAD_BYTES_PER_SECOND`): CDN режет сессию, если тянуть быстрее воспроизведения.
Просроченная ссылка обновляется через `DefaultVideoDownloadStreamRefresher` до повторной попытки.

## 9. UI

- Панель и таб дорожек Alloha показываются только когда источник действительно Alloha
  (`usesAlloha && alloha.hasAudioChoice`). Без этого таб с подписью «Alloha» (строка
  `player_mobile_audio_track`) появлялся бы и у Kodik-потоков с выбираемыми дорожками. См.
  `TvExoPlayerView` и `MobileNativePlayer`.
- Списки дорожек чистятся в `PlayerSourceStreamHandler.preparingStreamLoad` и
  `preparingStreamResolve`, чтобы не пережить смену источника. Ветка `preserveCurrentStream = true`
  их сохраняет: это восстановление того же источника на месте.

## 10. Отладка

Логи идут через `AnalyticsTracker.log`: в debug в logcat, в release тег `AllohaExtractor`
(список — `PersistedLogTags`) пишется в файл логов приложения (Настройки → О
приложении → Поделиться логами, при включённой «Настройки → Общие → Запись логов»). Остальные теги в
release no-op. Ссылки в строках усекаются до хоста (`AppLogDiagnosticSink`).

```bash
adb shell setprop log.tag.AllohaExtractor DEBUG
```

| Маркер                       | Значение                                                       |
|------------------------------|----------------------------------------------------------------|
| `bnsi audioTracks=[…]`       | Источники разобраны                                            |
| `open attempt N/2 failed: …` | Сбой сети или страницы при открытии; причина в хвосте          |
| `audio track selected id=…`  | Пользователь сменил озвучку                                    |

Отказы CDN (403 `client_blocked` и подобные) видны как ошибка воспроизведения Media3 и тег
`PlayerBuffering`; тело ответа с `X-VD` в логах приложения больше не пишется. Если CDN снова начнёт
отбивать, проверять заголовки в `allohaCdnHeaders` и их набор в `Stream.headers`.

## Замеры

- Эмулятор, один CDN (`*.vkvideo.cloud`), 04.11.2026: сырой URL из `bnsi` отдаёт master, плейлист и
  сегменты без `accepts-controls`, без токена `Authorizations`, без WebSocket и без cookie. Серия
  играла, обновление на ~100-й секунде (`generation=2`) прошло без единого `CDN failure`.
- Не проверено на других CDN (раньше часть из них отвечала 403 `token_decrypt` на сырой bnsi-URL) и на
  приставках. Если такое вернётся, лечится в `LiveAllohaStreamSession`/прокси, а не возвратом WebView.
- Запрос с Chrome-UA без `Accept-Language` и client hints получал 403 `client_blocked` (§5).
- Ссылка из `bnsi`, полученная в 20:15, в 20:24 всё ещё отдавала master, плейлист и сегменты: срок
  жизни как минимум 9 минут. Предела не искали.
- CDN режет клиента по объёму открытых сессий. После нескольких десятков запусков подряд с одного IP
  начинают сыпаться `client_blocked`/`session_blocked`, и статистика меряет блокировку, а не код.
  Между сериями нужны паузы. Разброс частоты отказов между сериями на одном коде доходил до
  42–100 %, поэтому выводы по 10–12 прогонам ненадёжны.

## История

До 04.11.2026 сессию открывал скрытый WebView: страница плеера грузилась в обёртке, JS перехватывал
XHR/fetch/WebSocket, из `config_update` брался `edge_hash` (`accepts-controls`), подписанный master
ловился из собственного запроса плеера. Он давал лишь то, что теперь получается одним HTTP-циклом, а
стоил запуска Chromium и нестабильной ротации (перезагрузка страницы не отдавала сигналы за 8 с в
половине случаев). Убран вместе с `AllohaWebViewPool`, `AllohaWrapperScript`, идентичностью (cookie
jar) и таймаутами ожидания сигналов.

Затем убраны `AllohaStreamProxy` (loopback HLS-сервер на ~900 строк) и сам объект сессии. Прокси существовал, чтобы
подставлять заголовки сессии и незаметно менять ссылки при ротации подписи. С HTTP-открытием
заголовки передаются плееру обычной картой, а ссылки не ротируются. Вместе с ним ушли кэш сегментов
и префетч, цикл обновления сессии (плеер и загрузки), `AllohaStreamSession`,
`AllohaPlaybackSessionRepository` с менеджером (10 секунд отсрочки закрытия при повороте экрана),
`OpenAllohaStreamSessionUseCase`, `PlayerAllohaSessionHandler`, флаги `reusePlaybackSession` и
`forceFreshAllohaSession`, `AllohaAssPositionFix` (обход бага Media3 с
`MarginV/L/R` в ASS: правил байты субтитров в прокси; см. ниже) и поле запроса
`sessionFallbackTtlSeconds`. Без прокси ASS-субтитры Alloha идут как есть: если позиция снова
уедет, вернуть обход как обёртку источника данных субтитров.
