package su.afk.yummy.tv.feature.details.rating

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.rating.RatingState.Effect
import su.afk.yummy.tv.feature.details.rating.RatingState.Event
import su.afk.yummy.tv.feature.details.rating.handler.RatingLoadResult
import su.afk.yummy.tv.feature.details.rating.handler.RatingMutationHandler
import su.afk.yummy.tv.feature.details.rating.handler.RatingMutationResult

class RatingViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val ratingMutationHandler: RatingMutationHandler = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val stringProvider: StringProvider = mockk()
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val userId = MutableStateFlow(USER_ID)
    private val summary = AnimeRatingSummary(userRating = 7)

    @Before
    fun setUp() {
        every { settingsStore.yaniUserId } returns userId
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        coEvery { ratingMutationHandler.load(ANIME_ID) } returns RatingLoadResult(
            ratingSummary = Result.success(summary),
            listStats = Result.success(AnimeListStats(mapOf(1 to 2))),
            userRating = Result.success(7),
        )
        coEvery { ratingMutationHandler.refreshSummary(ANIME_ID) } returns summary
    }

    private fun createViewModel() = RatingViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        ratingMutationHandler = ratingMutationHandler,
        settingsStore = settingsStore,
        stringProvider = stringProvider,
        analytics = DetailsAnalytics(tracker),
    )

    @Test
    fun `loads the summary stats and the own rating`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(summary, state.ratingSummary)
        assertEquals(mapOf(1 to 2), state.listStats.counts)
        assertEquals(7, state.selectedUserRating)
    }

    @Test
    fun `partial failure still shows the loaded part`() {
        coEvery { ratingMutationHandler.load(ANIME_ID) } returns RatingLoadResult(
            ratingSummary = Result.failure(IllegalStateException("boom")),
            listStats = Result.success(AnimeListStats()),
            userRating = Result.success(null),
        )

        val state = createViewModel().currentState

        assertNull(state.error)
        assertNull(state.selectedUserRating)
    }

    @Test
    fun `total failure shows the error and retry recovers`() {
        coEvery { ratingMutationHandler.load(ANIME_ID) } returns RatingLoadResult(
            ratingSummary = Result.failure(IllegalStateException("boom")),
            listStats = Result.failure(IllegalStateException("x")),
            userRating = Result.failure(IllegalStateException("y")),
        )
        val vm = createViewModel()
        assertEquals("boom", vm.currentState.error)
        coEvery { ratingMutationHandler.load(ANIME_ID) } returns RatingLoadResult(
            Result.success(summary),
            Result.success(AnimeListStats()),
            Result.success(3),
        )

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(3, vm.currentState.selectedUserRating)
    }

    @Test
    fun `selected rating is saved and the summary refreshed`() {
        coEvery { ratingMutationHandler.setRating(ANIME_ID, 9) } returns RatingMutationResult.Success
        val vm = createViewModel()

        vm.setEvent(Event.RatingSelected(9))

        assertEquals(9, vm.currentState.selectedUserRating)
        coVerify(exactly = 1) { ratingMutationHandler.setRating(ANIME_ID, 9) }
        coVerify(atLeast = 1) { ratingMutationHandler.refreshSummary(ANIME_ID) }
    }

    @Test
    fun `failed rating is rolled back to the previous one`() {
        coEvery { ratingMutationHandler.setRating(any(), any()) } returns RatingMutationResult.Failure
        val vm = createViewModel()

        vm.setEvent(Event.RatingSelected(9))

        assertEquals(7, vm.currentState.selectedUserRating)
    }

    @Test
    fun `deleted rating clears the selection and failure restores it`() {
        coEvery { ratingMutationHandler.deleteRating(ANIME_ID) } returns RatingMutationResult.Success
        val vm = createViewModel()

        vm.setEvent(Event.RatingDeleted)
        assertNull(vm.currentState.selectedUserRating)

        coEvery { ratingMutationHandler.setRating(any(), any()) } returns RatingMutationResult.Success
        vm.setEvent(Event.RatingSelected(5))
        coEvery { ratingMutationHandler.deleteRating(ANIME_ID) } returns RatingMutationResult.Failure
        vm.setEvent(Event.RatingDeleted)

        assertEquals(5, vm.currentState.selectedUserRating)
    }

    @Test
    fun `guest gets a toast and nothing is saved`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RatingSelected(9))
        vm.setEvent(Event.RatingDeleted)

        assertEquals(2, effects.size)
        assertTrue(effects.all { it is Effect.ShowToast })
        coVerify(exactly = 0) { ratingMutationHandler.setRating(any(), any()) }
        coVerify(exactly = 0) { ratingMutationHandler.deleteRating(any()) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val ANIME_ID = 4
        const val USER_ID = 5
    }
}
