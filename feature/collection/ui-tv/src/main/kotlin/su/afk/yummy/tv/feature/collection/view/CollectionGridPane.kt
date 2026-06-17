package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import kotlinx.coroutines.flow.distinctUntilChanged
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridItemFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.feature.collection.R

@Composable
internal fun CollectionGridPane(
    collection: CollectionDetail?,
    isLoading: Boolean,
    isVoteLoading: Boolean,
    error: String?,
    restoreFocusedItemOnEnter: Boolean = false,
    focusedItemId: Int?,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    onAnimeSelected: (Int) -> Unit,
    onItemFocused: (Int) -> Unit,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit,
    onVote: (CollectionVote) -> Unit,
    onRetry: () -> Unit,
    onFocusedItemRestoreHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            error != null -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }

            collection != null -> CollectionGrid(
                collection = collection,
                isVoteLoading = isVoteLoading,
                focusedItemId = focusedItemId,
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                restoreFocusedItemOnEnter = restoreFocusedItemOnEnter,
                onAnimeSelected = onAnimeSelected,
                onItemFocused = onItemFocused,
                onScrollPositionChanged = onScrollPositionChanged,
                onVote = onVote,
                onFocusedItemRestoreHandled = onFocusedItemRestoreHandled,
            )
        }
    }
}

@Composable
private fun CollectionGrid(
    collection: CollectionDetail,
    isVoteLoading: Boolean,
    focusedItemId: Int?,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    restoreFocusedItemOnEnter: Boolean = false,
    onAnimeSelected: (Int) -> Unit,
    onItemFocused: (Int) -> Unit,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit,
    onVote: (CollectionVote) -> Unit,
    onFocusedItemRestoreHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
    )
    val scope = rememberCoroutineScope()
    val animes = collection.animes
    val animeIds = remember(animes) { animes.map { it.id } }

    val headerFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember(animeIds) { List(animes.size) { FocusRequester() } }
    val gridItemFocusRequesters = remember(headerFocusRequester, focusRequesters) {
        listOf(headerFocusRequester) + focusRequesters
    }
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    // сбрасывается при смене коллекции — при первом входе фокус на заголовке
    var firstEntry by rememberSaveable(collection.id) { mutableStateOf(true) }
    var lastFocusedIndex by rememberSaveable(collection.id) {
        val idx = focusedItemId?.let(animeIds::indexOf)
            ?.coerceAtLeast(0) ?: 0
        mutableIntStateOf(idx)
    }
    var lastFocusedItemId by rememberSaveable(collection.id) {
        mutableStateOf(focusedItemId?.takeIf { it in animeIds })
    }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun currentIndexFor(itemId: Int?): Int? =
        itemId?.let(animeIds::indexOf)?.takeIf { it >= 0 }

    fun rememberFocusedAnime(index: Int) {
        firstEntry = false
        lastFocusedIndex = index
        lastFocusedItemId = animeIds.getOrNull(index)
    }

    fun restoreTargetIndex(): Int {
        if (animes.isEmpty()) return 0
        return (
                currentIndexFor(focusedItemId)
                    ?: currentIndexFor(lastFocusedItemId)
                    ?: lastFocusedIndex
                ).coerceIn(0, animes.lastIndex)
    }

    fun requestGridItemFocus(
        lazyGridIndex: Int,
        fallbackFocusRequester: FocusRequester,
        onRestoreFinished: () -> Unit = {},
    ) {
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridItemFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            itemIndex = lazyGridIndex,
            gridState = gridState,
            itemFocusRequesters = gridItemFocusRequesters,
            fallbackFocusRequester = fallbackFocusRequester,
            onRestoreFinished = {
                isRestoringFocus = false
                onRestoreFinished()
            },
        )
    }

    fun requestHeaderFocus() {
        requestGridItemFocus(
            lazyGridIndex = 0,
            fallbackFocusRequester = headerFocusRequester,
        )
    }

    fun requestAnimeFocus(
        index: Int,
        clearPendingRestore: Boolean = false,
    ) {
        if (animes.isEmpty()) return
        val target = index.coerceIn(0, animes.lastIndex)
        rememberFocusedAnime(target)
        requestGridItemFocus(
            lazyGridIndex = target + 1,
            fallbackFocusRequester = headerFocusRequester,
            onRestoreFinished = {
                if (clearPendingRestore) {
                    onFocusedItemRestoreHandled()
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(focusedItemId, animes) {
        currentIndexFor(focusedItemId)?.let { focusedIndex ->
            rememberFocusedAnime(focusedIndex)
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) -> onScrollPositionChanged(index, offset) }
    }

    // Lift the focused card's row to the top once focus settles (+1 accounts for
    // the header item). A cancellable effect keeps the focused row pinned so the
    // row below stays composed and DPAD-down preserves the column.
    LaunchedEffect(lastFocusedIndex, gridHasFocus) {
        if (gridHasFocus && !isRestoringFocus && !firstEntry && animes.isNotEmpty()) {
            gridState.scrollToItem(lastFocusedIndex.coerceIn(0, animes.lastIndex) + 1)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridColumnCount =
            (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                    (cardWidth.value + horizontalSpacing.value)).toInt().coerceAtLeast(1)

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = cardWidth),
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { state ->
                    val hadFocus = gridHasFocus
                    gridHasFocus = state.hasFocus
                    if (!state.hasFocus) {
                        isRestoringFocus = false
                    }
                    if (state.hasFocus && !hadFocus) {
                        if (firstEntry && !restoreFocusedItemOnEnter) {
                            requestHeaderFocus()
                        } else if (animes.isNotEmpty()) {
                            requestAnimeFocus(
                                index = restoreTargetIndex(),
                                clearPendingRestore = restoreFocusedItemOnEnter,
                            )
                        }
                    }
                }
                .focusGroup(),
            contentPadding = PaddingValues(
                start = TvScreenPadding.Horizontal,
                end = TvScreenPadding.Horizontal,
                top = TvScreenPadding.Vertical,
                bottom = TvScreenPadding.Vertical,
            ),
            verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CollectionHeader(
                    collection = collection,
                    isVoteLoading = isVoteLoading,
                    onVote = onVote,
                    titleFocusRequester = headerFocusRequester,
                    downFocusRequester = focusRequesters.firstOrNull(),
                )
            }

            itemsIndexed(animes, key = { _, anime -> anime.id }) { index, anime ->
                val stableOnClick = remember(anime.id) { { onAnimeSelected(anime.id) } }
                val stableOnFocused = remember(anime.id) { { onItemFocused(anime.id) } }
                CollectionAnimeCard(
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (event.key != Key.DirectionLeft) return@onPreviewKeyEvent false
                            if (index % gridColumnCount != 0) return@onPreviewKeyEvent false
                            runCatching { mainMenuFocusRequester?.requestFocus() }
                            mainMenuFocusRequester != null
                        }
                        .onFocusChanged { state ->
                            if (state.hasFocus && !isRestoringFocus) {
                                rememberFocusedAnime(index)
                            }
                        },
                    item = anime,
                    onClick = stableOnClick,
                    onFocused = stableOnFocused,
                )
            }
        }
    }
}
