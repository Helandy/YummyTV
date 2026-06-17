package su.afk.yummy.tv.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.feature.collection.view.CollectionGridPane

@Composable
fun CollectionTvScreen(
    state: CollectionState.State,
    effect: Flow<CollectionState.Effect>,
    onEvent: (CollectionState.Event) -> Unit,
) {
    val onAnimeSelected =
        remember(onEvent) { { animeId: Int -> onEvent(CollectionState.Event.AnimeSelected(animeId)) } }
    val onItemFocused =
        remember(onEvent) { { animeId: Int -> onEvent(CollectionState.Event.ItemFocused(animeId)) } }
    val onScrollPositionChanged = remember(onEvent) {
        { index: Int, offset: Int ->
            onEvent(
                CollectionState.Event.GridScrolled(
                    index,
                    offset
                )
            )
        }
    }
    val onRetry = remember(onEvent) { { onEvent(CollectionState.Event.RetrySelected) } }
    val onVote = remember(onEvent) {
        { vote: CollectionVote ->
            onEvent(
                CollectionState.Event.VoteSelected(vote)
            )
        }
    }
    val onFocusedItemRestoreHandled =
        remember(onEvent) { { onEvent(CollectionState.Event.FocusedItemRestoreHandled) } }

    CollectionGridPane(
        collection = state.collection,
        isLoading = state.isLoading,
        isVoteLoading = state.isVoteLoading,
        error = state.error,
        focusedItemId = state.focusedItemId,
        firstVisibleItemIndex = state.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset,
        onAnimeSelected = onAnimeSelected,
        onItemFocused = onItemFocused,
        onScrollPositionChanged = onScrollPositionChanged,
        onVote = onVote,
        restoreFocusedItemOnEnter = state.restoreFocusedItemOnEnter,
        onRetry = onRetry,
        onFocusedItemRestoreHandled = onFocusedItemRestoreHandled,
    )
}
