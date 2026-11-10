# Tenrai: пометки «Филлер/Рекап»

Tenrai (`api.tenrai.org`) — Jikan-совместимый API с данными MyAnimeList. Клиент берёт у него
пометки филлер/рекап для серий тайтла. Это не источник видео, поэтому модуль лежит не в `source/`,
а в `external/tenrai` (Gradle-путь `:external:tenrai`). Публичный Jikan закрыт 1 октября 2026;
Tenrai отдаёт те же ответы без ключа, до 4 запросов в секунду и 120 в минуту.

## Как устроено

- Контракт `EpisodeFlagsSource` лежит в `:feature:details:domain` (`.domain.anime.repository`):
  `getEpisodeFlags(malId, onFirstPage)` возвращает карту «номер серии строкой → `EpisodeFlags`»,
  серии без пометок опущены. Набор реализаций объявлен `@Multibinds` в `ExtraVideoRepositoriesModule`,
  поэтому он может быть пустым.
- `TenraiEpisodeFlagsSource` (`external/tenrai`) постранично читает `/v1/anime/{malId}/episodes`:
  первая страница нужна, чтобы узнать число страниц, остальные качаются параллельно (не более 3),
  старты запросов разнесены не реже, чем на 260 мс (~3,8 в секунду на все тайтлы сразу). 404 значит
  «серий нет». Ошибка любой страницы (в том числе 429) пробрасывается: неполный список в кэш не попадает.
  `onFirstPage` отдаёт пометки первой страницы, пока грузятся остальные, чтобы длинные тайтлы не ждали.
- `YaniAnimeRepository.observeAnimeEpisodeFlags` берёт первый источник из набора и кэширует результат
  в `DocumentCacheStorage` (`anime-mal-episodes-v4:{malId}:finished|ongoing`, TTL 7 дней). Документ кэша —
  `EpisodeFlagsBackupDto`, то есть только пометки; от источника он не зависит.
- Резерв: нет модуля или Tenrai не ответил (и свежего кэша нет) — пометки берутся из копии на YummyTV API
  (`YummyEpisodeFlagsApi`, `/episode-flags/{malId}`) с отдельным кэшем `anime-mal-episodes-backup-v1` на сутки.
  Картинок серий у резерва нет, а Tenrai-модуль их и не возвращает.
- Пометки нужны экрану деталей и плашке «Филлер/Рекап» в плеере; это необязательная надстройка,
  экран работает и без них.

## Отключение

В `gradle.properties`:

```
yummytv.external.tenrai=false
```

Модуль не подключается к сборке, в APK его нет, пометки идут из резерва на нашем API. Мастер-выключатель
`yummytv.source.enabled` Tenrai не затрагивает. То же получится, если удалить каталог `external/tenrai`:
`settings.gradle.kts` (`externalEnabled`) и `app/build.gradle.kts` (`findProject`) это переносят.

## Тесты

`TenraiEpisodeFlagsMapperTest` — в пометки попадают только серии с `filler`/`recap`, номер 0 отбрасывается;
`EpisodeFlagsBackupMapperTest` — то же для документа резерва.
