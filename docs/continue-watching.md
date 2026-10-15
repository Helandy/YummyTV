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

Watched — meaningful progress, где до конца серии осталось не больше допустимого остатка. Остаток
зависит от длины серии и настраивается (`WatchedThresholds`, настройки плеера):

| Длина серии       | Остаток по умолчанию | Диапазон настройки |
|-------------------|----------------------|--------------------|
| до 10 минут       | 1 минута             | 1–5 минут          |
| от 10 до 60 минут | 5 минут              | 1–15 минут         |
| больше 60 минут   | 10 минут             | 1–30 минут         |

Если серия не длиннее своего остатка, применяется запасное правило
`positionMs / durationMs >= 0.90`. Правило (`isWatchedProgress`,
`core/model/.../AnimeWatchProgress.kt`) вызывается синхронно из storage и presentation, поэтому
настройка зеркалится в процесс (`WatchedEpisodeRule`,
`WatchedEpisodeRuleSync` при старте), а не передаётся параметром.

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

## Серверная история на главной (last-watches)

Только главная: библиотека, виджет, ярлыки и `yummytv://continue/{id}` остаются на локальном
`ObserveContinueWatchingUseCase`.

`ObserveHomeContinueWatchingUseCase` объединяет локальный список с `GET /video/last-watches`
(`YaniRemoteContinueWatchingRepository`, карточки с `source = REMOTE`):

- гость — серверного списка нет; при смене `yaniUserId` он сбрасывается сразу;
- сначала отдаётся кэш (user-scoped document cache, любой давности), затем ответ сервера
  (TTL 60 с); обновление просят `load()`, pull-to-refresh и возврат на экран; ошибка сети оставляет
  прошлый список;
- серверная карточка: позиция и прогресс-бар — из `end_time` (прогресс пользователя), длина — из
  `duration`, дата — `date × 1000`;
- слияние (`mergeWithRemote`): одна карточка на `animeId`, побеждает больший `updatedAt`, при
  равенстве — локальная; итог по убыванию даты, на главной не больше 8 карточек;
- эхо: после входа `AccountAuthHandler` выгружает локальную историю (`POST /video`), и сервер
  ставит ей время синхронизации. Серверная запись, совпавшая с локальной (тайтл, `episodeGroupKey`,
  позиция ±15 с), берёт более раннюю локальную дату — иначе старые тайтлы прыгают в начало;
- серверная запись побеждает, только если прогресс meaningful и не watched (те же
  `isMeaningfulProgress`/`isWatchedProgress`, что и для локальных). Досмотренная серверная серия не
  перебивает локальную цель «следующая серия» (после серии 2 на пороге watched остаётся «Серия 3,
  0:00»);
- досмотренная серверная серия, которая свежее локальной (или локальной нет) и не раньше её по
  номеру, ведёт себя как плеер на watched-пороге: при включённой настройке «Следующая серия»
  (`suggestNextEpisodeOnWatched`) карточка становится следующей серией (`REMOTE_NEXT_EPISODE`:
  номер — целая часть `episodeNumberOrNull()` + 1, позиция и длина 0, без скриншота); настройка
  выключена или номер не распознан — тайтл скрыт;
- ручное удаление работает и для серверных карточек: запись с `updatedAt <= suppressedAt` скрыта, на
  сервере ничего не удаляется.

Запуск REMOTE-карточки всегда идёт с серверным прогрессом, независимо от настройки
`refreshContinueWatchingProgressOnLaunch`: `/anime/{id}/videos` обновляется, и если там есть `watched`
не старше карточки, берётся его видео (с озвучкой) и позиция. Иначе серия ищется по
`episodeGroupKey()`, позиция — из `end_time`. Тоста о переключении и миграции placeholder-серии для
REMOTE нет. После просмотра плеер пишет локальный прогресс, и карточка становится LOCAL.

### Карточка следующей серии (REMOTE_NEXT_EPISODE)

`last-watches` не говорит, сколько у тайтла серий, а запрашивать `/anime/{id}/videos` на каждую
карточку главной дорого. Поэтому карточка показывается оптимистично, а существование серии
проверяется при клике (`ResolveNextEpisodeLaunchUseCase`):

- `/videos` грузится строго из сети (`FetchAnimeVideosUseCase`: `refreshAnimeVideos` без сети молча
  отдаёт кэш и для этого не годится), серия ищется по `episodeGroupKey()`, старт с 0:00; озвучка и
  плеер — как у последнего `watched` тайтла в `/videos`, если у этой серии такие есть;
- серии нет — `ResolveContinueWatchingLaunchUseCase` возвращает `null`: главная показывает тост
  «Серии N пока нет» и открывает детали тайтла. Если список пришёл с сервера и не пустой, тайтл ещё и
  скрывается (suppression, как ручное удаление) — досмотренный до конца сериал не висит карточкой
  несуществующей серии. Новая активность (просмотр здесь или запись на сервере свежее) вернёт его;
- нет сети — решение по кэшу `/videos` (`GetCachedAnimeVideosUseCase`), ничего не скрывается:
  старый кэш мог не знать о новой серии.

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

## Запуск извне: ярлыки и виджет

`yummytv://continue/{animeId}` запускает «продолжить просмотр» так же, как карточка на главной.
`ContinueWatchingDeepLinkResolver` открывает `ContinueWatchingLaunchDestination` — экран-лоадер
(`ContinueWatchingLaunchViewModel`, `:feature:home`), который берёт запись тайтла из
`ObserveContinueWatchingUseCase`, разрешает её через `ResolveContinueWatchingLaunchUseCase` и
заменяет себя плеером (`nav.replace`). Нет записи, цели запуска или запуск упал — детали тайтла.
Тост о переключении на серверный прогресс здесь не показывается: экран сразу уходит. Экран
зарегистрирован и в мобильном, и в ТВ-графе, потому что резолверы общие для обоих интерфейсов.

Мобильный интерфейс публикует две поверхности на том же списке (`app/.../android`), обе
обновляются, пока жива `MobileActivity`:

- **Ярлыки** (`AppShortcutsPublisher`): два последних тайтла + «Поиск» (`ACTION_SEARCH` →
  `SystemSearchIntentHandler`) + «Загрузки» (`yummytv://downloads`). Все динамические: статический
  `shortcuts.xml` требует литерального `targetPackage`, а у debug/releasedebug свой суффикс
  applicationId.
- **Виджет** (`ContinueWatchingWidget`, Glance, `SizeMode.Exact`): до трёх карточек по ширине
  (не уже ~110dp). Перерисовывает его `ContinueWatchingWidgetUpdater`; периодического обновления
  нет — список локальный и вне приложения не меняется.

Постеры для обеих поверхностей — software-bitmap через синглтон Coil (`ContinueWatchingPosterUtils`):
hardware-bitmap не сериализуется ни в иконку ярлыка, ни в RemoteViews.
