# Поведение, зависящее от устройства

Где код проверяет тип устройства и что делает по-разному. Здесь только то, что есть в коде. Токен —
[network-and-auth.md](network-and-auth.md), память — [image-loading-and-memory.md](image-loading-and-memory.md),
плеер — [player-architecture.md](player-architecture.md).

## Определение телевизора

Единого флага нет: интерфейс выбирает пользователь (`AppInterfaceModePreferences`, диалог в
`InterfaceRouterActivity`), а в местах, где нужно знать тип устройства, код сам читает
`Configuration.UI_MODE_TYPE_TELEVISION`:

| Где                              | Что зависит                                                                |
|----------------------------------|----------------------------------------------------------------------------|
| `CastSupport`                    | На телевизоре Cast выключен (`Status.TELEVISION`)                          |
| `DeviceAwareTvIntegration`       | Интеграции ТВ работают только на телевизоре, иначе no-op                   |
| `YummyTvTheme`                   | Если `isTelevision` не задан, типографика ТВ выбирается по `uiMode`        |
| `AppMetricaAnalyticsInitializer` | Тип устройства для AppMetrica: `TV`, `CAR`, иначе `TABLET` при `smallestScreenWidthDp >= 600`, иначе `PHONE` |
| Генераторы baseline profile      | `isTelevisionDevice()` через `UiModeManager` пропускает «чужой» сценарий   |

## Токен: режим хранения

`TokenStorageMode` (KDoc в коде): на кастомных прошивках ТВ-боксов (SlimBox и подобных)
AndroidKeyStore бывает нерабочим, и тогда приложение понижается до `FALLBACK`, иначе вход невозможен.

- `KEYSTORE`: AES/GCM на ключе из AndroidKeyStore; `FALLBACK`: обфускация без Keystore.
- Переключение на `FALLBACK` происходит при отказе шифра на записи или на чтении; режим
  сохраняется в `token_storage_mode`.
- Событие `auth_token_storage_degraded` содержит `Build.MANUFACTURER`, `Build.MODEL`, `Build.DEVICE`,
  `SDK_INT`.
- Режим виден пользователю: `SettingsState.isFallbackSessionStorage`, на ТВ показывается в
  `SettingsTvAboutContent`.

Подробности алгоритма — в [network-and-auth.md](network-and-auth.md).

## Cast

`CastSupport.decision(context)` считается один раз на процесс и кэшируется:

| Статус                  | Когда                                                                        |
|-------------------------|------------------------------------------------------------------------------|
| `TELEVISION`            | Устройство — телевизор                                                       |
| `PLAY_SERVICES_TOO_OLD` | `isGooglePlayServicesAvailable(context, 222_600_000)` не вернул `SUCCESS`     |
| `CHECK_FAILED`          | Проверка бросила исключение                                                  |
| `SUPPORTED`             | Иначе                                                                        |

`PlayerCastPlayerFactory.createOrNull` оборачивает ExoPlayer в `CastPlayer` только при
`SUPPORTED`. Порог `222_600_000` — версия APK Google Play Services (`22.26.x`, август 2022), в
которой появилась асинхронная перегрузка `CastContext.getSharedInstance(Context, Executor)`
(KDoc в `CastSupport`).

## Интеграции ТВ

`DeviceAwareTvIntegration` делегирует в `TvIntegration` только на телевизоре, иначе отдаёт пустые
потоки и no-op. Внутри: Watch Next (`WatchNextManager`) и preview-канал (`PreviewChannelManager`).
Новый метод в `ITvIntegration` нужно продублировать в этом guard (KDoc класса).

## Память

`ActivityManager.isLowRamDevice` читается в двух местах:

- `CoilImageLoaderInstaller`: доля memory cache `0.10` вместо `0.15`;
- `PlayerExoPlayerFactory`: `setForceHighestSupportedBitrate(!isLowRamDevice)`.

Профиль буфера плеера определяется только настройкой `PlayerBufferProfile`. Подробности —
[image-loading-and-memory.md](image-loading-and-memory.md), [player-buffering.md](player-buffering.md).

## Сеть

- Прокси Alloha слушает `InetAddress.getByName("127.0.0.1")`, а не `getLoopbackAddress()`: на
  устройстве с предпочтением IPv6 последний даёт `::1`, и выдаваемые URL, ведущие на `127.0.0.1`,
  получили бы `ConnectException` (комментарий в `AllohaStreamProxy`).
- `network_security_config` разрешает cleartext только для `127.0.0.1` и `localhost`; запрос на
  LAN-адрес через OkHttp не проходит, поэтому передача сессии использует CIO-клиент
  ([local-auth-session-transfer.md](local-auth-session-transfer.md)).

## Block Store

Вызовы Block Store best-effort: на устройстве без Google Play Services `Task` падает, ошибка
пишется в `AnalyticsTracker.log` (тег `AuthTokenBackup`) и глотается; вход, выход и обновление
токена работают без бэкапа ([block-store-session-restore.md](block-store-session-restore.md)).
