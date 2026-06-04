package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.feature.collection.R

@Composable
internal fun CollectionGridPane(
    collection: CollectionDetail?,
    isLoading: Boolean,
    error: String?,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    onAnimeSelected: (Int) -> Unit,
    onItemFocused: (Int) -> Unit,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit,
    onRetry: () -> Unit,
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
                focusedItemId = focusedItemId,
                focusedPreview = focusedPreview,
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                onAnimeSelected = onAnimeSelected,
                onItemFocused = onItemFocused,
                onScrollPositionChanged = onScrollPositionChanged,
            )
        }
    }
}

@Composable
private fun CollectionGrid(
    collection: CollectionDetail,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    onAnimeSelected: (Int) -> Unit,
    onItemFocused: (Int) -> Unit,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
    )
    val scope = rememberCoroutineScope()
    val animes = collection.animes

    val headerFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember(animes.size) { List(animes.size) { FocusRequester() } }
    val gridHasFocus = remember { mutableStateOf(false) }
    val isRestoringFocus = remember { mutableStateOf(false) }
    // сбрасывается при смене коллекции — при первом входе фокус на заголовке
    var firstEntry by rememberSaveable(collection.id) { mutableStateOf(true) }
    var lastFocusedIndex by rememberSaveable(collection.id) {
        val idx = focusedItemId?.let { id -> animes.indexOfFirst { it.id == id } }
            ?.coerceAtLeast(0) ?: 0
        mutableIntStateOf(idx)
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) -> onScrollPositionChanged(index, offset) }
    }

    // Lift the focused card's row to the top once focus settles (+1 accounts for
    // the header item). A cancellable effect keeps the focused row pinned so the
    // row below stays composed and DPAD-down preserves the column.
    LaunchedEffect(lastFocusedIndex, gridHasFocus.value) {
        if (gridHasFocus.value && !isRestoringFocus.value && !firstEntry && animes.isNotEmpty()) {
            gridState.animateScrollToItem(lastFocusedIndex.coerceIn(0, animes.lastIndex) + 1)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 172.dp),
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged { state ->
                val hadFocus = gridHasFocus.value
                gridHasFocus.value = state.hasFocus
                if (!state.hasFocus) {
                    isRestoringFocus.value = false
                }
                if (state.hasFocus && !hadFocus && animes.isNotEmpty()) {
                    isRestoringFocus.value = true
                    scope.launch {
                        if (firstEntry) {
                            gridState.scrollToItem(0)
                            snapshotFlow {
                                gridState.layoutInfo.visibleItemsInfo.any { it.index == 0 }
                            }.first { it }
                            runCatching { headerFocusRequester.requestFocus() }
                        } else {
                            val target = lastFocusedIndex.coerceIn(0, animes.lastIndex)
                            gridState.scrollToItem(target + 1)
                            snapshotFlow {
                                gridState.layoutInfo.visibleItemsInfo.any { it.index == target + 1 }
                            }.first { it }
                            runCatching { focusRequesters[target].requestFocus() }
                        }
                        isRestoringFocus.value = false
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CollectionHeader(
                collection = collection,
                modifier = Modifier
                    .focusRequester(headerFocusRequester)
                    .focusProperties { if (focusRequesters.isNotEmpty()) down = focusRequesters[0] },
            )
        }

        itemsIndexed(animes, key = { _, anime -> anime.id }) { index, anime ->
            val stableOnClick = remember(anime.id) { { onAnimeSelected(anime.id) } }
            val stableOnFocused = remember(anime.id) { { onItemFocused(anime.id) } }
            CollectionAnimeCard(
                modifier = Modifier
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { state ->
                        if (state.hasFocus && !isRestoringFocus.value) {
                            firstEntry = false
                            lastFocusedIndex = index
                        }
                    },
                item = anime,
                screenshotUrls = if (anime.id == focusedItemId) focusedPreview?.screenshotUrls.orEmpty() else emptyList(),
                onClick = stableOnClick,
                onFocused = stableOnFocused,
            )
        }
    }
}
