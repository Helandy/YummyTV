package su.afk.yummy.tv.feature.schedule

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
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
    private val detailsNavigator: IDetailsNavigator = mockk()

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        repository = mockk<AnimeScheduleRepository>()
        with(detailsNavigator) {
            every { getDetailsDest(any()) } answers { DetailsKey(firstArg()) }
        }
    }

    private fun createViewModel() = ScheduleViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        getSchedule = GetAnimeScheduleUseCase(repository),
        nav = nav,
        detailsNavigator = detailsNavigator,
        analytics = ScheduleAnalytics(tracker),
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
        assertEquals("parsed message", state.error)
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

    private companion object {
        const val DAY_1 = 1_900_000_000L
        const val DAY_2 = DAY_1 + 3 * 86_400L

        fun day(epochSeconds: Long = DAY_1) = AnimeScheduleDay(
            title = "day",
            items = listOf(
                AnimeScheduleItem(
                    animeId = 1,
                    title = "anime",
                    posterUrl = null,
                    nextDateEpochSeconds = epochSeconds,
                    airedEpisodes = 3,
                    totalEpisodes = 12,
                ),
            ),
        )
    }
}

private data class DetailsKey(val animeId: Int) : NavKey
