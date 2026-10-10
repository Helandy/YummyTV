# Continue Watching

Правила выбора карточек «Продолжить просмотр». Список строится из локальных записей
`watch_progress`. Home feed и cache не выбирают отдельный remote-кандидат: `YaniHomeFeedRepository`
берёт уже подготовленные локальные элементы и подмешивает их в `HomeFeed`.

## Какие записи отображаются

Local-запись `watch_progress` попадает в список в одном из случаев:

- meaningful progress: `durationMs > 0`, `positionMs >= 30_000`, запись ещё не watched;
- unresolved progress: `durationMs = 0`, `positionMs >= 30_000` и есть `videoId`, `episode` или
  `episodeUrl`;
- continue target: `positionMs = 0`, `durationMs = 0`, непустые `episode` и `episodeUrl`.

## Кандидат внутри одного тайтла

Если для `animeId` несколько local-записей, `ContinueWatchingMerge.bestByAnime()` выбирает самую
дальнюю. Порядок приоритета:

1. Если у обеих распознаётся номер серии, побеждает более дальняя серия.
2. Если одна запись — continue target, а другая нет, `updatedAt` применяется только для сравнения
   target и progress.
3. Затем progress score, `positionMs`, `updatedAt`.

Пример: `1 серия` 19/25; `2 серия` 24/25, watched; `3 серия` 4/25. Результат — `3 серия`, 4/25. Если
после этого пользователь кликнул `1 серию`, кандидат остаётся `3 серия`: внутри local-истории
дальняя серия важнее свежести клика.

## Watched

Watched — meaningful progress, где до конца серии не больше 5 минут. Для серий длительностью 5 минут
и меньше: `positionMs / durationMs >= 0.90`.

При сохранении watched-прогресса плеер нормализует снимок до полного просмотра
(`positionMs = durationMs`). После этого `ContinueWatchingMerge.filterDisplayable()`:

- убирает саму watched-запись из списка;
- скрывает записи того же `animeId`, если watched-запись свежее или равна по `updatedAt`, а
  display-кандидат не дальше неё.

Так досмотренная серия не показывается, а более дальний continue target или прогресс следующей серии
остаются.

### Настройка «Следующая серия»

Достигнув watched-порога, текущая серия сохраняется как watched. Затем:

- настройка включена и следующая серия есть: плеер создаёт continue target для неё
  (`positionMs = 0`, `durationMs = 0`);
- настройка выключена или следующей серии нет: плеер скрывает тайтл через display suppression.
  История просмотра серий не удаляется.

## Home feed и cache

`observeContinueWatching()` возвращает локальный список из `WatchProgressStore`. В
`YaniHomeFeedRepository`:

- при чтении cache локальные элементы подставляются поверх cached feed;
- при refresh remote feed сохраняется без remote Continue Watching merge;
- `HomeContinueWatchingItem` маппится из `WatchProgressEntry` и сохраняет `positionMs`,
  `durationMs`,
  `videoId`, `episodeUrl`, player/dubbing и screenshot metadata.

Если для `animeId` в подготовленном списке всё ещё несколько записей, Home оставляет самую свежую по
`updatedAt`, затем по `positionMs`, `videoId` и `episode`.

## Ручное удаление

Удаление из Home/Library не блокирует `animeId` навсегда, а сохраняет suppression timestamp: записи
с
`updatedAt <= suppressedAt` скрываются, с `updatedAt > suppressedAt` снова отображаются. Новая
активность возвращает тайтл: `WatchProgressStore.save()` и `saveContinueTarget()` снимают
suppression для `animeId` перед сохранением.

Благодаря этому работает сценарий с несколькими устройствами: удалил тайтл на одном, продолжил позже
на другом, и тайтл снова виден.

## Запуск из карточки

`ResolveContinueWatchingLaunchUseCase` загружает список видео тайтла и возвращает нейтральную
playback-цель. Presentation превращает её в destination плеера.

- Позиция возобновления берётся из `positionMs`.
- Источник выбирается по `videoId`, затем `episodeUrl`, затем по совпадению `episode + player +
  dubbing`, затем по `episode`. Если exact-кандидат не поддерживается, берётся поддерживаемый
  источник той же серии или первый поддерживаемый.
- Запись с placeholder episode может быть мигрирована на доверенный реальный episode, если совпал
  `videoId` или `episodeUrl`.
