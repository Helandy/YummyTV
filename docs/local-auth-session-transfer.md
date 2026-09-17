# Вход на ТВ через телефон (передача сессии по локальной сети)

Вводить логин и пароль пультом неудобно, поэтому приложение умеет переносить уже существующую
сессию с телефона на ТВ по локальной сети. Сервер yani в этом не участвует: телефон и ТВ
договариваются напрямую, а подтверждением служит 6-значный PIN, который ТВ показывает на экране.

## Роли

| Устройство | Роль                      | Точка входа в UI                         |
|------------|---------------------------|------------------------------------------|
| ТВ         | HTTP-сервер + анонс в NSD | Экран аккаунта → «Войти с телефона»      |
| Телефон    | Поиск устройств + клиент  | Настройки → «Устройства» → «Войти на ТВ» |

Телефон должен быть авторизован — передаётся именно его refresh-токен.

## Как это выглядит целиком

```
ТВ                                          Телефон
│ StartLocalAuthServerSelected               │
│ PIN = SecureRandom (6 цифр)                │
│ embeddedServer(CIO, port = 0)              │
│ NSD register _yummytv_auth._tcp.           │
│──────── Pairing(pin, port) → UI ───────────│
│                                            │ DiscoveryStarted
│                                            │ NSD discover + resolve
│                                            │ выбор устройства, ввод PIN
│                                            │ PBKDF2(pin, salt) → AES-GCM(refreshToken)
│◀──────── POST /transfer {token, iv, salt} ─│
│ decrypt → signInWithToken(token)           │
│──────── 200 {"status":"ok"} ──────────────▶│
│ Success → сервер и NSD останавливаются     │ тост «Сессия успешно передана»
```

## Обнаружение

Тип сервиса — `_yummytv_auth._tcp.`, имя — `YummyTv-<Build.MODEL>`. Имя санитайзится (только буквы,
цифры, пробел и дефис) и режется до 63 байт: у приставок `MODEL` бывает длинным и с юникодом, а NSD
такое имя не примет.

Порт не выбирается заранее: сервер поднимается с `port = 0`, реальный порт читается из
`engine.resolvedConnectors()`. Предварительный `ServerSocket(0)` здесь был бы гонкой — между
закрытием сокета и стартом Ktor порт может занять другой процесс.

На стороне телефона резолв идёт строго по одному через `Mutex`: `NsdManager.resolveService` не
переносит параллельных вызовов и на старых API падает с «listener already in use». На API 34+
используется `registerServiceInfoCallback` и `hostAddresses`, ниже — устаревшие `resolveService` и
`host`. Тип сервиса сравнивается после `trim('.')`, потому что Android нормализует концевые точки
по-разному.

### Разрешение

На Android 13+ `NsdManager.discoverServices()` требует `NEARBY_WIFI_DEVICES`. Разрешение объявлено в
манифесте с `usesPermissionFlags="neverForLocation"` и запрашивается при открытии экрана «Войти на
ТВ». Без него поиск молча не находит ничего.

## Протокол

Единственный эндпоинт — `POST /transfer`:

```json
{
    "encryptedToken": "base64",
    "iv": "base64",
    "salt": "base64"
}
```

Ответы:

| Код   | Когда                                           |
|-------|-------------------------------------------------|
| `200` | `{"status":"ok"}`, сессия принята               |
| `400` | не расшифровалось — неверный PIN или битое тело |
| `410` | PIN истёк (TTL 3 минуты)                        |
| `429` | исчерпан лимит в 5 неудачных попыток            |
| `502` | токен принят, но `signInWithToken` упал         |

В теле ошибки приходит имя константы `LocalAuthError`, а не текст для показа: локализацию собирает
UI. Телефон разбирает это имя и показывает конкретную причину — PIN вводит человек у телефона,
поэтому «Неверный код» и «Срок действия кода истёк» должны быть видны именно там.

## Криптография

Ключ выводится из PIN, а PIN короткий — всего 10⁶ комбинаций. Простой хэш здесь не годится:
перехваченный по открытому HTTP трафик перебирался бы оффлайн за миллисекунды. Поэтому:

* KDF — `PBKDF2WithHmacSHA256`, 200 000 итераций, случайная 16-байтная соль, ключ 256 бит;
* шифр — `AES/GCM/NoPadding`, тег 128 бит, IV генерирует сам `Cipher`;
* соль и IV передаются рядом с шифротекстом, всё в Base64 `NO_WRAP`;
* PIN генерируется через `SecureRandom`.

Тег GCM заодно аутентифицирует сообщение: неверный PIN — это `AEADBadTagException`, то есть
отдельная
проверка PIN не нужна.

Ограничения по времени (3 минуты) и числу попыток (5) сужают окно онлайн-перебора; оффлайн-перебор
упирается в стоимость PBKDF2.

## Что происходит на ТВ после приёма токена

Вызывается `AccountRepository.signInWithToken(token)` — тот же хвост, что и у обычного входа по
паролю: запрос профиля, `saveProfile`, `settingsStore.setYaniAccount(...)`, очистка кэша документов
прошлого пользователя и только потом запись refresh-токена. Записать один токен мимо этого пути
нельзя: сессия считалась бы авторизованной, но `yaniUserId` остался бы от предыдущего аккаунта.

## Состояния и жизненный цикл сервера

`LocalAuthServerState`: `Idle` → `Pairing(pin, port, attemptsLeft, lastError)` → `Transferring` →
`Success`, плюс терминальный `Error(reason)`.

Ошибки разделены на два класса:

* **восстановимые** — неверный PIN. Сервер продолжает работать, состояние возвращается в `Pairing` с
  тем же кодом, но с `lastError = INVALID_PIN` и уменьшенным `attemptsLeft`. На ТВ под PIN-ом
  появляется «Неверный код» и счётчик оставшихся попыток;
* **терминальные** — `PIN_EXPIRED`, `TOO_MANY_ATTEMPTS`, `SERVICE_UNAVAILABLE`, `SIGN_IN_FAILED`.
  Этот PIN больше не примут, поэтому ViewModel сразу гасит сервер, а панель показывает причину и
  кнопку «Обновить код», которая поднимает сервер заново с новым PIN. Автоматически код не
  перевыпускается намеренно: ручное действие на ТВ не даёт набивать попытки удалённо.

Срок жизни PIN и лимит попыток живут в `PairingSession` — это чистая логика без Android и сети,
покрытая unit-тестом. Счётчик — `AtomicInteger`: CIO обрабатывает запросы конкурентно, обычная `var`
позволила бы проскочить лимит гонкой.

Сервер останавливается, когда:

* пришёл `Success` — сессия уже сохранена;
* пришла терминальная ошибка;
* пользователь нажал «Назад» на панели сопряжения;
* отменилась подписка на flow (например, ViewModel уничтожен) — срабатывает `awaitClose`.

Важная деталь: перед перезапуском используется `cancelServer()` (только отмена подписки, дальше
`awaitClose` сам снимает NSD-регистрацию и гасит движок), а не `stopServer()`. Полная остановка
дополнительно добивает все живые серверы репозитория отложенной корутиной, и та могла бы погасить
только что поднятый новый сервер.

Остановка идемпотентна: `ServerHandle` хранит флаг `stopped`, снимает NSD-регистрацию и зовёт
`server.stop()`. Все сетевые операции живут на `Dispatchers.IO` — старт Ktor и bind сокета на
главном
потоке дали бы `NetworkOnMainThreadException`.

При выходе из аккаунта `localAuthServerState` сбрасывается в `Idle`, иначе панель сопряжения
показала бы устаревший «Успешно».

## Аналитика

События живут в `LocalAuthAnalytics` и делятся по сторонам: `local_auth_tv_*` — там, где показывают
PIN, `local_auth_mobile_*` — там, где его вводят.

| Событие                                | Когда                              | Параметры       |
|----------------------------------------|------------------------------------|-----------------|
| `local_auth_tv_pairing_started`        | выбрано «Войти с телефона»         | —               |
| `local_auth_tv_pin_shown`              | сервис объявлен, код на экране     | —               |
| `local_auth_tv_wrong_pin`              | пришёл неверный код                | `attempts_left` |
| `local_auth_tv_pin_refreshed`          | нажато «Обновить код»              | —               |
| `local_auth_tv_transfer_success`       | сессия принята, ТВ авторизован     | —               |
| `local_auth_tv_pairing_failed`         | терминальная ошибка                | `reason`        |
| `local_auth_tv_pairing_cancelled`      | пользователь закрыл панель         | —               |
| `local_auth_mobile_screen`             | открыт экран «Войти на ТВ»         | —               |
| `local_auth_mobile_permission_result`  | итог запроса `NEARBY_WIFI_DEVICES` | `granted`       |
| `local_auth_mobile_discovery_failed`   | поиск не запустился                | —               |
| `local_auth_mobile_discovery_finished` | экран закрыт, итог поиска          | `device_count`  |
| `local_auth_mobile_device_selected`    | выбран ТВ из списка                | —               |
| `local_auth_mobile_transfer_selected`  | нажато «Передать сессию»           | —               |
| `local_auth_mobile_transfer_success`   | ТВ подтвердил приём                | —               |
| `local_auth_mobile_transfer_failure`   | передача не удалась                | `reason`        |

`reason` — имя `LocalAuthError` в нижнем регистре либо `unknown`, если ТВ не ответил вовсе.

**В трекер не уходят PIN, refresh-токен, адреса и имена найденных устройств** — это либо секреты,
либо данные локальной сети пользователя.

Две пары событий дают воронку: `tv_pin_shown` → `tv_transfer_success` и `mobile_screen` →
`mobile_transfer_success`. Отдельно стоит смотреть `mobile_discovery_finished` с `device_count = 0`:
это самый частый сбой, и он означает не баг, а разные сети или изоляцию клиентов на роутере.

## Известные ограничения

* **Между двумя эмуляторами не работает.** Multicast не проходит через сеть эмулятора, а каждый
  инстанс сидит за своим NAT с одинаковым `10.0.2.15`. Нужны два реальных устройства в одной сети.
* **Гостевые сети и AP isolation.** Если роутер изолирует клиентов или режет multicast, обнаружение
  не сработает даже на реальном железе.
* **Окно после исчерпания попыток.** Сервер гасится по состоянию, но уже начатые в этот момент
  запросы получат `429` — это ожидаемо и на UI не влияет.

## Где смотреть код

Механика разнесена по `feature/account/data/.../localauth/`, репозиторий остался тонким
оркестратором: заводит сеанс, поднимает сервер, вешает анонс и следит за временем жизни.

| Слой         | Файл                                                                                                                 |
|--------------|----------------------------------------------------------------------------------------------------------------------|
| Оркестрация  | `feature/account/data/.../repository/NsdLocalAuthRepository.kt`                                                      |
| Протокол     | `feature/account/data/.../localauth/LocalAuthContract.kt`                                                            |
| Политика PIN | `feature/account/data/.../localauth/PairingSession.kt`                                                               |
| HTTP-сервер  | `feature/account/data/.../localauth/LocalAuthServer.kt`                                                              |
| Анонс NSD    | `feature/account/data/.../localauth/NsdAdvertiser.kt`                                                                |
| Поиск NSD    | `feature/account/data/.../localauth/NsdDeviceDiscovery.kt`                                                           |
| Клиент       | `feature/account/data/.../localauth/SessionTransferClient.kt`                                                        |
| Криптография | `feature/account/data/.../utils/LocalAuthCrypto.kt`                                                                  |
| DTO          | `feature/account/data/.../dto/SessionTransferDto.kt`                                                                 |
| Контракт     | `feature/account/domain/.../repository/LocalAuthRepository.kt`                                                       |
| Состояния    | `feature/account/domain/.../model/LocalAuthServerState.kt`, `.../model/LocalAuthError.kt`                            |
| Presentation | `feature/account/presentation/.../account/handler/AccountLocalAuthHandler.kt`, `.../localauth/LocalAuthViewModel.kt` |
| UI ТВ        | `feature/account/ui-tv/.../view/LocalAuthPanel.kt`                                                                   |
| UI телефона  | `feature/account/ui-mobile/.../localauth/LocalAuthMobileScreen.kt`                                                   |
