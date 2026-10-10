# Документация

Технические заметки по внутреннему устройству приложения. Описывают механику приложения в некоторых
важных местах.

## Архитектура и UI

- [architecture.md](architecture.md) — карта модулей, слои фичи, правила зависимостей, как экран
  попадает в навигацию. Точка входа в остальные доки.
- [navigation.md](navigation.md) — Navigation3: два уровня back stack, сцены, как добавить экран,
  диплинки.
- [mvi-presentation.md](mvi-presentation.md) — `State` / `Event` / `Effect`, `BaseViewModel`,
  handler'ы, одноразовые события, связь с Compose.
- [mobile-tv-ui-sharing.md](mobile-tv-ui-sharing.md) — один APK для двух интерфейсов: коллизии
  файлов и строк, общая тема, `CompositionLocal`.
- [tv-focus.md](tv-focus.md) — фокус и скролл на ТВ: гриды, возврат фокуса, потеря фокуса в меню.
- [insets-and-sheets.md](insets-and-sheets.md) — нижние инсеты, `BaseScreen`, высота и отступы
  шторок.
- [image-loading-and-memory.md](image-loading-and-memory.md) — Coil, качество постеров, Paging, ключи
  lazy-списков, коллекции в состоянии.

## Данные и API

- [yani-api.md](yani-api.md) — контракт и странности API yani: серии, прогресс, профиль, сезоны,
  расписание.
- [network-and-auth.md](network-and-auth.md) — HTTP-клиенты, токен и режимы хранения, ошибки,
  `network_security_config`.
- [storage-and-cache.md](storage-and-cache.md) — Room, DataStore, offline-first и TTL, очистка,
  очередь мутаций.
- [watch-progress.md](watch-progress.md) — путь прогресса просмотра: локальная запись, отправка на
  сервер, Watch Next, запуск с другого устройства.

## Плеер

- [player-architecture.md](player-architecture.md) — ViewModel, поведения источников, сервис
  медиа-сессии, подключение UI, различия ТВ и мобилки.
- [other-extractors.md](other-extractors.md) — как iframe-URL превращается в поток: общая схема и
  Kodik, VK, Rutube, Sibnet, Aksor, Zedfilm.
- [alloha-player.md](alloha-player.md) — Alloha: добыча сессии через WebView, loopback-прокси,
  ротация, лестница отказа. Открывать при любой правке `extractor/alloha/`.
- [cvh-player.md](cvh-player.md) — CVH: подписанные ссылки okcdn, переезд на резервный узел,
  User-Agent, Cast.
- [player-buffering.md](player-buffering.md) — буфер `DefaultLoadControl`, различия Kodik, CVH и
  Alloha, диагностика остановок.
- [video-download.md](video-download.md) — офлайн-загрузки: воркер, стратегии, ключи кэша, экспорт.
- [continue-watching.md](continue-watching.md) — правила карточек «Продолжить просмотр».

## Аккаунт

- [local-auth-session-transfer.md](local-auth-session-transfer.md) — вход на ТВ через телефон по
  локальной сети: NSD, QR, протокол, шифрование.
- [block-store-session-restore.md](block-store-session-restore.md) — восстановление входа после
  переустановки через Block Store.
- [password-manager-autofill.md](password-manager-autofill.md) — сохранение пароля в менеджере
  паролей.
- [subscriptions.md](subscriptions.md) — подписки на озвучку: контракт yani API, ключи, два экрана.

## Сборка, выпуск и окружение

- [release-and-ci.md](release-and-ci.md) — типы сборки, CI, выпуск на GitHub, самообновление.
- [beta-updates.md](beta-updates.md) — бета-канал: нумерация версий, публикация, выбор релиза.
- [baseline-profile.md](baseline-profile.md) — генерация и проверка baseline profile, бенчмарки.
- [startup-metrics.md](startup-metrics.md) — замер холодного старта у пользователей.
- [analytics-and-logging.md](analytics-and-logging.md) — события, ошибки, файл логов, что нельзя
  отправлять.
- [tv-device-quirks.md](tv-device-quirks.md) — где код зависит от типа устройства: ТВ, Cast, режим
  хранения токена, память, loopback.

## Разработка

- [git-hooks-and-code-style.md](git-hooks-and-code-style.md) — хуки, формат коммитов, ktlint, lint.
- [unit.md](unit.md) — как оформлять unit-тесты.
