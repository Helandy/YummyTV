# Сборка, CI и выпуск

Как собирается приложение, что проверяет CI и как релиз доходит до пользователя. Правила версий и
бета-канала — [beta-updates.md](beta-updates.md). Профиль
запуска — [baseline-profile.md](baseline-profile.md). Локальные проверки до
коммита — [git-hooks-and-code-style.md](git-hooks-and-code-style.md).

## Версия и идентификатор

В `gradle.properties`:

| Свойство                | Значение сейчас   | Назначение                                                |
|-------------------------|-------------------|-----------------------------------------------------------|
| `yummytv.applicationId` | `su.afk.yummy.tv` | Базовый id; debug получает суффикс `.debug`               |
| `yummytv.versionName`   | `1.25`            | Версия для пользователя, сравнивается компаратором        |
| `yummytv.versionCode`   | `128`             | Только растёт, иначе APK не встанет поверх установленного |

Ключи (`yummytv.appmetricaApiKey`, `yummytv.varioqubClientId`) читаются из `local.properties`
(комментарий в `gradle.properties`) и попадают в `BuildConfig` через `buildConfigSecret`
(`build-logic/.../BuildSecrets.kt`). KDoc: не найден — пустая строка, сборка проходит,
соответствующий SDK не инициализируется (`AppMetricaAnalyticsInitializer` при пустом ключе выходит).

Имя файла APK: `YummyTV-<versionName>-<buildType>.apk`.

## Типы сборки

| Тип                                       | Суффикс id      | R8 / shrink             | Подпись                                               | Для чего                                               |
|-------------------------------------------|-----------------|-------------------------|-------------------------------------------------------|--------------------------------------------------------|
| `debug`                                   | `.debug`        | нет                     | debug                                                 | —                                                      |
| `release`                                 | —               | да                      | `signingConfig` в репозитории не задан                | —                                                      |
| `releaseDebug`                            | `.releasedebug` | да                      | debug                                                 | Проверить результат минификации без релизного keystore |
| `benchmarkRelease` / `nonMinifiedRelease` | —               | см. baseline-profile.md | debug                                                 | Генерация профиля и бенчмарки                          |

Комментарии в `app/build.gradle.kts`: `releaseDebug` — релизная сборка (R8 + shrinkResources),
подписанная debug-ключом, чтобы можно было ставить и проверять результат минификации без релизного
keystore; библиотечные модули этот тип не объявляют, поэтому для них `matchingFallbacks` берёт
release-вариант.

Целевой и compile SDK задаются в `gradle/libs.versions.toml` (`android-compileSdk`), `targetSdk`
равен ему. Java 21, core library desugaring (`desugar_jdk_libs`).

## CI (GitHub Actions)

| Workflow            | Когда                                  | Что делает                                                   |
|---------------------|----------------------------------------|--------------------------------------------------------------|
| `android-build.yml` | `pull_request`, push в `main`/`master` | Lint, unit-тесты, `assembleDebug`, `assembleRelease`, сводка |
| `unit-tests.yml`    | push в любую ветку                     | `testDebugUnitTest`, при падении выгружает отчёты на 7 дней  |

Особенности `android-build.yml`:

- Версия платформы Android SDK берётся из каталога (`android-compileSdk`): начиная с API 36
  платформы публикуются с минорной версией (`android-37.0`), и скрипт добавляет `.0`, если её нет.
- Шаги Lint и тестов идут с `continue-on-error`, чтобы сборка и сводка выполнились при любом исходе;
  в конце отдельный шаг роняет job, если упал lint или тесты.
- Сводка в `GITHUB_STEP_SUMMARY`: статусы шагов, число тестов и упавших, ошибки и предупреждения
  lint, размер debug APK.
- `concurrency` отменяет устаревшие прогоны одной ветки.
- `assembleRelease` в CI собирается без секретов (поля `BuildConfig` пустые) и без подписи:
  проверяется, что release-вариант вообще собирается и R8 проходит. APK из CI не публикуется.

## Выпуск релиза

Что зафиксировано в репозитории:

- Версия: `yummytv.versionName` и `yummytv.versionCode` в `gradle.properties`; правила нумерации и
  тегов — [beta-updates.md](beta-updates.md) (стабильный `v1.21.1`, бета `b1.21.1.2` с флагом
  pre-release).
- Коммит версии называется `[RELEASE] <версия>` (например, `[RELEASE] 1.25`); тег `RELEASE`
  разрешён хуком `commit-msg`.
- Дистрибуция — GitHub Releases `Helandy/YummyTV` (`UpdateConfig.GITHUB_OWNER`/`GITHUB_REPO`).
- Конфигурации подписи для типа `release` в проекте нет: ни `signingConfig`, ни keystore, ни
  скрипта подписи.
- `GitHubReleaseMapper`: из релиза берётся первый asset, оканчивающийся на `.apk`, иначе первый
  asset; релиз без asset'ов и черновик в обновления не попадают.

## Самообновление

Модуль `feature:update` (`api`, `domain`, `data`, `presentation`, `ui`).

```
старт приложения → MainSideEffectsHandler.checkForUpdates
  GitHubUpdateRepository: GET /repos/Helandy/YummyTV/releases?per_page=100
  выбор релиза: самая большая версия среди недрафтовых с APK, с учётом настройки «Бета-версии»
  UpdateDialog (обязательный, если версия ниже минимальной из feature toggle)
  ApkDownloaderImpl: update.apk.part → проверка ZIP → rename в update.apk (cacheDir)
  ApkInstallerImpl: PackageInstaller (MODE_FULL_INSTALL), подтверждение пользователя
```

Детали:

- Скачивание идёт через `@UnauthenticatedJsonClient` без общего таймаута запроса (APK качается
  долго): ограничен только простой сокета (30 с). Тело стримится (`prepareGet`), а не буферизуется.
- Во время загрузки поднимается foreground-сервис `UpdateDownloadService` (тип `dataSync`) с
  уведомлением и прогрессом.
- Если у приложения нет разрешения «Установка неизвестных приложений», открывается системный экран
  настроек и бросается `UpdatePermissionRequiredException`.
- Установка — через `PackageInstaller`: итог приходит в `BroadcastReceiver` с уникальным action на
  сессию; `STATUS_PENDING_USER_ACTION` перенаправляет на системное подтверждение.
- После успешного обновления `AppStartupMaintenanceRunner` по смене `VERSION_CODE` удаляет
  `update.apk` из кэша.
- Если автоматическая установка не удалась, в диалоге есть ссылка на страницу
  `https://github.com/Helandy/YummyTV/releases/latest`.

Ограничения: одна страница `/releases?per_page=100` (`RELEASES_PER_PAGE`); проверка при запуске
(`MainSideEffectsHandler.checkForUpdates`).

## Разрешения, относящиеся к выпуску

`REQUEST_INSTALL_PACKAGES` (самообновление), `FOREGROUND_SERVICE_DATA_SYNC` (загрузка обновления и
видео), `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (плеер), `POST_NOTIFICATIONS`. Типы
`foregroundServiceType`: `dataSync` (`androidx.work.impl.foreground.SystemForegroundService` для WorkManager и `UpdateDownloadService`), `mediaPlayback`
(`PlayerMediaSessionService`).
