# MVI в presentation

Как устроен экран: `State` / `Event` / `Effect`, ViewModel, handler'ы и связь с Compose. Правила
расположения файлов (`.handler`, `.model`, `.mapper`, `.utils`, `.view`) лежат в `AGENTS.md` и здесь
не повторяются. База — модуль `core:mvi` (`BaseViewModel`, `CoroutineViewModel`, `UiState`,
`UiEvent`, `UiEffect`). Описано только то, что есть в коде.

## Контракты

KDoc из `core:mvi`:

| Тип        | Назначение                                                                     |
|------------|--------------------------------------------------------------------------------|
| `UiState`  | Маркер состояния экрана — единственный источник данных для отрисовки           |
| `UiEvent`  | Маркер намерения пользователя, приходящего из UI в `BaseViewModel`             |
| `UiEffect` | Маркер одноразового эффекта (навигация, тост), который не хранится в состоянии |

Экран объявляется классом-контейнером (пример — `TopState`):

```kotlin
class TopState {
    data class State(
        val selectedType: AnimeTopType = AnimeTopType.TV,
        val items: Flow<PagingData<AnimeTopItem>> = flowOf(PagingData.empty()),
        val showTitleYear: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class TypeSelected(val type: AnimeTopType) : Event
        data class AnimeSelected(val animeId: Int) : Event
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
```

`TopState.Effect` пустой: тип нужен как параметр `BaseViewModel`.

## BaseViewModel

`BaseViewModel<S : UiState, E : UiEvent, F : UiEffect> : CoroutineViewModel()`:

| Член                              | Реализация                                                                                     |
|-----------------------------------|------------------------------------------------------------------------------------------------|
| `createInitialState()`            | `protected abstract`; вызывается лениво (`MutableStateFlow(createInitialState())` под `by lazy`) |
| `state: StateFlow<S>`             | `_state.asStateFlow()`                                                                         |
| `currentState`                    | `_state.value`                                                                                 |
| `setState { … }`                  | `_state.update { it.reducer() }`                                                               |
| `effect: SharedFlow<F>`           | `MutableSharedFlow<F>()` (без replay и без буфера)                                             |
| `setEffect(effect)`               | `viewModelScope.launch { _effect.emit(effect) }`                                               |
| `setEvent(event)` / `onEvent`     | `setEvent` вызывает `protected abstract onEvent`                                               |
| `errorHandler`, `retryStorage`    | `protected abstract val`, наследники объявляют `override val` в конструкторе                   |
| `onRetry()`                       | `protected open`, по умолчанию пустой                                                          |
| `Throwable.userMessage(fallback)` | Для сетевых ошибок — сообщение из `ErrorHandler` и исходный текст второй строкой; иначе `message ?: fallback ?: errorHandler.parse(...)` |

`CoroutineViewModel`: `viewModelScope = CoroutineScope(Main + SupervisorJob() + CoroutineExceptionHandler)`,
обработчик зовёт `onError`; `onCleared` отменяет job.

### Необработанные ошибки

`BaseViewModel.onError(exception)`:

1. `retryKey = "<ИмяКласса>:<System.nanoTime()>"`, `retryStorage.put(retryKey) { onRetry() }`;
2. `errorHandler.parse(t = exception, navigate = true, retryKey, owner = ИмяКласса)`: открывается
   экран ошибки, «Повторить» выполнит сохранённое действие.

Классификация ошибок и тексты — [network-and-auth.md](network-and-auth.md).

`runSuspendCatching` (`core/common/.../coroutines/CoroutineResultUtils.kt`) перехватывает
`Throwable` и вызывает `currentCoroutineContext().ensureActive()` до формирования `Result.failure`,
то есть отмену корутины не превращает в ошибку.

## ViewModel

Пример — `TopViewModel`:

```kotlin
@HiltViewModel
class TopViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeTop: GetAnimeTopUseCase,
    settingsStore: AppearanceSettingsStore,
    accountSettingsStore: YaniAccountSettingsStore,
    private val analytics: TopAnalytics,
) : BaseViewModel<TopState.State, TopState.Event, TopState.Effect>() {

    override fun createInitialState() = TopState.State(items = createPagingFlow(AnimeTopType.TV))

    init {
        analytics.eventScreenOpened()
        settingsStore.showTopTitleYear
            .onEach { showTitleYear -> setState { copy(showTitleYear = showTitleYear) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: TopState.Event) { /* when (event) { … } */ }
}
```

- Зависимости ViewModel — use case'ы `domain`, `core:*` и `api`-модули других фич; `presentation`
  не зависит от `data` (build-файлы модулей).
- Навигация — через `INavigationManager` и навигаторы из `api` ([navigation.md](navigation.md)):
  `nav.navigate(detailsNavigator.getDetailsDest(event.animeId))`.
- ViewModel с аргументом: `@HiltViewModel(assistedFactory = XxxViewModel.Factory::class)` и
  `@AssistedInject` (`PlayerViewModel` получает `PlayerDestination`).
- Аналитика экрана — отдельный `XxxAnalytics` ([analytics-and-logging.md](analytics-and-logging.md)).

### Handler'ы

`XxxHandler` лежит в `.handler` рядом с ViewModel (правило `AGENTS.md`). Встречаются два вида:

- обычный класс с `suspend`-методами, который вызывает ViewModel
  (`EpisodeWatchLaterHandler.toggle(...)` в `feature/details/presentation/.../episodes/handler/`);
- класс, меняющий состояние экрана через узкий хост: `PlayerStateHost` (`state`, `scope`,
  `update { }`) и `PlayerSourceHost`, см. [player-architecture.md](player-architecture.md).

Если handler должен пережить `viewModelScope`, он получает `@IoApplicationScope`-scope:
`PlayerNavigationDelegate.ioScope` для финального сохранения прогресса.

## Связь с Compose

`ScreenNavigator(viewModel, content)` (`core/designsystem/.../baseScreen/ScreenNavigator.kt`):

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
content(state, viewModel.effect, viewModel::setEvent)
```

Публичный вход экрана — `XxxScreen(state, effect, onEvent)` в `XxxMobileScreen.kt` /
`XxxTvScreen.kt` (правило `AGENTS.md`). Подписка на эффекты — в самом экране или графе, например в
`MobileMainGraph`:

```kotlin
LaunchedEffect(effect) {
    effect.collect { eff ->
        when (eff) {
            is MainState.Effect.ShowToast -> toast.show(eff.message)
        }
    }
}
```

Правила `AGENTS.md` для UI: логика не переносится в UI-компоненты; пользовательские строки лежат в
ресурсах и читаются через `stringResource`; UI-модели в `.model`, мапперы в `.mapper`, форматтеры в
`.utils`; дочерние композабли в `.view`.

## Одноразовые события

KDoc `GlobalToastState`: сообщение приходит одноразовым эффектом ViewModel и живёт только в UI,
поэтому после пересоздания экрана тост не всплывает повторно, как было бы из `State`.

- `GlobalToastState.show(message)`: новое сообщение заменяет текущее и перезапускает таймер
  (`GLOBAL_TOAST_DURATION = 3.seconds`); таймер живёт в `rememberCoroutineScope`, поэтому
  отменяется вместе с экраном (`rememberGlobalToastState`).
- В части экранов тост показывается системным `Toast.makeText` по эффекту
  (`LibraryState.Effect.ShowToast` → `Toast.makeText(context, event.message, LENGTH_SHORT)`).
- Другие эффекты в коде: `DiscardAutofill` (`AccountViewModel`, `RegistrationViewModel`,
  [password-manager-autofill.md](password-manager-autofill.md)), `HomeState.Effect.ShowToast`.

## Коллекции и Paging в состоянии

- Коллекции в `*State` — `kotlinx.collections.immutable`
  ([image-loading-and-memory.md](image-loading-and-memory.md)).
- Для экранов с пагинацией `State` хранит `Flow<PagingData<T>>`; `TopViewModel.createPagingFlow`
  собирает `Pager(...)` с `OffsetPagingSource(itemKey = { it.id })` и `.cachedIn(viewModelScope)`, а
  при смене типа топа и языка контента заменяет поток новым.

## Тесты

Правила — [unit.md](unit.md): `BaseUnitTest` подменяет `Dispatchers.Main`, `createViewModel(...)`
собирает ViewModel с моками, эффекты читаются через `collectEmissions(vm.effect)`. Тесты пишутся
только по явной просьбе (`AGENTS.md`).
