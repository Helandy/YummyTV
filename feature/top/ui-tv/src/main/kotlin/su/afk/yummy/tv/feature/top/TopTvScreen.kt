package su.afk.yummy.tv.feature.top

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.utils.LocalTopTvActiveDestination
import su.afk.yummy.tv.feature.top.view.TopBrowser

@Composable
fun TopTvScreen(
    state: TopState.State,
    effect: Flow<TopState.Effect>,
    onEvent: (TopState.Event) -> Unit,
) {
    val isActiveDestination = LocalTopTvActiveDestination.current
    val onItemSelected =
        remember(onEvent) { { item: AnimeTopItem -> onEvent(TopState.Event.AnimeSelected(item.id)) } }
    val onTypeSelected =
        remember(onEvent) { { type: AnimeTopType -> onEvent(TopState.Event.TypeSelected(type)) } }
    val onLoadMore = remember(onEvent) { { onEvent(TopState.Event.LoadMore) } }

    TopBrowser(
        items = state.items,
        selectedType = state.selectedType,
        isLoading = state.isLoading,
        isLoadingMore = state.isLoadingMore,
        canLoadMore = state.canLoadMore,
        error = state.error,
        isActiveDestination = isActiveDestination,
        onItemSelected = onItemSelected,
        onTypeSelected = onTypeSelected,
        onLoadMore = onLoadMore,
    )
}
