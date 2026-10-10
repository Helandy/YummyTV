# API yani

Как приложение работает с `https://api.yani.tv` (`YANI_BASE_URL`): клиент, заголовки и поля, у
которых есть особенности. Всё описано по коду: DTO, мапперы, KDoc и комментарии. Подписки — в
[subscriptions.md](subscriptions.md).

Разбор ответов лояльный: `ignoreUnknownKeys = true`, у большинства полей дефолты и `null`.

## Клиент

`buildYaniHttpClient` (`core/network/.../yani/YaniHttpClientFactory.kt`):

- заголовки на хост `api.yani.tv`: `X-Application`, `Lang`, `Authorization: Bearer <refresh-токен>`
  (если вызов не задал `Authorization` сам); значения из `YaniRequestHeaderCache`;
- таймауты: соединение 20 с, запрос 40 с, сокет 40 с;
- `HttpRequestRetry`: два повтора, пауза `500 мс × номер повтора`, только для `GET` (на 5xx и
  исключения, кроме `CancellationException`). Комментарий в коде: мутации могут быть неидемпотентны;
- `ContentNegotiation` с `YaniApiJson`, `ContentEncoding` (gzip, deflate).

`YaniApiJson`:

```kotlin
ignoreUnknownKeys = true
explicitNulls = false
coerceInputValues = true
```

Комментарий в коде: yani регулярно шлёт `null` там, где по схеме строка или число (например,
`sub.dubbing` в `/users/{id}/lists/subs`). Для non-null поля с дефолтом `explicitNulls` этого не
покрывает, и без `coerceInputValues` такой ответ роняет разбор целиком. Поля без дефолта (тела
запросов) флаг не затрагивает.

### Логи

В debug (`BuildConfig.DEBUG`) стоит Ktor `Logging` с `LogLevel.BODY`, логгер `Logger.ANDROID`
обёрнут в `SensitiveDataMaskingLogger`. Заголовки `Authorization`, `Cookie`, `Set-Cookie`,
`X-Application` маскируются. Подробности — [network-and-auth.md](network-and-auth.md).

## Видео

### `GET /anime/{id}/videos`

`AnimeVideo.episode` и `AnimeScreenshot.episode` нормализуются при входе в приложение
(`String.normalizedEpisodeNumber()` в `AnimeStorageMapper` и `YaniAnimeMapper`):

- KDoc: yani присылает одну и ту же серию и как `"02"`, и как `"1"`, из-за чего точные сравнения
  строк разваливают выбор озвучки и балансера;
- ведущие нули срезаются только у чисто числовых номеров, чтобы не портить `"OVA 1"` и подобные.

Для группировки и дедупликации используется `String.episodeGroupKey()`
(`trim().trimStart('0').ifEmpty { trim() }.lowercase()`), для числового сравнения —
`episodeNumberOrNull()` (поддерживает `,` как разделитель); оба в
`core/common/.../utils/episode/EpisodeUtils.kt`. `isPlaceholderEpisode()` считает заглушкой пустую
строку и `"-"`.

Поля `subscribed` и `watched` в видео — пользовательские, см.
[subscriptions.md](subscriptions.md).

### `PUT /video/{videoId}`

`YaniAccountApi.markWatched` → тело `YaniPutVideoBodyDto(time, duration, times)`, ответ
`YaniBooleanResponseDto`.

| Поле       | Тип        | Что отправляет клиент                                              |
|------------|------------|--------------------------------------------------------------------|
| `time`     | `Int`      | Позиция в секундах, зажата в `[0, duration − 10]`                  |
| `duration` | `Int`      | Длина серии в секундах                                             |
| `times`    | `List<Int>`| Секунды, которые ещё не были отправлены (дельта), по умолчанию пусто |

Реализация — `PlayerProgressHandler`. Комментарии в коде:

- «Сервер СУММИРУЕТ присланные `times`, поэтому каждую секунду шлём ровно один раз (дельта =
  watched − synced)»: `syncedSecondsByVideoId` хранит уже отправленные секунды;
- `WATCH_END_TOLERANCE_SECONDS = 10`: хвост эпизода, который на сервер не отправляется как
  позиция и секунда (как в веб-клиенте);
- `REMOTE_PROGRESS_SYNC_INTERVAL_MS = 10_000`; плановая отправка не встаёт в очередь за идущей
  (`syncMutex.tryLock()`), финальная (`force`) дожидается замка;
- секунда считается отправленной только при `accepted == true`;
- отправка форсируется один раз при достижении «просмотрено»
  (`completionAttemptedVideoIds`), после успеха повторы прекращаются (`completedRemoteVideoIds`).

Другие методы того же API: `POST /video` (`syncWatched`, список `YaniPostVideoItemDto`) и
`DELETE /video` (`removeWatched`, список id).

## Профиль

### `PATCH /profile`

Тело `YaniProfileUpdateBodyDto`:

```
about, bdate (String), sex (Int), lists_privacy (String),
hide { shiki, tg, vk, discord },        // Boolean
notifications { tg, vk }                // Boolean
```

Чтение (`GET /profile`) использует другие имена и типы:

| Что                     | Запись (`YaniProfileUpdateBodyDto`)                    | Чтение (`YaniProfileDto`)                                         |
|-------------------------|--------------------------------------------------------|-------------------------------------------------------------------|
| Приватность соцсетей    | `hide.shiki/tg/vk/discord`                             | `privacy.shiki_public/tg_public/vk_public/discord_public`         |
| Уведомления             | `notifications.tg`, `notifications.vk`                 | `notifications.telegram`, `notifications.vk` (`YaniProfileNotificationSettingsDto`) |
| Дата рождения           | `bdate: String`                                        | `bdate: Long?` (epoch-секунды)                                    |

`YaniProfileSettingsRepository.updateProfile` пишет `hide.* = !update.show*`, а читает
`privacy.*_public` как `show*` (`YaniSocialMapper`): то есть `hide` — инвертированное «показывать».
После записи профиль перечитывается отдельным `GET` (`accountRepository.refreshProfile()`).

Аватар и баннер: `uploadAvatar`, `uploadBanner`, `deleteAvatar`, `deleteBanner` по `userId`,
отдельно от `PATCH`.

## Списки и расписание

### Сезон

`YaniUserAnimeDto.season: JsonElement?`. KDoc: 1 — зима, 2 — весна, 3 — лето, 4 — осень, 0 —
неизвестен; тип сырой, потому что у yani он местами плавает между числом и слагом
(`winter`/`spring`/`summer`/`fall`), а несовпадение типа уронило бы разбор всего списка; разбирается
вручную в `toAnimeSeason`.

`AnimeSeason` (`core/model`): `WINTER("winter")`, `SPRING`, `SUMMER`, `FALL("fall")`;
`fromNumber(n) = entries.getOrNull(n - 1)` (ноль и вне диапазона дают `null`), `fromSlug(slug)`.
В `YaniAnimeDto` поле `season` — `Int?`.

### Следующая серия

KDoc `YaniUserAnimeDto.nextEpisode`: в списках пользователя дата выхода следующей серии — плоское
поле `next_episode` (epoch-секунды), а не блок `episodes` с `next_date`, как в `/anime/{id}` и
расписании. У завершённых тайтлов здесь дата последней вышедшей серии либо поля нет.

`releaseCountdown(nextDateEpochSeconds, nowEpochSeconds)` возвращает `null`, если даты нет или серия
уже вышла (дата в прошлом); иначе значение в днях, часах или минутах (`EpisodeReleaseCountdown`).
Маппер аккаунта берёт `nextEpisode?.takeIf { it > 0L }`.

### Расписание

`YaniScheduleAnimeDto`: `anime_id`, `title`, `poster`, `episodes { count, aired, next_date,
prev_date }`.

Дата последней вышедшей серии — `AnimeScheduleItem.lastAiredSeconds(nowSeconds)`
(`feature/home/domain/.../utils/HomeScheduleUtils.kt`):

```kotlin
if (airedEpisodes == null) return null
return listOfNotNull(previousDateEpochSeconds, nextDateEpochSeconds)
    .filter { it in 1..nowSeconds }
    .maxOrNull()
```

KDoc функции: основной источник `prev_date`, но у части тайтлов он нулевой, зато `next_date` уже в
прошлом (сервер не передвинул его после выхода серии), и тогда прошедший `next_date` и есть дата
последней серии; будущий `prev_date` отбрасывается; у анонсов без вышедших серий `prev_date`
заполнен датой-заглушкой, общей для пачки тайтлов, поэтому дата отдаётся только при `aired > 0`
(`airedEpisodes` не `null`).
