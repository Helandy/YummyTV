package su.afk.yummy.tv.feature.top100

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.top100.AnimeTopItem
import su.afk.yummy.tv.domain.top100.AnimeTopType
import su.afk.yummy.tv.feature.top100.view.Top100Content

@Composable
fun Top100TvScreen(
    state: Top100State.State,
    effect: Flow<Top100State.Effect>,
    onEvent: (Top100State.Event) -> Unit,
) {
    val onItemSelected = remember(onEvent) { { item: AnimeTopItem -> onEvent(Top100State.Event.AnimeSelected(item.id)) } }
    val onTypeSelected = remember(onEvent) { { type: AnimeTopType -> onEvent(Top100State.Event.TypeSelected(type)) } }
    val onItemFocused = remember(onEvent) { { animeId: Int -> onEvent(Top100State.Event.ItemFocused(animeId)) } }
    val onLoadMore = remember(onEvent) { { onEvent(Top100State.Event.LoadMore) } }

    Top100Content(
        items = state.items,
        selectedType = state.selectedType,
        isLoading = state.isLoading,
        isLoadingMore = state.isLoadingMore,
        canLoadMore = state.canLoadMore,
        error = state.error,
        focusedItemId = state.focusedItemId,
        focusedPreview = state.focusedPreview,
        onItemSelected = onItemSelected,
        onTypeSelected = onTypeSelected,
        onItemFocused = onItemFocused,
        onLoadMore = onLoadMore,
    )
}
