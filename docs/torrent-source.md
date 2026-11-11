# Источник «Торренты» (`source/torrent`)

Необязательный модуль (как `source/anime4k`): нет каталога `source/torrent` — нет и функции. Отключается
`yummytv.source.torrent=false` в `gradle.properties`.

## Что делает пользователь

1. Настройки → API → «Торренты»: тумблер (по умолчанию выключен), адрес TorrServer (+ логин/пароль), индексатор
   JacRed (по умолчанию `https://jac-red.ru`, ключ не нужен) или Jackett (адрес + API-ключ).
2. В выборе озвучки появляется пункт «Поиск торрента» для каждой серии.
3. Выбор → диалог со списком раздач (по сидам) → диалог с файлами раздачи (файл нужной серии первым) →
   плеер играет поток TorrServer.

## Как устроено

- `TorrentExtraVideoRepository` (`ExtraVideoRepository`) отдаёт по одному `AnimeVideo` на серию:
  `source://torrent/{animeId}/{episode}?q=…`, отрицательный id (прогресс локальный). Пункта нет, пока торренты
  не включены и не настроены.
- `confirmOpen` вызывается деталкой до перехода в плеер: `TorrentPicker` показывает диалоги выбора через
  `ChoiceDialogCenter` (`core/designsystem`, хост `ChoiceDialogHost` стоит рядом с `ConfirmDialogHost` в
  главных графах) и кладёт выбор в `TorrentSelectionHolder` (живёт 15 минут). Отмена = остаёмся на деталке.
- `TorrentExtraStreamRepository` (`ExtraStreamRepository`) строит адрес
  `{TorrServer}/stream/{имя}?link={hash}&index={n}&play`; если выбора нет (вход через «Продолжить»),
  показывает диалог сам.
- Индексаторы: JacRed `GET /api/v1.0/torrents?search=`, Jackett `GET /api/v2.0/indexers/all/results`.
  TorrServer: `POST /torrents` (`add`, затем опрос `get`, пока не появятся `file_stats`).
- Сеть: `@TorrentHttpClient` (`core/network`). Поток с TorrServer играет Media3 через общий стек плеера.

## Ограничения

- В `network_security_config` включён cleartext глобально (TorrServer обычно `http://192.168.x.x`).
- Раздачи часто сезонные: серия выбирается по номеру в имени файла, автосопоставления нет.
- Дефолтные индексаторы и их формат ответов проверены только по документации, на живом сервере не гонялись.
- Встроенный TorrServer в APK не поддерживается: нужен внешний адрес.
