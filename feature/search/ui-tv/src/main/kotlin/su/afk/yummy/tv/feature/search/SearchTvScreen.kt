package su.afk.yummy.tv.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.search.SearchItem
import su.afk.yummy.tv.feature.search.view.SearchContent

@Composable
fun SearchTvScreen(
    state: SearchState.State,
    effect: Flow<SearchState.Effect>,
    onEvent: (SearchState.Event) -> Unit,
) {
    val onQueryChanged = remember(onEvent) { { q: String -> onEvent(SearchState.Event.QueryChanged(q)) } }
    val onSearchSubmitted = remember(onEvent) { { onEvent(SearchState.Event.SearchSubmitted) } }
    val onItemSelected = remember(onEvent) { { item: SearchItem -> onEvent(SearchState.Event.ItemSelected(item.id)) } }
    val onItemFocused = remember(onEvent) { { animeId: Int -> onEvent(SearchState.Event.ItemFocused(animeId)) } }
    val onLoadMore = remember(onEvent) { { onEvent(SearchState.Event.LoadMore) } }

    SearchContent(
        query = state.query,
        items = state.items,
        isLoading = state.isLoading,
        canLoadMore = state.canLoadMore,
        focusedItemId = state.focusedItemId,
        focusedPreview = state.focusedPreview,
        onQueryChanged = onQueryChanged,
        onSearchSubmitted = onSearchSubmitted,
        onItemSelected = onItemSelected,
        onItemFocused = onItemFocused,
        onLoadMore = onLoadMore,
    )
}
