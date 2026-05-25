package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvPosterCardDefaults
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalTopBarFocusRequester
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.feature.library.CollapsedPanelWidth
import su.afk.yummy.tv.feature.library.R

@Composable
internal fun LibraryGrid(
    items: List<LibraryEntry>,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    gridFocusRequester: FocusRequester,
    sidePanelFocusRequester: FocusRequester,
    onAnimeSelected: (Int) -> Unit,
    onItemFocused: (Int) -> Unit,
    onRemoveLibraryEntry: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val focusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }
    val topBarFocusRequester = LocalTopBarFocusRequester.current
    var lastFocusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var pendingFocusAfterDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var leftEdgeIndexes by remember { mutableStateOf(emptySet<Int>()) }

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
        val focusedIndex = items.indexOfFirst { it.animeId == focusedItemId }
        if (focusedIndex >= 0) {
            lastFocusedIndex = focusedIndex
        }
    }

    LaunchedEffect(items.size, pendingFocusAfterDeleteIndex) {
        val pendingIndex = pendingFocusAfterDeleteIndex ?: return@LaunchedEffect
        isRestoringFocus = true
        if (items.isEmpty()) {
            runCatching { topBarFocusRequester?.requestFocus() }
        } else {
            val target = pendingIndex.coerceIn(0, items.lastIndex)
            lastFocusedIndex = target
            gridState.scrollToItem(target)
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.index == target } }
                .first { it }
            runCatching { focusRequesters[target].requestFocus() }
        }
        pendingFocusAfterDeleteIndex = null
        isRestoringFocus = false
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

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 172.dp),
        modifier = modifier
            .fillMaxSize()
            .focusRequester(gridFocusRequester)
            .onFocusChanged { state ->
                val hadFocus = gridHasFocus
                gridHasFocus = state.hasFocus
                if (!state.hasFocus) isRestoringFocus = false
                if (state.hasFocus && !hadFocus && items.isNotEmpty()) {
                    isRestoringFocus = true
                    scope.launch {
                        val target = lastFocusedIndex.coerceIn(0, items.lastIndex)
                        gridState.scrollToItem(target)
                        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.index == target } }
                            .first { it }
                        runCatching { focusRequesters[target].requestFocus() }
                        isRestoringFocus = false
                    }
                }
            }
            .focusGroup(),
        contentPadding = PaddingValues(
            start = CollapsedPanelWidth + TvScreenPadding.Horizontal,
            end = TvScreenPadding.Horizontal,
            top = TvScreenPadding.Vertical,
            bottom = TvScreenPadding.Vertical,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.animeId }) { index, item ->
            var columnHasFocus by remember { mutableStateOf(false) }
            val stableOnClick = remember(item.animeId) { { onAnimeSelected(item.animeId) } }
            val stableOnFocused = remember(item.animeId) { { onItemFocused(item.animeId) } }
            val stableOnDelete = remember(item.animeId, index) {
                {
                    pendingFocusAfterDeleteIndex = index
                    onRemoveLibraryEntry(item.animeId)
                }
            }
            Column(
                modifier = Modifier
                    .width(TvPosterCardDefaults.Width)
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { state ->
                        columnHasFocus = state.hasFocus
                        if (state.hasFocus && gridHasFocus && !isRestoringFocus) {
                            lastFocusedIndex = index
                        }
                    }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TvTitleCard(
                    title = item.title,
                    posterUrl = item.posterUrl,
                    onClick = stableOnClick,
                    onFocused = stableOnFocused,
                    screenshotUrls = if (item.animeId == focusedItemId) focusedPreview?.screenshotUrls.orEmpty() else emptyList(),
                    modifier = Modifier.focusProperties {
                        if (index == 0 || index in leftEdgeIndexes) {
                            left = sidePanelFocusRequester
                        } else {
                            focusRequesters.getOrNull(index - 1)?.let { left = it }
                        }
                        focusRequesters.getOrNull(index + 1)?.let { right = it }
                    },
                )
                if (columnHasFocus) {
                    LibraryDeleteButton(
                        onClick = stableOnDelete,
                        modifier = Modifier
                            .width(TvPosterCardDefaults.Width)
                            .focusProperties {
                                if (index == 0 || index in leftEdgeIndexes) {
                                    left = sidePanelFocusRequester
                                } else {
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
}
