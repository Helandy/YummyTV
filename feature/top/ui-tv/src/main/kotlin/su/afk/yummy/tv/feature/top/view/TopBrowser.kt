package su.afk.yummy.tv.feature.top.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TopBrowser(
    items: List<AnimeTopItem>,
    selectedType: AnimeTopType,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    error: String?,
    focusedItemId: Int?,
    restoreFocusedItemOnEnter: Boolean,
    isActiveDestination: Boolean,
    onItemSelected: (AnimeTopItem) -> Unit,
    onTypeSelected: (AnimeTopType) -> Unit,
    onItemFocused: (Int) -> Unit,
    onFocusedItemRestoreHandled: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val gridFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    val scope = rememberCoroutineScope()
    val typeFocusRequesters = remember { List(AnimeTopType.entries.size) { FocusRequester() } }
    var leftEdgeIndexes by remember { mutableStateOf(emptySet<Int>()) }

    var lastFocusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    var wasActiveDestination by remember { mutableStateOf(isActiveDestination) }
    var suppressNextFocusedRowScroll by remember { mutableStateOf(false) }
    val focusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }
    val focusedItemIndex = focusedItemId
        ?.let { id -> items.indexOfFirst { item -> item.id == id } }
        ?: -1
    val selectedTypeFocusRequester =
        typeFocusRequesters.getOrNull(AnimeTopType.entries.indexOf(selectedType).coerceAtLeast(0))
    val hasFocusableContent = items.isNotEmpty() && !isLoading
    val preferredContentFocusRequester = if (hasFocusableContent) {
        gridFocusRequester
    } else {
        selectedTypeFocusRequester ?: gridFocusRequester
    }

    fun restoreTargetIndex(): Int {
        if (items.isEmpty()) return 0
        return if (focusedItemIndex in items.indices) {
            focusedItemIndex
        } else {
            lastFocusedIndex.coerceIn(0, items.lastIndex)
        }
    }

    suspend fun requestFocusUntilTimeout(requester: FocusRequester): Boolean =
        withTimeoutOrNull(TOP_FOCUS_RESTORE_TIMEOUT_MILLIS) {
            var focused = false
            while (!focused) {
                withFrameNanos { }
                focused = runCatching { requester.requestFocus() }.getOrDefault(false)
            }
            focused
        } ?: false

    suspend fun requestVisibleItemFocus(index: Int): Boolean {
        val requester = focusRequesters.getOrNull(index) ?: return false
        val visible = gridState.layoutInfo.visibleItemsInfo.any { it.index == index }
        if (!visible) return false
        suppressNextFocusedRowScroll = true
        return requestFocusUntilTimeout(requester)
    }

    val launchItemFocusRestore = {
            index: Int,
            fallbackFocusRequester: FocusRequester,
            clearPendingRestore: Boolean,
        ->
        if (items.isNotEmpty()) {
            val target = index.coerceIn(0, items.lastIndex)
            lastFocusedIndex = target
            restoreFocusJob?.cancel()
            isRestoringFocus = true
            restoreFocusJob = scope.launch {
                var completed = false
                try {
                    val restoredVisibleItem = if (clearPendingRestore) {
                        false
                    } else {
                        requestVisibleItemFocus(target)
                    }
                    if (!restoredVisibleItem) {
                        repeat(TOP_FOCUS_RESTORE_INITIAL_FRAME_WAIT) {
                            withFrameNanos { }
                        }
                        gridState.scrollToItem(target)
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.any { it.index == target }
                        }.first { it }
                        val itemRequester = focusRequesters.getOrNull(target)
                        val restoredScrolledItem =
                            itemRequester?.let { requestFocusUntilTimeout(it) } ?: false
                        if (!restoredScrolledItem) {
                            requestFocusUntilTimeout(fallbackFocusRequester)
                        }
                    }
                    completed = true
                } finally {
                    isRestoringFocus = false
                    if (completed && clearPendingRestore) {
                        onFocusedItemRestoreHandled()
                    }
                }
            }
        }
    }
    val shouldLoadMore by remember(gridState, items.size, canLoadMore, isLoading, isLoadingMore) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            items.isNotEmpty() &&
                    canLoadMore &&
                    !isLoading &&
                    !isLoadingMore &&
                    total > 0 &&
                    lastVisible >= total - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(gridState, items.size) {
        snapshotFlow {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            val minX = visibleItems.minOfOrNull { it.offset.x }
            if (minX == null) {
                emptySet()
            } else {
                visibleItems.filter { it.offset.x == minX }.map { it.index }.toSet()
            }
        }.collect { leftEdgeIndexes = it }
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(
        isActiveDestination,
        restoreFocusedItemOnEnter,
        focusedItemId,
        items,
        focusRequesters,
        gridHasFocus,
    ) {
        val returnedToTop = isActiveDestination && !wasActiveDestination
        wasActiveDestination = isActiveDestination

        if (!isActiveDestination) {
            gridHasFocus = false
            isRestoringFocus = false
            return@LaunchedEffect
        }
        if (focusedItemIndex in items.indices) {
            lastFocusedIndex = focusedItemIndex
        }
        if (items.isEmpty()) {
            return@LaunchedEffect
        }

        val shouldRestoreItem = returnedToTop || (restoreFocusedItemOnEnter && !gridHasFocus)
        if (shouldRestoreItem) {
            launchItemFocusRestore(
                restoreTargetIndex(),
                gridFocusRequester,
                restoreFocusedItemOnEnter,
            )
        }
    }

    // Keep normal card-to-card navigation anchored, but don't move the list
    // after restoring focus to a card that was already visible.
    LaunchedEffect(lastFocusedIndex, gridHasFocus, isRestoringFocus) {
        if (!gridHasFocus || isRestoringFocus || items.isEmpty()) {
            return@LaunchedEffect
        }
        if (suppressNextFocusedRowScroll) {
            suppressNextFocusedRowScroll = false
            return@LaunchedEffect
        }
        gridState.scrollToItem(lastFocusedIndex.coerceIn(0, items.lastIndex))
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusProperties {
                onEnter = {
                    if (hasFocusableContent) {
                        launchItemFocusRestore(
                            restoreTargetIndex(),
                            gridFocusRequester,
                            restoreFocusedItemOnEnter,
                        )
                    } else {
                        selectedTypeFocusRequester?.requestFocus()
                    }
                }
            }
            .focusGroup(),
    ) {
        TopFilterTabs(
            selectedType = selectedType,
            contentCanFocus = items.isNotEmpty() && !isLoading,
            onTypeSelected = onTypeSelected,
            contentFocusRequester = gridFocusRequester,
            typeFocusRequesters = typeFocusRequesters,
            mainMenuFocusRequester = mainMenuFocusRequester,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator()

                error != null && items.isEmpty() -> Text(text = error)

                else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val horizontalSpacing = TvCardSpacing.Horizontal
                    val gridHorizontalPadding =
                        TvScreenPadding.Horizontal + TvScreenPadding.Horizontal
                    val gridColumnCount =
                        (((maxWidth - gridHorizontalPadding).value + horizontalSpacing.value) /
                                (cardWidth.value + horizontalSpacing.value)).toInt()
                            .coerceAtLeast(1)

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = cardWidth),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(gridFocusRequester)
                            .focusProperties {
                                onEnter = {
                                    if (hasFocusableContent) {
                                        launchItemFocusRestore(
                                            restoreTargetIndex(),
                                            gridFocusRequester,
                                            restoreFocusedItemOnEnter,
                                        )
                                    }
                                }
                            }
                            .onFocusChanged { state ->
                                val hadFocus = gridHasFocus
                                gridHasFocus = state.hasFocus
                                if (!state.hasFocus) {
                                    isRestoringFocus = false
                                }
                                if (
                                    state.hasFocus &&
                                    !hadFocus &&
                                    !restoreFocusedItemOnEnter &&
                                    items.isNotEmpty()
                                ) {
                                    launchItemFocusRestore(
                                        lastFocusedIndex.coerceIn(0, items.lastIndex),
                                        gridFocusRequester,
                                        false,
                                    )
                                }
                            }
                            .focusGroup()
                            .focusable(),
                        contentPadding = PaddingValues(
                            start = TvScreenPadding.Horizontal,
                            end = TvScreenPadding.Horizontal,
                            top = 16.dp,
                            bottom = TvScreenPadding.Vertical,
                        ),
                        verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            val stableOnClick = remember(item, index) {
                                {
                                    lastFocusedIndex = index
                                    onItemFocused(item.id)
                                    onItemSelected(item)
                                }
                            }
                            val stableOnFocused = remember(item.id) { { onItemFocused(item.id) } }
                            TopAnimeCard(
                                item = item,
                                rank = index + 1,
                                onClick = stableOnClick,
                                onFocused = stableOnFocused,
                                modifier = Modifier
                                    .focusRequester(focusRequesters[index])
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when (event.key) {
                                            Key.DirectionUp -> {
                                                if (index < gridColumnCount) {
                                                    runCatching { selectedTypeFocusRequester?.requestFocus() }
                                                    true
                                                } else {
                                                    false
                                                }
                                            }

                                            Key.DirectionLeft -> {
                                                if (index !in leftEdgeIndexes) {
                                                    val target = index - 1
                                                    scope.launch {
                                                        gridState.scrollToItem(target)
                                                        snapshotFlow {
                                                            gridState.layoutInfo.visibleItemsInfo.any { it.index == target }
                                                        }.first { it }
                                                        runCatching { focusRequesters[target].requestFocus() }
                                                    }
                                                    true
                                                } else {
                                                    runCatching { mainMenuFocusRequester?.requestFocus() }
                                                    mainMenuFocusRequester != null
                                                }
                                            }

                                            else -> {
                                                val target = when (event.key) {
                                                    Key.DirectionDown -> index + gridColumnCount
                                                    Key.DirectionUp -> index - gridColumnCount
                                                    else -> return@onPreviewKeyEvent false
                                                }
                                                when {
                                                    target in items.indices -> {
                                                        scope.launch {
                                                            gridState.scrollToItem(target)
                                                            snapshotFlow {
                                                                gridState.layoutInfo.visibleItemsInfo.any { it.index == target }
                                                            }.first { it }
                                                            runCatching { focusRequesters[target].requestFocus() }
                                                        }
                                                        true
                                                    }

                                                    else -> false
                                                }
                                            }
                                        }
                                    }
                                    .onFocusChanged { state ->
                                        if (state.hasFocus) {
                                            lastFocusedIndex = index
                                            onItemFocused(item.id)
                                            if (isRestoringFocus && restoreFocusedItemOnEnter && item.id == focusedItemId) {
                                                onFocusedItemRestoreHandled()
                                            }
                                        }
                                    },
                            )
                        }

                        if (isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                TvLoadingFooter()
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 4
private const val TOP_FOCUS_RESTORE_TIMEOUT_MILLIS = 500L
private const val TOP_FOCUS_RESTORE_INITIAL_FRAME_WAIT = 2
