# Источник «Mikai»

Балансер mikai.me (украинская озвучка и субтитры). Модуль `source/mikai/data` необязательный:
подключается только вместе с
`source/availability`, `source/ashdi` и `source/fenix` (см. `settings.gradle.kts`, в
`app/build.gradle.kts` нужны
`:source:availability:data`, `:source:ashdi:data`, `:source:fenix:data` и `:source:mikai:data`).
Серверная часть и расписание обхода: `YummyTV_API/docs/mikai.md`. Собственного domain-модуля у Mikai
нет: поток разбирают общие `source/ashdi` и `source/fenix` (их же
использует [AnimeON](animeon-source.md)).

## Как работает

- `MikaiExtraVideoRepository` берёт наличие через `FindSourceAvailabilityUseCase("mikai", …)`.
  Вариант — команда на одном провайдере плеера: `key` = `провайдер:id команды`, `title` — имя
  команды, у серии `id` — запись в плеере (`/vod/<id>`),
  `url` — страница плеера. В список попадают варианты провайдеров **Ashdi** (`ashdi:…`) и **Fenix**
  (`fenix:…`). У Ashdi серии без числового id (его берут и из `url`) пропускаются, видео получают
  `iframeUrl = source://mikai/ashdi/{id}`. У Fenix `id` — токен серии из
  `https://fenixplay.xyz/vod/<токен>` (его берут и из `url`, отбросив `#embed=…`), `iframeUrl =
  source://mikai/fenix/{токен}`, к названию команды добавляется « (Fenix)», чтобы отличать её от той
  же команды в Ashdi. Отрицательный id у видео (прогресс локальный). Субтитры Ashdi (`kind = subs`)
  получают к названию команды пометку « (субтитры)»; субтитры Fenix пропускаются: их `.vtt`
  приложение не подключает, серия играла бы без текста.
- `MikaiExtraStreamRepository` для Ashdi строит `https://ashdi.vip/vod/{id}` и отдаёт его
  `ResolveAshdiStreamsUseCase`: `file:` на странице → HLS-мастер → карта качеств по возрастанию
  (лучшее по умолчанию — последнее). Для Fenix строит
  `https://fenixplay.xyz/vod/{токен}` и отдаёт его `ResolveFenixStreamsUseCase`: `FENIX_PLAYLIST`
  страницы → мастер озвучки, чей `vod_url` равен адресу страницы → `?token=` из
  `FENIX_MEDIA_ACCESS`; мастер играет адаптивно (звук отдельной дорожкой, карты качеств нет).
  Заголовки не нужны.
- Нет файла — `Unavailable` («серия недоступна»), гео-блок — `Unavailable` с отдельным текстом
  («недоступно в вашем регионе»), сетевой сбой — `Failed`. Тексты en/ru/uk.

## Ограничения

- Гео-блок Ashdi (RU, BY) — как у AnimeON, см. [animeon-source.md](animeon-source.md).
- Fenix из РФ играет без VPN (проверено вручную: страница, мастер, сегменты). Токен доступа живёт
  сутки, поэтому страница читается при каждом воспроизведении.
- Не проверено вживую: Ashdi из РФ закрыт, поэтому формат записей `id`/`url` у серий сверен только с
  документацией сервера и тестами, а не с реальным ответом `/api/availability`.
