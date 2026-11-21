package su.afk.yummy.tv.feature.schedule

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.library.model.FAVORITE_ONLY_LIBRARY_LIST_ID
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryAnimeIdsUseCase
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator

class ScheduleViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private lateinit var repository: AnimeScheduleRepository
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var libraryFlow: MutableStateFlow<List<LibraryItem>>
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val appClock: AppClock = mockk()

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        repository = mockk()
        libraryFlow = MutableStateFlow(emptyList())
        libraryRepository = mockk {
            every { observeAll() } returns libraryFlow
        }
        every { appClock.nowMillis() } returns 1_700_000_000_000L
        with(detailsNavigator) {
            every { getDetailsDest(any()) } answers { DetailsKey(firstArg()) }
        }
    }

    private fun createViewModel() = ScheduleViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        getSchedule = GetAnimeScheduleUseCase(repository),
        observeLibraryAnimeIds = ObserveLibraryAnimeIdsUseCase(libraryRepository),
        nav = nav,
        detailsNavigator = detailsNavigator,
        analytics = ScheduleAnalytics(tracker),
        accountSettingsStore = mockk<YaniAccountSettingsStore> { every { yaniContentLanguage } returns emptyFlow() },
        appClock = appClock,
    )

    private fun givenSchedule(vararg days: AnimeScheduleDay) {
        coEvery { repository.getSchedule() } returns days.toList()
    }

    @Test
    fun `loads the schedule on creation`() {
        givenSchedule(day())

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf(day()), state.days)
        assertEquals(1, state.tvSchedule.dayGroups.size)
    }

    @Test
    fun `load failure exposes the parsed message`() {
        coEvery { repository.getSchedule() } throws IllegalStateException("boom")

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("boom", (state.error as UiText.Failure).cause.message)
    }

    @Test
    fun `retry reloads and clears the error`() {
        coEvery { repository.getSchedule() } throws IllegalStateException("boom") andThen listOf(day())
        val vm = createViewModel()

        vm.setEvent(ScheduleState.Event.RetrySelected)

        coVerify(exactly = 2) { repository.getSchedule() }
        assertNull(vm.currentState.error)
        assertEquals(1, vm.currentState.days.size)
    }

    @Test
    fun `failed refresh keeps the shown days without an error`() {
        coEvery { repository.getSchedule() } returns listOf(day()) andThenThrows IllegalStateException("boom")
        val vm = createViewModel()

        vm.setEvent(ScheduleState.Event.RefreshRequested)

        assertFalse(vm.currentState.isRefreshing)
        assertNull(vm.currentState.error)
        assertEquals(1, vm.currentState.days.size)
    }

    @Test
    fun `refresh replaces the days`() {
        coEvery { repository.getSchedule() } returns listOf(day()) andThen listOf(day(), day(epochSeconds = DAY_2))
        val vm = createViewModel()

        vm.setEvent(ScheduleState.Event.RefreshRequested)

        assertFalse(vm.currentState.isRefreshing)
        assertEquals(2, vm.currentState.days.size)
    }

    @Test
    fun `selecting an available date updates the timeline`() {
        givenSchedule(day(), day(epochSeconds = DAY_2))
        val vm = createViewModel()
        val days = vm.currentState.tvSchedule.availableEpochDays
        assertEquals(2, days.size)

        vm.setEvent(ScheduleState.Event.DateSelected(days.last()))

        assertEquals(days.last(), vm.currentState.tvSchedule.selectedEpochDay)
    }

    @Test
    fun `selecting an unknown date keeps the current one`() {
        givenSchedule(day())
        val vm = createViewModel()
        val before = vm.currentState.tvSchedule.selectedEpochDay

        vm.setEvent(ScheduleState.Event.DateSelected(-1L))

        assertEquals(before, vm.currentState.tvSchedule.selectedEpochDay)
    }

    @Test
    fun `selecting an anime opens its details`() {
        givenSchedule()
        createViewModel().setEvent(ScheduleState.Event.AnimeSelected(42))

        verify(exactly = 1) { nav.navigate(DetailsKey(42)) }
    }

    @Test
    fun `back leaves the screen`() {
        givenSchedule()
        createViewModel().setEvent(ScheduleState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `yesterday release adds a yesterday tab without selecting it`() {
        givenSchedule(day(previousEpochSeconds = NOW - 86_400L))

        val timeline = createViewModel().currentState.tvSchedule

        assertEquals(2, timeline.dayGroups.size)
        assertEquals(timeline.availableEpochDays.last(), timeline.selectedEpochDay)
    }

    @Test
    fun `releases older than yesterday are dropped`() {
        givenSchedule(day(previousEpochSeconds = NOW - 2 * 86_400L))

        assertEquals(1, createViewModel().currentState.tvSchedule.dayGroups.size)
    }

    @Test
    fun `placeholder previous date of an announcement is ignored`() {
        givenSchedule(day(previousEpochSeconds = NOW - 86_400L, airedEpisodes = null))

        assertEquals(1, createViewModel().currentState.tvSchedule.dayGroups.size)
    }

    @Test
    fun `library filter narrows schedule to watching, planned, and favorites without extra network calls`() {
        libraryFlow.value = listOf(
            LibraryItem(animeId = 2, title = "watching", listId = 0),
            LibraryItem(animeId = 3, title = "fav", listId = FAVORITE_ONLY_LIBRARY_LIST_ID, isFavorite = true),
            LibraryItem(animeId = 4, title = "planned", listId = 1),
            LibraryItem(animeId = 5, title = "completed", listId = 2),
            LibraryItem(animeId = 6, title = "dropped", listId = 3),
            LibraryItem(animeId = 7, title = "postponed", listId = 5),
        )
        givenSchedule(
            day(animeId = 1, epochSeconds = DAY_1),
            day(animeId = 5, epochSeconds = DAY_1 + 60L),
            day(animeId = 6, epochSeconds = DAY_1 + 120L),
            day(animeId = 7, epochSeconds = DAY_1 + 180L),
            day(animeId = 2, epochSeconds = DAY_2),
            day(animeId = 3, epochSeconds = DAY_2 + 60L),
            day(animeId = 4, epochSeconds = DAY_2 + 120L),
        )
        val vm = createViewModel()
        assertEquals(2, vm.currentState.tvSchedule.dayGroups.size)

        vm.setEvent(ScheduleState.Event.InLibraryFilterToggled)

        assertTrue(vm.currentState.onlyInLibrary)
        assertEquals(1, vm.currentState.tvSchedule.dayGroups.size)
        assertEquals(
            listOf(2, 3, 4),
            vm.currentState.tvSchedule.selectedGroup?.items?.map { it.item.animeId },
        )

        vm.setEvent(ScheduleState.Event.InLibraryFilterToggled)

        assertFalse(vm.currentState.onlyInLibrary)
        assertEquals(2, vm.currentState.tvSchedule.dayGroups.size)
        coVerify(exactly = 1) { repository.getSchedule() }
    }

    @Test
    fun `active library filter reacts to local library updates`() {
        givenSchedule(
            day(animeId = 10, epochSeconds = DAY_1),
            day(animeId = 20, epochSeconds = DAY_2),
        )
        val vm = createViewModel()
        vm.setEvent(ScheduleState.Event.InLibraryFilterToggled)
        assertTrue(vm.currentState.tvSchedule.dayGroups.isEmpty())

        libraryFlow.value = listOf(LibraryItem(animeId = 20, title = "new ongoing", listId = 0))

        assertEquals(1, vm.currentState.tvSchedule.dayGroups.size)
        assertEquals(
            listOf(20),
            vm.currentState.tvSchedule.selectedGroup?.items?.map { it.item.animeId },
        )
    }

    @Test
    fun `refresh preserves active library filter`() {
        libraryFlow.value = listOf(LibraryItem(animeId = 2, title = "mine", listId = 0))
        coEvery { repository.getSchedule() } returns
            listOf(day(animeId = 1, epochSeconds = DAY_1)) andThen
            listOf(day(animeId = 1, epochSeconds = DAY_1), day(animeId = 2, epochSeconds = DAY_2))
        val vm = createViewModel()
        vm.setEvent(ScheduleState.Event.InLibraryFilterToggled)
        assertTrue(vm.currentState.tvSchedule.dayGroups.isEmpty())

        vm.setEvent(ScheduleState.Event.RefreshRequested)

        assertTrue(vm.currentState.onlyInLibrary)
        assertEquals(1, vm.currentState.tvSchedule.dayGroups.size)
        assertEquals(2, vm.currentState.tvSchedule.selectedGroup?.items?.single()?.item?.animeId)
    }

    private companion object {
        const val NOW = 1_700_000_000L
        const val DAY_1 = 1_900_000_000L
        const val DAY_2 = DAY_1 + 3 * 86_400L

        fun day(
            animeId: Int = 1,
            epochSeconds: Long = DAY_1,
            previousEpochSeconds: Long? = null,
            airedEpisodes: Int? = 3,
        ) = AnimeScheduleDay(
            title = "day",
            items = listOf(
                AnimeScheduleItem(
                    animeId = animeId,
                    title = "anime $animeId",
                    poster = null,
                    nextDateEpochSeconds = epochSeconds,
                    previousDateEpochSeconds = previousEpochSeconds,
                    airedEpisodes = airedEpisodes,
                    totalEpisodes = 12,
                ),
            ),
        )
    }
}

private data class DetailsKey(val animeId: Int) : NavKey
