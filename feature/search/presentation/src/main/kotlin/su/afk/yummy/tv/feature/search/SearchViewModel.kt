package su.afk.yummy.tv.feature.search

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.usecase.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.search.handler.AnimePreviewFocusHandler
import su.afk.yummy.tv.feature.search.handler.SearchPagingHandler
import su.afk.yummy.tv.feature.search.handler.SearchPagingRequest
import su.afk.yummy.tv.feature.search.handler.SearchPagingResult
import su.afk.yummy.tv.feature.search.presentation.R
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getSearchFilterOptions: GetSearchFilterOptionsUseCase,
    private val stringProvider: StringProvider,
    private val animePreviewFocusHandler: AnimePreviewFocusHandler,
    private val searchPagingHandler: SearchPagingHandler,
) : BaseViewModelNew<SearchState.State, SearchState.Event, SearchState.Effect>(savedStateHandle) {

    override fun createInitialState() = SearchState.State()

    private companion object {
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
            is SearchState.Event.ExternalSearchSubmitted -> onExternalSearchSubmitted(event.query)
            is SearchState.Event.ItemSelected -> {
                setState { copy(focusedItemId = event.animeId, restoreFocusedItemOnEnter = true) }
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
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
            SearchState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }
        }
    }

    private fun onExternalSearchSubmitted(query: String) {
        val normalizedQuery = query.trim()
        searchPagingHandler.cancel()
        animePreviewFocusHandler.cancelFocus()
        setState {
            copy(
                query = normalizedQuery,
                items = emptyList(),
                offset = 0,
                filters = SearchFilters.EMPTY,
                draftFilters = SearchFilters.EMPTY,
                isFilterPanelOpen = false,
                focusedItemId = null,
                focusedPreview = null,
                restoreFocusedItemOnEnter = false,
                canLoadMore = false,
                error = null,
            )
        }
        if (normalizedQuery.isBlank()) {
            setState { copy(isLoading = false) }
            return
        }
        loadSearch(
            SearchPagingRequest(
                normalizedQuery,
                SearchFilters.EMPTY,
                offset = 0,
                replace = true
            )
        )
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        setState { copy(focusedItemId = animeId, focusedPreview = null) }
        animePreviewFocusHandler.focus(
            scope = viewModelScope,
            animeId = animeId,
            isCurrentFocus = { currentState.focusedItemId == animeId },
            onCachedPreview = { preview, _ -> setState { copy(focusedPreview = preview) } },
            onLoadedPreview = { result ->
                if (result.isCurrentFocus) {
                    setState { copy(focusedPreview = result.preview) }
                }
            }
        )
    }

    private fun onQueryChanged(query: String) {
        searchPagingHandler.cancel()
        animePreviewFocusHandler.cancelFocus()
        setState {
            copy(
                query = query,
                items = emptyList(),
                offset = 0,
                error = null,
                canLoadMore = false,
                focusedItemId = null,
                focusedPreview = null,
                restoreFocusedItemOnEnter = false,
            )
        }
        if (query.isBlank() && currentState.filters.isEmpty) {
            setState { copy(isLoading = false) }
            return
        }
        loadSearch(
            request = SearchPagingRequest(query, currentState.filters, offset = 0, replace = true),
            debounceMs = DEBOUNCE_MS,
        )
    }

    private fun onSearchSubmitted() {
        val query = currentState.query
        if (query.isBlank() && currentState.filters.isEmpty) return
        searchPagingHandler.cancel()
        loadSearch(SearchPagingRequest(query, currentState.filters, offset = 0, replace = true))
    }

    private fun loadMore() {
        val s = currentState
        if (searchPagingHandler.canLoadMore(s.query, s.filters, s.isLoading, s.canLoadMore)) {
            loadSearch(SearchPagingRequest(s.query, s.filters, offset = s.offset, replace = false))
        }
    }

    private fun applyFilters() {
        val query = currentState.query
        val filters = searchPagingHandler.normalizedYears(currentState.draftFilters)
        setState {
            copy(
                filters = filters,
                draftFilters = filters,
                isFilterPanelOpen = false,
                items = emptyList(),
                offset = 0,
                focusedItemId = null,
                focusedPreview = null,
                restoreFocusedItemOnEnter = false,
                canLoadMore = false,
            )
        }
        searchPagingHandler.cancel()
        animePreviewFocusHandler.cancelFocus()
        if (query.isBlank() && filters.isEmpty) {
            setState { copy(isLoading = false) }
            return
        }
        loadSearch(SearchPagingRequest(query, filters, offset = 0, replace = true))
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
                restoreFocusedItemOnEnter = false,
                canLoadMore = false,
            )
        }
        searchPagingHandler.cancel()
        animePreviewFocusHandler.cancelFocus()
        if (query.isBlank()) {
            setState { copy(isLoading = false) }
            return
        }
        loadSearch(SearchPagingRequest(query, SearchFilters.EMPTY, offset = 0, replace = true))
    }

    private fun updateDraft(update: SearchFilters.() -> SearchFilters) {
        setState { copy(draftFilters = draftFilters.update()) }
    }

    private fun loadSearch(
        request: SearchPagingRequest,
        debounceMs: Long = 0L,
    ) {
        searchPagingHandler.load(
            scope = viewModelScope,
            request = request,
            debounceMs = debounceMs,
            onLoading = { loadingRequest ->
                setState {
                    if (loadingRequest.replace) copy(isLoading = true, error = null) else copy(
                        isLoading = true
                    )
                }
            },
            onResult = ::applySearchResult,
        )
    }

    private fun applySearchResult(result: SearchPagingResult) {
        when (result) {
            is SearchPagingResult.Success -> {
                setState {
                    copy(
                        isLoading = false,
                        items = if (result.request.replace) result.page.items else items + result.page.items,
                        offset = result.page.nextOffset,
                        canLoadMore = result.page.canLoadMore,
                    )
                }
            }

            is SearchPagingResult.Failure -> {
                setState {
                    copy(
                        isLoading = false,
                        error = result.error.message ?: stringProvider.get(R.string.search_error),
                    )
                }
            }
        }
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
}
