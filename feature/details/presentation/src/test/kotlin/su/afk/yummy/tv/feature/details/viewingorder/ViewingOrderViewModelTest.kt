package su.afk.yummy.tv.feature.details.viewingorder

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
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import java.io.IOException

class ViewingOrderViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private lateinit var repository: AnimeRepository
    private val detailsNavigator: IDetailsNavigator = mockk()

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        repository = mockk<AnimeRepository>()
        with(detailsNavigator) {
            every { getDetailsDest(any()) } answers { DetailsKey(firstArg()) }
        }
    }

    private fun createViewModel() = ViewingOrderViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getAnimeDetails = GetAnimeDetailsUseCase(repository),
        analytics = DetailsAnalytics(tracker),
    )

    @Test
    fun `loads the viewing order of the title`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns details(listOf(item(1), item(2)))

        val state = createViewModel().currentState

        assertEquals(ANIME_ID, state.currentAnimeId)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf(1, 2), state.items.map { it.animeId })
    }

    @Test
    fun `network failure shows the localized message with the original text`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } throws IOException("Unable to resolve host")

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("parsed message\nUnable to resolve host", state.error)
    }

    @Test
    fun `other failure shows the exception message`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } throws IllegalStateException("boom")

        assertEquals("boom", createViewModel().currentState.error)
    }

    @Test
    fun `retry reloads after a failure`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } throws IllegalStateException("boom") andThen
            details(listOf(item(7)))
        val vm = createViewModel()

        vm.setEvent(ViewingOrderState.Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(listOf(7), vm.currentState.items.map { it.animeId })
        coVerify(exactly = 2) { repository.getAnimeDetails(ANIME_ID) }
    }

    @Test
    fun `selecting an item opens its details`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns details(emptyList())

        createViewModel().setEvent(ViewingOrderState.Event.AnimeSelected(5))

        verify(exactly = 1) { nav.navigate(DetailsKey(5)) }
    }

    @Test
    fun `back leaves the screen`() {
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns details(emptyList())

        createViewModel().setEvent(ViewingOrderState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private data class DetailsKey(val animeId: Int) : NavKey

    private companion object {
        const val ANIME_ID = 10

        fun item(id: Int) = AnimeViewingOrderItem(
            animeId = id,
            title = "title $id",
            relation = null,
            type = null,
            episodesCount = null,
            poster = null,
            year = null,
            rating = null,
        )

        fun details(order: List<AnimeViewingOrderItem>) = AnimeDetails(
            id = ANIME_ID,
            animeUrl = "url",
            title = "title",
            description = "",
            poster = null,
            rating = AnimeRating(null, null, null, null, null),
            genres = emptyList(),
            year = null,
            ageRating = null,
            views = null,
            status = null,
            type = null,
            episodes = null,
            otherTitles = emptyList(),
            creators = emptyList(),
            studios = emptyList(),
            viewingOrder = order,
            screenshots = emptyList(),
        )
    }
}
