package su.afk.yummy.tv.feature.top100.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.top100.model.AnimeTopItem
import su.afk.yummy.tv.domain.top100.model.AnimeTopType
import su.afk.yummy.tv.feature.top100.utils.launchRestoreTop100ItemFocus

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Top100Browser(
    items: List<AnimeTopItem>,
    selectedType: AnimeTopType,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    error: String?,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
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
    val scope = rememberCoroutineScope()
    val typeFocusRequesters = remember { List(AnimeTopType.entries.size) { FocusRequester() } }

    var lastFocusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    var wasActiveDestination by remember { mutableStateOf(isActiveDestination) }
    val focusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }
    val focusedItemIndex = focusedItemId
        ?.let { id -> items.indexOfFirst { item -> item.id == id } }
        ?: -1
    val selectedTypeFocusRequester =
        typeFocusRequesters.getOrNull(AnimeTopType.entries.indexOf(selectedType).coerceAtLeast(0))
    val restoreItemFocusRequester =
        if (restoreFocusedItemOnEnter && focusedItemIndex in items.indices) {
            focusRequesters.getOrNull(focusedItemIndex)
        } else {
            null
        }
    val preferredContentFocusRequester =
        restoreItemFocusRequester ?: selectedTypeFocusRequester ?: gridFocusRequester
    val targetFocusedIndex = {
        if (focusedItemIndex in items.indices) {
            focusedItemIndex
        } else {
            lastFocusedIndex.coerceIn(0, items.lastIndex)
        }
    }
    val launchItemFocusRestore = {
            index: Int,
            fallbackFocusRequester: FocusRequester,
            clearPendingRestore: Boolean,
        ->
        if (items.isNotEmpty()) {
            val target = index.coerceIn(0, items.lastIndex)
            lastFocusedIndex = target
            isRestoringFocus = true
            restoreFocusJob = launchRestoreTop100ItemFocus(
                previousJob = restoreFocusJob,
                scope = scope,
                itemIndex = target,
                gridState = gridState,
                itemFocusRequesters = focusRequesters,
                fallbackFocusRequester = fallbackFocusRequester,
                onRestoreFinished = {
                    isRestoringFocus = false
                    if (clearPendingRestore) {
                        onFocusedItemRestoreHandled()
                    }
                },
            )
        }
    }
    val requestCardFocus = { index: Int ->
        if (items.isNotEmpty()) {
            launchItemFocusRestore(index, gridFocusRequester, false)
        }
    }
    val requestLastFocusedCard = {
        requestCardFocus(lastFocusedIndex)
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
        val returnedToTop100 = isActiveDestination && !wasActiveDestination
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

        val shouldRestoreItem = returnedToTop100 || (restoreFocusedItemOnEnter && !gridHasFocus)
        if (shouldRestoreItem) {
            launchItemFocusRestore(
                targetFocusedIndex(),
                selectedTypeFocusRequester ?: gridFocusRequester,
                restoreFocusedItemOnEnter,
            )
        }
    }

    // Lift the focused card's row to the top, but only once focus has settled.
    // Running this as a cancellable effect (instead of launching a scroll on every
    // focus event) keeps the focused row pinned at the top so the row below stays
    // composed and DPAD-down preserves the column.
    LaunchedEffect(lastFocusedIndex, gridHasFocus) {
        if (gridHasFocus && !isRestoringFocus && items.isNotEmpty()) {
            gridState.animateScrollToItem(lastFocusedIndex.coerceIn(0, items.lastIndex))
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo }
            .collect { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@collect
                val total = layoutInfo.totalItemsCount
                if (total > 0 && lastVisible >= total - 4 && canLoadMore && !isLoadingMore) {
                    onLoadMore()
                }
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
                    if (isActiveDestination && restoreFocusedItemOnEnter && items.isNotEmpty()) {
                        launchItemFocusRestore(
                            targetFocusedIndex(),
                            selectedTypeFocusRequester ?: gridFocusRequester,
                            true,
                        )
                    } else {
                        selectedTypeFocusRequester?.requestFocus() ?: requestLastFocusedCard()
                    }
                }
            }
            .focusGroup(),
    ) {
        Top100FilterTabs(
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
                    val horizontalSpacing = 16.dp
                    val gridHorizontalPadding =
                        TvScreenPadding.Horizontal + TvScreenPadding.Horizontal
                    val gridColumnCount =
                        (((maxWidth - gridHorizontalPadding).value + horizontalSpacing.value) /
                                (172.dp.value + horizontalSpacing.value)).toInt().coerceAtLeast(1)

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 172.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(gridFocusRequester)
                            .focusProperties {
                                onEnter = {
                                    requestLastFocusedCard()
                                }
                            }
                            .onFocusChanged { state ->
                                val hadFocus = gridHasFocus
                                gridHasFocus = state.hasFocus
                                if (!state.hasFocus) {
                                    isRestoringFocus = false
                                }
                                if (state.hasFocus && !hadFocus && !restoreFocusedItemOnEnter && items.isNotEmpty()) {
                                    launchItemFocusRestore(
                                        lastFocusedIndex.coerceIn(0, items.lastIndex),
                                        gridFocusRequester,
                                        false,
                                    )
                                }
                            }
                            .focusGroup(),
                        contentPadding = PaddingValues(
                            start = TvScreenPadding.Horizontal,
                            end = TvScreenPadding.Horizontal,
                            top = 16.dp,
                            bottom = TvScreenPadding.Vertical,
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            Top100AnimeCard(
                                item = item,
                                rank = index + 1,
                                onClick = stableOnClick,
                                onFocused = stableOnFocused,
                                screenshotUrls = if (item.id == focusedItemId) focusedPreview?.screenshotUrls.orEmpty() else emptyList(),
                                modifier = Modifier
                                    .focusRequester(focusRequesters[index])
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        if (event.key == Key.DirectionUp && index < gridColumnCount) {
                                            runCatching { selectedTypeFocusRequester?.requestFocus() }
                                            return@onPreviewKeyEvent true
                                        }
                                        val target = when (event.key) {
                                            Key.DirectionUp -> index - gridColumnCount
                                            Key.DirectionDown -> index + gridColumnCount
                                            Key.DirectionLeft -> index - 1
                                            else -> return@onPreviewKeyEvent false
                                        }
                                        when {
                                            target in items.indices -> {
                                                scope.launch {
                                                    gridState.animateScrollToItem(target)
                                                    snapshotFlow {
                                                        gridState.layoutInfo.visibleItemsInfo.any { it.index == target }
                                                    }.first { it }
                                                    runCatching { focusRequesters[target].requestFocus() }
                                                }
                                                true
                                            }

                                            event.key == Key.DirectionUp || event.key == Key.DirectionLeft -> false
                                            else -> true
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
