# Аналитика и логи

Как приложение отправляет события и ошибки, что пишет в logcat и в файл логов, и что оттуда нельзя
выводить. Раньше эти описания повторялись в трёх документах плеера, теперь они здесь.

## Три канала

| Канал                 | Куда                                   | Включён                               | Что туда попадает                                                                                   |
|-----------------------|----------------------------------------|---------------------------------------|-----------------------------------------------------------------------------------------------------|
| События и ошибки      | AppMetrica                             | release                               | `track`, `reportError`                                                                              |
| Диагностика в logcat  | logcat, тег из вызова                  | debug                                 | Любой `log(tag)`, события «Would send…»                                                             |
| Файл логов приложения | `cacheDir/logs/{current,previous}.log` | release и debug, если включена запись | Теги из `PersistedLogTags` (release), logcat процесса (читает коллектор), необработанные исключения |

Выбор реализации — `AnalyticsModule` по `BuildConfig.DEBUG`:

| Интерфейс                   | debug                           | release                          |
|-----------------------------|---------------------------------|----------------------------------|
| `AnalyticsTracker`          | `LogcatAnalyticsTracker`        | `AppMetricaAnalyticsTracker`     |
| `AnalyticsInitializer`      | `NoOpAnalyticsInitializer`      | `AppMetricaAnalyticsInitializer` |
| `AnalyticsDeviceIdProvider` | `NoOpAnalyticsDeviceIdProvider` | `AppMetricaDeviceIdProvider`     |

В debug ничего не уходит в AppMetrica: вместо этого в logcat (тег `Analytics`) пишется
`Would send analytics event: <имя>, params=<…>`.

## AnalyticsTracker

```kotlin
interface AnalyticsTracker {
    fun track(eventName: String, params: Map<String, String> = emptyMap())
    fun reportError(message: String, throwable: Throwable, groupIdentifier: String? = null)
    fun log(tag: String, throwable: Throwable? = null, message: () -> String)
}
```

| Метод         | Поведение в release                                                                                |
|---------------|----------------------------------------------------------------------------------------------------|
| `track`       | `AppMetrica.reportEvent`; пустое имя игнорируется                                                  |
| `reportError` | `AppMetrica.reportError` (с группой или без) и `sendEventsBuffer()`; пустое сообщение игнорируется |
| `log`         | No-op, кроме тегов из `PersistedLogTags.persisted`: они пишутся в файл логов                       |

`log` принимает лямбду сообщения: строка не строится, если никто её не читает.

### Что не репортится как ошибка

`Throwable.isReportableError()` отсекает `CancellationException` и `IOException`: отмена корутины и
сетевые сбои (DNS, обрыв, таймаут) не баги приложения и шумели бы впустую. Та же политика у
`ErrorHandlerImpl` ([network-and-auth.md](network-and-auth.md)).

### Необработанные ошибки корутин

`ErrorCoroutineAnalytics.reportCoroutineError(owner, throwable)` отправляет `reportError` с группой
`coroutine_error` и сообщением `<owner>: <класс исключения>`. Вызывается из `ErrorHandler.parse`,
если задан `owner` и ошибка не сетевая.

## События

Каждая фича держит свой `XxxAnalytics` (≈18 штук), который внедряется в ViewModel и содержит методы
`eventXxx()`. Правила:

- В `TopAnalytics` и `SearchAnalytics` имена событий и параметров — константы (`EVENT_…`,
  `PARAM_…`) внутри класса, у каждого метода KDoc: что произошло и какие параметры.
- Параметры собираются `analyticsParamsOf("key" to value, ...)`: `null` и пустые строки
  отбрасываются, остальное приводится `toString()`.
- Пользовательский текст не отправляется: поиск шлёт `has_query` и `filter_count`, а не строку
  запроса.
- В `TopAnalytics` перечисление кодируется `name.lowercase()`.

Пример (`TopAnalytics`):

```kotlin
fun eventTypeSelected(type: AnimeTopType) {
    tracker.track(EVENT_TYPE_SELECTED, analyticsParamsOf(PARAM_TYPE to type.name.lowercase()))
}
```

### Что не уходит в аналитику

- Передача сессии по LAN: код, содержимое QR, refresh-токен, адреса и имена найденных устройств в
  трекер не отправляются ([local-auth-session-transfer.md](local-auth-session-transfer.md)).
- Поиск: вместо строки запроса `has_query` и `filter_count`.
- Свободные диагностические строки `log(...)` в AppMetrica никогда не пересылаются
  (комментарий в `AppMetricaAnalyticsTracker.log`): только теги `PersistedLogTags` и только в файл
  логов приложения.

## Файл логов приложения

Запись **выключена по умолчанию**: Настройки → Общие → «Запись логов» (`AppLogRecordingSettings`).

| Состояние  | Поведение                                                                                                     |
|------------|---------------------------------------------------------------------------------------------------------------|
| Выключена  | Коллектор не запущен, `PersistedLogTags` не пишутся, файлы стёрты, «Поделиться логами» скрыто                 |
| Включена   | Пишется заголовок сессии (версия, устройство, Android), ставится обработчик падений, читается logcat процесса |
| Выключение | Остановка чтения logcat и `store.clear()`: логи прошлого сеанса удаляются                                     |

### Устройство

| Класс                     | Роль                                                                                                            |
|---------------------------|-----------------------------------------------------------------------------------------------------------------|
| `AppLogFileStore`         | `cacheDir/logs`: `current.log` и `previous.log`, ротация при 2 МБ, не попадает в бэкап                          |
| `AppLogCollector`         | Читает logcat процесса и пишет в хранилище; синхронно пишет необработанные исключения перед падением            |
| `AppLogDiagnosticSink`    | Приёмник `DiagnosticLogSink` для release: пишет строки с тегами `PersistedLogTags` напрямую в `AppLogFileStore` |
| `AppLogExporter`          | Склеивает файлы ротации в один `.txt` для «Поделиться логами», прошлые экспорты удаляются                       |
| `AppLogRecordingSettings` | Флаг записи                                                                                                     |

KDoc `AppLogDiagnosticSink`: пишет диагностику плеера прямо в `AppLogFileStore`, минуя logcat: в
release logcat-трекера нет, а `AppLogCollector` читает только logcat. В debug sink не используется,
иначе строки продублировались бы: там их уже подбирает коллектор.

Формат строки совпадает с `logcat -v threadtime` (`MM-dd HH:mm:ss.SSS pid tid D tag: msg`), поэтому
один и тот же `grep` работает и по logcat, и по файлу.

### Маскирование

`AppLogDiagnosticSink` перед записью прогоняет сообщение и стек через `maskSensitiveUrls()`:

- ссылки усекаются до `схема://хост/...`;
- значения параметров `token`, `sig`, `signature`, `key`, `auth`, `session`, `hash`, `expires`
  заменяются на `***`.

KDoc функции: в файл логов, который пользователь отправляет разработчику, не должны попадать
подписанные ссылки.

### Теги, сохраняемые в release

Единственное место, где это решается, — `PersistedLogTags.persisted`:

| Тег                  | Откуда                                                                         |
|----------------------|--------------------------------------------------------------------------------|
| `PlayerBuffering`    | `PlayerBufferingAnalyticsListener`, [player-buffering.md](player-buffering.md) |
| `PlayerViewModel`    | `PlayerAnalytics`                                                              |
| `PlayerMediaSession` | `PlayerServiceUtils.kt` (`PLAYER_SERVICE_LOG_TAG`, сервис медиа-сессии)         |
| `PlayerLoudness`     | `PlayerLoudnessNormalizer`                                                     |
| `PlayerExtractor`    | `PlayerExtractorLog` (`extractor/common/`), [other-extractors.md](other-extractors.md) |
| `AllohaExtractor`    | [alloha-player.md](alloha-player.md)                                           |
| `AllohaStreamProxy`  | [alloha-player.md](alloha-player.md)                                           |
| `CvhExtractor`       | [cvh-player.md](cvh-player.md)                                                 |

KDoc `PersistedLogTags`: это единственное место, где решается, что именно сохраняется; модули
плеера берут теги отсюда, поэтому переименование не отключит запись молча.
