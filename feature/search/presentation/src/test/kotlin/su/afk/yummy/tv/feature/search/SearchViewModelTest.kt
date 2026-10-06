package su.afk.yummy.tv.feature.search

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.settings.LastSearchSnapshot
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SearchSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.domain.search.repository.SearchRepository
import su.afk.yummy.tv.domain.search.usecase.GetRandomAnimeUseCase
import su.afk.yummy.tv.domain.search.usecase.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.domain.search.usecase.SearchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.search.SearchState.Event

class SearchViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val repository: SearchRepository = mockk()
    private val analytics: SearchAnalytics = mockk(relaxed = true)
    private val searchSettings: SearchSettingsStore = mockk()
    private val saveLastSearch = MutableStateFlow(false)
    private val lastSnapshot = MutableStateFlow(LastSearchSnapshot())
    private val detailsKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { searchSettings.saveLastSearchEnabled } returns saveLastSearch
        every { searchSettings.lastSearchSnapshot } returns lastSnapshot
        coEvery { searchSettings.setLastSearchSnapshot(any()) } returns Unit
        every { detailsNavigator.getDetailsDest(any()) } returns detailsKey
        coEvery { repository.getFilterOptions() } returns SearchFilterOptions()
    }

    private fun createViewModel() = SearchViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getSearchFilterOptions = GetSearchFilterOptionsUseCase(repository),
        getRandomAnime = GetRandomAnimeUseCase(repository),
        search = SearchUseCase(repository),
        analytics = analytics,
        searchSettings = searchSettings,
    )

    @Test
    fun `starts with loaded filter options and no search`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoadingFilterOptions)
        assertFalse(state.isSearchActive)
        assertEquals("", state.query)
        verify(exactly = 1) { analytics.eventScreenOpened() }
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
        lastSnapshot.value = LastSearchSnapshot(query = "naruto", genres = setOf("action"), sortName = "YEAR")

        val state = createViewModel().currentState

        assertEquals("naruto", state.query)
        assertEquals(setOf("action"), state.filters.genres)
        assertEquals(SearchSort.YEAR, state.filters.sort)
        assertTrue(state.isSearchActive)
    }

    @Test
    fun `last search is ignored when disabled`() {
        lastSnapshot.value = LastSearchSnapshot(query = "naruto")

        val state = createViewModel().currentState

        assertEquals("", state.query)
        assertFalse(state.isSearchActive)
    }

    @Test
    fun `typing waits for the debounce before searching`() {
        val vm = createViewModel()

        vm.setEvent(Event.QueryChanged("naruto"))

        assertEquals("naruto", vm.currentState.query)
        assertFalse(vm.currentState.isSearchActive)

        testScheduler.advanceTimeBy(3_001)
        testScheduler.runCurrent()

        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `clearing the query before the debounce cancels the search`() {
        val vm = createViewModel()
        vm.setEvent(Event.QueryChanged("naruto"))
        testScheduler.advanceTimeBy(1_000)

        vm.setEvent(Event.QueryChanged(""))
        testScheduler.advanceTimeBy(5_000)
        testScheduler.runCurrent()

        assertFalse(vm.currentState.isSearchActive)
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
    fun `empty submit does nothing`() {
        val vm = createViewModel()

        vm.setEvent(Event.SearchSubmitted)

        assertFalse(vm.currentState.isSearchActive)
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
    fun `blank external search clears the results`() {
        val vm = createViewModel()
        vm.setEvent(Event.ExternalSearchSubmitted("bleach"))

        vm.setEvent(Event.ExternalSearchSubmitted("  "))

        assertFalse(vm.currentState.isSearchActive)
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
        vm.setEvent(Event.SortSelected(SearchSort.YEAR))
        vm.setEvent(Event.SortDirectionToggled)

        val draft = vm.currentState.draftFilters
        assertEquals(setOf("tv"), draft.types)
        assertEquals(setOf("ongoing"), draft.statuses)
        assertEquals(setOf("winter"), draft.seasons)
        assertEquals(setOf(16), draft.ageRatings)
        assertEquals(SearchSort.YEAR, draft.sort)
        assertTrue(draft.sortForward)
        assertTrue(vm.currentState.filters.isEmpty)
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
    fun `reset filters without a query clears the results`() {
        val vm = createViewModel()
        vm.setEvent(Event.GenreToggled("action"))
        vm.setEvent(Event.ApplyFilters)
        assertTrue(vm.currentState.isSearchActive)

        vm.setEvent(Event.ResetFilters)

        assertTrue(vm.currentState.filters.isEmpty)
        assertFalse(vm.currentState.isSearchActive)
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
        coEvery { repository.getRandomAnime() } returns SearchItem(9, "Random", null, null, null)
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
    fun `item selection and back navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.ItemSelected(5))
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { detailsNavigator.getDetailsDest(5) }
        verify(exactly = 1) { nav.navigate(detailsKey) }
        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `filters can be applied without changes`() {
        val vm = createViewModel()
        vm.setEvent(Event.OpenFilters)

        vm.setEvent(Event.ApplyFilters)

        assertFalse(vm.currentState.isFilterPanelOpen)
        assertEquals(SearchFilters.EMPTY, vm.currentState.filters)
    }
}
