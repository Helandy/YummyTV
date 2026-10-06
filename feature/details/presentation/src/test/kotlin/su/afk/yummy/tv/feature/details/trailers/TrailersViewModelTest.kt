package su.afk.yummy.tv.feature.details.trailers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeTrailersUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.trailers.TrailersState.Event

class TrailersViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val repository: AnimeRepository = mockk()

    private fun createViewModel() = TrailersViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        getAnimeTrailers = GetAnimeTrailersUseCase(repository),
        analytics = DetailsAnalytics(tracker),
    )

    @Test
    fun `loads the trailers`() {
        coEvery { repository.getAnimeTrailers(ANIME_ID) } returns listOf(AnimeTrailer("https://youtube.com/embed/abc"))

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.trailers.size)
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { repository.getAnimeTrailers(ANIME_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("boom", vm.currentState.error)
        coEvery { repository.getAnimeTrailers(ANIME_ID) } returns listOf(AnimeTrailer("https://x"))

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(1, vm.currentState.trailers.size)
        coVerify(exactly = 2) { repository.getAnimeTrailers(ANIME_ID) }
    }

    @Test
    fun `back leaves the screen`() {
        coEvery { repository.getAnimeTrailers(ANIME_ID) } returns emptyList()

        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val ANIME_ID = 4
    }
}
