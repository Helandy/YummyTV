# Навигация

Навигация построена на Jetpack Navigation3 (`NavDisplay`, `NavKey`) и собственном
`NavigationManager`. Один менеджер и общие стеки обслуживают оба интерфейса, телефонный и ТВ. Код
ядра — `core/navigation/`. Расположение модулей и слоёв — в [architecture.md](architecture.md).

## Основные понятия

| Понятие              | Что это                                                                                                     |
|----------------------|-------------------------------------------------------------------------------------------------------------|
| `NavKey`             | `@Serializable` destination экрана, лежит в `api`-модуле фичи                                               |
| `RootTab`            | Корневой таб: `ACCOUNT`, `SEARCH`, `HOME`, `POSTS`, `COLLECTIONS`, `SCHEDULE`, `TOP`, `LIBRARY`, `SETTINGS` |
| `INavigationManager` | Узкий интерфейс навигации для фич и `core:*`                                                                |
| `NavigationManager`  | Реализация (`internal`), синглтон на процесс                                                                |
| `NavRegistrar`       | Точка регистрации экранов фичи в общем `entryProvider`                                                      |
| `AppNavHost`         | `NavDisplay` + стеки + провайдер экранов                                                                    |

## Два уровня back stack

```
RootTab → свой стек      (ACCOUNT, SEARCH, HOME, … — по одному на таб)
appBackStack             (оверлей поверх табов: экраны вне таб-бара)
```

- Пока `appBackStack` не пуст, `backStack` и все операции от него (`back`, `backTwo`, `popBackTo`,
  `navigate`) работают с ним, а не со стеком текущего таба.
- Стеки табов сохраняются при переключении: вернувшись на таб, пользователь видит его историю.
- Корень каждого таба задаёт `RootTabsModule` (`feature/main/presentation/.../di/`): например,
  `RootTab.HOME → HomeDestination`, `RootTab.SEARCH → SearchDestination()`. Набор табов общий,
  определяет его main-фича.
- Стек — один на оба интерфейса: в него может попасть ключ, зарегистрированный только в наборе
  регистраторов другой платформы. Для этого в `AppNavHost` есть fallback: неизвестный ключ тут же
  делает `navManager.back()`, это штатная ситуация, а не ошибка.

### Операции

| Метод                                 | Поведение                                                                            |
|---------------------------------------|--------------------------------------------------------------------------------------|
| `navigate(dest)`                      | В текущий стек. Дубль сверху игнорируется                                            |
| `navigateApp(dest)`                   | В `appBackStack`, поверх табов                                                       |
| `navigateDetail(dest)`                | Для списка и детали в двухпанельной раскладке: деталь того же типа сверху заменяется |
| `replace(dest)`                       | Заменить верхний экран                                                               |
| `back()`                              | Закрыть оверлей → экран таба → если корень и таб не HOME, перейти на HOME            |
| `backTwo()`, `popBackTo(...)`         | Снять два экрана / вернуться к экрану, `inclusive` включает его самого               |
| `popToRoot()`                         | Очистить `appBackStack` и стек таба до его корня                                     |
| `switchRoot(root, reselectPopToRoot)` | Сменить таб. Повторный выбор того же таба сбрасывает его до корня                    |
| `replaceRoot(root, dest)`             | Очистить оверлей, заменить стек таба одним `dest`, выбрать таб                       |
| `resetAllRoots()`                     | Все табы в корни, выбран HOME                                                        |

`navigateDetail` сравнивает только класс ключа. Вызывать нужно из экранов списков. Переход внутри
детали к детали того же типа («похожие посты» из поста) идёт через `navigate`, иначе он заменит
текущую деталь, а не встанет поверх. Причина: в двухпанельной раскладке список виден рядом, и без
замены «назад» листал бы историю деталей `[Posts, PostA, PostB]`.

### Сохранение при пересоздании

`AppNavHost` держит `rememberNavBackStack` для `appBackStack` и для каждого `RootTab`
(`rememberSaveable`), а выбранный таб сохраняется в `savedCurrentRoot`. `attachBackStacks`
привязывает сохраняемые стеки к менеджеру и переносит накопленное, если навигация началась раньше
привязки (например, `navigate` из `MainViewModel.init`).

## Как добавить экран

1. Destination в `api` фичи (`@Serializable`, `NavKey`):

```kotlin
@Serializable
data class DetailsDestination(val animeId: Int) : NavKey
```

2. Если экран открывают из других фич, в том же `api` объявляется навигатор
   (`IDetailsNavigator.getDetailsDest(animeId)`), реализация живёт в `presentation`.
3. Точка входа платформы в `api`: `ITvXxxEntry` / `IMobileXxxEntry`, оба наследуют `NavRegistrar`.
4. Регистратор в `ui-tv` / `ui-mobile` (сокращённо, `TopNavRegistrar` из `feature/top/ui-tv`):

```kotlin
class TopNavRegistrar @Inject constructor() : ITvTopEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<TopDestination> {
                val viewModel = hiltViewModel<TopViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    TopTvScreen(state, effect, onEvent)
                }
            }
        }
}
```

5. Hilt: `@Binds fun bind(impl: TopNavRegistrar): ITvTopEntry` в модуле
   `@InstallIn(SingletonComponent)`.
6. Регистратор добавляется в `TvNavigationHolder` / `MobileNavigationHolder`
   (`feature/main/ui-tv`, `feature/main/ui-mobile`).

`NavigationHolder` перечисляет точки входа как параметры конструктора, поэтому забытый биндинг —
ошибка Dagger при сборке, а не отсутствующий экран в рантайме.

ViewModel с аргументами создаётся через assisted-фабрику с ключом экрана:
`hiltViewModel<PostDetailsViewModel, PostDetailsViewModel.Factory>(key = "post-${destination.postId}") { it.create(destination.postId) }`
(`PostsNavRegistrar`).

## Связь между фичами

Фича не ссылается на экраны другой фичи. Связь идёт через `api`:

- `NavKey` destination + навигатор-контракт (`IDetailsNavigator`);
- `INavigationManager` для самих переходов.

`core:*` не может зависеть от `feature:*` (проверяет конвенционный плагин). Если core нужен
контракт, порт объявляется в core и реализуется в фиче: так устроены `NavRegistrar` и
`DeepLinkResolver`.

## Сцены

`AppNavHost` принимает `extraSceneStrategies`, а `BottomOverlaySceneStrategy` включается всегда.

| Механизм                                       | Для чего                                                                                                      |
|------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `bottomOverlay()` в `metadata` entry           | Диалог или bottom sheet: предыдущий экран остаётся в композиции под scrim'ом                                  |
| `ListDetailSceneStrategy` (Material3 adaptive) | Список + деталь на широком окне (посты, чат, рецензии)                                                        |
| `listPaneAnchor()`                             | Метка списка. `RequireListPaneSceneStrategy` пропускает двухпанельную сцену, только если список реально в ней |
| `FullscreenDestination` (маркер на ключе)      | Экран на всё окно (плеер, просмотр картинок): мобильный граф прячет рейку                                     |
| `CompactListPaneDestination` (маркер)          | Деталь, рядом с которой список сворачивается в узкую колонку (чат)                                            |

KDoc:

- `bottomOverlay()`: экран, который сам по себе не полноэкранный контент, а диалог или bottom sheet,
  которому для scrim'а нужен видимый фон позади; без пометки Navigation3 в single-pane режиме
  композирует только последний entry, и шторка оказалась бы поверх пустоты. Используется в
  `DetailsNavRegistrar` (мобильный).
- `RequireListPaneSceneStrategy`: Material list-detail стратегия строит сцену и из одной детали
  (например, рецензии, открытой со страницы аниме) и рисует слева пустую панель; обёртка пропускает
  двухпанельную сцену, только если в ней есть экран с `listPaneAnchor()`.

### `contentKey` плеера

`entry<PlayerDestination>(clazzContentKey = { PLAYER_CONTENT_KEY })`. KDoc константы: ключ плеера
подменяется на лету при смене серии, и без стабильного `contentKey` Navigation3 считал бы это новой
записью: пересоздались бы `NavEntry`, `ViewModelStore` и ViewModel, а воспроизведение началось бы
заново.

## Диплинки

```
Activity.onCreate / onNewIntent
  → searchIntentHandler.handle(intent)       системный поиск
  → DeepLinkHandler.handle(intent)
      resolvers.firstNotNullOfOrNull { it.resolve(link) } → navManager.navigate(key)
```

- Схема своя, `yummytv://<host>/…`. `DeepLinkReference.appLinkHost` возвращает хост только для этой
  схемы.
- Резолвер — `DeepLinkResolver`, регистрируется как `@Binds @IntoSet`. Он возвращает `NavKey` или
  `null`. Порядок обхода набора не определён, поэтому резолверы не должны пересекаться.
- Сейчас хосты: `home` (`RootTabDeepLinkResolver`), `details/{animeId}` (`DetailsDeepLinkResolver`),
  `downloads` (`VideoDownloadDeepLinkResolver`), локальное видео (`LocalVideoDeepLinkResolver`).
- `intent-filter` объявлены в `app/src/main/AndroidManifest.xml` на `InterfaceRouterActivity`
  (он пробрасывает исходный Intent в `MobileActivity` / `TvActivity`): `yummytv://details`,
  `yummytv://home`, `yummytv://downloads` и MIME `video/*`.
- `LocalVideoDeepLinkResolver` ловит `ACTION_VIEW` с `video/*` или схемами `content://` / `file://`
  и открывает плеер с локальным файлом. Имя в заголовок берётся из `DISPLAY_NAME` (для `content://`)
  или последнего сегмента пути, ошибка запроса даёт пустое имя и не ломает воспроизведение.
- `core:deeplink` не знает, какие схемы существуют: фичи регистрируют свои резолверы сами.
