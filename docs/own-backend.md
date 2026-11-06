# Свой сервер: что приложению нужно от API (TMDB, наличие озвучек, статика)

Сервер YummyTV **не прокси**. Это индекс и кэш: он заранее обходит сайты, хранит «что где есть» и
отдаёт приложению ссылки. Видео и HLS он не хранит и не отдаёт, пользователей к чужим сайтам не
проксирует, поиска по сайтам в реальном времени нет. Плейлисты и потоки приложение берёт у источника
само.

Мой сервер вам использовать не нужно: **сделайте свой**. Приложение знает только контракт трёх ручек
ниже, поэтому подойдёт любая реализация с тем же JSON.

Без своего сервера приложение работает. Что именно пропадает (проверено по коду):

- названия и описания серий (ручка 1): `getAnimeEpisodeInfo` при сбое отдаёт пустую карту;
- внешние озвучки и источники, которые спрашивают наличие у сервера (ручка 2): AiLiberty, AniLibria,
  Amedia, AniDub, AnimeON, Mikai, Animevost, Animebesst, Dreamerscast, Sameband, StudioMIR, Aniboom,
  Anistar; anime4k ходит на свои ручки того же сервера;
- копии картинок на вашем хосте (ручка 3), но это запасной вариант.

Что остаётся: превью серий берутся у Kodik (кадры TMDB отсутствуют, цепочка идёт дальше);
скриншоты деталки приходят от yani (`ScreenshotSourcesMapper`: нет кадров TMDB, остаются yani); сам
yani, Kodik и остальные источники, не зависящие от сервера, и плеер работают как обычно.

## Как указать свой сервер

Настоящие адреса в git не хранятся, в репозитории заглушки `self.made`. Свои значения кладутся в
`local.properties`:

| Ключ                 | Что                                | По умолчанию (заглушка)     |
|----------------------|------------------------------------|-----------------------------|
| `yummytv.apiBaseUrl` | база API, вместе с `/api` на конце | `https://api.self.made/api` |
| `yummytv.staticHost` | хост раздачи картинок без схемы    | `static.self.made`          |

Читаются в `core/network/build.gradle.kts` (`YUMMY_TV_API_BASE_URL`, `STATIC_HOST`). Базу API можно
ещё подменить удалённо через feature-флаги (`SiteApiConfig.apply`): принимается только `https`,
пустое значение игнорируется. Подпись запросов: интерфейс `SiteRequestSigner` в
`core/network/yummy`; реализацию (секрет и сертификат APK) даёт отдельный модуль, без неё запросы
уходят неподписанными. Свой сервер может подпись не требовать.

## 1. TMDB: серии, описания, превью

- Клиент: `YummyEpisodesApi` (`feature/details/data`),
  `GET {baseUrl}/anime/mal/{malId}?lang=ru|uk|en`, подписанный.
- `404` клиент считает ответом «серий нет» (нет маппинга MAL → TMDB), а не ошибкой. Остальные коды
  это ошибка.
- Ответ (DTO `YummyEpisodesDto`): `malId`, `tmdbId`, `seasonNumber`, `episodeOffset` (смещение,
  которое сервер уже применил к номерам серий), `episodes[]` (`episodeNumber`,
  `title`, `overview`, `previewUrl`, `previewMirrorUrl`, `stills`). Кадры `stills` нужны только у
  серии 1 (ряд «Скриншоты», у каждого `mirrorUrl`). Других полей приложение не читает: если ваш
  сервер отдаёт
  ещё бэкдропы, постеры или логотипы сериала (`images`), клиент их игнорирует.
- Превью серии приложение пробует по цепочке (`DefaultEpisodeThumbnailResolver`): `previewUrl` (CDN
  TMDB) → `previewMirrorUrl` (ваш хост статики) → Kodik. Не получилось скачать один вариант, берётся
  следующий.
- **Сервер не проксирует картинки TMDB**: в ответе только ссылки на `image.tmdb.org`, приложение
  грузит их напрямую (у части пользователей CDN заблокирован, отсюда DoH в `ImageHostsDns` и
  запасные источники).
- Для своего сервера нужны свой токен TMDB (v4) и сопоставление MAL → TMDB (мы берём датасет
  Fribb/anime-lists).

## 2. Available: наличие озвучек

- Клиент: модуль `source/availability` (`AvailabilityApi`, `FindSourceAvailabilityUseCase`),
  подписанный `POST {baseUrl}/availability`. Подробно: [ailiberty-source.md](ailiberty-source.md).
- Запрос: названия (латинский оригинал первым), `malId`, год, тип, число серий. Ответ: `sources[]`
  (`sourceId`, `key`, `url`, `episodesAvailable`, `episodesTotal`, `ongoing`, `qualities`,
  `variants` с озвучками и сериями, `mirrors`) и `failed`.
- Пример ответа (сокращён):

  ```json
  {
    "sources": [
      {"sourceId": "ailiberty", "key": "dorohedoro", "url": "https://ailiberty.top/releases/dorohedoro",
       "episodesAvailable": 12, "episodesTotal": 12, "ongoing": false, "qualities": ["480p", "720p", "1080p"]}
    ],
    "failed": []
  }
  ```

  `variants` (необязательно) описывает озвучки со своими сериями:
  `{"key": "9006", "kind": "voice|dubbing", "episodes": [{"number": 1, "durationSec": 1560}]}`.
  Нет `variants` — одна озвучка, серии `1..episodesAvailable`.
- **Один запрос на тайтл для всех балансеров**, кэш 5 минут. Источник в `failed` это ошибка
  (`IOException`), а не «тайтла нет»: он не пропадает из выдачи.
- Индекс лежит на сервере, ссылок на потоки в нём нет (они подписаны и протухают). Приложение
  открывает `url` и само вынимает плейлист в момент воспроизведения.
- Как подключить свой источник в приложении: свой `ExtraVideoRepository`, который зовёт
  `FindSourceAvailabilityUseCase("<sourceId>", lookup)`.
  Примеры: [ailiberty-source.md](ailiberty-source.md), [anidub-source.md](anidub-source.md), [animeon-source.md](animeon-source.md)
  и другие `*-source.md`. Своего `sourceId` в ответе сервера достаточно;

## 3. Static: постеры и кадры серий

Сервер статики только раздаёт готовые AVIF-файлы, ничего не проксирует: отдаёт
`/img/still/<hash>.avif` (кадры серий) и `/posters/mega/<id>.avif` (постеры yani), остальное `404`.
Чужие `Referer`/`Origin` получают `403`; приложение этих заголовков не шлёт.

- **Постеры.** Выбора качества нет, везде версия `mega` (AVIF, ~28 КБ). Если yani ответил `404`/
  `5xx` или сеть упала, `FallbackAsyncImage` переходит на копию: `posterMirrorUrl` меняет домен
  `static.yani.tv` на `yummytv.staticHost`, путь и `?v=…` остаются. Копия есть только у AVIF `mega`,
  а AVIF приходит, только если устройство его декодирует (`ImageFormatProbe`, заголовок
  `Accept: image/avif`). Подробности: [image-loading-and-memory.md](image-loading-and-memory.md),
  код `PosterMirrorUtils.kt` (`core/model`).
- **Кадры серий.** `previewMirrorUrl` из ответа TMDB-ручки. Файла может ещё не быть (`404`), тогда
  следующий источник в цепочке.
- Хост статики попадает в короткие таймауты картинок (3 с / 5 с, `imageHostTimeouts`), чтобы быстро
  уходить на запасной источник.
- Прочее по `/static/` на том же сервере (например декор аватарки) берётся от
  `SiteApiConfig.staticBaseUrl` = база API без `/api` + `/static/`.

Своя раздача делается любым статическим хостингом (nginx, S3, CDN): достаточно положить AVIF-файлы
по этим путям. Для постера `<id>` это число из имени файла в ссылке yani
(`posters/mega/1636989985.avif` → `/posters/mega/1636989985.avif`). Кадр серии лежит как
`/img/still/<hash>.avif`, где `hash` — первые 20 hex SHA-1 от пути кадра TMDB (`/abc.jpg`), и эту
ссылку сервер отдаёт в `previewMirrorUrl`. Наполнять хранилище можно чем угодно: скачать постер со
`static.yani.tv`, кадр с `image.tmdb.org` (`w780`), кадр перегнать в AVIF.

## Что проверить после подключения своего сервера

1. `local.properties` содержит `yummytv.apiBaseUrl` (с `/api`) и `yummytv.staticHost`, сборка
   пересобрана.
2. Экран деталей: у серий появились названия и превью (ручка 1).
3. У источников с серверным индексом (AiLiberty, AniLibria, Amedia и др.) появляются озвучки (ручка
   2).
4. Постер, который yani не отдал, подтягивается с вашего хоста (ручка 3).
