package su.afk.yummy.tv.feature.top

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.view.TopBrowser

@Composable
fun TopTvScreen(
    state: TopState.State,
    effect: Flow<TopState.Effect>,
    isActiveDestination: Boolean = true,
    onEvent: (TopState.Event) -> Unit,
) {
    val onItemSelected =
        remember(onEvent) { { item: AnimeTopItem -> onEvent(TopState.Event.AnimeSelected(item.id)) } }
    val onTypeSelected =
        remember(onEvent) { { type: AnimeTopType -> onEvent(TopState.Event.TypeSelected(type)) } }
    val onItemFocused =
        remember(onEvent) { { animeId: Int -> onEvent(TopState.Event.ItemFocused(animeId)) } }
    val onFocusedItemRestoreHandled =
        remember(onEvent) { { onEvent(TopState.Event.FocusedItemRestoreHandled) } }
    val onLoadMore = remember(onEvent) { { onEvent(TopState.Event.LoadMore) } }

    TopBrowser(
        items = state.items,
        selectedType = state.selectedType,
        isLoading = state.isLoading,
        isLoadingMore = state.isLoadingMore,
        canLoadMore = state.canLoadMore,
        error = state.error,
        focusedItemId = state.focusedItemId,
        focusedPreview = state.focusedPreview,
        restoreFocusedItemOnEnter = state.restoreFocusedItemOnEnter,
        isActiveDestination = isActiveDestination,
        onItemSelected = onItemSelected,
        onTypeSelected = onTypeSelected,
        onItemFocused = onItemFocused,
        onFocusedItemRestoreHandled = onFocusedItemRestoreHandled,
        onLoadMore = onLoadMore,
    )
}
