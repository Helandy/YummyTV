# Вход на ТВ через телефон

Вводить логин и пароль пультом неудобно, поэтому приложение переносит существующую сессию с телефона
на ТВ по локальной сети. Сервер yani не участвует: устройства договариваются напрямую,
подтверждением служит 10-значный код с экрана ТВ. Рядом с кодом ТВ рисует QR: на телефоне его
достаточно отсканировать, ни выбирать ТВ, ни набирать код не нужно. Ручной ввод остаётся запасным
путём.

Телефон должен быть авторизован: передаётся его refresh-токен.

| Устройство | Роль                      | Вход в UI                                |
|------------|---------------------------|------------------------------------------|
| ТВ         | HTTP-сервер + анонс в NSD | Экран аккаунта → «Войти с телефона»      |
| Телефон    | Поиск устройств + клиент  | Настройки → «Устройства» → «Войти на ТВ» |

## Последовательность

```
ТВ                                          Телефон
│ StartLocalAuthServerSelected               │
│ запрос доступа к локальной сети            │
│ код = SecureRandom (10 символов)           │
│ embeddedServer(CIO, port = 0)              │
│ NSD register _yummytv_auth._tcp.           │
│ onServiceRegistered → serviceName          │
│──── Pairing(pin, port, serviceName) → UI ──│ QR: yummytv://pair?d=<serviceName>&c=<код>
│                                            │ NSD discover + resolve
│                                            │ скан QR (или выбор ТВ и ввод кода руками)
│                                            │ PBKDF2(код, salt) → AES-GCM(refreshToken)
│◀──────── POST /transfer {token, iv, salt} ─│
│ decrypt → signInWithToken(token)           │
│──────── 200 {"status":"ok"} ──────────────▶│
│ Success → сервер и NSD останавливаются     │ тост «Сессия успешно передана»
```

## Обнаружение

Тип сервиса `_yummytv_auth._tcp.`, имя `YummyTv-<Build.MODEL>`. Имя санитайзится (буквы, цифры,
пробел, дефис) и режется до 63 байт: у приставок `MODEL` бывает длинным и с юникодом, NSD такое имя
не примет.

- Сервер поднимается с `port = 0`, реальный порт читается из `engine.resolvedConnectors()`.
  Предварительный `ServerSocket(0)` был бы гонкой: порт между закрытием и стартом Ktor может занять
  другой процесс.
- Регистрация анонса обёрнута в `runCatching`: без разрешения на локальную сеть `registerService`
  кидает `SecurityException` синхронно, мимо `onRegistrationFailed`, и без перехвата падал бы
  `callbackFlow`.
- Телефон резолвит строго по одному через `Mutex`: `NsdManager.resolveService` не переносит
  параллельных вызовов и на старых API падает с «listener already in use». На API 34+ используются
  `registerServiceInfoCallback` и `hostAddresses`, ниже — `resolveService` и `host`. Тип сервиса
  сравнивается после `trim('.')`: Android нормализует концевые точки по-разному.

### Разрешения

Нужны два, оба запрашиваются в рантайме:

- `NEARBY_WIFI_DEVICES`: с Android 13 без него `NsdManager.discoverServices()` молча ничего не
  находит. Объявлено с `usesPermissionFlags="neverForLocation"`.
- `ACCESS_LOCAL_NETWORK`: с Android 16 Local Network Protection режет весь доступ к локальной
  подсети, включая mDNS. Симптом: в logcat `AppOps: Operation not found … op=ACCESS_LOCAL_NETWORK`,
  `registerService` уходит в `onRegistrationFailed`, на ТВ вместо кода «Не удалось объявить ТВ в
  локальной сети». Нужно обеим сторонам.

Список для текущего SDK собирает `localNetworkPermissions()` в `core:designsystem`, там же
`rememberLocalNetworkPermissionGate`: общая для ТВ и телефона последовательность «своё объяснение →
системный запрос → при отказе навсегда настройки приложения». Диалоги каждый UI рисует сам: на ТВ
нужен FocusRequester, иначе фокус уходит в боковое меню.

Отказ — не сбой сети: у него своя причина `LocalAuthError.PERMISSION_DENIED` и свой текст, иначе
пользователь шёл бы чинить роутер.

## QR-код

ТВ кодирует `yummytv://pair?d=<имя NSD-сервиса>&c=<код>`, значения URL-encoded. Формат и разбор —
`LocalAuthPairingPayload` (domain), одна точка для обеих сторон.

- IP и порта в QR нет: у приставки бывает несколько интерфейсов (Ethernet, Wi-Fi, VPN), выбрать
  нужный адрес на стороне ТВ ненадёжно. Телефон ищет ТВ по имени сервиса через NSD и получает уже
  проверенный адрес. В QR то же, что и так на экране, плюс публичное имя из mDNS.
- Имя берётся из `onServiceRegistered`, а не из `deviceServiceName()`: при конфликте имён NSD
  переименовывает сервис (`YummyTv-X (2)`). Реальное имя сохраняется в `PairingSession.serviceName`
  и попадает в `LocalAuthServerState.Pairing.serviceName`, в том числе при повторных `Pairing` после
  неверного кода. Имена сравниваются без учёта регистра: mDNS регистронезависим.

### Отрисовка на ТВ

QR генерирует zxing core (`ErrorCorrectionLevel.M`, поле в один модуль), рисует `Canvas`
(`LocalAuthQrCode`). Всегда чёрный по белому на белой подложке, независимо от темы: инвертированный
QR многие сканеры не читают, а без светлой рамки теряется «тихая зона».

### Сканирование на телефоне

Используется Google Code Scanner (`play-services-code-scanner`): системный экран из Play Services
сам работает с камерой, поэтому ни разрешение `CAMERA`, ни CameraX не нужны.

Модуль сканера качается по требованию через `ModuleInstallClient`. Meta-data
`com.google.mlkit.vision.DEPENDENCIES` не объявлена: APK общий, и модуль качался бы на все ТВ и на
телефоны, которые этим входом не пользуются.

- При открытии экрана «Войти на ТВ» стартует фоновый `installModules` без слушателя. Если модуль уже
  есть, Play Services ничего не делают.
- Если к нажатию модуля нет, кнопка показывает «Загружаем сканер…», `installModules` идёт со
  `InstallStatusListener`, по `STATE_COMPLETED` скан стартует сам. `STATE_FAILED`, `STATE_CANCELED`
  и отсутствие Play Services сводятся к `QrScanFailed`.
- Состояние загрузки живёт в `LocalAuthQrScannerState` (UI): presentation о Play Services не знает.

Результат скана в `LocalAuthViewModel`:

| Отсканировано                          | Поведение                                                                      |
|----------------------------------------|--------------------------------------------------------------------------------|
| QR с именем, ТВ уже найден             | ТВ выбирается, код подставляется, перенос стартует сразу                       |
| QR с именем, ТВ ещё не найден          | `pendingDeviceId`, «Ищем этот телевизор…»; перенос при появлении в NSD         |
| Голый код (до 12 символов с пробелами) | Код подставляется; если в списке один ТВ, он выбирается и перенос стартует     |
| Что-то другое                          | `LOCAL_AUTH_INVALID_QR` — «Это не QR-код YummyTV»                              |
| Сканер не запустился                   | `LOCAL_AUTH_SCANNER_UNAVAILABLE` — нет Play Services, предлагается ручной ввод |

Отмена скана ничего не показывает. Ручной выбор ТВ сбрасывает `pendingDeviceId`. Ограничение длины
голого кода нужно, чтобы `normalize()` не выцепил десять «валидных» символов из чужого URL.

## Транспорт

Передача идёт на `http://<ip>:<порт>`, обычный HTTP внутри локальной сети. Общий Ktor-клиент
приложения не подходит: он на OkHttp и сверяется с `network_security_config.xml`, где cleartext
разрешён только для `127.0.0.1` и `localhost`. Запрос на `192.168.x.x` не доходит до сокета: на ТВ
пусто, на телефоне мгновенный отказ без причины.

Поэтому сопряжение использует свой клиент на Ktor CIO (`@LocalAuthHttpClient`, собирается в
`AccountDataModule`): CIO работает на сырых сокетах и политику не спрашивает, а `base-config`
остаётся строгим для остального приложения. Таймауты: 5 с на соединение, 10 с на запрос.

Клиент узкого назначения. Внешний URL в него отправлять нельзя: проверка TLS-политики к такому
запросу не применится.

## Протокол

Единственный эндпоинт — `POST /transfer`:

```json
{
    "encryptedToken": "base64",
    "iv": "base64",
    "salt": "base64"
}
```

| Код   | Когда                                          |
|-------|------------------------------------------------|
| `200` | `{"status":"ok"}`, сессия принята              |
| `400` | Не расшифровалось: неверный код или битое тело |
| `410` | Код истёк (TTL 3 минуты)                       |
| `429` | Исчерпан лимит в 5 неудачных попыток           |
| `502` | Токен принят, но `signInWithToken` упал        |

В теле ошибки приходит имя константы `LocalAuthError`, не текст: локализацию собирает UI. Телефон
показывает конкретную причину, потому что код вводит человек у телефона.

## Код сопряжения

Формат задаёт `LocalAuthCode` (domain), им пользуются генерация, фильтр ввода и вёрстка обоих UI:

- длина 10, показ и ввод группами по 5;
- алфавит `0123456789ACDEFHJKMNPRTUVWXY` — 28 символов. Выброшены `B`, `G`, `I`, `L`, `O`, `Q`, `S`,
  `Z`: их путают с цифрами, когда код читают с экрана через комнату;
- `normalize()` поднимает регистр, приводит двойники к цифрам (`B`→8, `I`/`L`→1, `O`/`Q`→0, `S`→5,
  `Z`→2) и отбрасывает всё остальное.

Буква из алфавита обязана переживать `normalize()` неизменной. Если символ добавить в алфавит и в
таблицу двойников, верный код начнёт отвергаться; на это есть unit-тест.

### Шифрование

- KDF `PBKDF2WithHmacSHA256`, 200 000 итераций, случайная 16-байтная соль, ключ 256 бит.
- Шифр `AES/GCM/NoPadding`, тег 128 бит, IV генерирует `Cipher`.
- Соль и IV идут рядом с шифротекстом, всё в Base64 `NO_WRAP`.
- Код генерируется через `SecureRandom`.

Тег GCM аутентифицирует сообщение: неверный код — `AEADBadTagException`, отдельная проверка кода не
нужна.

### Длина кода

Лимиты времени (3 минуты) и попыток (5) ограничивают онлайн-перебор, но не перехват: шифротекст едет
по открытому HTTP, и получивший его перебирает код офлайн. Достать его можно и без сниффинга:
поднять в сети поддельный mDNS-сервис с похожим именем и ждать, пока пользователь его выберет.

Шесть цифр — 10⁶ вариантов, порядка получаса на одной видеокарте даже при 200 000 итераций. Десять
символов этого алфавита — 28¹⁰ ≈ 3·10¹⁴ (~2⁴⁸), полный перебор уходит за сотни лет. Оценки грубые,
но разница в порядках. Принципиально другой уровень дал бы PAKE; он сознательно не сделан.

## Приём токена на ТВ

Вызывается `AccountRepository.signInWithToken(token)`, тот же хвост, что и у входа по паролю: запрос
профиля, `saveProfile`, `settingsStore.setYaniAccount(...)`, очистка кэша документов прошлого
пользователя, и только потом запись refresh-токена. Записать токен мимо этого пути нельзя: сессия
считалась бы авторизованной, но `yaniUserId` остался бы от прошлого аккаунта.

## Состояния и жизненный цикл сервера

`LocalAuthServerState`: `Idle` → `Pairing(pin, port, serviceName, attemptsLeft, lastError)` →
`Transferring` → `Success`, плюс терминальный `Error(reason)`.

- Восстановимая ошибка — неверный код. Сервер продолжает работать, состояние возвращается в
  `Pairing`
  с тем же кодом, `lastError = INVALID_PIN` и уменьшенным `attemptsLeft`. ТВ показывает «Неверный
  код» и счётчик попыток.
- Терминальные: `PIN_EXPIRED`, `TOO_MANY_ATTEMPTS`, `SERVICE_UNAVAILABLE`, `PERMISSION_DENIED`,
  `SIGN_IN_FAILED`. Код больше не принимается, ViewModel гасит сервер, панель показывает причину и
  кнопку «Обновить код», которая поднимает сервер заново с новым кодом. Автоматически код не
  перевыпускается: ручное действие на ТВ не даёт набивать попытки удалённо.

Срок жизни кода и лимит попыток живут в `PairingSession` (чистая логика, покрыта unit-тестом).
Счётчик — `AtomicInteger`: CIO обрабатывает запросы конкурентно, обычная `var` пропустила бы лимит.

Сервер останавливается при `Success`, терминальной ошибке, «Назад» на панели сопряжения и отмене
подписки на flow (`awaitClose`).

Перед перезапуском используется `cancelServer()` (отмена подписки, дальше `awaitClose` снимает
NSD-регистрацию и гасит движок), а не `stopServer()`: полная остановка добивает живые серверы
репозитория отложенной корутиной, и та погасила бы только что поднятый новый сервер.

Остановка идемпотентна: `ServerHandle` хранит флаг `stopped`, снимает NSD-регистрацию и зовёт
`server.stop()`. Сетевые операции идут на `Dispatchers.IO`: старт Ktor и bind сокета на главном
потоке дают `NetworkOnMainThreadException`. При выходе из аккаунта `localAuthServerState`
сбрасывается в `Idle`, иначе панель показала бы устаревший «Успешно».

## Аналитика

События `LocalAuthAnalytics`: `local_auth_tv_*` — где показывают код, `local_auth_mobile_*` — где
вводят.

| Событие                                | Когда                          | Параметры       |
|----------------------------------------|--------------------------------|-----------------|
| `local_auth_tv_pairing_started`        | Выбрано «Войти с телефона»     | —               |
| `local_auth_tv_pin_shown`              | Сервис объявлен, код на экране | —               |
| `local_auth_tv_wrong_pin`              | Пришёл неверный код            | `attempts_left` |
| `local_auth_tv_pin_refreshed`          | Нажато «Обновить код»          | —               |
| `local_auth_tv_transfer_success`       | Сессия принята, ТВ авторизован | —               |
| `local_auth_tv_pairing_failed`         | Терминальная ошибка            | `reason`        |
| `local_auth_tv_pairing_cancelled`      | Пользователь закрыл панель     | —               |
| `local_auth_mobile_screen`             | Открыт экран «Войти на ТВ»     | —               |
| `local_auth_mobile_permission_result`  | Итог запроса доступа к сети    | `granted`       |
| `local_auth_mobile_discovery_failed`   | Поиск не запустился            | —               |
| `local_auth_mobile_discovery_finished` | Экран закрыт, итог поиска      | `device_count`  |
| `local_auth_mobile_device_selected`    | Выбран ТВ из списка            | —               |
| `local_auth_mobile_qr_scanned`         | Итог скана QR                  | `result`        |
| `local_auth_mobile_transfer_selected`  | Нажато «Передать сессию»       | —               |
| `local_auth_mobile_transfer_success`   | ТВ подтвердил приём            | —               |
| `local_auth_mobile_transfer_failure`   | Передача не удалась            | `reason`        |

`reason` — имя `LocalAuthError` в нижнем регистре или `unknown`, если ТВ не ответил. `result` —
`valid`, `invalid` или `unavailable` (сканер не запустился).

В трекер не уходят код, содержимое QR, refresh-токен, адреса и имена найденных устройств.

Воронки: `tv_pin_shown` → `tv_transfer_success` и `mobile_screen` → `mobile_transfer_success`.
`mobile_discovery_finished` с `device_count = 0` — самый частый сбой, и это разные сети или изоляция
клиентов на роутере, а не баг.

## Ограничения

- Между двумя эмуляторами не работает: multicast не проходит через сеть эмулятора, каждый сидит за
  своим NAT с одинаковым `10.0.2.15`. Нужны два реальных устройства в одной сети.
- Гостевые сети и AP isolation: если роутер изолирует клиентов или режет multicast, обнаружение не
  сработает даже на реальном железе.
- После исчерпания попыток уже начатые запросы получат `429`: ожидаемо, на UI не влияет.
- Скан QR требует Google Play Services. Без них (Huawei, кастомные прошивки) остаётся ручной ввод,
  экран об этом говорит.
- Старые сборки несовместимы: формат кода менялся вместе с длиной, а до Android 16 приложению не
  требовался `ACCESS_LOCAL_NETWORK`. Версия протокола по сети не передаётся, диагностика только по
  логам.

## Код

Механика в `feature/account/data/.../localauth/`, репозиторий остаётся тонким оркестратором.

| Слой          | Файл                                                                                                                                                                |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Оркестрация   | `feature/account/data/.../repository/NsdLocalAuthRepository.kt`                                                                                                     |
| Протокол      | `.../localauth/LocalAuthContract.kt`                                                                                                                                |
| Политика кода | `.../localauth/PairingSession.kt`                                                                                                                                   |
| HTTP-сервер   | `.../localauth/LocalAuthServer.kt`                                                                                                                                  |
| NSD           | `.../localauth/NsdAdvertiser.kt`, `.../localauth/NsdDeviceDiscovery.kt`                                                                                             |
| Клиент        | `.../localauth/SessionTransferClient.kt`, `.../di/LocalAuthHttpClient.kt`, `.../di/AccountDataModule.kt`                                                            |
| Криптография  | `feature/account/data/.../utils/LocalAuthCrypto.kt`                                                                                                                 |
| DTO           | `feature/account/data/.../dto/SessionTransferDto.kt`                                                                                                                |
| Domain        | `feature/account/domain/.../model/` (`LocalAuthCode`, `LocalAuthPairingPayload`, `LocalAuthServerState`, `LocalAuthError`), `.../repository/LocalAuthRepository.kt` |
| Разрешения    | `core/designsystem/.../permissions/LocalNetworkPermissions.kt`                                                                                                      |
| Presentation  | `feature/account/presentation/.../account/handler/AccountLocalAuthHandler.kt`, `.../localauth/LocalAuthViewModel.kt`                                                |
| UI ТВ         | `feature/account/ui-tv/.../view/` (`LocalAuthPanel`, `LocalNetworkPermissionTvDialog`, `LocalAuthQrCode`), `.../utils/LocalAuthQrUtils.kt`                          |
| UI телефона   | `feature/account/ui-mobile/.../localauth/` (`LocalAuthMobileScreen`, `LocalAuthPinInput`, `LocalAuthScanQrButton`, `utils/LocalAuthQrScanner`)                      |
