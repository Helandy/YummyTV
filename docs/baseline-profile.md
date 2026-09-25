# Baseline Profile

Baseline profile — список классов и методов, которые ART компилирует в машинный код сразу при
установке. Ускоряет холодный старт и первые прокрутки Compose-экранов, особенно на слабых
ТВ-приставках. Приложение ставится мимо Play (самообновление), поэтому профиль на устройство
кладёт `androidx.profileinstaller`.

## Что такое профиль

Текстовый список правил: «эти классы и методы понадобятся — скомпилируй их заранее».
Каждая строка — класс (`L...;`) или метод (`L...;->имя(аргументы)возврат`) с флагами впереди:

| Флаг | Значение |
|---|---|
| `H` (hot) | метод вызывается часто — компилировать в машинный код при установке |
| `S` (startup) | вызывается во время старта |
| `P` (post-startup) | вызывается после старта (прокрутка, переходы) |
| без флагов, строка `L...;` | класс — загрузить и проверить заранее |

```
HSPLsu/afk/yummy/tv/android/di/DaggerYummyTvApplication_HiltComponents_SingletonC$ViewModelCImpl;->detailsAnalytics()Lsu/afk/yummy/tv/feature/details/DetailsAnalytics;
Lsu/afk/yummy/tv/android/Hilt_InterfaceRouterActivity;
```

Правил ~60 тыс.: в профиль попадает не только наш код, но и всё, что он дёргает в Compose,
Coroutines, Ktor, Coil и т. д. При сборке AGP переводит текст в бинарный
`assets/dexopt/baseline.prof` (~30 КБ) и объединяет с профилями библиотек. При установке APK
`profileinstaller` передаёт его ART, и тот компилирует перечисленное сразу, а не через
несколько дней фоновой оптимизации.

Файлов два:

- `baseline-prof.txt` — всё, что выполняется в ключевых сценариях; ART компилирует это заранее;
- `startup-prof.txt` — только холодный старт; по нему R8 кладёт код старта в первый DEX,
  чтобы при запуске читать с диска меньше.

## Как устроено

- Профиль **коммитится** в `app/src/release/generated/baselineProfiles/`.
- Обычная `assembleRelease` (локально и в CI) эмулятор не запускает — только упаковывает
  закоммиченный файл (`automaticGenerationDuringBuild = false`).
- Сценарии, генераторы и бенчмарки — в модуле `:baselineprofile`:
  - `journey/` — общие шаги (старт, лента, детали, поиск, вкладки / DPAD-навигация);
  - `generator/` — `StartupProfileGenerator` (только старт), `MobileBaselineProfileGenerator`,
    `TvBaselineProfileGenerator` (каждый пропускает «чужой» тип устройства);
  - `benchmark/` — `StartupBenchmark`, `MobileScrollBenchmark`, `TvGridBenchmark`, каждый в двух
    режимах: `None` (как свежая установка без профиля) и `BaselineProfile`.
- Плеер в сценарии не входит: он зависит от сети и внешних балансеров.

## Команды

```bash
./gradlew generateBaselineProfiles
```

Поднимает эмуляторы (без окон), генерирует профиль, гасит эмуляторы. ~15–20 минут.
После — проверить diff в `app/src/release/generated/baselineProfiles/` и закоммитить.

```bash
./gradlew runProfileBenchmarks
```

Поднимает эмуляторы, гоняет бенчмарки на минифицированной сборке и печатает таблицу
«без профиля / с профилем» (копия — `baselineprofile/build/reports/baseline-profile-benchmark.md`).
На эмуляторе абсолютные цифры шумные — смотреть на разницу между режимами.

`./gradlew :baselineprofile:benchmarkReport` — перепечатать таблицу по последнему прогону.

Режимы «без профиля / с профилем» гоняются на одном APK, а раскладка DEX по startup-профилю
закладывается при сборке и есть в обоих. Поэтому таблица показывает эффект AOT-компиляции,
а не раскладки. Чтобы померить раскладку, запусти второй прогон на APK без неё
и сравни с обычным отчётом:

```bash
./gradlew runProfileBenchmarks -Pyummytv.profile.dexLayout=false
```

Отчёт пишется в `baselineprofile/build/reports/baseline-profile-benchmark-no-dex-layout.md`,
обычный остаётся в `baseline-profile-benchmark.md`. Флаг выключает раскладку только у
`benchmarkRelease`; release он не трогает. Раскладка в первую очередь влияет на холодный старт.

Флаг задаёт свойство AGP `android.experimental.r8.dex-startup-optimization` напрямую: настройку
`dexLayoutOptimization` плагин baselineprofile применяет только к release/releaseDebug, а
`benchmarkRelease` берёт дефолт AGP (в AGP 9 раскладка включена по умолчанию).

### Эмуляторы

- Список AVD — свойство `yummytv.profile.avds` в `gradle.properties` (телефон + Android TV).
  Свои имена — в `~/.gradle/gradle.properties` или `-Pyummytv.profile.avds=Phone,Tv`.
- Уже запущенные эмуляторы и подключённые устройства переиспользуются и не гасятся.
  Сценарии идут на **всех** подключённых устройствах — лишние лучше отключить.
- `PROFILE_EMULATOR_WINDOW=1 ./gradlew ...` — показать окна эмуляторов.
- Нужен API 28+ (на API 33+ root не нужен; на 28–32 — образы без Google Play) и сеть:
  сценарии открывают экраны с реальными данными.
- Gradle Managed Devices не используются: они не поддерживают образы Android TV.

## Замеры (24.09.2026)

Эмуляторы Pixel 10 Pro XL (API 37) и Android TV 1080p (API 34) на Apple Silicon,
`./gradlew runProfileBenchmarks`:

| | Без профиля | С профилем | Разница |
|---|---:|---:|---:|
| Холодный старт, телефон (медиана) | 374.6 мс | 313.1 мс | −16 % |
| Холодный старт, ТВ (медиана) | 208.1 мс | 200.2 мс | −4 % |
| Прокрутка главной, телефон, кадр P99 | 71.0 мс | 61.2 мс | −14 % |
| DPAD по сетке, ТВ, перебор кадра P90 | 3.1 мс | 1.6 мс | −48 % |

Эмулятор на мощном хосте занижает выигрыш: на слабой приставке интерпретатор дороже,
разница должна быть больше. P50 кадров шумит в пределах ±10 %, 5 итераций для кадров мало.
На реальной приставке не мерено.

## Когда перегенерировать

Перед релизом, если заметно менялись экраны, навигация или UI-зависимости
(Compose BOM, navigation, paging). В среднем — раз в несколько релизов.

## Варианты сборки в Android Studio

Плагин добавляет варианты `nonMinifiedRelease*` (генерация: без обфускации, чтобы правила
совпадали с кодом) и `benchmarkRelease*` (бенчмарки: минифицированный release, подписанный
debug-ключом). Для обычной работы их выбирать не нужно. `*ReleaseDebug` появляются из-за
build type `releaseDebug`.

## Test tags

Сценарии находят элементы по `Modifier.testTag`, которые видны UiAutomator как resource-id
(`testTagsAsResourceId` в `MobileActivity`/`TvActivity`). При переделке экранов теги сохранять:

| Тег | Где |
|---|---|
| `home_feed` | лента главной (mobile `HomeMobileScreen`, TV `HomeDashboard`) |
| `home_search` | плашка поиска на мобильной главной |
| `anime_card` | карточка тайтла в ряду мобильной главной (`HomeFeedSectionRow`) |
| `details_root` | экран деталей (mobile `DetailsMobileScreen`, TV `DetailsTvScreen`) |
| `main_tab` | вкладки нижней навигации (`MobileMainScaffold`) |

Если сценарий перестал доходить до экрана — профиль молча «похудеет». Проверка после генерации:

```bash
grep -c 'feature/details' app/src/release/generated/baselineProfiles/baseline-prof.txt
```

## Проверка, что профиль в APK

```bash
unzip -l app/build/outputs/apk/release/*.apk | grep dexopt
```

Должен быть `assets/dexopt/baseline.prof`.
