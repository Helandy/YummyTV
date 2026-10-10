# Экстракторы балансеров

Как приложение превращает iframe-URL от yani в ссылку на поток. Два самых сложных экстрактора
описаны отдельно: [alloha-player.md](alloha-player.md) (живая сессия и прокси) и
[cvh-player.md](cvh-player.md) (подписанные mp4 и переезд узла). Здесь — общая схема и остальные:
Kodik, VK, Rutube, Sibnet, Aksor, Zedfilm.

Код: `feature/player/data/.../extractor/`. Определение балансера по URL —
`core/common/.../utils/player/PlayerUrlUtils.kt`.

## Общая схема

```
DefaultPlayerStreamRepository.resolve(request)
  extractors.firstOrNull { it.supports(iframeUrl) }   → иначе PlayerStreamResolveResult.Unsupported
  SessionAware (Alloha)  → extract() без кэша, сессией управляет менеджер
  остальные              → кэш по "iframeUrl|autoQualityLabel", TTL 3 мин, 8 записей
```

Контракт `PlayerStreamExtractor`: `supports(url)` и `extract(request, context)`. Результат —
`PlayerStreamResolveResult`:

| Вариант          | Смысл                                                                                 |
|------------------|---------------------------------------------------------------------------------------|
| `Stream`         | Ссылка, заголовки, карта качеств, для CVH — `failoverHost`, для Alloha — дорожки      |
| `KodikBlocked`   | Kodik ответил страницей с сообщением (не 2xx): показывается оверлей с текстом         |
| `Unavailable`    | Источник ответил, что потока нет: `VideoNotFound`, `AccessForbidden`, `RegionBlocked` |
| `Failed(reason)` | Не удалось разобрать; `reason` — технический шаг, пользователю не показывается        |
| `Unsupported`    | Ни один экстрактор не подошёл                                                         |

Только `Stream` попадает в кэш резолва.

### Определение по URL

`supports` — подстрока в URL без учёта регистра:

| Балансер | Признак                                          |
|----------|--------------------------------------------------|
| Kodik    | `kodik`                                          |
| Aksor    | `aksor.tv`                                       |
| CVH      | `iframecvh` или строка ровно `cvh`               |
| Alloha   | `alloha`                                         |
| VK       | `vk.com`, `vkvideo`, `video_ext.php`, `iframevk` |
| Rutube   | `rutube.ru`                                      |
| Sibnet   | `sibnet.ru`                                      |
| Zedfilm  | `zedfilm.ru`, `hlamer.ru`                        |

Экстракторы лежат в `Set<PlayerStreamExtractor>`, `DefaultPlayerStreamRepository` берёт первый,
у которого `supports(url)` истинно.

`playerDisplayOrderPriority()` задаёт порядок показа в выборе плеера: CVH (0), Kodik (1), остальные
(2).

### Общие утилиты (`extractor/common/`)

- `PlayerHttpClient.fetchText` / `fetchJson` — GET с `throwOnFailure`.
- `orderQualityMap(raw, keys, keyAliases)` — упорядочивает качества по `STANDARD_QUALITY_KEYS`
  (`auto`, `144p` … `2160p`), неизвестные метки идут в конец.
- `withAutoQualityLabel(label)` — переименовывает ключ `auto` в локализованную метку из запроса.
- `ExtractedStream(url, headers, qualities)` и `toResolveResult()` — общая форма до маппинга в
  domain.
- `PlayerExtractorLog` — `logExtractorFailure(source, url, step, throwable)`.

Выбранный по умолчанию поток — последняя запись карты качеств (самое высокое качество).

### Заголовки и User-Agent

Каждый экстрактор берёт UA из `BrowserUserAgentProvider` в момент запроса (настройка «User-Agent
балансеров», см. [cvh-player.md](cvh-player.md), §4). Поток получает заголовки, с которыми его
резолвили, поэтому смена UA действует со следующего резолва.

## Kodik

HLS. Нет Auto-качества: у каждого качества свой манифест (`…/720.mp4:hls:manifest.m3u8`),
переключение качества меняет URL.

Цепочка в `KodikExtractor`:

1. GET iframe-страницы (`Referer: https://yani.tv/`), забираются cookies. Не 2xx → `KodikBlocked` с
   текстом из `<div class="message">`.
2. Из страницы достаются `urlParams`, `videoInfo.type`, `videoInfo.hash`, `videoInfo.id` и путь
   скрипта плеера `app.player_single…`. Любое отсутствие → `Failed` с названием шага.
3. Путь эндпоинта зашит в скрипте плеера как `atob("…")` (base64). Он разбирается из скрипта и
   кэшируется по URL скрипта: скрипт версионирован в URL, и без кэша каждый resolve качал бы сотни
   КБ JS. Если ничего не декодировалось — путь по умолчанию `/ftor`.
4. POST формы на эндпоинт с `d`, `d_sign`, `pd`, `pd_sign`, `ref`, `ref_sign`, `type`, `hash`, `id`
   и cookies страницы. Cookies обязательны для авторизации `/ftor`.
5. Ответ — `links["240".."1080"][0].src`. Ссылка либо обычная (`//…`), либо закодирована: ROT18 по
   буквам и base64.
6. `HEAD` каждого качества параллельно (`resolveQualityStreamUrl`): если в манифесте реальное
   качество ниже заявленного, подставляется URL с заявленным и проверяется на доступность. Иначе
   метка исправляется на фактическую.

Если эндпоинт не отдал ссылок, закэшированный путь сбрасывается: он мог устареть на стороне Kodik, и
следующий resolve перечитает скрипт.

Превью серий (`core/utils/.../kodik/`: `KodikThumbnail`, `KodikThumbnailFetcher`,
`ResolveKodikThumbnailUrlUseCase`, дисковый кэш) строятся по kodik-iframe серии (ключ кэша
`kodik_thumb:<нормализованный iframe>`); код лежит в `core:utils`, а не в фиче плеера.

## VK

Страница плеера (`vk.com/video_ext.php?...`). Экстрактор:

- берёт iframe, ищет в нём ссылку на `video_ext.php`, грузит её и разбирает исходник;
- качества вытаскиваются regex'ами: `url1080`/`mp4_1080` … `url240`/`mp4_240`, затем `hls_fmp4`,
  `hls`, общий `url`;
- если ни одной ссылки нет, а на странице есть `id="video_ext_msg"`, это не сбой разбора, а заглушка
  VK. Сообщение «не найден» (или его отсутствие) даёт `Unavailable(VideoNotFound)`, другая причина
  (приватное, региональное) — `Unavailable(message)` с собственным текстом VK. Маркер ASCII-шный: не
  зависит от кодировки страницы;
- заголовки потока строятся от реального `sourceUrl` страницы.

## Rutube

1. `videoId` — 32 hex-символа из URL.
2. `GET https://rutube.ru/api/play/options/<id>/?no_404=true`.
3. Поток — `video_balancer.m3u8` или `video_balancer.default` (строка `"null"` считается пустой).
4. Мастер-плейлист разбирается по `#EXT-X-STREAM-INF` и `RESOLUTION=WxH`, качество — по высоте
   (144…2160). Нераспознанная высота пропускается.
5. Если мастер не загрузился, остаётся одно качество `auto` (метка из запроса).

## Sibnet

Прогрессивный mp4.

- GET страницы плеера (`Referer: https://yani.tv/`). `403` → `Unavailable(AccessForbidden)`:
  Sibnet закрыл страницу для этой сети, видео не при чём, повтор не поможет.
- Ссылка ищется по двум паттернам: `player.src([{src: "…"}])` и `<source src="…">`. Относительные
  ссылки приводятся к абсолютным по `playerUrl`.
- Заголовки потока: `Referer` страницы плеера, `Origin: https://video.sibnet.ru`, `User-Agent`.
- Качеств нет, одна ссылка.

## Aksor

- Идентификатор — `hash` из пути `…/video/<hash>` (или последний сегмент).
- Основной путь: `GET https://player.aksor.tv/api/video/<hash>` → `qualities` с ключами `q360`,
  `q480`,
  `q720`, `q1080`, `q2k`, `q4k`. Метки: `360p` … `1080p`, `2K`, `4K`.
- Запасной путь: страница плеера. Ищется `meta`-ссылка на видео, затем скрипты страницы.
- CDN-пути могут содержать пробелы (папки озвучек вроде `JAM CLUB`). Браузеры кодируют их сами,
  HTTP-стек Android отправляет как есть, и часть узлов отвечает `404`. Поэтому пробелы заменяются на
  `%20` в `sanitizeStreamUrl`.

## Zedfilm

Только статический разбор страницы iframe, WebView-фолбэка нет.

1. GET iframe-страницы (`Referer: https://yani.tv/`, `Accept-Language: ru-RU,…`,
   `Accept-Encoding: gzip`), тело декодируется как `windows-1251`. Без языка и без gzip сайт отвечает
   404 «Видео не найдено» на живую ссылку (замер curl на `zedfilm.ru/<id>`), поэтому оба заголовка
   заданы явно. gzip безопасен только на клиенте с плагином `ContentEncoding`
   (`NetworkModule.provideHttpClient`): он и декодирует ответ. В заголовки потока (`Stream.headers`)
   эти два заголовка не попадают: поток читает Media3, и для медиа у него `identity`.
   На странице есть `video_Init('<base64>')` с JSON: `url` (DASH `.mpd`), `url2` (mp4 запасной), `dash`, `type`,
   `tracks`.
2. Дополнительно перебираются regex'ы по HTML: `.mpd`, `.m3u8`, `.mp4` в тексте, в
   `file/src/source`, в `<source>`. Рекламные ссылки отбрасываются (`doubleclick`, `yandex`, а
   также `ads`/`ima` как отдельные слова в пути).
3. Качество определяется по тексту URL без хоста (`144…2160`, `p` необязателен), иначе `auto`.
4. Если кандидатов нет или страница не загрузилась, результат `Failed("Zedfilm: no stream found")`, а
   причина пишется в лог (`failed to load iframe page` или `no stream URLs found in iframe page`).

Хосты: `zedfilm.ru` и `hlamer.ru`, референс `https://yani.tv/`.

## Сводка

| Балансер | Формат                         | Заголовки потока (из кода)                                  | Особый исход                               |
|----------|--------------------------------|-------------------------------------------------------------|--------------------------------------------|
| Kodik    | HLS, манифест на качество      | только `User-Agent` (`userAgents.streamHeaders()`)          | `KodikBlocked`                             |
| CVH      | mp4 (okcdn)                    | только `User-Agent` (`userAgents.streamHeaders()`)          | `failoverHost` в `Stream`                  |
| Alloha   | HLS через loopback-прокси      | сессия, [alloha-player.md](alloha-player.md)                | `openSession`, `AllohaSourceUnavailable`   |
| VK       | mp4 / HLS (`hls`, `hls_fmp4`)  | `Referer` источника, `Origin: https://vk.com`, `User-Agent` | `Unavailable` при `video_ext_msg`          |
| Rutube   | HLS                            | `Referer`, `Origin: https://rutube.ru`, `User-Agent`        | Мастер не загрузился → одно качество       |
| Sibnet   | mp4                            | `Referer`, `Origin: https://video.sibnet.ru`, `User-Agent`  | 403 → `Unavailable(AccessForbidden)`       |
| Aksor    | mp4 по качествам `q360…q4k`    | `Referer`, `User-Agent`                                     | Запасной разбор страницы                   |
| Zedfilm  | DASH `.mpd` / mp4 / m3u8       | `Referer`, `Origin: https://hlamer.ru`, `User-Agent`        | Нет потока на странице → `Failed`          |

## Где регистрируются балансеры

- Определение по URL: `isXxxPlayerUrl()` и `isSupportedPlayerUrl()` в `PlayerUrlUtils.kt`;
  порядок показа — `playerDisplayOrderPriority()`.
- Реализации: `@Binds @IntoSet` в `PlayerDataModule` (`feature/player/data/.../di/`), потребитель —
  `Set<PlayerStreamExtractor>` в `DefaultPlayerStreamRepository`.
- Ошибки пишутся через `analyticsTracker.logExtractorFailure(source, url, step, throwable)`.
- Карта качеств — `LinkedHashMap`; общие помощники `orderQualityMap` и `withAutoQualityLabel` в
  `extractor/common/QualityMap.kt`.
