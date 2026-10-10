# Документация

Технические заметки по внутреннему устройству приложения. Описывают механику приложения в некоторых
важных местах.

## Плеер

- [alloha-player.md](alloha-player.md) — Alloha: добыча сессии через WebView, loopback-прокси,
  ротация, лестница отказа. Открывать при любой правке `extractor/alloha/`.
- [cvh-player.md](cvh-player.md) — CVH: подписанные ссылки okcdn, переезд на резервный узел,
  User-Agent, Cast.
- [player-buffering.md](player-buffering.md) — буфер `DefaultLoadControl`, различия Kodik, CVH и
  Alloha, диагностика остановок.
- [continue-watching.md](continue-watching.md) — правила карточек «Продолжить просмотр».

## Аккаунт

- [local-auth-session-transfer.md](local-auth-session-transfer.md) — вход на ТВ через телефон по
  локальной сети: NSD, QR, протокол, шифрование.
- [block-store-session-restore.md](block-store-session-restore.md) — восстановление входа после
  переустановки через Block Store.
- [password-manager-autofill.md](password-manager-autofill.md) — сохранение пароля в менеджере
  паролей.
- [subscriptions.md](subscriptions.md) — подписки на озвучку: контракт yani API, ключи, два экрана.

## Сборка и релизы

- [beta-updates.md](beta-updates.md) — бета-канал: нумерация версий, публикация, выбор релиза.
- [baseline-profile.md](baseline-profile.md) — генерация и проверка baseline profile, бенчмарки.
- [startup-metrics.md](startup-metrics.md) — замер холодного старта у пользователей.

## Разработка

- [unit.md](unit.md) — как оформлять unit-тесты.
