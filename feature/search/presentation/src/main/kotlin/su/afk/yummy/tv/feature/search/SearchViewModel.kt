package su.afk.yummy.tv.feature.search

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.anime.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.search.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.domain.search.SearchFilters
import su.afk.yummy.tv.domain.search.SearchPage
import su.afk.yummy.tv.domain.search.SearchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.search.presentation.R
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val search: SearchUseCase,
    private val getSearchFilterOptions: GetSearchFilterOptionsUseCase,
    private val getAnimePreview: GetAnimePreviewUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<SearchState.State, SearchState.Event, SearchState.Effect>(savedStateHandle) {

    override fun createInitialState() = SearchState.State()

    private var searchJob: Job? = null
    private var previewJob: Job? = null

    private companion object {
        const val PAGE_SIZE = 20
        const val DEBOUNCE_MS = 3_000L
    }

    init {
        viewModelScope.launch {
            setState { copy(isLoadingFilterOptions = true) }
            runCatching { getSearchFilterOptions() }.onSuccess { options ->
                setState { copy(filterOptions = options, isLoadingFilterOptions = false) }
            }.onFailure {
                setState { copy(isLoadingFilterOptions = false) }
            }
        }
    }

    override fun onEvent(event: SearchState.Event) {
        when (event) {
            is SearchState.Event.QueryChanged -> onQueryChanged(event.query)
            is SearchState.Event.ItemSelected -> nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            is SearchState.Event.ItemFocused -> onItemFocused(event.animeId)
            SearchState.Event.SearchSubmitted -> onSearchSubmitted()
            SearchState.Event.LoadMore -> loadMore()
            SearchState.Event.OpenFilters -> setState { copy(isFilterPanelOpen = true, draftFilters = filters) }
            SearchState.Event.CloseFilters -> setState { copy(isFilterPanelOpen = false, draftFilters = filters) }
            SearchState.Event.ApplyFilters -> applyFilters()
            SearchState.Event.ResetFilters -> resetFilters()
            is SearchState.Event.GenreToggled -> updateDraft {
                copy(
                    genres = genres.toggle(event.id),
                    excludedGenres = excludedGenres - event.id,
                )
            }
            is SearchState.Event.ExcludedGenreToggled -> updateDraft {
                copy(
                    excludedGenres = excludedGenres.toggle(event.id),
                    genres = genres - event.id,
                )
            }
            is SearchState.Event.TypeToggled -> updateDraft { copy(types = types.toggle(event.id)) }
            is SearchState.Event.StatusToggled -> updateDraft { copy(statuses = statuses.toggle(event.id)) }
            is SearchState.Event.SeasonToggled -> updateDraft { copy(seasons = seasons.toggle(event.id)) }
            is SearchState.Event.AgeRatingToggled -> updateDraft { copy(ageRatings = ageRatings.toggle(event.value)) }
            is SearchState.Event.FromYearChanged -> updateDraft { copy(fromYear = event.year) }
            is SearchState.Event.ToYearChanged -> updateDraft { copy(toYear = event.year) }
            is SearchState.Event.SortSelected -> updateDraft { copy(sort = event.sort) }
            SearchState.Event.SortDirectionToggled -> updateDraft { copy(sortForward = !sortForward) }
        }
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        previewJob?.cancel()
        setState { copy(focusedItemId = animeId, focusedPreview = null) }
        previewJob = viewModelScope.launch {
            delay(600)
            runCatching { getAnimePreview(animeId) }.onSuccess { preview ->
                setState { copy(focusedPreview = preview) }
            }
        }
    }

    private fun onQueryChanged(query: String) {
        setState { copy(query = query, items = emptyList(), offset = 0, error = null, canLoadMore = false) }
        searchJob?.cancel()
        if (query.isBlank() && currentState.filters.isEmpty) {
            setState { copy(isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            load(query, currentState.filters, offset = 0, replace = true)
        }
    }

    private fun onSearchSubmitted() {
        val query = currentState.query
        if (query.isBlank() && currentState.filters.isEmpty) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { load(query, currentState.filters, offset = 0, replace = true) }
    }

    private fun loadMore() {
        val s = currentState
        if (!s.isLoading && s.canLoadMore && (s.query.isNotBlank() || !s.filters.isEmpty)) {
            viewModelScope.launch { load(s.query, s.filters, offset = s.offset, replace = false) }
        }
    }

    private fun applyFilters() {
        val query = currentState.query
        val filters = currentState.draftFilters.normalizedYears()
        setState {
            copy(
                filters = filters,
                draftFilters = filters,
                isFilterPanelOpen = false,
                items = emptyList(),
                offset = 0,
                focusedItemId = null,
                focusedPreview = null,
                canLoadMore = false,
            )
        }
        searchJob?.cancel()
        if (query.isBlank() && filters.isEmpty) {
            setState { copy(isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch { load(query, filters, offset = 0, replace = true) }
    }

    private fun resetFilters() {
        val query = currentState.query
        setState {
            copy(
                filters = SearchFilters.EMPTY,
                draftFilters = SearchFilters.EMPTY,
                items = emptyList(),
                offset = 0,
                focusedItemId = null,
                focusedPreview = null,
                canLoadMore = false,
            )
        }
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch { load(query, SearchFilters.EMPTY, offset = 0, replace = true) }
    }

    private fun updateDraft(update: SearchFilters.() -> SearchFilters) {
        setState { copy(draftFilters = draftFilters.update()) }
    }

    private suspend fun load(query: String, filters: SearchFilters, offset: Int, replace: Boolean) {
        setState { if (replace) copy(isLoading = true, error = null) else copy(isLoading = true) }
        runCatching { loadVisiblePage(query, filters, offset) }.fold(
            onSuccess = { page ->
                setState {
                    copy(
                        isLoading = false,
                        items = if (replace) page.items else items + page.items,
                        offset = page.nextOffset,
                        canLoadMore = page.canLoadMore,
                    )
                }
            },
            onFailure = { e ->
                setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.search_error)) }
            },
        )
    }

    private suspend fun loadVisiblePage(
        query: String,
        filters: SearchFilters,
        offset: Int,
    ): SearchPage {
        var page = search(query, filters, PAGE_SIZE, offset)
        while (page.items.isEmpty() && page.canLoadMore && page.nextOffset > offset) {
            val nextPage = search(query, filters, PAGE_SIZE, page.nextOffset)
            page = page.copy(
                items = nextPage.items,
                nextOffset = nextPage.nextOffset,
                canLoadMore = nextPage.canLoadMore,
            )
        }
        return page
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

    private fun SearchFilters.normalizedYears(): SearchFilters {
        val from = fromYear
        val to = toYear
        return if (from != null && to != null && from > to) {
            copy(fromYear = to, toYear = from)
        } else {
            this
        }
    }
}
