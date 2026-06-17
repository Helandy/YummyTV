package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridItemFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.feature.library.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LibraryGrid(
    items: List<LibraryItem>,
    focusedItemId: Int?,
    gridFocusRequester: FocusRequester,
    selectedTabFocusRequester: FocusRequester,
    restoreFocusedItemToken: Int,
    focusStateKey: String,
    onAnimeSelected: (Int) -> Unit,
    onItemFocused: (Int) -> Unit,
    onRemoveEntry: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val itemIds = remember(items) { items.map { it.animeId } }
    val focusRequesters = remember(itemIds) { List(items.size) { FocusRequester() } }
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val posterQuality = LocalPosterQuality.current
    val cardWidth = currentTvTitleCardDimensions().width
    var lastFocusedIndex by rememberSaveable(focusStateKey) {
        mutableIntStateOf(focusedItemId?.let(itemIds::indexOf)?.takeIf { it >= 0 } ?: 0)
    }
    var lastFocusedItemId by rememberSaveable(focusStateKey) {
        mutableStateOf(focusedItemId?.takeIf { it in itemIds })
    }
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoringFromMainMenu by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var pendingFocusAfterDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDeletedItemId by remember { mutableStateOf<Int?>(null) }
    var leftEdgeIndexes by remember { mutableStateOf(emptySet<Int>()) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun currentIndexFor(itemId: Int?): Int? =
        itemId?.let(itemIds::indexOf)?.takeIf { it >= 0 }

    fun rememberFocusedItem(index: Int) {
        lastFocusedIndex = index
        lastFocusedItemId = itemIds.getOrNull(index)
    }

    fun restoreTargetIndex(): Int {
        if (items.isEmpty()) return 0
        return (
                currentIndexFor(focusedItemId)
                    ?: currentIndexFor(lastFocusedItemId)
                    ?: lastFocusedIndex
                ).coerceIn(0, items.lastIndex)
    }

    fun requestItemFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester = gridFocusRequester,
        clearMainMenuRestore: Boolean = true,
    ) {
        if (items.isEmpty()) return
        val target = index.coerceIn(0, items.lastIndex)
        rememberFocusedItem(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridItemFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            itemIndex = target,
            gridState = gridState,
            itemFocusRequesters = focusRequesters,
            fallbackFocusRequester = fallbackFocusRequester,
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

    LaunchedEffect(focusedItemId, items) {
        currentIndexFor(focusedItemId)?.let { focusedIndex ->
            rememberFocusedItem(focusedIndex)
        }
    }

    LaunchedEffect(restoreFocusedItemToken) {
        if (restoreFocusedItemToken <= 0 || items.isEmpty()) return@LaunchedEffect
        requestItemFocus(
            index = restoreTargetIndex(),
            fallbackFocusRequester = selectedTabFocusRequester,
            clearMainMenuRestore = false,
        )
    }

    LaunchedEffect(items, pendingFocusAfterDeleteIndex, pendingDeletedItemId) {
        val pendingIndex = pendingFocusAfterDeleteIndex ?: return@LaunchedEffect
        val deletedItemId = pendingDeletedItemId ?: return@LaunchedEffect
        if (items.any { it.animeId == deletedItemId }) return@LaunchedEffect
        if (items.isEmpty()) {
            isRestoringFocus = true
            lastFocusedItemId = null
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

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumnCount),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(gridFocusRequester)
                .focusProperties {
                    onEnter = {
                        restoringFromMainMenu = requestedFocusDirection == FocusDirection.Right
                        requestItemFocus(if (restoringFromMainMenu) 0 else restoreTargetIndex())
                    }
                }
                .onFocusChanged { state ->
                    val hadFocus = gridHasFocus
                    gridHasFocus = state.hasFocus
                    if (!state.hasFocus) {
                        isRestoringFocus = false
                        restoringFromMainMenu = false
                    }
                    if (state.hasFocus && !hadFocus && items.isNotEmpty()) {
                        val target = if (restoringFromMainMenu) 0 else restoreTargetIndex()
                        requestItemFocus(target)
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
            itemsIndexed(items, key = { _, item -> item.animeId }) { index, item ->
                val stableOnClick = remember(item.animeId) { { onAnimeSelected(item.animeId) } }
                val stableOnFocused = remember(item.animeId) { { onItemFocused(item.animeId) } }
                val stableOnDelete = remember(item.animeId, index) {
                    {
                        pendingFocusAfterDeleteIndex = index
                        pendingDeletedItemId = item.animeId
                        val immediateTarget = if (index < items.lastIndex) index + 1 else index - 1
                        if (immediateTarget >= 0) {
                            rememberFocusedItem(immediateTarget)
                            runCatching { focusRequesters[immediateTarget].requestFocus() }
                        } else {
                            lastFocusedItemId = null
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
                    modifier = Modifier
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                                return@onPreviewKeyEvent false
                            }
                            if (index !in leftEdgeIndexes) {
                                requestItemFocus(index - 1)
                                true
                            } else {
                                runCatching { mainMenuFocusRequester?.requestFocus() }
                                mainMenuFocusRequester != null
                            }
                        }
                        .onFocusChanged { state ->
                            if (state.hasFocus && gridHasFocus && !isRestoringFocus) {
                                rememberFocusedItem(index)
                            }
                        }
                        .focusGroup(),
                    cardModifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionUp &&
                                index < gridColumnCount
                            ) {
                                runCatching { selectedTabFocusRequester.requestFocus() }
                                true
                            } else {
                                false
                            }
                        }
                        .focusProperties {
                            if (index !in leftEdgeIndexes) {
                                focusRequesters.getOrNull(index - 1)?.let { left = it }
                            }
                            focusRequesters.getOrNull(index + 1)?.let { right = it }
                        },
                    deleteModifier = Modifier.focusProperties {
                        if (index !in leftEdgeIndexes) {
                            focusRequesters.getOrNull(index - 1)?.let { left = it }
                        }
                        focusRequesters.getOrNull(index + 1)?.let { right = it }
                        up = focusRequesters[index]
                    },
                )
            }
        }
    }
}

private fun LibraryItem.posterUrl(quality: PosterQuality): String? = poster.posterUrl(quality)

private fun LibraryPoster?.posterUrl(quality: PosterQuality): String? = when (quality) {
    PosterQuality.LOW -> this?.medium ?: this?.big ?: this?.fullsize ?: this?.small
    PosterQuality.STANDARD -> this?.big ?: this?.medium ?: this?.fullsize ?: this?.small
    PosterQuality.MEGA -> this?.mega ?: this?.big ?: this?.medium ?: this?.fullsize ?: this?.small
    PosterQuality.HIGH -> this?.fullsize ?: this?.mega ?: this?.big ?: this?.medium ?: this?.small
}
