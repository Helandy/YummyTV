package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusedGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.R
import su.afk.yummy.tv.feature.library.utils.posterUrl
import su.afk.yummy.tv.feature.library.utils.tvDateText
import su.afk.yummy.tv.feature.library.utils.tvUserRating

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryGrid(
    tab: LibraryTab,
    items: List<LibraryItem>,
    gridFocusRequester: FocusRequester,
    selectedTabFocusRequester: FocusRequester,
    focusStateKey: String,
    onAnimeSelected: (Int) -> Unit,
    onRemoveEntry: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val itemIds = remember(items) { items.map { it.animeId } }
    val focusRequesters = remember(itemIds) { List(items.size) { FocusRequester() } }
    val itemFocusRequesters = remember(itemIds, focusRequesters) {
        itemIds.zip(focusRequesters).toMap()
    }
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>(focusStateKey)
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val posterQuality = LocalPosterQuality.current
    val cardWidth = currentTvTitleCardDimensions().width
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoringFromMainMenu by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var pendingFocusAfterDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDeletedItemId by remember { mutableStateOf<Int?>(null) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun rememberFocusedItem(index: Int) {
        itemIds.getOrNull(index)?.let { key ->
            focusRestoreState.onItemFocused(key, index)
        }
    }

    fun restoreTargetIndex(): Int {
        if (items.isEmpty()) return 0
        return focusRestoreState.targetIndex(itemIds)?.coerceIn(0, items.lastIndex) ?: 0
    }

    fun gridFallbackFocusRequester(): FocusRequester =
        focusRequesters.getOrNull(restoreTargetIndex()) ?: selectedTabFocusRequester

    fun requestItemFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester? = null,
        clearMainMenuRestore: Boolean = true,
    ) {
        if (items.isEmpty()) return
        val target = index.coerceIn(0, items.lastIndex)
        rememberFocusedItem(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = itemIds,
            gridState = gridState,
            itemFocusRequesters = itemFocusRequesters,
            fallbackFocusRequester = fallbackFocusRequester ?: gridFallbackFocusRequester(),
            fallbackIndex = target,
            onRestoreFinished = {
                isRestoringFocus = false
                if (clearMainMenuRestore) {
                    restoringFromMainMenu = false
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(items, pendingFocusAfterDeleteIndex, pendingDeletedItemId) {
        val pendingIndex = pendingFocusAfterDeleteIndex ?: return@LaunchedEffect
        val deletedItemId = pendingDeletedItemId ?: return@LaunchedEffect
        if (items.any { it.animeId == deletedItemId }) return@LaunchedEffect
        if (items.isEmpty()) {
            isRestoringFocus = true
            focusRestoreState.clear()
            repeat(6) {
                runCatching { selectedTabFocusRequester.requestFocus() }
                withFrameNanos { }
            }
            isRestoringFocus = false
        } else {
            val target = pendingIndex.coerceIn(0, items.lastIndex)
            requestItemFocus(
                index = target,
                fallbackFocusRequester = selectedTabFocusRequester,
            )
        }
        pendingFocusAfterDeleteIndex = null
        pendingDeletedItemId = null
    }

    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.library_empty_list),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridHorizontalPadding = TvScreenPadding.Horizontal + TvScreenPadding.Horizontal
        val gridColumnCount =
            (((maxWidth - gridHorizontalPadding).value + horizontalSpacing.value) /
                    (cardWidth.value + horizontalSpacing.value)).toInt().coerceAtLeast(1)
        val gridSpacingWidth = horizontalSpacing * (gridColumnCount - 1).coerceAtLeast(0)
        val adaptiveCardWidth =
            (maxWidth - gridHorizontalPadding - gridSpacingWidth) / gridColumnCount

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides TvFocusedGridBringIntoViewSpec,
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumnCount),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(gridFocusRequester)
                    .tvFocusRestorer(
                        fallback = gridFallbackFocusRequester(),
                        enabled = items.isNotEmpty(),
                    )
                    .onFocusChanged { state ->
                        val hadFocus = gridHasFocus
                        gridHasFocus = state.hasFocus
                        if (!state.hasFocus) {
                            isRestoringFocus = false
                            restoringFromMainMenu = false
                        }
                        if (state.isFocused && !hadFocus && items.isNotEmpty() && !isRestoringFocus) {
                            val target = if (restoringFromMainMenu) 0 else restoreTargetIndex()
                            requestItemFocus(target)
                        }
                    }
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
                itemsIndexed(items, key = { _, item -> item.animeId }) { index, item ->
                    val stableOnClick = remember(item.animeId, index) {
                        {
                            rememberFocusedItem(index)
                            onAnimeSelected(item.animeId)
                        }
                    }
                    val stableOnFocused =
                        remember(item.animeId, index) { { rememberFocusedItem(index) } }
                    val rating = item.tvUserRating()
                    val stableOnDelete = remember(item.animeId, index) {
                        {
                            pendingFocusAfterDeleteIndex = index
                            pendingDeletedItemId = item.animeId
                            val immediateTarget =
                                if (index < items.lastIndex) index + 1 else index - 1
                            if (immediateTarget >= 0) {
                                rememberFocusedItem(immediateTarget)
                                runCatching { focusRequesters[immediateTarget].requestFocus() }
                            } else {
                                focusRestoreState.clear()
                                runCatching { selectedTabFocusRequester.requestFocus() }
                            }
                            onRemoveEntry(item.animeId)
                        }
                    }
                    LibraryAnimeCard(
                        title = item.title,
                        posterUrl = item.posterUrl(posterQuality),
                        onClick = stableOnClick,
                        onFocused = stableOnFocused,
                        onDelete = stableOnDelete,
                        cardWidth = adaptiveCardWidth,
                        subtitle = item.tvDateText(tab),
                        posterOverlay = rating?.let {
                            {
                                RatingBadge(
                                    rating = it,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                )
                            }
                        },
                        modifier = Modifier
                            .onFocusChanged { state ->
                                if (state.hasFocus && gridHasFocus && !isRestoringFocus) {
                                    rememberFocusedItem(index)
                                }
                            }
                            .tvFocusRestorer(fallback = focusRequesters[index]),
                        cardModifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .focusProperties {
                                if (index % gridColumnCount == 0) {
                                    mainMenuFocusRequester?.let { left = it }
                                }
                                if (index < gridColumnCount) {
                                    up = selectedTabFocusRequester
                                }
                            },
                        deleteModifier = Modifier.focusProperties {
                            if (index % gridColumnCount == 0) {
                                mainMenuFocusRequester?.let { left = it }
                            }
                            up = focusRequesters[index]
                        },
                    )
                }
            }
        }
    }
}
