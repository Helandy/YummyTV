# Источник «AniLibria»

Родные потоки AniLibria как ещё один балансер внутри озвучки AniLibria. Модули `source/anilibria/{domain,data}` —
необязательные (как `source/anime4k`): нет каталога — приложение собирается без них.

## Как работает
- `AniLibriaExtraVideoRepository` (`ExtraVideoRepository`) ищет релиз: `GET aniliberty.top/api/v1/app/search/releases`
  по названиям тайтла (до 3 запросов), берёт запись с `mal.id == details.malId` без гео/копирайт-блокировки,
  затем `GET /anime/releases/{id}` → серии. Видео получают `iframeUrl = source://anilibria/{releaseId}/{ordinal}`,
  отрицательный id (прогресс локальный), тайминги опенинга из `opening`.
- `requiresMatchingDubbing = true`: после `mergeExtraDubbings` остаются только видео озвучек, которые есть у Yani
  (`retainBaseDubbings`). Нет озвучки AniLibria у Yani — балансер не показывается.
- `AniLibriaExtraStreamRepository` (`ExtraStreamRepository`) отдаёт `hls_480/720/1080` как `qualities` (по возрастанию).
- Кэш релиза в памяти, 5 минут. Сетевой сбой не кэшируется.

## Ограничения
- Нужен `malId` тайтла; без него источника нет.
- Спешлы с дробным номером серии пропускаются. Тайминги эндинга не используются (в API они от конца серии).
- Ссылки несут `isWithVideoAds*`; плейлисты с этими параметрами и без них одинаковы (проверено), рекламы в них нет.
