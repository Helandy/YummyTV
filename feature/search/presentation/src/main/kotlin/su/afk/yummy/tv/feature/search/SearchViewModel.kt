package su.afk.yummy.tv.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.usecase.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.domain.search.usecase.SearchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SearchViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getSearchFilterOptions: GetSearchFilterOptionsUseCase,
    private val search: SearchUseCase,
    private val analytics: SearchAnalytics,
) : BaseViewModelNew<SearchState.State, SearchState.Event, SearchState.Effect>(savedStateHandle) {

    override fun createInitialState() = SearchState.State()

    private var searchJob: Job? = null

    private companion object {
        val DEBOUNCE = 3.seconds
        const val PAGE_SIZE = 20
    }

    init {
        analytics.eventScreenOpened()
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
                analytics.eventAnimeSelected(event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            SearchState.Event.SearchSubmitted -> onSearchSubmitted()
            SearchState.Event.RetrySelected -> analytics.eventRetry()
            SearchState.Event.BackSelected -> nav.back()
            SearchState.Event.OpenFilters -> {
                analytics.eventFiltersOpened()
                setState { copy(isFilterPanelOpen = true, draftFilters = filters) }
            }

            SearchState.Event.CloseFilters -> {
                analytics.eventFiltersClosed()
                setState { copy(isFilterPanelOpen = false, draftFilters = filters) }
            }

            SearchState.Event.ApplyFilters -> applyFilters()
            SearchState.Event.ResetFilters -> resetFilters()
            SearchState.Event.ResetDraftFilters -> resetDraftFilters()
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
            is SearchState.Event.StatusToggled -> updateDraft {
                copy(
                    statuses = statuses.toggle(
                        event.id
                    )
                )
            }

            is SearchState.Event.SeasonToggled -> updateDraft { copy(seasons = seasons.toggle(event.id)) }
            is SearchState.Event.AgeRatingToggled -> updateDraft {
                copy(
                    ageRatings = ageRatings.toggle(
                        event.value
                    )
                )
            }

            is SearchState.Event.FromYearChanged -> updateDraft { copy(fromYear = event.year) }
            is SearchState.Event.ToYearChanged -> updateDraft { copy(toYear = event.year) }
            is SearchState.Event.SortSelected -> updateDraft { copy(sort = event.sort) }
            SearchState.Event.SortDirectionToggled -> updateDraft { copy(sortForward = !sortForward) }
        }
    }

    private fun onExternalSearchSubmitted(query: String) {
        val normalizedQuery = query.trim()
        searchJob?.cancel()
        setState {
            copy(
                query = normalizedQuery,
                filters = SearchFilters.EMPTY,
                draftFilters = SearchFilters.EMPTY,
                isFilterPanelOpen = false,
            )
        }
        if (normalizedQuery.isBlank()) {
            setEmptyResults()
            return
        }
        analytics.eventExternalSearchSubmitted()
        setSearchResults(normalizedQuery, SearchFilters.EMPTY)
    }

    private fun onQueryChanged(query: String) {
        searchJob?.cancel()
        setState {
            copy(
                query = query,
                results = flowOf(PagingData.empty()),
                isSearchActive = false,
            )
        }
        if (query.isBlank() && currentState.filters.isEmpty) {
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE)
            setSearchResults(query.trim(), currentState.filters)
        }
    }

    private fun onSearchSubmitted() {
        val query = currentState.query.trim()
        val filters = currentState.filters
        if (query.isBlank() && filters.isEmpty) return
        analytics.eventManualSearchSubmitted(
            hasQuery = query.isNotBlank(),
            filterCount = filters.activeCount,
        )
        searchJob?.cancel()
        setState { copy(query = query) }
        setSearchResults(query, filters)
    }

    private fun applyFilters() {
        val query = currentState.query
        val filters = currentState.draftFilters.normalizedYears()
        if (filters == currentState.filters) {
            setState { copy(isFilterPanelOpen = false, draftFilters = filters) }
            return
        }
        analytics.eventFiltersApplied(
            hasQuery = query.isNotBlank(),
            filterCount = filters.activeCount,
        )
        setState {
            copy(
                filters = filters,
                draftFilters = filters,
                isFilterPanelOpen = false,
            )
        }
        searchJob?.cancel()
        if (query.isBlank() && filters.isEmpty) {
            setEmptyResults()
            return
        }
        setSearchResults(query, filters)
    }

    private fun resetFilters() {
        val query = currentState.query
        analytics.eventFiltersReset(
            hasQuery = query.isNotBlank(),
            filterCount = currentState.filters.activeCount,
        )
        setState {
            copy(
                filters = SearchFilters.EMPTY,
                draftFilters = SearchFilters.EMPTY,
            )
        }
        searchJob?.cancel()
        if (query.isBlank()) {
            setEmptyResults()
            return
        }
        setSearchResults(query, SearchFilters.EMPTY)
    }

    private fun resetDraftFilters() {
        setState { copy(draftFilters = SearchFilters.EMPTY) }
    }

    private fun updateDraft(update: SearchFilters.() -> SearchFilters) {
        setState { copy(draftFilters = draftFilters.update()) }
    }

    private fun setEmptyResults() {
        setState {
            copy(
                results = flowOf(PagingData.empty()),
                isSearchActive = false,
            )
        }
    }

    private fun setSearchResults(query: String, filters: SearchFilters) {
        if (query.isBlank() && filters.isEmpty) {
            setEmptyResults()
            return
        }
        val pagingFlow = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                OffsetPagingSource { limit, offset ->
                    loadSearchPage(query, filters, limit, offset)
                }
            },
        ).flow.cachedIn(viewModelScope)
        setState { copy(results = pagingFlow, isSearchActive = true) }
    }

    private suspend fun loadSearchPage(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): OffsetPage<SearchItem> =
        runCatching {
            search(query, filters, limit, offset)
        }.fold(
            onSuccess = { page ->
                OffsetPage(
                    items = page.items,
                    nextOffset = page.nextOffset,
                    canLoadMore = page.canLoadMore,
                )
            },
            onFailure = { error ->
                analytics.eventLoadError(query, filters, offset, error)
                throw error
            },
        )

    private fun SearchFilters.normalizedYears(): SearchFilters {
        val from = fromYear
        val to = toYear
        return if (from != null && to != null && from > to) {
            copy(fromYear = to, toYear = from)
        } else {
            this
        }
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> =
        if (value in this) this - value else this + value
}
