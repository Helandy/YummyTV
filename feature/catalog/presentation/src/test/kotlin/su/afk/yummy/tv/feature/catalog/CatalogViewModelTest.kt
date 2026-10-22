package su.afk.yummy.tv.feature.catalog

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.settings.LastSearchSnapshot
import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SearchSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.catalog.model.SearchFilterOptions
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.model.SearchSort
import su.afk.yummy.tv.domain.catalog.repository.SearchHistoryRepository
import su.afk.yummy.tv.domain.catalog.repository.SearchRepository
import su.afk.yummy.tv.domain.catalog.usecase.AddSearchHistoryQueryUseCase
import su.afk.yummy.tv.domain.catalog.usecase.ClearSearchHistoryUseCase
import su.afk.yummy.tv.domain.catalog.usecase.GetRandomAnimeUseCase
import su.afk.yummy.tv.domain.catalog.usecase.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.domain.catalog.usecase.ObserveSearchHistoryUseCase
import su.afk.yummy.tv.domain.catalog.usecase.SearchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.catalog.CatalogState.Event
import su.afk.yummy.tv.feature.catalog.navigator.CatalogSearchOpener

class CatalogViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val repository: SearchRepository = mockk()
    private val analytics: CatalogAnalytics = mockk(relaxed = true)
    private val searchSettings: SearchSettingsStore = mockk()
    private val saveLastSearch = MutableStateFlow(false)
    private val lastSnapshot = MutableStateFlow(LastSearchSnapshot())
    private val showTitleYear = MutableStateFlow(false)
    private val titleCardLayout = MutableStateFlow(TitleCardLayout.TILE)
    private val detailsKey: NavKey = mockk()
    private val searchOpener = CatalogSearchOpener(nav)
    private val historyRepository: SearchHistoryRepository = mockk()
    private val history = MutableStateFlow<List<String>>(emptyList())

    @Before
    fun setUp() {
        every { searchSettings.saveLastSearchEnabled } returns saveLastSearch
        every { searchSettings.lastSearchSnapshot } returns lastSnapshot
        coEvery { searchSettings.setLastSearchSnapshot(any()) } returns Unit
        every { detailsNavigator.getDetailsDest(any()) } returns detailsKey
        coEvery { repository.getFilterOptions() } returns SearchFilterOptions()
        every { historyRepository.observe() } returns history
        coEvery { historyRepository.update(any()) } answers {
            history.value = firstArg<(List<String>) -> List<String>>().invoke(history.value)
        }
        coEvery { historyRepository.clear() } answers { history.value = emptyList() }
    }

    private fun createViewModel() = CatalogViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getSearchFilterOptions = GetSearchFilterOptionsUseCase(repository),
        getRandomAnime = GetRandomAnimeUseCase(repository),
        search = SearchUseCase(repository),
        analytics = analytics,
        searchSettings = searchSettings,
        accountSettingsStore = mockk<YaniAccountSettingsStore> { every { yaniContentLanguage } returns emptyFlow() },
        appearanceSettingsStore = mockk<AppearanceSettingsStore> {
            every { showCatalogTitleYear } returns showTitleYear
            every { titleCardLayout } returns this@CatalogViewModelTest.titleCardLayout
        },
        searchOpener = searchOpener,
        observeSearchHistory = ObserveSearchHistoryUseCase(historyRepository),
        addSearchHistoryQuery = AddSearchHistoryQueryUseCase(historyRepository),
        clearSearchHistory = ClearSearchHistoryUseCase(historyRepository),
    )

    @Test
    fun `starts with loaded filter options and the catalog`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoadingFilterOptions)
        assertTrue(state.isSearchActive)
        assertEquals("", state.query)
        verify(exactly = 1) { analytics.eventScreenOpened() }
    }

    @Test
    fun `title year follows the appearance setting`() {
        val vm = createViewModel()
        assertFalse(vm.currentState.showTitleYear)

        showTitleYear.value = true

        assertTrue(vm.currentState.showTitleYear)
    }

    @Test
    fun `title card layout follows the appearance setting`() {
        val vm = createViewModel()
        assertEquals(TitleCardLayout.TILE, vm.currentState.titleCardLayout)

        titleCardLayout.value = TitleCardLayout.ROW

        assertEquals(TitleCardLayout.ROW, vm.currentState.titleCardLayout)
    }

    @Test
    fun `failed filter options stop loading`() {
        coEvery { repository.getFilterOptions() } throws IllegalStateException("boom")

        val state = createViewModel().currentState

        assertFalse(state.isLoadingFilterOptions)
    }

    @Test
    fun `last search is restored when enabled`() {
        saveLastSearch.value = true
        lastSnapshot.value = LastSearchSnapshot(query = "naruto", genres = setOf("action"), sortName = "RATING")

        val state = createViewModel().currentState

        assertEquals("naruto", state.query)
        assertEquals(setOf("action"), state.filters.genres)
        assertEquals(SearchSort.RATING, state.filters.sort)
        assertTrue(state.isSearchActive)
    }

    @Test
    fun `last search is ignored when disabled`() {
        lastSnapshot.value = LastSearchSnapshot(query = "naruto")

        val state = createViewModel().currentState

        assertEquals("", state.query)
        assertTrue(state.isSearchActive)
    }

    @Test
    fun `typing waits for the debounce before searching`() {
        val vm = createViewModel()

        vm.setEvent(Event.QueryChanged("naruto"))

        assertEquals("naruto", vm.currentState.query)
        assertFalse(vm.currentState.isSearchActive)
        assertTrue(vm.currentState.isSearchPending)

        testScheduler.advanceTimeBy(801)
        testScheduler.runCurrent()

        assertTrue(vm.currentState.isSearchActive)
        assertFalse(vm.currentState.isSearchPending)
    }

    @Test
    fun `clearing the query before the debounce brings the catalog back`() {
        val vm = createViewModel()
        vm.setEvent(Event.QueryChanged("naruto"))
        testScheduler.advanceTimeBy(300)

        vm.setEvent(Event.QueryChanged(""))
        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()

        assertEquals("", vm.currentState.query)
        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `submit searches the trimmed query immediately`() {
        val vm = createViewModel()
        vm.setEvent(Event.QueryChanged("  naruto "))

        vm.setEvent(Event.SearchSubmitted)

        assertEquals("naruto", vm.currentState.query)
        assertTrue(vm.currentState.isSearchActive)
        verify(exactly = 1) { analytics.eventManualSearchSubmitted(hasQuery = true, filterCount = 0) }
    }

    @Test
    fun `empty submit shows the catalog`() {
        val vm = createViewModel()

        vm.setEvent(Event.SearchSubmitted)

        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `external search resets the filters and searches`() {
        val vm = createViewModel()
        vm.setEvent(Event.OpenFilters)
        vm.setEvent(Event.GenreToggled("action"))
        vm.setEvent(Event.ApplyFilters)

        vm.setEvent(Event.ExternalSearchSubmitted(" bleach "))

        assertEquals("bleach", vm.currentState.query)
        assertTrue(vm.currentState.filters.isEmpty)
        assertTrue(vm.currentState.isSearchActive)
        assertFalse(vm.currentState.isFilterPanelOpen)
    }

    @Test
    fun `blank external search shows the catalog`() {
        val vm = createViewModel()
        vm.setEvent(Event.ExternalSearchSubmitted("bleach"))

        vm.setEvent(Event.ExternalSearchSubmitted("  "))

        assertEquals("", vm.currentState.query)
        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `genre and excluded genre exclude each other in the draft`() {
        val vm = createViewModel()

        vm.setEvent(Event.GenreToggled("action"))
        vm.setEvent(Event.ExcludedGenreToggled("action"))

        assertTrue(vm.currentState.draftFilters.genres.isEmpty())
        assertEquals(setOf("action"), vm.currentState.draftFilters.excludedGenres)

        vm.setEvent(Event.ExcludedGenreToggled("action"))
        assertTrue(vm.currentState.draftFilters.excludedGenres.isEmpty())
    }

    @Test
    fun `draft collects the filter choices without touching the applied filters`() {
        val vm = createViewModel()

        vm.setEvent(Event.TypeToggled("tv"))
        vm.setEvent(Event.StatusToggled("ongoing"))
        vm.setEvent(Event.SeasonToggled("winter"))
        vm.setEvent(Event.AgeRatingToggled(16))
        vm.setEvent(Event.SortSelected(SearchSort.RATING))
        vm.setEvent(Event.SortDirectionToggled)

        val draft = vm.currentState.draftFilters
        assertEquals(setOf("tv"), draft.types)
        assertEquals(setOf("ongoing"), draft.statuses)
        assertEquals(setOf("winter"), draft.seasons)
        assertEquals(setOf(16), draft.ageRatings)
        assertEquals(SearchSort.RATING, draft.sort)
        assertTrue(draft.sortForward)
        assertTrue(vm.currentState.filters.isEmpty)
    }

    @Test
    fun `type status season and age ratings are single choice`() {
        val vm = createViewModel()

        vm.setEvent(Event.TypeToggled("tv"))
        vm.setEvent(Event.TypeToggled("ova"))
        vm.setEvent(Event.StatusToggled("ongoing"))
        vm.setEvent(Event.StatusToggled("released"))
        vm.setEvent(Event.SeasonToggled("winter"))
        vm.setEvent(Event.SeasonToggled("fall"))
        vm.setEvent(Event.AgeRatingToggled(12))
        vm.setEvent(Event.AgeRatingToggled(16))

        val draft = vm.currentState.draftFilters
        assertEquals(setOf("ova"), draft.types)
        assertEquals(setOf("released"), draft.statuses)
        assertEquals(setOf("fall"), draft.seasons)
        assertEquals(setOf(16), draft.ageRatings)
    }

    @Test
    fun `choosing the same value again clears it`() {
        val vm = createViewModel()

        vm.setEvent(Event.TypeToggled("tv"))
        vm.setEvent(Event.TypeToggled("tv"))

        assertTrue(vm.currentState.draftFilters.types.isEmpty())
    }

    @Test
    fun `top ascending is the default sort and not counted as a filter`() {
        val vm = createViewModel()

        val draft = vm.currentState.draftFilters
        assertEquals(SearchSort.TOP, draft.sort)
        assertTrue(draft.sortForward)
        assertEquals(0, draft.activeCount)
    }

    @Test
    fun `direction toggle reverses the default Top and counts as one filter`() {
        val vm = createViewModel()

        vm.setEvent(Event.SortDirectionToggled)

        val draft = vm.currentState.draftFilters
        assertEquals(SearchSort.TOP, draft.sort)
        assertFalse(draft.sortForward)
        assertEquals(1, draft.activeCount)
    }

    @Test
    fun `apply swaps reversed years and closes the panel`() {
        val vm = createViewModel()
        vm.setEvent(Event.OpenFilters)
        vm.setEvent(Event.FromYearChanged(2020))
        vm.setEvent(Event.ToYearChanged(2010))

        vm.setEvent(Event.ApplyFilters)

        assertEquals(2010, vm.currentState.filters.fromYear)
        assertEquals(2020, vm.currentState.filters.toYear)
        assertFalse(vm.currentState.isFilterPanelOpen)
        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `reset draft clears only the draft`() {
        val vm = createViewModel()
        vm.setEvent(Event.GenreToggled("action"))
        vm.setEvent(Event.ApplyFilters)
        vm.setEvent(Event.TypeToggled("tv"))

        vm.setEvent(Event.ResetDraftFilters)

        assertTrue(vm.currentState.draftFilters.isEmpty)
        assertEquals(setOf("action"), vm.currentState.filters.genres)
    }

    @Test
    fun `reset filters without a query shows the catalog`() {
        val vm = createViewModel()
        vm.setEvent(Event.GenreToggled("action"))
        vm.setEvent(Event.ApplyFilters)
        assertTrue(vm.currentState.isSearchActive)

        vm.setEvent(Event.ResetFilters)

        assertTrue(vm.currentState.filters.isEmpty)
        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `search is persisted only when saving is enabled`() {
        saveLastSearch.value = true
        val vm = createViewModel()

        vm.setEvent(Event.QueryChanged("naruto"))
        vm.setEvent(Event.SearchSubmitted)

        coVerify(exactly = 1) { searchSettings.setLastSearchSnapshot(match { it.query == "naruto" }) }
    }

    @Test
    fun `random anime opens its details`() {
        coEvery { repository.getRandomAnime() } returns SearchItem(id = 9, title = "Random", posterUrl = null, rating = null, year = null)
        val vm = createViewModel()

        vm.setEvent(Event.RandomAnimeSelected)

        verify(exactly = 1) { detailsNavigator.getDetailsDest(9) }
        verify(exactly = 1) { nav.navigate(detailsKey) }
        assertFalse(vm.currentState.isRandomAnimeLoading)
    }

    @Test
    fun `empty random response goes to the error handler and stops loading`() {
        coEvery { repository.getRandomAnime() } returns null
        val vm = createViewModel()

        vm.setEvent(Event.RandomAnimeSelected)

        verify(exactly = 1) { errorHandler.parse(any(), true, any(), any()) }
        assertFalse(vm.currentState.isRandomAnimeLoading)
    }

    @Test
    fun `item selection opens details`() {
        val vm = createViewModel()

        vm.setEvent(Event.ItemSelected(5))

        verify(exactly = 1) { detailsNavigator.getDetailsDest(5) }
        verify(exactly = 1) { nav.navigate(detailsKey) }
    }

    @Test
    fun `filters can be applied without changes`() {
        val vm = createViewModel()
        vm.setEvent(Event.OpenFilters)

        vm.setEvent(Event.ApplyFilters)

        assertFalse(vm.currentState.isFilterPanelOpen)
        assertEquals(SearchFilters.EMPTY, vm.currentState.filters)
    }
    @Test
    fun `search focus request from another feature is shown until the screen handles it`() {
        val vm = createViewModel()
        assertFalse(vm.currentState.isSearchFocusRequested)

        searchOpener.openSearch()
        assertTrue(vm.currentState.isSearchFocusRequested)

        vm.setEvent(Event.SearchFocusHandled)
        assertFalse(vm.currentState.isSearchFocusRequested)
    }

    @Test
    fun `submitted and opened queries are remembered newest first`() {
        val vm = createViewModel()
        vm.setEvent(Event.QueryChanged(" naruto "))
        vm.setEvent(Event.SearchSubmitted)
        vm.setEvent(Event.QueryChanged("bleach"))
        vm.setEvent(Event.ItemSelected(1))

        assertEquals(listOf("bleach", "naruto"), vm.currentState.searchHistory)
    }

    @Test
    fun `blank submit is not remembered`() {
        val vm = createViewModel()

        vm.setEvent(Event.SearchSubmitted)

        assertTrue(vm.currentState.searchHistory.isEmpty())
    }

    @Test
    fun `history query is searched right away and moves to the top`() {
        history.value = listOf("naruto", "bleach")
        val vm = createViewModel()

        vm.setEvent(Event.SearchHistorySelected("bleach"))

        assertEquals("bleach", vm.currentState.query)
        assertTrue(vm.currentState.isSearchActive)
        assertEquals(listOf("bleach", "naruto"), vm.currentState.searchHistory)
    }

    @Test
    fun `clear empties the history`() {
        history.value = listOf("naruto")
        val vm = createViewModel()

        vm.setEvent(Event.SearchHistoryCleared)

        assertTrue(vm.currentState.searchHistory.isEmpty())
    }
}
