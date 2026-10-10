# Сеть, токен и ошибки

Как приложение ходит в сеть, где лежит токен и что происходит при отказе Keystore. Контракт самого
API — [yani-api.md](yani-api.md). Вход через
телефон — [local-auth-session-transfer.md](local-auth-session-transfer.md), восстановление после
переустановки — [block-store-session-restore.md](block-store-session-restore.md).

## Клиенты

Все в `core/network/`. База — один `OkHttpClient()` без настроек (`NetworkModule`), клиенты Ktor
строятся поверх него (`engine { preconfigured = okHttpClient }`), поэтому пул соединений и диспетчер
общие.

| Клиент                          | Где                                   | Таймауты (connect / request)                                                               | Назначение                                                           |
|---------------------------------|---------------------------------------|--------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| Yani (`YaniHttpClientProvider`) | `buildYaniHttpClient`                 | 20 с / 40 с (+ socket 40 с)                                                                | API yani: заголовки сессии, ретраи GET, `YaniApiJson`                |
| Общий `HttpClient`              | `NetworkModule.provideHttpClient`     | 15 с / 20 с по умолчанию; `KtorPlayerHttpClient` ставит свои: GET 10 / 15 с, POST 8 / 10 с | Экстракторы: HTML, m3u8, без JSON и без токена; плагин `ContentEncoding` здесь нагружен (Zedfilm без gzip отвечает 404) |
| `@UnauthenticatedJsonClient`    | `NetworkModule`                       | 15 с / 20 с                                                                                | Публичные сторонние API (GitHub): `ContentNegotiation`, без токена   |
| `@LocalAuthHttpClient`          | `AccountDataModule` (feature/account) | 5 с / 10 с                                                                                 | Передача сессии по LAN, движок CIO                                   |
| Coil                            | `CoilImageLoaderInstaller`            | по умолчанию                                                                               | Картинки, [image-loading-and-memory.md](image-loading-and-memory.md) |

Нельзя:

- Вешать `ContentNegotiation` на общий клиент. Он ходит в плееры и за m3u8/HTML, и навязанный
  `Accept: application/json` менял бы ответы этих запросов. Поэтому JSON-клиент отдельный.
- Использовать клиент yani для сторонних API: он добавляет токены и ходит только в
  `api.yani.tv` (плагин `YaniApplicationHeader` проверяет хост).
- Отправлять в `@LocalAuthHttpClient` внешний URL: он на CIO и обходит `network_security_config`, а
  проверка TLS-политики к такому запросу не применится.

### Клиент yani

`YaniHttpClientProvider.get()` строит клиент лениво и один раз (mutex + `Dispatchers.IO`).
`YaniRequestHeaderCache` держит `applicationToken` и `refreshToken` в `@Volatile` полях, обновляемых
подпиской на потоки хранилищ (`collectLatest`). Язык `Lang` читается из хранилища на каждый запрос:
кэшированное поле отставало бы после смены языка, и первый запрос уходил бы со старым заголовком.

Заголовки на `api.yani.tv`:

| Заголовок       | Значение                                                    |
|-----------------|-------------------------------------------------------------|
| `X-Application` | Токен приложения                                            |
| `Lang`          | Код языка контента (`yaniContentLanguage.apiCode`)          |
| `Authorization` | `Bearer <refresh-токен>`, если вызов сам не задал заголовок |

Если вызов задаёт `Authorization` явно (проверка токена при входе: `getProfile(token)`), заголовок
из кэша его не перетирает.

Ретраи: два повтора, пауза 500 мс × номер, только GET на 5xx и исключения (кроме отмены). Мутации не
ретраятся: они могут быть неидемпотентны.

### Логирование

В debug Ktor `Logging` с `LogLevel.BODY`, тег logcat — `Ktor Client` (с пробелом). Два слоя защиты:

- `sanitizeHeader` маскирует `Authorization`, `Cookie`, `Set-Cookie`, `X-Application`;
- `SensitiveDataMaskingLogger` заменяет в JSON значения `password`, `login`, `email`, `token`,
  `access_token`, `refresh_token`, `encryptedToken`, `hash`, `recaptcha_response`,
  `g-recaptcha-response` на `***`.

Новое секретное поле в теле запроса нужно добавлять в регулярное выражение маскирования, иначе оно
попадёт в logcat и экспортируемые логи.

## network_security_config

`app/src/main/res/xml/network_security_config.xml`:

- cleartext разрешён только для `127.0.0.1` и `localhost` (loopback-прокси Alloha);
- `base-config` запрещает cleartext;
- `trust-anchors`: системные сертификаты и `@raw/isrg_root_x1` (ресурс в `app/src/main/res/raw`).

Следствие для LAN: OkHttp сверяется с этим файлом, и запрос на `192.168.x.x` по HTTP не доходит до
сокета. Поэтому передача сессии по локальной сети использует CIO-клиент, см.
[local-auth-session-transfer.md](local-auth-session-transfer.md).

## Токен

Один refresh-токен, он же носитель авторизации:
`AccountSession.isAuthorized = refreshToken.isNotBlank()`. Хранение — `SecureYaniAuthPreferences`
(`SharedPreferences yani_auth_secure_preferences`), логика шифрования — `AuthTokenStorage`.

### Режимы хранения

| Режим      | Шифр                                                                                          | Когда                              |
|------------|-----------------------------------------------------------------------------------------------|------------------------------------|
| `KEYSTORE` | `KeystoreTokenCipher`: AES/GCM/NoPadding на ключе `yummy_tv_yani_auth_key` из AndroidKeyStore | По умолчанию                       |
| `FALLBACK` | `FallbackTokenCipher`: XOR-маскирование + Base64, без Keystore                                | Keystore не работает на устройстве |

Запись маркируется режимом: первый символ `*` (keystore) или `~` (fallback). Записи без метки
считаются keystore-записями (старый формат). Base64 `NO_WRAP` этих символов не содержит.

KDoc `FallbackTokenCipher`: «обфускация, а не криптография»; запись всё так же лежит в
app-private SharedPreferences, но её восстановление не зависит от системного хранилища ключей.
KDoc `TokenStorageMode`: на кастомных прошивках ТВ-боксов (SlimBox и прочие сборки) AndroidKeyStore
бывает нерабочим, иначе вход невозможен вовсе. Маска — фиксированный массив байт (`XOR`).

### Алгоритм

Чтение (`read`):

1. Пустая запись → `""`.
2. Расшифровка шифром режима записи, три попытки с паузами 0 / 300 / 1000 мс: Keystore2 на холодном
   старте может быть ещё не поднят, это не повод терять сессию.
3. Не расшифровалось, но `selfTest()` шифра проходит (шифр исправен, запись битая) → запись
   удаляется, отчёт об ошибке.
4. Не расшифровалось и `selfTest()` падает (шифр неисправен) → `onStorageFailure`, и если запись
   keystore-режима, хранилище переключается на `FALLBACK`. Запись при этом не удаляется.

Запись (`write`):

1. Пустой токен → запись удаляется.
2. Шифрование в текущем режиме. Отказ keystore → переключение на `FALLBACK` и повтор: успешный вход
   не должен превращаться в ошибку.
3. Удалось в запасном режиме → событие `auth_token_storage_degraded` (производитель, модель,
   `SDK_INT`, класс исключения). Не удалось нигде → отчёт об ошибке и ничего не сохранено.

Режим сохраняется в `token_storage_mode` и переживает перезапуск. Возврата в `KEYSTORE` нет.

Из KDoc и комментариев:

- токен удаляется только тогда, когда шифр заведомо исправен (`selfTest`), а конкретная запись всё
  равно не расшифровывается; «раньше любая ошибка keystore молча стирала сессию»;
- отчёты об ошибках хранилища обёрнуты в `runCatching`: исключение отсюда прилетело бы в
  `SupervisorJob`-scope `YaniRequestHeaderCache` и убило бы процесс.

### Жизненный цикл токена

| Событие                                          | Что происходит                                                                                                                                                                     |
|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Вход (пароль, QR/NSD, регистрация)               | `signInWithTokenInternal`: `GET /profile` с токеном → запись токена → профиль и настройки → `authTokenBackup.save`. Любая ошибка после записи откатывает сессию (`rollbackSignIn`) |
| Обновление                                       | `refreshToken()`: `GET /profile/token` → профиль → запись токена и бэкап. Не получили токен → остаёмся на кэше профиля                                                             |
| Выход                                            | `logout()`: `POST /profile/logout` (ошибка игнорируется), чистка документ-кэша пользователя, `expireAllVideos()`, бэкап, токен, аккаунт в настройках                                     |
| Старт без сессии                                 | Восстановление из Block Store через тот же `signInWithTokenInternal`                                                                                                               |
| Сервер отверг токен (401/403) при восстановлении | Запись в Block Store удаляется                                                                                                                                                     |

Принцип входа: токен — единственный носитель авторизации, поэтому он пишется первым, а любая ошибка
после записи откатывает всё целиком. Иначе приложение оставалось бы «полувошедшим»: запросы уже
авторизованы, а экран входа крутится по кругу. Профиль в кэше не критичен для входа: ошибка записи
профиля в кэш не прерывает вход, а отдаёт аккаунт из ответа.

Смена пользователя: `clearPreviousDocumentCacheIfNeeded` сравнивает прошлый `userId`, поэтому чистка
идёт до `setYaniAccount`, иначе она сравнила бы пользователя сам с собой.

## Ошибки

`ErrorHandler.parse(throwable, navigate, retryKey, owner)` приводит исключение к `ErrorItem` для UI
(ViewModel вызывает из каждого `catch`).

| Класс                      | Заголовок и текст                                                 |
|----------------------------|-------------------------------------------------------------------|
| `CancellationException`    | Пробрасывается дальше, не обрабатывается                          |
| `ResponseException` (Ktor) | По коду: 401, 403, 404, 429, 500, 502, 503, 504, иначе общий HTTP |
| `SocketTimeoutException`   | «Таймаут»                                                         |
| `IOException`              | «Нет соединения»                                                  |
| Остальное                  | Сообщение исключения или общий текст                              |

- `navigate = true` открывает экран ошибки через `INavigationManager` (`ErrorDestinationFactory`).
- `retryKey` связывает «Повторить» с действием в `RetryStorage` (`put`/`consume`/`remove`): экран
  ошибки не знает, что повторять, действие кладёт ViewModel.
- Ошибки, не связанные с сетью, при заданном `owner` уходят в аналитику корутин
  (`ErrorCoroutineAnalytics`); сетевые не репортятся, чтобы не засорять отчёты.
- Строки ошибок лежат в ресурсах `err_title_*` и `err_msg_*`, `StringProvider` читает их без
  Compose.
