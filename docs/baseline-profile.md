# Baseline Profile

Baseline profile — список классов и методов, которые ART компилирует в машинный код при установке.
Ускоряет холодный старт и первые прокрутки Compose-экранов, особенно на слабых ТВ-приставках.
Приложение ставится мимо Play (самообновление), поэтому профиль на устройство кладёт
`androidx.profileinstaller`.

Профиль — текст из правил двух видов: класс `L...;` или метод `L...;->имя(аргументы)возврат` с
флагами впереди (`H` — часто вызывается, `S` — во время старта, `P` — после старта). Правил ~60
тыс.:
в профиль попадают не только наш код, но и всё, что он дёргает в Compose, Coroutines, Ktor, Coil.
AGP собирает текст в бинарный `assets/dexopt/baseline.prof` (~30 КБ) и объединяет с профилями
библиотек.

Файлов два:

- `baseline-prof.txt` — всё, что выполняется в ключевых сценариях; ART компилирует это заранее;
- `startup-prof.txt` — только холодный старт; по нему R8 кладёт код старта в первый DEX.

## Как устроено

- Профиль коммитится в `app/src/release/generated/baselineProfiles/`.
- Обычная `assembleRelease` (локально и в CI) эмулятор не запускает, а только упаковывает
  закоммиченный файл (`automaticGenerationDuringBuild = false`).
- Сценарии, генераторы и бенчмарки — в модуле `:baselineprofile`:
    - `journey/` — общие шаги (старт, лента, детали, поиск, вкладки / DPAD-навигация);
    - `generator/` — `StartupProfileGenerator` (только старт), `MobileBaselineProfileGenerator`,
      `TvBaselineProfileGenerator`, `MobilePlayerProfileGenerator`, `TvPlayerProfileGenerator`
      (каждый пропускает чужой тип устройства);
    - `benchmark/` — `StartupBenchmark`, `MobileScrollBenchmark`, `TvGridBenchmark`, каждый в
      режимах
      `None` (как свежая установка) и `BaselineProfile`.
- Плеер вынесен в отдельные генераторы: поток берётся у внешнего балансера. Если он не пришёл за 45
  с, сценарий возвращается на главную и профиль просто не включит код воспроизведения (проверка —
  см. «Проверка профиля»).

## Команды

```bash
./gradlew generateBaselineProfiles
```

Поднимает эмуляторы без окон, генерирует профиль, гасит эмуляторы. ~15–20 минут. После — проверить
diff в `app/src/release/generated/baselineProfiles/` и закоммитить.

```bash
./gradlew runProfileBenchmarks
```

Гоняет бенчмарки на минифицированной сборке и печатает таблицу «без профиля / с профилем» (копия —
`baselineprofile/build/reports/baseline-profile-benchmark.md`). На эмуляторе абсолютные цифры
шумные, смотреть на разницу между режимами. Время старта у реальных пользователей — в
[startup-metrics.md](startup-metrics.md).

`./gradlew :baselineprofile:benchmarkReport` перепечатывает таблицу по последнему прогону.

Оба режима гоняются на одном APK, а раскладка DEX по startup-профилю закладывается при сборке и есть
в обоих, поэтому таблица показывает эффект AOT-компиляции, а не раскладки. Чтобы померить раскладку,
нужен второй прогон на APK без неё:

```bash
./gradlew runProfileBenchmarks -Pyummytv.profile.dexLayout=false
```

Отчёт — `baselineprofile/build/reports/baseline-profile-benchmark-no-dex-layout.md`, обычный
остаётся в `baseline-profile-benchmark.md`. Флаг выключает раскладку только у `benchmarkRelease`. Он
задаёт свойство AGP `android.experimental.r8.dex-startup-optimization` напрямую: настройку
`dexLayoutOptimization` плагин применяет только к release/releaseDebug, а `benchmarkRelease` берёт
дефолт AGP (в AGP 9 раскладка включена). Раскладка влияет в первую очередь на холодный старт.

### Эмуляторы

- Список AVD — свойство `yummytv.profile.avds` в `gradle.properties` (телефон + Android TV). Свои
  имена — в `~/.gradle/gradle.properties` или `-Pyummytv.profile.avds=Phone,Tv`.
- Уже запущенные эмуляторы и подключённые устройства переиспользуются и не гасятся. Сценарии идут на
  всех подключённых устройствах, лишние лучше отключить.
- `PROFILE_EMULATOR_WINDOW=1 ./gradlew ...` показывает окна эмуляторов.
- Нужен API 28+ (на API 33+ root не нужен, на 28–32 — образы без Google Play) и сеть: сценарии
  открывают экраны с реальными данными.
- Gradle Managed Devices не используются: они не поддерживают образы Android TV.

## Когда перегенерировать

Перед релизом, если заметно менялись экраны, навигация или UI-зависимости (Compose BOM, navigation,
paging). В среднем раз в несколько релизов.

## Варианты сборки в Android Studio

Плагин добавляет `nonMinifiedRelease*` (генерация: без обфускации, чтобы правила совпали с кодом) и
`benchmarkRelease*` (бенчмарки: минифицированный release, подписанный debug-ключом). Для обычной
работы выбирать их не нужно. `*ReleaseDebug` появляются из-за build type `releaseDebug`.

## Test tags

Сценарии находят элементы по `Modifier.testTag`, видимым UiAutomator как resource-id
(`testTagsAsResourceId` в `MobileActivity`/`TvActivity`). При переделке экранов теги сохранять:

| Тег             | Где                                                                                                    |
|-----------------|--------------------------------------------------------------------------------------------------------|
| `home_feed`     | Лента главной (mobile `HomeMobileScreen`, TV `HomeDashboard`)                                          |
| `home_search`   | Плашка поиска на мобильной главной                                                                     |
| `anime_card`    | Карточка тайтла в ряду мобильной главной (`HomeFeedSectionRow`)                                        |
| `details_root`  | Экран деталей (mobile `DetailsMobileScreen`, TV `DetailsTvScreen`)                                     |
| `main_tab`      | Вкладки нижней навигации (`MobileMainScaffold`)                                                        |
| `watch_button`  | Кнопка «Смотреть» (mobile `DetailsPrimaryActions`, TV `DetailsActionButton`)                           |
| `player_screen` | Корень экрана плеера, есть и пока грузится поток (`PlayerMobileScreen`, `PlayerTvScreen`)              |
| `picker_option` | Вариант в шторке/диалоге выбора озвучки и плеера; окна диалогов включают `testTagsAsResourceId` у себя |
| `player_view`   | Плеер с готовым потоком (`MobileNativePlayer`, `TvExoPlayerView`)                                      |

Если сценарий перестал доходить до экрана, профиль молча «худеет».

## Проверка профиля

Профиль содержит код нужных фич:

```bash
for f in details player schedule top library; do echo "$f: $(grep -c "feature/$f" app/src/release/generated/baselineProfiles/baseline-prof.txt)"; done
```

Профиль попал в APK (должен быть `assets/dexopt/baseline.prof`):

```bash
unzip -l app/build/outputs/apk/release/*.apk | grep dexopt
```

## Замеры (24.09.2026)

Эмуляторы Pixel 10 Pro XL (API 37) и Android TV 1080p (API 34) на Apple Silicon,
`./gradlew runProfileBenchmarks`:

|                                      | Без профиля | С профилем | Разница |
|--------------------------------------|------------:|-----------:|--------:|
| Холодный старт, телефон (медиана)    |    374.6 мс |   313.1 мс |   −16 % |
| Холодный старт, ТВ (медиана)         |    208.1 мс |   200.2 мс |    −4 % |
| Прокрутка главной, телефон, кадр P99 |     71.0 мс |    61.2 мс |   −14 % |
| DPAD по сетке, ТВ, перебор кадра P90 |      3.1 мс |     1.6 мс |   −48 % |

Эмулятор на мощном хосте занижает выигрыш: на слабой приставке интерпретатор дороже, разница должна
быть больше. P50 кадров шумит в пределах ±10 %, 5 итераций для кадров мало. На реальной приставке не
мерено.
