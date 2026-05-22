package su.afk.yummy.tv.feature.search

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.AnimePreview
import su.afk.yummy.tv.domain.search.SearchItem

class SearchState {
    data class State(
        val query: String = "",
        val items: List<SearchItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val canLoadMore: Boolean = false,
        val offset: Int = 0,
        val focusedItemId: Int? = null,
        val focusedPreview: AnimePreview? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data class ItemSelected(val animeId: Int) : Event
        data class ItemFocused(val animeId: Int) : Event
        data object SearchSubmitted : Event
        data object LoadMore : Event
    }

    sealed interface Effect : UiEffect
}
