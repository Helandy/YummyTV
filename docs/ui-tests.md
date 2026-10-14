# UI-тесты (Compose, androidTest): как оформлять

Инструментальные тесты экранов на Compose UI Testing. Работают на устройстве или эмуляторе,
лежат в `src/androidTest`. Юнит-тесты ViewModel и use case — отдельно, см. [unit.md](unit.md).
Тесты пишем только по явной просьбе (см. `AGENTS.md`).

## Что покрываем

- Экран целиком по контракту `XxxScreen(state, effect, onEvent)`: состояние подаём напрямую,
  события собираем в список. Сеть, DI и репозитории не нужны.
- Проверяем то, что видит пользователь: тексты, раскрытие/сворачивание, клики и событие,
  которое ушло в `onEvent`.
- Не покрываем: плеер, WebView-экстракторы, реальные CDN — это флаки, их проверяем руками.
- ТВ-фокус по возможности проверяем на изолированных композаблах (карточка, грид), а не на
  всём приложении.

## Подключение в модуле

Эталон — `:feature:faq:ui-mobile`. В `build.gradle.kts` модуля:

```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    debugImplementation(libs.compose.uiTest.manifest)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.uiTest.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

## Структура теста

- Файл: `src/androidTest/kotlin/<пакет экрана>/XxxMobileScreenTest.kt`, класс — `XxxMobileScreenTest`.
  Мобильные и ТВ-тесты держим в своих модулях `ui-mobile` / `ui-tv`, имена не пересекаем.
- JUnit4 и `createComposeRule()`; `@get:Rule val composeRule`.
- Экран оборачиваем в `ScreenPreviewTheme` (`core:designsystem.preview`), как в превью.
- События собираем в поле: `private val events = mutableListOf<XxxState.Event>()`,
  `onEvent = { events += it }`.
- Одна функция `setScreen(...)` создаёт содержимое; параметры состояния — её аргументы.

```kotlin
class FaqMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<FaqState.Event>()

    private fun setScreen() {
        composeRule.setContent {
            ScreenPreviewTheme {
                FaqMobileScreen(FaqState.State, emptyFlow(), onEvent = { events += it })
            }
        }
    }
}
```

## Поиск элементов

- Строки берём из ресурсов, не хардкодим: устройство может быть в ru/uk/en.
  `InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.xxx)`.
- `R` фичи — из `...presentation.R`; строки дизайн-системы — `su.afk.yummy.tv.core.designsystem.R`
  (импорт с алиасом, чтобы не путать).
- Находим по тексту (`onNodeWithText`) или `contentDescription` (`onNodeWithContentDescription`).
  `testTag` добавляем только когда иначе элемент не найти.
- Исчезновение анимированного элемента проверяем через `composeRule.waitUntil(timeoutMillis = …)`
  и `onAllNodesWithText(...).fetchSemanticsNodes().isEmpty()`.

## Запуск

```bash
./gradlew :feature:faq:ui-mobile:connectedDebugAndroidTest
```

- Gradle гоняет тесты на всех подключённых устройствах. Нужное выбираем через `ANDROID_SERIAL`
  (серийник из `adb devices`):

  ```bash
  ANDROID_SERIAL=emulator-5554 ./gradlew :feature:faq:ui-mobile:connectedDebugAndroidTest
  ```
- Эмулятор без окна: `emulator -avd <имя> -no-window -no-audio -no-boot-anim`; готов, когда
  `adb shell getprop sys.boot_completed` вернёт `1`.
- Устройство должно быть разбужено: у спящей приставки нет окна, все тесты падают с
  `No compose hierarchies found in the app`. Лечится `adb shell input keyevent KEYCODE_WAKEUP`.
- Отчёт: `<модуль>/build/reports/androidTests/connected/debug/index.html`.

## Частые ловушки

- **Paging.** `PagingData.from(...)` и `PagingData.empty()` без `sourceLoadStates` оставляют список в
  состоянии Loading: экран рисует скелетон, а не данные. Передавайте
  `LoadStates(refresh = NotLoading(false), prepend = NotLoading(true), append = NotLoading(true))`.
- **Дубли текста.** Название экрана часто встречается дважды (топ-бар и заголовок секции), а в
  русской локали ещё и совпадает с подписью кнопки. Кнопку ищите по
  `hasText(...) and hasClickAction()`, дубль заголовка проверяйте через `assertCountEquals`.
- **LazyColumn.** Ниже экрана элементы не скомпонованы, `performScrollTo()` их не найдёт. Сначала
  `onNode(hasScrollAction()).performScrollToNode(hasText(...))`.
- **HTML-контент.** Парсер публикаций и рецензий разбирает теги: передавайте `<p>текст</p>`, а не
  голый текст.
- **Экраны с сервисами.** `PlayerMobileScreen` поднимает Hilt-сервис воспроизведения и без
  `@HiltAndroidApp` падает. Для плеера тестируем отдельные `view`-компоненты.
