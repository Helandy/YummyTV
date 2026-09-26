package su.afk.yummy.tv.feature.details.collections.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.focus.launchTvLazyGridKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.focus.rememberTvGridStartExtent
import su.afk.yummy.tv.core.designsystem.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.tv.TvTitleCard
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.collections.utils.posterUrl

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CollectionsGrid(
    collections: List<AnimeCollectionSummary>,
    onCollectionSelected: (Int) -> Unit,
) {
    val posterQuality = LocalPosterQuality.current
    val cardWidth = currentTvTitleCardDimensions().width
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val collectionIds = remember(collections) { collections.map { it.id } }
    val gridFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember(collectionIds) { List(collections.size) { FocusRequester() } }
    val itemFocusRequesters = remember(collectionIds, focusRequesters) {
        collectionIds.zip(focusRequesters).toMap()
    }
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>()
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun rememberFocusedCollection(index: Int) {
        collectionIds.getOrNull(index)?.let { key -> focusRestoreState.onItemFocused(key, index) }
    }

    fun restoreTargetIndex(): Int {
        if (collections.isEmpty()) return 0
        return focusRestoreState.targetIndex(collectionIds)?.coerceIn(0, collections.lastIndex) ?: 0
    }

    fun requestCollectionFocus(index: Int) {
        if (collections.isEmpty()) return
        val target = index.coerceIn(0, collections.lastIndex)
        rememberFocusedCollection(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = collectionIds,
            gridState = gridState,
            itemFocusRequesters = itemFocusRequesters,
            fallbackFocusRequester = focusRequesters.getOrNull(target) ?: gridFocusRequester,
            fallbackIndex = target,
            lazyIndexOffset = 1,
            onRestoreFinished = { isRestoringFocus = false },
        )
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridColumnCount =
            (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                (cardWidth.value + horizontalSpacing.value)).toInt()
                .coerceAtLeast(1)

        val gridStartExtent = rememberTvGridStartExtent(TvScreenPadding.Vertical, TvCardSpacing.Vertical)

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(TvCardSpacing.Vertical),
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = cardWidth),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = TvScreenPadding.Vertical,
                    bottom = TvScreenPadding.Vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(gridFocusRequester)
                    .tvFocusRestorer(
                        fallback = focusRequesters.getOrNull(restoreTargetIndex())
                            ?: FocusRequester.Default,
                    )
                    .onFocusChanged { state ->
                        val hadFocus = gridHasFocus
                        gridHasFocus = state.hasFocus
                        if (!state.hasFocus) {
                            isRestoringFocus = false
                        }
                        if (state.isFocused && !hadFocus && !isRestoringFocus && collections.isNotEmpty()) {
                            requestCollectionFocus(restoreTargetIndex())
                        }
                    },
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.details_collections_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = gridStartExtent.measure,
                    )
                }
                itemsIndexed(
                    collections,
                    key = { index, collection -> lazyKey("collection", collection.id, index) }) { index, collection ->
                    val stableOnClick = remember(collection.id, index) {
                        {
                            rememberFocusedCollection(index)
                            onCollectionSelected(collection.id)
                        }
                    }
                    val stableOnFocused =
                        remember(collection.id, index) { { rememberFocusedCollection(index) } }
                    TvTitleCard(
                        title = collection.title,
                        posterUrl = collection.posterUrl(posterQuality),
                        onClick = stableOnClick,
                        onFocused = stableOnFocused,
                        modifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .tvWholeItemBringIntoView(gridStartExtent.takeIf { index < gridColumnCount })
                            .tvLazyGridRowFocusNavigation(
                                index = index,
                                columnCount = gridColumnCount,
                                itemCount = collections.size,
                                gridState = gridState,
                                scope = scope,
                                focusRequesterAt = focusRequesters::getOrNull,
                                // нулевой lazy-индекс занимает шапка «Коллекции»
                                lazyIndexOffset = 1,
                            ),
                    )
                }
            }
        }
    }
}
