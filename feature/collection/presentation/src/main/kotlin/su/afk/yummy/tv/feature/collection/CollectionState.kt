package su.afk.yummy.tv.feature.collection

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.collection.model.CollectionDetail

class CollectionState {
    data class State(
        val isLoading: Boolean = true,
        val collection: CollectionDetail? = null,
        val error: String? = null,
        val focusedItemId: Int? = null,
        val focusedPreview: AnimePreview? = null,
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class AnimeSelected(val animeId: Int) : Event
        data class ItemFocused(val animeId: Int) : Event
        data class GridScrolled(val index: Int, val offset: Int) : Event
    }

    sealed interface Effect : UiEffect
}
