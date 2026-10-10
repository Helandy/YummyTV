# Git-хуки и стиль кода

Что проверяется локально до коммита и пуша, как оформлять сообщения коммитов и откуда берутся
правила форматирования. Структурные правила модулей и файлов (пакеты `.domain.model`, `.view`,
`.handler` и т. д.) лежат в `AGENTS.md` и здесь не дублируются.
CI — [release-and-ci.md](release-and-ci.md).

## Установка

```bash
./scripts/install-git-hooks.sh
```

Скрипт проверяет наличие `.githooks/{commit-msg,pre-commit,pre-push,ktlint.sh}`, делает их
исполняемыми и ставит `git config core.hooksPath .githooks`. Хуки лежат в репозитории и работают без
сторонних менеджеров. Повторная установка безопасна.

## Хуки

| Хук          | Что делает                                                                 | Обход                          |
|--------------|----------------------------------------------------------------------------|--------------------------------|
| `commit-msg` | Проверяет заголовок сообщения: формат `[ТЕГ] описание`                     | `git commit --no-verify`       |
| `pre-commit` | `ktlint --format` по staged `*.kt`/`*.kts`, добавляет исправления в индекс | `SKIP_KTLINT=1`, `--no-verify` |
| `pre-push`   | `./gradlew :app:lintDebug`, печатает число ошибок и предупреждений         | `git push --no-verify`         |

### commit-msg

Заголовок обязан соответствовать `^\[(BUGFIX|CORE|MOBILE|TV|DOCS|CI|RELEASE|FEATURE|UI)\] .+`.

Допустимые теги: `BUGFIX`, `CORE`, `MOBILE`, `TV`, `DOCS`, `CI`, `RELEASE`, `FEATURE`, `UI`.
Примеры из сообщения хука: `[CORE] refactor catalog loading`, `[TV] fix focus on details screen`,
`[BUGFIX] handle empty schedule`. Версионный коммит в истории: `[RELEASE] 1.25`.

Без тега разрешены служебные коммиты: `Merge …`, `Revert …`, `fixup! …`, `squash! …`, `amend! …`.
Пустые строки и строки-комментарии в начале сообщения пропускаются, заголовком считается первая
значимая строка.

### pre-commit: ktlint

- Версия ktlint закреплена в `.githooks/ktlint.sh` (`KTLINT_VERSION`) вместе с SHA-256. Jar
  скачивается один раз в `~/.cache/ktlint` и сверяется по контрольной сумме; при несовпадении хук
  падает, а не запускает неизвестный файл.
- Нет `curl`, `java`, утилиты SHA-256 или сети: проверка стиля пропускается (`exit 0`), а не
  блокирует коммит.
- Файлы, где есть только staged-изменения, форматируются (`-F`) и заново добавляются в индекс.
- Файлы, у которых есть ещё и unstaged-изменения, только проверяются и не правятся: автоформат
  втянул бы чужие правки в коммит. Для них нужно поправить руками или застейджить файл целиком.
- Неисправимые нарушения блокируют коммит.

### pre-push: lint

`./gradlew :app:lintDebug --stacktrace`. После запуска хук читает `app/build/reports/lint-results-debug.xml`
и печатает число `severity="Error"` и `severity="Warning"`. Ненулевой код возврата Gradle блокирует push.
CI запускает тот же `:app:lintDebug` (`android-build.yml`).

## Правила форматирования

`.editorconfig` в корне:

| Параметр                                                                                          | Значение                                            |
|---------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| кодировка, перевод строки, отступ                                                                 | `utf-8`, `lf`, 4 пробела                            |
| финальная пустая строка, trailing spaces                                                          | вставлять / обрезать (для `*.md` trailing разрешён) |
| `ktlint_code_style`                                                                               | `intellij_idea`                                     |
| `max_line_length`                                                                                 | `off`                                               |
| `ktlint_standard_function-signature`, `…blank-line-between-when-conditions`, `…function-naming`, `…property-naming` | `disabled`                          |

## Compose stability

`configureComposeCompiler` (`build-logic/.../ConventionPlugins.kt`) добавляет
`config/compose-stability.conf` в `stabilityConfigurationFiles` каждого Compose-модуля, если файл
существует. Комментарий в файле: «Стабильные модели из чистых JVM-domain модулей, где нельзя ставить
@Immutable (без Compose)»; дальше построчно полные имена классов (например,
`su.afk.yummy.tv.domain.account.model.UserStats`).

Отчёты стабильности включаются `-PenableComposeCompilerReports=true`, вывод в
`build/compose_compiler`.
