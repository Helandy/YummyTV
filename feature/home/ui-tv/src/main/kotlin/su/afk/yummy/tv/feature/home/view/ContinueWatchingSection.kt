package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.home.R

@Composable
internal fun ContinueWatchingSection(
    items: List<HomeContinueWatchingItem>,
    onItemSelected: (HomeContinueWatchingItem) -> Unit,
    rowFocusRequester: FocusRequester? = null,
    registerFocusHandler: ((suspend () -> Boolean)?) -> Unit = {},
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val rowHasFocus = remember { mutableStateOf(false) }
    val isRestoring = remember { mutableStateOf(false) }
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    var focusMoveJob by remember { mutableStateOf<Job?>(null) }
    var lastFocusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var currentFocusedIndex by remember { mutableIntStateOf(-1) }
    val rowFocusRequesterToUse = rowFocusRequester ?: remember { FocusRequester() }
    val focusRequesters = remember(items.size) {
        List(items.size) { FocusRequester() }
    }

    fun HomeContinueWatchingItem.focusKey(): String = "$animeId:$videoId:$episode:$episodeUrl"

    // Вход в ряд всегда ведёт на первую карточку: список отсортирован по свежести,
    // и после просмотра самая актуальная запись оказывается первой.
    fun focusRequesterForItem(index: Int): FocusRequester =
        if (index == 0) rowFocusRequesterToUse else focusRequesters[index]

    suspend fun requestItemFocus(index: Int) {
        val target = index.coerceIn(0, items.lastIndex)
        val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == target }
        if (!isVisible) {
            listState.scrollToItem(target)
            withFrameNanos { }
        }
        runCatching { focusRequesterForItem(target).requestFocus() }
        withFrameNanos { }
        runCatching { focusRequesterForItem(target).requestFocus() }
    }

    fun cancelPendingFocusMove() {
        focusMoveJob?.cancel()
        focusMoveJob = null
        isRestoring.value = false
    }

    fun rememberFocusedItem(index: Int) {
        currentFocusedIndex = index
        lastFocusedIndex = index
    }

    // Обработчик для requestRowFocus дашборда: rowFocusRequester прикреплён к первой карточке,
    // которая может быть не скомпонована, если ряд проскроллен вправо — сначала подкручиваем
    // ряд к началу, иначе requestFocus() гарантированно фейлится.
    val restoreItemFocus by rememberUpdatedState<suspend () -> Boolean>(handler@{
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == 0 }) {
            listState.scrollToItem(0)
            withFrameNanos { }
        }
        runCatching { focusRequesterForItem(0).requestFocus() }.getOrDefault(false)
    })
    DisposableEffect(Unit) {
        registerFocusHandler { restoreItemFocus() }
        onDispose { registerFocusHandler(null) }
    }

    // Пока ряд не в фокусе, держим первую карточку скомпонованной — к ней прикреплён
    // rowFocusRequester, через который в ряд входят снаружи и в обход обработчика.
    LaunchedEffect(items) {
        if (rowHasFocus.value) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == 0 }) {
            listState.scrollToItem(0)
        }
    }

    Column {
        HomeSectionHeader(
            title = stringResource(R.string.continue_watching),
            active = rowHasFocus.value,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
            contentPadding = PaddingValues(horizontal = TvScreenPadding.Horizontal, vertical = 8.dp),
            modifier = Modifier
                .focusProperties {
                    upFocusRequester?.let { up = it }
                    downFocusRequester?.let { down = it }
                }
                .onFocusChanged { state ->
                    val hadFocus = rowHasFocus.value
                    rowHasFocus.value = state.hasFocus
                    if (!state.hasFocus) {
                        currentFocusedIndex = -1
                        cancelPendingFocusMove()
                    }
                    if (state.hasFocus && !hadFocus) {
                        isRestoring.value = true
                        focusMoveJob?.cancel()
                        focusMoveJob = scope.launch {
                            requestItemFocus(0)
                            rememberFocusedItem(0)
                            isRestoring.value = false
                        }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            val focusedIndex = currentFocusedIndex
                                .takeIf { it in items.indices }
                                ?: lastFocusedIndex
                            if (focusedIndex <= 0) {
                                cancelPendingFocusMove()
                                mainMenuFocusRequester?.requestFocus()
                                mainMenuFocusRequester != null
                            } else {
                                false
                            }
                        }

                        Key.DirectionRight -> false

                        Key.DirectionUp -> {
                            cancelPendingFocusMove()
                            onMoveUp?.invoke()
                            onMoveUp != null
                        }

                        Key.DirectionDown -> {
                            cancelPendingFocusMove()
                            onMoveDown?.invoke()
                            onMoveDown != null
                        }

                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> false

                        else -> false
                    }
                }
                .tvFocusRestorer(
                    fallback = focusRequesterForItem(0),
                ),
        ) {
            itemsIndexed(items = items, key = { _, e -> e.focusKey() }) { index, entry ->
                ContinueWatchingCard(
                    entry = entry,
                    onFocused = {
                        currentFocusedIndex = index
                        if (rowHasFocus.value && !isRestoring.value) rememberFocusedItem(index)
                    },
                    onClick = {
                        rememberFocusedItem(index)
                        onItemSelected(entry)
                    },
                    modifier = Modifier.focusRequester(focusRequesterForItem(index)),
                    leftFocusRequester = mainMenuFocusRequester.takeIf { index == 0 },
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = downFocusRequester,
                )
            }
        }
    }
}
