# Инсеты и шторки на мобильном интерфейсе

Кто задаёт отступы под системные панели и как считается высота bottom sheet. Только то, что есть в
коде (`core/designsystem/baseScreen/`, `core/designsystem/mobile/bar/`). Общий код двух интерфейсов —
[mobile-tv-ui-sharing.md](mobile-tv-ui-sharing.md).

## BaseScreen

Параметр `contentWindowInsets` по умолчанию:

```kotlin
WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
```

То есть `BaseScreen` учитывает верх и горизонтальные инсеты, но не нижний. Нижний отступ задают:

- для корневых экранов вкладок: `MobileBottomBarDefaults.contentBottomPadding` (экраны
  `HomeMobileScreen`, `TopMobileGrid`, `ScheduleMobileScreen`, `CollectionMobileScreen`,
  `LibraryMobileContinueWatchingGrid` передают его как `contentPadding`);
- для остальных экранов: сам экран (в `feature/` 42 места используют `navigationBarsPadding()`).

### `MobileBottomBarDefaults`

| Член                        | Значение                                                   |
|-----------------------------|------------------------------------------------------------|
| `BarHeight`                 | `64.dp`: высота бара без системного инсета                 |
| `ExtraContentBottomPadding` | `16.dp`                                                    |
| `contentBottomPadding`      | Зависит от `LocalMobileNavigationLayout`                   |

`contentBottomPadding` (KDoc: нижний бар сам занимает место вместе с инсетом системной навигации,
поэтому над ним нужен только зазор; при рейке или скрытой навигации контент доходит до края окна и
сам уходит от системного инсета):

| `MobileNavigationLayout` | Что означает                                         | `contentBottomPadding`     |
|--------------------------|------------------------------------------------------|----------------------------|
| `BottomBar`              | Нижний бар на компактной ширине                      | `16.dp`                    |
| `Rail`                   | Боковая рейка (планшет, развёрнутый складной)        | `navigationBars` + `16.dp` |
| `Hidden`                 | Вне корня таба, в плеере, на обязательном обновлении | `navigationBars` + `16.dp` |

`LocalMobileNavigationLayout` предоставляет `MobileMainScaffold`.

## Шторки

`BaseBottomSheet` (обёртка над `ModalBottomSheet`) и `BaseBottomSheetCustom`.

KDoc `BaseBottomSheet`:

- `contentWindowInsets` у материала отключены (`{ WindowInsets(0.dp) }`). По умолчанию Material сам
  вешает внутрь шторки `windowInsetsPadding(safeDrawing.only(Top + Bottom))`, и этот паддинг лежит
  снаружи нашего `heightIn(max)`: появившийся статус-бар прибавлялся бы к лимиту, и шторка уезжала
  бы вверх (заметнее всего в ландшафте). Нижний инсет применяется вручную.
- Контент получает `navigationBarsPadding()` и `padding(contentPadding)`.
- Дефолтный `contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp)`. Нижние
  `18.dp` нужны на случай нулевого навигационного инсета (скрытые бары в плеере, ландшафт с боковой
  3-кнопочной панелью): иначе последний элемент прилипает к краю экрана. Переопределяя
  `contentPadding`, нижний отступ нужно сохранить.
- `scrollableContent = true` включается, если в `content` нет собственного скролла; с `LazyColumn`
  внутри включать нельзя: краш «infinity maximum height constraints».
- `rememberBottomOverscrollGuard()` гасит остаточный scroll/fling, когда список короче максимальной
  высоты и упирается в нижний край; иначе остаток жеста уходит в сам `ModalBottomSheet`, и тот
  дёргается вверх.

KDoc `BaseBottomSheetCustom`: для контента, который сам управляет корневым layout'ом (например,
`LazyColumn` с собственными insets). `content` получает `maxHeight` и сам ограничивает себя
`Modifier.heightIn(max = maxHeight)`. Так как `contentWindowInsets` отключены, нижний инсет `content`
обязан применить сам: `Modifier.windowInsetsPadding(WindowInsets.navigationBars)`.

### Высота

`rememberBottomSheetMaxHeight()`:

```
(screenHeightDp − statusBars.top − DRAG_HANDLE_HEIGHT (48.dp)) × MAX_HEIGHT_FRACTION (0.85)
```

KDoc функции:

- доля берётся от высоты под статус-баром, а не от полной `screenHeightDp`: на широких и невысоких
  окнах (например, разложенный foldable) 85 % полного экрана может превысить высоту под
  статус-баром, и шторка заезжала бы под него;
- drag handle вычитается, чтобы доля описывала шторку целиком, а не только контент: в ландшафте,
  где вся высота окна около 450dp, ручка занимала заметную часть свободной полосы;
- результат ограничен снизу нулём (`coerceAtLeast(0.dp)`).

Доля `0.85` одна для всех ориентаций.
