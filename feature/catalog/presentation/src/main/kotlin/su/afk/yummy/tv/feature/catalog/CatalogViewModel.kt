package su.afk.yummy.tv.feature.catalog

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SearchSettingsStore
import su.afk.yummy.tv.core.preferences.settings.contentLanguageChanges
import su.afk.yummy.tv.core.utils.paging.OffsetPage
import su.afk.yummy.tv.core.utils.paging.OffsetPagingSource
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.model.SearchSort
import su.afk.yummy.tv.domain.catalog.usecase.GetRandomAnimeUseCase
import su.afk.yummy.tv.domain.catalog.usecase.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.domain.catalog.usecase.SearchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.catalog.mapper.toLastSearchSnapshot
import su.afk.yummy.tv.feature.catalog.mapper.toSearchFilters
import su.afk.yummy.tv.feature.catalog.utils.normalizedYears
import su.afk.yummy.tv.feature.catalog.utils.toggle
import su.afk.yummy.tv.feature.catalog.utils.toggleSingle
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CatalogViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getSearchFilterOptions: GetSearchFilterOptionsUseCase,
    private val getRandomAnime: GetRandomAnimeUseCase,
    private val search: SearchUseCase,
    private val analytics: CatalogAnalytics,
    private val searchSettings: SearchSettingsStore,
    private val accountSettingsStore: YaniAccountSettingsStore,
    appearanceSettingsStore: AppearanceSettingsStore,
) : BaseViewModel<CatalogState.State, CatalogState.Event, CatalogState.Effect>() {

    override fun createInitialState() = CatalogState.State()

    private var searchJob: Job? = null

    private companion object {
        val DEBOUNCE = 800.milliseconds
        const val PAGE_SIZE = 20
    }

    init {
        analytics.eventScreenOpened()
        loadFilterOptions()
        restoreLastSearchOrShowCatalog()
        // Названия фильтров и результаты приходят на языке контента: при его смене грузим заново.
        appearanceSettingsStore.showCatalogTitleYear
            .onEach { showTitleYear -> setState { copy(showTitleYear = showTitleYear) } }
            .launchIn(viewModelScope)
        accountSettingsStore.contentLanguageChanges()
            .onEach {
                loadFilterOptions()
                if (currentState.isSearchActive) setSearchResults(currentState.query, currentState.filters)
            }
            .launchIn(viewModelScope)
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            setState { copy(isLoadingFilterOptions = true) }
            runSuspendCatching { getSearchFilterOptions() }.onSuccess { options ->
                setState { copy(filterOptions = options, isLoadingFilterOptions = false) }
            }.onFailure {
                setState { copy(isLoadingFilterOptions = false) }
            }
        }
    }

    /** Без сохранённого поиска экран открывается каталогом по топу, а не пустым. */
    private fun restoreLastSearchOrShowCatalog() {
        viewModelScope.launch {
            val snapshot = searchSettings.lastSearchSnapshot.first()
            if (!searchSettings.saveLastSearchEnabled.first() || snapshot.isEmpty) {
                if (currentState.query.isBlank() && !currentState.isSearchActive) {
                    setSearchResults("", currentState.filters)
                }
                return@launch
            }
            val query = snapshot.query
            val filters = snapshot.toSearchFilters()
            setState { copy(query = query, filters = filters, draftFilters = filters) }
            setSearchResults(query, filters)
        }
    }

    override fun onEvent(event: CatalogState.Event) {
        when (event) {
            is CatalogState.Event.QueryChanged -> onQueryChanged(event.query)
            is CatalogState.Event.ExternalSearchSubmitted -> onExternalSearchSubmitted(event.query)
            is CatalogState.Event.ItemSelected -> {
                analytics.eventAnimeSelected(event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            CatalogState.Event.SearchSubmitted -> onSearchSubmitted()
            CatalogState.Event.RetrySelected -> analytics.eventRetry()
            CatalogState.Event.RandomAnimeSelected -> openRandomAnime()
            CatalogState.Event.OpenFilters -> {
                analytics.eventFiltersOpened()
                setState { copy(isFilterPanelOpen = true, draftFilters = filters) }
            }

            CatalogState.Event.CloseFilters -> {
                analytics.eventFiltersClosed()
                setState { copy(isFilterPanelOpen = false, draftFilters = filters) }
            }

            CatalogState.Event.ApplyFilters -> applyFilters()
            CatalogState.Event.ResetFilters -> resetFilters()
            CatalogState.Event.ResetDraftFilters -> resetDraftFilters()
            is CatalogState.Event.GenreToggled -> updateDraft {
                copy(
                    genres = genres.toggle(event.id),
                    excludedGenres = excludedGenres - event.id,
                )
            }

            is CatalogState.Event.ExcludedGenreToggled -> updateDraft {
                copy(
                    excludedGenres = excludedGenres.toggle(event.id),
                    genres = genres - event.id,
                )
            }

            // Тип, статус, сезон и возраст — одиночный выбор, как на сайте
            is CatalogState.Event.TypeToggled -> updateDraft { copy(types = types.toggleSingle(event.id)) }
            is CatalogState.Event.StatusToggled -> updateDraft { copy(statuses = statuses.toggleSingle(event.id)) }
            is CatalogState.Event.SeasonToggled -> updateDraft { copy(seasons = seasons.toggleSingle(event.id)) }
            is CatalogState.Event.AgeRatingToggled -> updateDraft {
                copy(ageRatings = ageRatings.toggleSingle(event.value))
            }

            is CatalogState.Event.FromYearChanged -> updateDraft { copy(fromYear = event.year) }
            is CatalogState.Event.ToYearChanged -> updateDraft { copy(toYear = event.year) }
            is CatalogState.Event.SortSelected -> updateDraft {
                copy(
                    sort = event.sort,
                    sortForward = event.sort.defaultForward,
                )
            }

            CatalogState.Event.SortDirectionToggled -> updateDraft { copy(sortForward = !sortForward) }
        }
    }

    private fun openRandomAnime() {
        if (currentState.isRandomAnimeLoading) return
        analytics.eventRandomAnimeRequested()
        viewModelScope.launch {
            setState { copy(isRandomAnimeLoading = true) }
            try {
                val anime = getRandomAnime()
                    ?: error("Random anime response is empty")
                analytics.eventRandomAnimeOpened(anime.id)
                nav.navigate(detailsNavigator.getDetailsDest(anime.id))
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                analytics.eventRandomAnimeLoadError(error)
                throw error
            } finally {
                setState { copy(isRandomAnimeLoading = false) }
            }
        }
    }

    override fun onRetry() {
        openRandomAnime()
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
        if (normalizedQuery.isNotBlank()) analytics.eventExternalSearchSubmitted()
        setSearchResults(normalizedQuery, SearchFilters.EMPTY)
    }

    private fun onQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            // Очистили поле — сразу возвращаем каталог, без ожидания debounce.
            setState { copy(query = query) }
            setSearchResults("", currentState.filters)
            return
        }
        setState {
            copy(
                query = query,
                results = flowOf(PagingData.empty()),
                isSearchActive = false,
                isSearchPending = true,
            )
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE)
            setSearchResults(query.trim(), currentState.filters)
        }
    }

    private fun onSearchSubmitted() {
        val query = currentState.query.trim()
        val filters = currentState.filters
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
        setSearchResults(query, SearchFilters.EMPTY)
    }

    private fun resetDraftFilters() {
        setState { copy(draftFilters = SearchFilters.EMPTY) }
    }

    private fun updateDraft(update: SearchFilters.() -> SearchFilters) {
        setState { copy(draftFilters = draftFilters.update()) }
    }

    private fun setSearchResults(query: String, filters: SearchFilters) {
        persistLastSearchIfEnabled(query, filters)
        val pagingFlow = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                OffsetPagingSource(itemKey = { it.id }) { limit, offset ->
                    loadSearchPage(query, filters, limit, offset)
                }
            },
        ).flow.cachedIn(viewModelScope)
        setState { copy(results = pagingFlow, isSearchActive = true, isSearchPending = false) }
    }

    private suspend fun loadSearchPage(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): OffsetPage<SearchItem> =
        runSuspendCatching {
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

    private fun persistLastSearchIfEnabled(query: String, filters: SearchFilters) {
        viewModelScope.launch {
            if (searchSettings.saveLastSearchEnabled.first()) {
                searchSettings.setLastSearchSnapshot(filters.toLastSearchSnapshot(query))
            }
        }
    }
}
