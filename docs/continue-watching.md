# Continue Watching

Этот документ фиксирует текущее техническое поведение карточек "Продолжить просмотр".
Он описывает правила выбора display-кандидата, а не пользовательский FAQ.

Список строится из локальных записей `watch_progress`. Home feed и cache не выбирают отдельный
remote-кандидат для Continue Watching: `YaniHomeFeedRepository` берет уже подготовленные локальные
элементы и подмешивает их в `HomeFeed`.

## Какие записи отображаются

В Continue Watching попадают local-записи `watch_progress`, если это один из случаев:

- meaningful progress: `durationMs > 0`, `positionMs >= 30_000` и запись еще не watched;
- unresolved progress: `durationMs = 0`, `positionMs >= 30_000` и есть `videoId`, `episode`
  или `episodeUrl`;
- continue target: `positionMs = 0`, `durationMs = 0`, непустые `episode` и `episodeUrl`.

Watched-запись не отображается сама и может скрыть более ранние
или не более дальние записи того же `animeId` через `ContinueWatchingMerge.filterDisplayable()`.

## Local-кандидат внутри одного тайтла

Если для одного `animeId` есть несколько local-записей, выбирается самая дальняя запись через
`ContinueWatchingMerge.bestByAnime()`.

Порядок приоритета:

- если у обеих записей распознается номер серии, побеждает более дальняя серия;
- если одна запись является continue target, а другая нет, `updatedAt` применяется только для
  сравнения target-vs-progress;
- затем сравниваются progress score, `positionMs` и `updatedAt`.

Пример:

- `1 серия`: `19 / 25`;
- `2 серия`: `24 / 25`, watched;
- `3 серия`: `4 / 25`.

Local result: `3 серия`, `4 / 25`.

Если пользователь после этого кликнул по `1 серии`, local-кандидат все равно остается `3 серия`,
потому что внутри local-истории "самая дальняя серия" важнее свежести клика.

## Watched-записи

Watched-запись - это meaningful progress, где до конца серии осталось не больше 5 минут.
Для серий длительностью 5 минут или меньше используется fallback: `positionMs / durationMs >= 0.90`.

При сохранении watched-прогресса player нормализует снимок до полного просмотра:
`positionMs = durationMs`. После этого `ContinueWatchingMerge.filterDisplayable()`:

- удаляет саму watched-запись из display-списка;
- скрывает записи того же `animeId`, если watched-запись свежее или равна по `updatedAt` и
  display-кандидат не дальше watched-записи.

Это позволяет не показывать уже досмотренную серию, но оставить возможность показать более дальний
continue target или прогресс следующей серии.

## Home feed/cache

`observeContinueWatching()` возвращает локальный список из `WatchProgressStore`.

`YaniHomeFeedRepository` использует этот список для Home:

- при чтении cache локальные Continue Watching элементы подставляются поверх cached feed;
- при refresh remote feed сохраняется без remote Continue Watching merge;
- `HomeContinueWatchingItem` маппится из `WatchProgressEntry` и сохраняет `positionMs`,
  `durationMs`, `videoId`, `episodeUrl`, player/dubbing и screenshot metadata.

Если для одного `animeId` в подготовленном списке все еще есть несколько записей, Home оставляет
самую свежую по `updatedAt`, затем по `positionMs`, `videoId` и `episode`.

## Manual suppression

Ручное удаление из Continue Watching не блокирует `animeId` навсегда.

Удаление из Home/Library сохраняет suppression timestamp:

- записи с `updatedAt <= suppressedAt` скрываются;
- записи с `updatedAt > suppressedAt` снова отображаются.

Новая активность возвращает тайтл в список: `WatchProgressStore.save()` и
`WatchProgressStore.saveContinueTarget()` удаляют suppression для `animeId` перед сохранением.

Это позволяет сценарию с несколькими устройствами работать ожидаемо: если пользователь удалил тайтл
на одном устройстве, а потом позже продолжил его на другом, более свежая активность снова покажет
тайтл в Continue Watching.

## Настройка "Следующая серия"

Завершение текущей серии означает достижение watched-порога: до конца осталось не больше 5 минут,
а для серий длительностью 5 минут или меньше - прогресс не меньше 90%. Когда серия достигает этого
порога, текущая серия сохраняется как watched с `positionMs = durationMs`.

Если настройка "Следующая серия" включена и есть следующая серия, player дополнительно создает
continue target для следующей серии: `positionMs = 0`, `durationMs = 0`.

Если настройка выключена или следующей серии нет, player скрывает тайтл из Continue Watching через
display suppression. История просмотра серий при этом не удаляется.

## Запуск из карточки

При выборе карточки `ContinueWatchingLaunchHandler` загружает список видео тайтла и резолвит
актуальный источник через `resolveContinueWatchingTarget()`.

Правила запуска:

- позиция возобновления берется из `positionMs`;
- актуальный источник выбирается по `videoId`, затем `episodeUrl`, затем по совпадению
  `episode + player + dubbing`, затем по `episode`; если exact-кандидат не поддерживается,
  выбирается поддерживаемый источник той же серии или первый поддерживаемый источник;
- если запись была сохранена с placeholder episode, она может быть мигрирована на доверенный
  реальный episode, когда совпадает `videoId` или `episodeUrl`.
