# Фокус на ТВ

Как устроено управление фокусом и скроллом в ТВ-интерфейсе (DPAD). Общее лежит в
`core/designsystem/.../focus/` и `feature/main/ui-tv/.../view/` (`TvMainScaffold`,
`TvMainFocusController`, `TvMainContentPane`, `TvSideMenu`). Описано только то, что есть в коде
(в том числе в KDoc и комментариях).

## Что подключать новому экрану

| Задача                                               | Что использовать                                                                                           |
|------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| Вертикальный грид карточек                           | `rememberTvTopAnchoredGridBringIntoViewSpec` + `tvWholeItemBringIntoView` + `tvLazyGridRowFocusNavigation` |
| Горизонтальная карусель                              | `TvCenteredCarouselBringIntoViewSpec`                                                                      |
| Вернуть фокус на ту же карточку после «Назад»        | `rememberTvLazyFocusRestoreState` + `launchTvLazyGridKeyFocusRestore` / `launchTvLazyListKeyFocusRestore`  |
| Экран с лоадером и «Повторить»                       | `rememberTvStateFocusHandoff`                                                                              |
| Сфокусировать то, что ещё может не быть в композиции | `requestFocusUntilTimeout`                                                                                 |
| Карточка с рамкой и увеличением при фокусе           | `tvFocusableClick` (скейл `1.04f`, рамка `3.dp`)                                                           |
| Группа с запоминанием последнего элемента            | `tvFocusRestorer` (`focusRestorer` + `focusGroup`)                                                         |

Контекст из `TvMainScaffold` приходит через `CompositionLocal`: `LocalMainMenuFocusRequester`
(боковое меню) и `LocalPreferredContentFocusRequester` (регистрация «куда садить фокус в
контенте»).

## Скролл грида под фокус

`TvPivotBringIntoViewSpec` (`TvFocusedGridBringIntoViewSpec.kt`) — `BringIntoViewSpec`, чистая
функция от `(offset, size, containerSize)` без общего изменяемого состояния (KDoc: общий mutable
object однажды уже ломал скролл по всему приложению).

| Спек                                         | Параметры                                              | Для чего                                         |
|----------------------------------------------|--------------------------------------------------------|--------------------------------------------------|
| `rememberTvTopAnchoredGridBringIntoViewSpec(rowSpacing)` | `skipIfFullyVisible = false`, `pivotOffsetPx = rowSpacing` | Карточные вертикальные гриды              |
| `TvCenteredCarouselBringIntoViewSpec`        | `skipIfFullyVisible = false`, `centered = true`        | Горизонтальные ряды (`SimilarTab`, `ViewingOrderRow`, `ReviewDetailsTvScreen`) |
| `TvCenteredGridBringIntoViewSpec`            | `skipIfFullyVisible = true`, `centered = true`         | Грид, где промах центрирует ряд                  |
| `TvFocusedGridBringIntoViewSpec`             | `skipIfFullyVisible = true`, пивот 12 %                | Прежнее поведение                                |

KDoc `rememberTvTopAnchoredGridBringIntoViewSpec`:

- сфокусированный ряд всегда встаёт к верхней кромке, контент прокручивается под ним (как у
  Netflix); пивот — ровно `rowSpacing` от кромки: низ предыдущего ряда оказывается на самой кромке
  и не виден, а место под скейл фокуса (`1.04` ≈ 7dp) остаётся; `rowSpacing` должен совпадать с
  `verticalArrangement` грида;
- `skipIfFullyVisible = false`: со skip спек пересчитывается каждый кадр анимации
  (`ContentInViewNode.afterFrame`) и паркует ряд впритык к нижней кромке, фокус «ездит» по экрану
  вместо контента; лишнего подскролла при переходе вбок нет, ряд уже на пивоте и дистанция 0;
- допуска на скейл фокуса нет: карточки грида обязаны быть обёрнуты в `tvWholeItemBringIntoView`
  (с допуском ряд останавливался раньше пивота и над ним торчал низ предыдущего ряда);
- предыдущий ряд при таком пивоте не скомпонован: DPAD вверх/вниз страхует
  `tvLazyGridRowFocusNavigation`, шапку над первым рядом — `tvWholeItemBringIntoView`.

### `tvWholeItemBringIntoView(gridStart)`

KDoc: любой запрос `bringIntoView` изнутри ячейки (фокус на карточке или на кнопках под ней)
подменяется прямоугольником всей ячейки в обычном, неувеличенном размере. Вешается на внешний
модификатор карточки, снаружи скейла.

- Скейл фокуса (`tvFocusableClick`, `1.04`) — `graphicsLayer`, а `ContentInViewNode` видит уже
  увеличенные границы: верх выше реального на ~7dp и меняется по ходу анимации пружины. Размер
  ячейки берётся из layout, скейл на него не влияет.
- Кнопки под карточкой («Детали», «Удалить»): без подмены на пивот встаёт сама кнопка, а карточка
  уезжает за кромку; параллельный запрос на ячейку не помогает, потому что `ContentInViewNode`
  снимает запрос кнопки, только когда она сама дошла до пивота.
- `gridStart: TvGridStartExtent?` нужен только карточкам первого ряда: прямоугольник расширяется
  вверх до начала контента, и вместе с рядом видна шапка грида и её фокусируемые элементы.
  `rememberTvGridStartExtent(contentTopPadding, rowSpacing, hasHeader)`; шапка вешает на корень
  своего item'а `.measure`.

## DPAD вверх/вниз: `tvLazyGridRowFocusNavigation`

KDoc: штатный focus search Compose, упираясь в ещё не скомпонованный ряд, подтягивает beyond-bounds
ровно один элемент (`addNextInterval` двигает границу на один индекс) и отдаёт фокус первому же
найденному: с 3-й карточки фокус прыгает на 1-ю (вверх — на последнюю). При верхнем пивоте ряд над
сфокусированным всегда за верхней кромкой, поэтому страховка срабатывает на каждом шаге вверх; вниз
— когда ряд ещё не доехал до пивота, например при удержании DPAD.

Поведение (`onPreviewKeyEvent`, только `KeyDown` для `DirectionUp` / `DirectionDown`):

1. целевой индекс = `index ± columnCount`; вне `0 until itemCount` — событие не трогается;
2. если целевая карточка уже скомпонована — работает обычный поиск фокуса;
3. иначе: `scrollToItem(0)` для первого ряда, для остальных `scrollToItem(targetLazyIndex,
   scrollOffset = beforeContentPadding − mainAxisItemSpacing)`, ожидание компоновки и
   `requestFocusUntilTimeout(requester цели)`.

Параметры: `index` (в данных), `columnCount`, `itemCount`, `gridState`, `scope`,
`focusRequesterAt(index)`, `lazyIndexOffset` (шапка, спаны). Для кнопок под карточкой модификатор
вешается на нижние кнопки ячейки.

Используют: `TopBrowser`, `CollectionsCatalogGrid`, `CollectionGridPane`, `RelationTvScreen`,
`ScreenshotsTvScreen`, `TrailersTvScreen`, `EpisodesGrid`, `CollectionsGrid`, `LibraryGrid`,
`ContinueWatchingGrid`, `SearchResultsGrid`, `MySubscriptionsTvScreen`, `ReviewsListTvScreen`.

## Возврат фокуса после «Назад»

### Состояние

`TvLazyFocusRestoreState<Key>` (`rememberTvLazyFocusRestoreState(vararg inputs)`): ключ и индекс
последней сфокусированной карточки в `rememberSaveable`.

| Метод                    | Что делает                                                                 |
|--------------------------|----------------------------------------------------------------------------|
| `onItemFocused(key, index)` | Запомнить (вызывается из `onFocusChanged` карточки)                     |
| `targetIndex(keys)`      | Индекс ключа в `keys`, иначе сохранённый индекс в пределах списка, иначе `null` для пустого списка |
| `clear()`                | Сбросить ключ и индекс                                                     |

### Восстановление

`launchTvLazyGridKeyFocusRestore(...)` / `launchTvLazyListKeyFocusRestore(...)` отменяют предыдущую
задачу и в `scope.launch`:

1. берут целевой индекс (`restoreState.targetIndex(keys)` или `fallbackIndex`);
2. если карточка есть в `layoutInfo.visibleItemsInfo`, ждут два кадра и просят фокус;
3. иначе `scrollToItem(index + lazyIndexOffset)`, ждут видимости (`snapshotFlow`), затем
   `requestFocusUntilTimeout`; общий таймаут `500 мс`;
4. не получилось — фокус на `fallbackFocusRequester`; в конце вызывается `onRestoreFinished`.

`requestFocusUntilTimeout(requester, timeout = 500 мс)` на каждом кадре зовёт
`runCatching { requester.requestFocus() }.getOrDefault(false)` до первого `true` или таймаута.

### Скаффолд

`TvMainContentPane` вешает на контент `focusRequester(contentFocusRequester)`,
`focusProperties { left = selectedRootFocusRequester }` и
`tvFocusRestorer(fallback = currentPreferredContentFocusRequester ?: FocusRequester.Default)`.

`TvMainFocusEffects` (в `TvMainFocusController.kt`) обрабатывает `pendingContentFocusRequest`: если
preferred requester ещё не зарегистрирован, ждёт `400 мс`
(`PENDING_CONTENT_FOCUS_FALLBACK_DELAY_MILLIS`). Комментарий в коде: во время nav-перехода старый
экран ещё в композиции, и фокус через `contentFocusRequester`/`focusRestorer` «успешно» сел бы на
уходящий экран и потерялся бы при его dispose (падая в боковое меню); поэтому ждут регистрацию
preferred requester'а (эффект перезапускается по смене ключа) либо отдают fallback после перехода.

Регистрация идёт через `LocalPreferredContentFocusRequester.current?.invoke(requester)`.

### Пример: `SimilarTab`

Комментарий в коде: пока карточка ни разу не фокусировалась, дефолтный фокус на кнопке видимости
рекомендации вверху вкладки; как только фокус побывал на карточке, она регистрируется как
preferred-фокус экрана, иначе после Back из деталей другого тайтла фокус улетает на первый
фокусируемый элемент вкладки (кнопку), потому что сам `LazyRow` не является точкой входа фокуса и
его `focusRestorer` явно не запрашивается. Реализация: `hasFocusedItem` в `rememberSaveable`,
`DisposableEffect` регистрирует `focusRequesters[restoreIndex()]` или `null`.

### Пример: `TopBrowser`

Держит `lastFocusedIndex`, `isRestoringFocus`, `restoreFocusJob`, `focusRestoreState`;
`itemFocusRequesters = itemIds.zip(focusRequesters).toMap()` (привязка requester'а к `id`, а не к
индексу, потому что при пагинации индексы плывут); при смене типа топа `focusRestoreState.clear()`
и `scrollToItem(0)`.

## Фокус при загрузке: `TvStateFocusHandoff`

KDoc: пока экран грузится, в нём нет фокусируемых элементов, и фокус падает в боковое меню (оно
раскрывается), а стартовая попытка скаффолда посадить фокус в контент сгорает по таймауту.
Плейсхолдер (лоадер или «Повторить») держит фокус и регистрируется как preferred-фокус контента;
когда он уходит из композиции, фокус забирает контент.

- `rememberTvStateFocusHandoff(placeholder: TvStatePlaceholder?, contentFocusRequester)`;
  `TvStatePlaceholder`: `Loading`, `Error`.
- `Modifier.tvFocusablePlaceholder(handoff)` на лоадер, `Modifier.tvStateFocusTracking(handoff,
  kind)` на кнопку «Повторить».
- `shouldFocusContent` истинно, если фокус был на плейсхолдере, а плейсхолдер сменился контентом;
  `onContentFocused()` снимает флаг.
- Потеря фокуса, пока этот плейсхолдер ещё на экране, считается уходом пользователя (например, в
  меню), и фокус у него не отбирается; потеря из-за смены плейсхолдера флаг не сбрасывает.

Подключено в `HomeTvScreen`, `ScheduleTvScreen`, `PostsTvScreen`.

### `onEnter` в `TvMainScaffold`

Комментарий в коде: удалили узел с фокусом (лоадер сменился ошибкой или контентом) — Android
возвращает фокус в Compose «на вход» (`FocusDirection.Enter`), и без обработчика он садится в первый
по порядку элемент — боковое меню, которое тут же раскрывается. На внешнем `Box` стоит
`focusProperties { onEnter = … }` + `focusGroup()`: при `Enter` (и `showMainMenu`) фокус
запрашивается у `focusController.contentFocusRequester`; если там нечего фокусировать, вход идёт как
обычно.

`TvSideMenu`: `LaunchedEffect(selectedRoot, expanded)` при раскрытом меню запрашивает фокус у строки
выбранного корня через `requestFocusUntilTimeout`.

`BackHandler` в `TvMainScaffold` включён и при `pendingContentFocusRequest` (комментарий:
«pendingContentFocus покрывает окно nav-перехода: пока фокус контента не установлен, BACK
возвращает в меню вместо сворачивания приложения»).

## Края карточных рядов

Крайним карточкам горизонтальных рядов задают `left = FocusRequester.Cancel` /
`right = FocusRequester.Cancel` (`HeroBannerPage`, `HomeCarousel`, `PostDetailsTvScreen`), чтобы
фокус не уходил за край.

## Типовые каркасы ТВ

В `feature/*/ui-tv` нет ни одного вызова `BaseScreen` (в `ui-mobile` — 45 файлов). Для состояний
используются `core:designsystem/tv` (`TvStateContent`, `TvStateMessage`, `TvAppendErrorFooter`) и
фокус-компоненты из `focus/`.
