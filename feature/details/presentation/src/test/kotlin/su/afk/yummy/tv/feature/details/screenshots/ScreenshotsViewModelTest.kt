package su.afk.yummy.tv.feature.details.screenshots

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
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewNavigator
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.animeDetails
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsState.Event

class ScreenshotsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val repository: AnimeRepository = mockk()
    private val imageViewNavigator: IImageViewNavigator = mockk()
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { imageViewNavigator(any(), any(), any(), any(), any(), any(), any(), any()) } returns navKey
    }

    private fun createViewModel() = ScreenshotsViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        getAnimeDetails = GetAnimeDetailsUseCase(repository),
        analytics = DetailsAnalytics(tracker),
        imageViewNavigator = imageViewNavigator,
    )

    private fun screenshot(id: Int, full: String?, small: String? = null) = AnimeScreenshot(id, null, small, full)

    @Test
    fun `loads the title and its screenshots`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns
            animeDetails(title = "Anime", screenshots = listOf(screenshot(1, "a"), screenshot(2, "b")))

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("Anime", state.title)
        assertEquals(2, state.screenshots.size)
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("boom", vm.currentState.error)
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns animeDetails(title = "Anime")

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals("Anime", vm.currentState.title)
    }

    @Test
    fun `selected screenshot opens the gallery of all full urls`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns animeDetails(
            screenshots = listOf(screenshot(1, "full1"), screenshot(2, null, small = "small2"), screenshot(3, "full3")),
        )
        val vm = createViewModel()

        vm.setEvent(Event.ScreenshotSelected(2))

        verify(exactly = 1) {
            imageViewNavigator(
                imageUrl = "full3",
                imageUrls = listOf("full1", "small2", "full3"),
                selectedIndex = 2,
                service = any(),
                creatorName = any(),
                postId = any(),
                postTitle = any(),
                thumbnailUrls = any(),
            )
        }
        verify(exactly = 1) { nav.navigate(navKey) }
    }

    @Test
    fun `unknown index is ignored`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns animeDetails(screenshots = listOf(screenshot(1, "a")))
        val vm = createViewModel()

        vm.setEvent(Event.ScreenshotSelected(5))

        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `back leaves the screen`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns animeDetails()

        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val ANIME_ID = 4
    }
}
