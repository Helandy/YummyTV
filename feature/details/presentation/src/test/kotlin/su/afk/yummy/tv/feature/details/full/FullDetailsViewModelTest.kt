package su.afk.yummy.tv.feature.details.full

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
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
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.animeDetails
import su.afk.yummy.tv.feature.details.full.FullDetailsState.Event
import su.afk.yummy.tv.feature.details.navigator.DetailsRelationKind

class FullDetailsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val repository: AnimeRepository = mockk()
    private val stringProvider: StringProvider = mockk()
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { stringProvider.get(any<Int>()) } returns "load error"
        every { detailsNavigator.getRelationDest(any(), any(), any()) } returns navKey
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns animeDetails(id = ANIME_ID, title = "Anime")
    }

    private fun createViewModel() = FullDetailsViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        getAnimeDetails = GetAnimeDetailsUseCase(repository),
        stringProvider = stringProvider,
        analytics = DetailsAnalytics(tracker),
        detailsNavigator = detailsNavigator,
    )

    @Test
    fun `loads the full details`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("Anime", state.details?.title)
        assertNull(state.error)
    }

    @Test
    fun `failed load shows the exception message and retry recovers`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("boom", vm.currentState.error)
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns animeDetails(id = ANIME_ID)

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
    }

    @Test
    fun `genre studio and director open their relation screens`() {
        val vm = createViewModel()

        vm.setEvent(Event.GenreSelected(1))
        vm.setEvent(Event.StudioSelected(2, "studio-url"))
        vm.setEvent(Event.DirectorSelected(3))

        verify(exactly = 1) { detailsNavigator.getRelationDest(DetailsRelationKind.GENRE, 1, null) }
        verify(exactly = 1) { detailsNavigator.getRelationDest(DetailsRelationKind.STUDIO, 2, "studio-url") }
        verify(exactly = 1) { detailsNavigator.getRelationDest(DetailsRelationKind.DIRECTOR, 3, null) }
        verify(exactly = 3) { nav.navigate(navKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val ANIME_ID = 4
    }
}
