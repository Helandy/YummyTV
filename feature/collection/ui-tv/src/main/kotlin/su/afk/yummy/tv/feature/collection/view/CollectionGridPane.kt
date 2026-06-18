package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
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
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    onAnimeSelected: (Int) -> Unit,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit,
    onVote: (CollectionVote) -> Unit,
    onRetry: () -> Unit,
    loadingFocusRequester: FocusRequester,
    retryFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(loadingFocusRequester)
                    .focusable(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            error != null -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                TvRetryButton(
                    text = stringResource(R.string.retry),
                    modifier = Modifier.focusRequester(retryFocusRequester),
                    onClick = onRetry,
                )
            }

            collection != null -> CollectionGrid(
                collection = collection,
                isVoteLoading = isVoteLoading,
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                onAnimeSelected = onAnimeSelected,
                onScrollPositionChanged = onScrollPositionChanged,
                onVote = onVote,
            )
        }
    }
}

@Composable
private fun CollectionGrid(
    collection: CollectionDetail,
    isVoteLoading: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    onAnimeSelected: (Int) -> Unit,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit,
    onVote: (CollectionVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
    )
    val scope = rememberCoroutineScope()
    val animes = collection.animes
    val animeIds = remember(animes) { animes.map { it.id } }

    val gridFocusRequester = remember { FocusRequester() }
    val headerFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember(animeIds) { List(animes.size) { FocusRequester() } }
    val animeFocusRequesters = remember(animeIds, focusRequesters) {
        animeIds.zip(focusRequesters).toMap()
    }
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>(collection.id)
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    // сбрасывается при смене коллекции — при первом входе фокус на заголовке
    var firstEntry by rememberSaveable(collection.id) { mutableStateOf(true) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun rememberFocusedAnime(index: Int) {
        firstEntry = false
        animeIds.getOrNull(index)?.let { key ->
            focusRestoreState.onItemFocused(key, index)
        }
    }

    fun restoreTargetIndex(): Int {
        if (animes.isEmpty()) return 0
        return focusRestoreState.targetIndex(animeIds)?.coerceIn(0, animes.lastIndex) ?: 0
    }

    fun gridFallbackFocusRequester(): FocusRequester =
        if (firstEntry) {
            headerFocusRequester
        } else {
            focusRequesters.getOrNull(restoreTargetIndex()) ?: headerFocusRequester
        }

    fun requestAnimeFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester,
    ) {
        if (animes.isEmpty()) return
        val target = index.coerceIn(0, animes.lastIndex)
        rememberFocusedAnime(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = animeIds,
            gridState = gridState,
            itemFocusRequesters = animeFocusRequesters,
            fallbackFocusRequester = fallbackFocusRequester,
            fallbackIndex = target,
            lazyIndexOffset = 1,
            onRestoreFinished = {
                isRestoringFocus = false
            },
        )
    }

    fun requestHeaderFocus() {
        restoreFocusJob?.cancel()
        isRestoringFocus = false
        runCatching { headerFocusRequester.requestFocus() }
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) -> onScrollPositionChanged(index, offset) }
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
                .focusRequester(gridFocusRequester)
                .tvFocusRestorer(fallback = gridFallbackFocusRequester())
                .onFocusChanged { state ->
                    val hadFocus = gridHasFocus
                    gridHasFocus = state.hasFocus
                    if (!state.hasFocus) {
                        isRestoringFocus = false
                    }
                    if (state.isFocused && !hadFocus && !isRestoringFocus) {
                        if (firstEntry) {
                            requestHeaderFocus()
                        } else if (animes.isNotEmpty()) {
                            requestAnimeFocus(restoreTargetIndex(), headerFocusRequester)
                        }
                    }
                }
                .focusable(),
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
                val stableOnClick = remember(anime.id, index) {
                    {
                        rememberFocusedAnime(index)
                        onAnimeSelected(anime.id)
                    }
                }
                val stableOnFocused = remember(anime.id, index) { { rememberFocusedAnime(index) } }
                CollectionAnimeCard(
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .focusProperties {
                            if (index % gridColumnCount == 0) {
                                mainMenuFocusRequester?.let { left = it }
                            }
                            if (index < gridColumnCount) {
                                up = headerFocusRequester
                            }
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
