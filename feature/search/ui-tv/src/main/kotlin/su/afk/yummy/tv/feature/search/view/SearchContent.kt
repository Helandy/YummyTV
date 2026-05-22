package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.anime.AnimePreview
import su.afk.yummy.tv.domain.search.SearchItem
import su.afk.yummy.tv.feature.search.R

@Composable
internal fun SearchContent(
    query: String,
    items: List<SearchItem>,
    isLoading: Boolean,
    canLoadMore: Boolean,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onItemSelected: (SearchItem) -> Unit,
    onItemFocused: (Int) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            canLoadMore && total > 0 && lastVisible >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    val focusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }
    var lastFocusedIndex by rememberSaveable {
        val idx = focusedItemId?.let { id -> items.indexOfFirst { it.id == id } }?.coerceAtLeast(0) ?: 0
        mutableIntStateOf(idx)
    }
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }

    // Lift the focused card's row to the top once focus settles. A cancellable
    // effect keeps the focused row pinned so the row below stays composed and
    // DPAD-down preserves the column.
    LaunchedEffect(lastFocusedIndex, gridHasFocus) {
        if (gridHasFocus && !isRestoringFocus && items.isNotEmpty()) {
            gridState.animateScrollToItem(lastFocusedIndex.coerceIn(0, items.lastIndex))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSearchSubmitted()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TvScreenPadding.Horizontal, vertical = 12.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                items.isEmpty() && isLoading -> TvLoadingScreen()
                items.isEmpty() && query.isNotBlank() -> {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 172.dp),
                        contentPadding = PaddingValues(
                            start = TvScreenPadding.Horizontal,
                            end = TvScreenPadding.Horizontal,
                            top = 8.dp,
                            bottom = TvScreenPadding.Vertical,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged { state ->
                                val hadFocus = gridHasFocus
                                gridHasFocus = state.hasFocus
                                if (!state.hasFocus) {
                                    isRestoringFocus = false
                                }
                                if (state.hasFocus && !hadFocus && items.isNotEmpty()) {
                                    isRestoringFocus = true
                                    scope.launch {
                                        val target = lastFocusedIndex.coerceIn(0, items.lastIndex)
                                        gridState.scrollToItem(target)
                                        snapshotFlow {
                                            gridState.layoutInfo.visibleItemsInfo.any { it.index == target }
                                        }.first { it }
                                        runCatching { focusRequesters[target].requestFocus() }
                                        isRestoringFocus = false
                                    }
                                }
                            }
                            .focusGroup(),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            val stableOnClick = remember(item.id) { { onItemSelected(item) } }
                            val stableOnFocused = remember(item.id) { { onItemFocused(item.id) } }
                            TvTitleCard(
                                title = item.title,
                                posterUrl = item.posterUrl,
                                onClick = stableOnClick,
                                screenshotUrls = if (item.id == focusedItemId) focusedPreview?.screenshotUrls.orEmpty() else emptyList(),
                                onFocused = stableOnFocused,
                                modifier = Modifier
                                    .focusRequester(focusRequesters[index])
                                    .onFocusChanged { state ->
                                        if (state.hasFocus && !isRestoringFocus) {
                                            lastFocusedIndex = index
                                        }
                                    },
                                posterOverlay = item.rating?.let { rating ->
                                    {
                                        Text(
                                            text = "%.2f".format(rating),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
                                        )
                                    }
                                },
                            )
                        }
                        if (isLoading && items.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TvLoadingFooter()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
