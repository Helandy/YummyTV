# Unit-тесты: как оформлять

## База

- Каждый unit-тест наследуется от `BaseUnitTest` (`:core:testing`, подключается как
  `testImplementation(project(":core:testing"))`). База сама:
    - подменяет `Dispatchers.Main` на `UnconfinedTestDispatcher` (`MainDispatcherRule`);
    - вызывает `unmockkAll()` после каждого теста.
      `@get:Rule val mainDispatcher` и свой `@After { unmockkAll() }` в тестах не нужны.
- JUnit4, mockk. `@MockK` и `MockKAnnotations` не используем.
- Тесты пишем только по явной просьбе (см. `AGENTS.md`).

## Моки

- Relaxed-мок — инициализатором поля: `private val nav: INavigationManager = mockk(relaxed = true)`.
- Мок с поведением — поле `= mockk()`, а `every { … }` / `coEvery { … }` — в `@Before fun setUp()`.
- Изменяемое состояние (`MutableStateFlow`, списки-накопители) — тоже поле; JUnit создаёт новый
  экземпляр класса на каждый тест, поэтому моки и состояние между тестами не делятся.
- Заглушка строк: `every { strings.get(any<Int>()) } answers { "res${firstArg<Int>()}" }`.
- `NavKey` не определяем в каждом тесте: `private val navKey: NavKey = mockk()`.

## Создание тестируемого объекта

- Одна функция `createViewModel(...)` (или `createHandler()`/`createBehavior()`), параметры экрана —
  её аргументы с дефолтами. Тест вызывает её сам, когда моки уже настроены.
- Use case оборачиваем вокруг мока репозитория: `GetXxxUseCase(repository)`. Если зависимость
  сложная (handler, use case с составным результатом), мокаем её саму.

## Корутины и эффекты

- `viewModelScope` работает на тестовом `Main`, поэтому большинство тестов — обычные синхронные
  `@Test`.
- `delay`/debounce двигаем через `testScheduler.advanceTimeBy(...)` + `runCurrent()` из базы.
- Одноразовые `Effect` собираем в `runTest { val effects = collectEmissions(vm.effect) … }`
  (`core:testing`, `FlowTestUtils.kt`).
- Код на реальном `Dispatchers.Default` (`withContext`) проверяем `verify(timeout = 2_000) { … }`.

## Что покрывать в ViewModel

Стартовое состояние; загрузку (успех / ошибка / retry); основные события и их переходы состояния;
оптимистичные мутации с откатом; гостя там, где нужна авторизация; навигацию (`nav.navigate`/
`back` + нужный `*Navigator`); одноразовые эффекты.

## Имена и структура

- Имя теста — предложение в обратных кавычках на английском:
  `` `failed load shows the message and retry recovers` ``.
- Общие тестовые данные модуля — `internal`-билдеры в `XxxTestData.kt` рядом с тестами (например
  `animeDetails()`, `chatMessage()`), не копируем их между файлами.
- Константы (`ANIME_ID`, `USER_ID`) — в `private companion object` в конце класса.
- Тест лежит в том же пакете, что и тестируемый класс; `internal`-типы модуля доступны напрямую.

## Ограничения JVM-тестов

- `android.util.Patterns` и подобные Android-API в JVM недоступны — такие ветки не покрываем, а
  причину указываем в KDoc класса теста.
- Чистая логика без моков тоже наследуется от `BaseUnitTest`: накладные расходы нулевые, зато
  правило единое.
