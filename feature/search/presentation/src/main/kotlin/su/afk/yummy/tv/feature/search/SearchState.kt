package su.afk.yummy.tv.feature.search

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.AnimePreview
import su.afk.yummy.tv.domain.search.SearchFilterOptions
import su.afk.yummy.tv.domain.search.SearchFilters
import su.afk.yummy.tv.domain.search.SearchItem
import su.afk.yummy.tv.domain.search.SearchSort

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
        val filters: SearchFilters = SearchFilters.EMPTY,
        val draftFilters: SearchFilters = SearchFilters.EMPTY,
        val filterOptions: SearchFilterOptions = SearchFilterOptions(),
        val isFilterPanelOpen: Boolean = false,
        val isLoadingFilterOptions: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data class ItemSelected(val animeId: Int) : Event
        data class ItemFocused(val animeId: Int) : Event
        data object SearchSubmitted : Event
        data object LoadMore : Event
        data object OpenFilters : Event
        data object CloseFilters : Event
        data object ApplyFilters : Event
        data object ResetFilters : Event
        data class GenreToggled(val id: String) : Event
        data class ExcludedGenreToggled(val id: String) : Event
        data class TypeToggled(val id: String) : Event
        data class StatusToggled(val id: String) : Event
        data class SeasonToggled(val id: String) : Event
        data class AgeRatingToggled(val value: Int) : Event
        data class FromYearChanged(val year: Int?) : Event
        data class ToYearChanged(val year: Int?) : Event
        data class SortSelected(val sort: SearchSort) : Event
        data object SortDirectionToggled : Event
    }

    sealed interface Effect : UiEffect
}
