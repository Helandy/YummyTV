package su.afk.yummy.tv.feature.top.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusedGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvAppendErrorFooter
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.utils.uiMessage
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun TopBrowser(
    pagingItems: LazyPagingItems<AnimeTopItem>,
    selectedType: AnimeTopType,
    isActiveDestination: Boolean,
    showTitleYear: Boolean,
    onItemSelected: (AnimeTopItem) -> Unit,
    onTypeSelected: (AnimeTopType) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val gridFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    val scope = rememberCoroutineScope()
    val typeFocusRequesters = remember { List(AnimeTopType.entries.size) { FocusRequester() } }
    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append
    val itemCount = pagingItems.itemCount
    val snapshotItems = pagingItems.itemSnapshotList.items
    val isLoading = refreshState is LoadState.Loading
    val refreshError = (refreshState as? LoadState.Error)?.error?.uiMessage()
    val itemIds = remember(snapshotItems) { snapshotItems.map { it.id } }
    val focusRequesters = remember(selectedType, itemCount) { List(itemCount) { FocusRequester() } }
    val itemFocusRequesters = remember(itemIds, focusRequesters) {
        itemIds.zip(focusRequesters).toMap()
    }
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>(selectedType.name)
    var lastFocusedIndex by remember { mutableStateOf(0) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    var pendingContentFocusType by remember { mutableStateOf<AnimeTopType?>(null) }
    var observedSelectedType by remember { mutableStateOf(selectedType) }
    val firstItemFocusRequester = focusRequesters.firstOrNull()
    val selectedTypeFocusRequester =
        typeFocusRequesters.getOrNull(AnimeTopType.entries.indexOf(selectedType).coerceAtLeast(0))
    val hasFocusableContent = itemCount > 0 && !isLoading
    val preferredItemIndex = when {
        itemCount == 0 -> -1
        else -> (focusRestoreState.targetIndex(itemIds) ?: lastFocusedIndex).coerceIn(
            0,
            itemCount - 1
        )
    }
    val gridFallbackFocusRequester = focusRequesters.getOrNull(preferredItemIndex)
        ?: selectedTypeFocusRequester
        ?: FocusRequester.Default
    val retryFocusRequester = remember { FocusRequester() }
    val showRefreshError = refreshError != null && itemCount == 0
    val preferredContentFocusRequester = when {
        hasFocusableContent -> gridFocusRequester
        showRefreshError -> retryFocusRequester
        else -> selectedTypeFocusRequester ?: gridFocusRequester
    }
    val tabContentFocusRequester = firstItemFocusRequester ?: gridFocusRequester

    fun rememberFocusedItem(index: Int) {
        val target = index.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        lastFocusedIndex = target
        itemIds.getOrNull(target)?.let { key ->
            focusRestoreState.onItemFocused(key, target)
        }
    }

    fun restoreTargetIndex(): Int {
        if (itemCount == 0) return 0
        return (focusRestoreState.targetIndex(itemIds) ?: lastFocusedIndex).coerceIn(
            0,
            itemCount - 1
        )
    }

    suspend fun requestFocusUntilTimeout(requester: FocusRequester): Boolean =
        withTimeoutOrNull(TOP_FOCUS_RESTORE_TIMEOUT) {
            var focused = false
            while (!focused) {
                withFrameNanos { }
                focused = runCatching { requester.requestFocus() }.getOrDefault(false)
            }
            focused
        } ?: false

    fun requestFirstContentItemFocus() {
        if (!hasFocusableContent) {
            pendingContentFocusType = selectedType
            return
        }
        val firstItemRequester = focusRequesters.firstOrNull() ?: return
        rememberFocusedItem(0)
        restoreFocusJob?.cancel()
        scope.launch {
            gridState.scrollToItem(0)
            snapshotFlow {
                gridState.layoutInfo.visibleItemsInfo.any { it.index == 0 }
            }.first { it }
            requestFocusUntilTimeout(firstItemRequester)
        }
    }

    fun requestContentFocusForType(type: AnimeTopType) {
        if (type != selectedType) {
            pendingContentFocusType = type
            onTypeSelected(type)
            return
        }
        if (hasFocusableContent) {
            requestFirstContentItemFocus()
        } else {
            pendingContentFocusType = type
        }
    }

    val launchItemFocusRestore = {
            index: Int,
            fallbackFocusRequester: FocusRequester,
        ->
        if (itemCount > 0) {
            val target = index.coerceIn(0, itemCount - 1)
            rememberFocusedItem(target)
            restoreFocusJob?.cancel()
            isRestoringFocus = true
            restoreFocusJob = launchTvLazyGridKeyFocusRestore(
                previousJob = restoreFocusJob,
                scope = scope,
                restoreState = focusRestoreState,
                keys = itemIds,
                gridState = gridState,
                itemFocusRequesters = itemFocusRequesters,
                fallbackFocusRequester = fallbackFocusRequester,
                fallbackIndex = target,
                onRestoreFinished = {
                    isRestoringFocus = false
                },
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(selectedType) {
        if (observedSelectedType == selectedType) return@LaunchedEffect
        observedSelectedType = selectedType
        focusRestoreState.clear()
        lastFocusedIndex = 0
        isRestoringFocus = false
        restoreFocusJob?.cancel()
        gridState.scrollToItem(0)
    }

    LaunchedEffect(
        selectedType,
        hasFocusableContent,
        focusRequesters,
        pendingContentFocusType,
    ) {
        if (pendingContentFocusType != selectedType || !hasFocusableContent) {
            return@LaunchedEffect
        }
        val firstItemRequester = focusRequesters.firstOrNull() ?: return@LaunchedEffect
        pendingContentFocusType = null
        rememberFocusedItem(0)
        gridState.scrollToItem(0)
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.any { it.index == 0 }
        }.first { it }
        requestFocusUntilTimeout(firstItemRequester)
    }

    LaunchedEffect(
        isActiveDestination,
        itemCount,
        focusRequesters,
        gridHasFocus,
    ) {
        if (!isActiveDestination) {
            gridHasFocus = false
            isRestoringFocus = false
            return@LaunchedEffect
        }
        if (itemCount == 0) {
            return@LaunchedEffect
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
            .focusGroup(),
    ) {
        TopFilterTabs(
            selectedType = selectedType,
            contentCanFocus = itemCount > 0 && !isLoading,
            onTypeSelected = onTypeSelected,
            onContentFocusRequested = ::requestContentFocusForType,
            contentFocusRequester = tabContentFocusRequester,
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
                isLoading -> TvLoadingScreen()

                showRefreshError -> TvStateMessage(
                    title = refreshError.orEmpty(),
                    icon = Icons.Filled.Warning,
                    onRetry = onRetry,
                    retryFocusRequester = retryFocusRequester,
                )

                else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val horizontalSpacing = TvCardSpacing.Horizontal
                    val gridHorizontalPadding =
                        TvScreenPadding.Horizontal + TvScreenPadding.Horizontal
                    val gridColumnCount =
                        (((maxWidth - gridHorizontalPadding).value + horizontalSpacing.value) /
                                (cardWidth.value + horizontalSpacing.value)).toInt()
                            .coerceAtLeast(1)

                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides TvFocusedGridBringIntoViewSpec,
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = cardWidth),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(gridFocusRequester)
                                .tvFocusRestorer(
                                    fallback = gridFallbackFocusRequester,
                                    enabled = hasFocusableContent,
                                )
                                .onFocusChanged { state ->
                                    gridHasFocus = state.hasFocus
                                    if (state.isFocused && hasFocusableContent && !isRestoringFocus) {
                                        launchItemFocusRestore(
                                            restoreTargetIndex(),
                                            gridFallbackFocusRequester,
                                        )
                                    }
                                    if (!state.hasFocus) {
                                        isRestoringFocus = false
                                    }
                                }
                                .focusable(),
                            contentPadding = PaddingValues(
                                start = TvScreenPadding.Horizontal,
                                end = TvScreenPadding.Horizontal,
                                top = 16.dp,
                                bottom = TvScreenPadding.Vertical + TvFocusedCardBottomInset,
                            ),
                            verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                        ) {
                            items(
                                count = itemCount,
                                key = pagingItems.itemKey { it.id },
                                contentType = { "item" },
                            ) { index ->
                                val item = pagingItems[index] ?: return@items
                                val stableOnClick = remember(item, index) {
                                    {
                                        rememberFocusedItem(index)
                                        onItemSelected(item)
                                    }
                                }
                                val stableOnFocused =
                                    remember(item.id, index) { { rememberFocusedItem(index) } }
                                TopAnimeCard(
                                    item = item,
                                    rank = index + 1,
                                    showTitleYear = showTitleYear,
                                    onClick = stableOnClick,
                                    onFocused = stableOnFocused,
                                    modifier = Modifier
                                        .focusRequester(focusRequesters[index])
                                        .focusProperties {
                                            if (index % gridColumnCount == 0) {
                                                mainMenuFocusRequester?.let { left = it }
                                            }
                                            if (index < gridColumnCount) {
                                                selectedTypeFocusRequester?.let { up = it }
                                            }
                                        }
                                        .onFocusChanged { state ->
                                            if (state.hasFocus) {
                                                rememberFocusedItem(index)
                                            }
                                        },
                                )
                            }

                            if (appendState is LoadState.Loading) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    contentType = { "footer" },
                                ) {
                                    TvLoadingFooter()
                                }
                            }

                            if (appendState is LoadState.Error) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    contentType = { "footer" },
                                ) {
                                    TvAppendErrorFooter(
                                        message = appendState.error.uiMessage(),
                                        onRetry = { pagingItems.retry() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val TOP_FOCUS_RESTORE_TIMEOUT = 500.milliseconds
private val TvFocusedCardBottomInset = 24.dp
