package su.afk.yummy.tv.feature.watchlater

import androidx.navigation3.runtime.NavKey
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.watchlater.model.WatchLaterItem
import su.afk.yummy.tv.domain.watchlater.repository.WatchLaterRepository
import su.afk.yummy.tv.domain.watchlater.usecase.ObserveWatchLaterUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.PruneWatchedWatchLaterUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.RemoveWatchLaterEpisodeUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator

class WatchLaterViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private lateinit var items: MutableStateFlow<List<WatchLaterItem>>
    private val repository: WatchLaterRepository = mockk()
    private val detailsNavigator: IDetailsNavigator = mockk()

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        items = MutableStateFlow<List<WatchLaterItem>>(emptyList())
        with(repository) {
            every { observeAll() } returns items
            coEvery { pruneWatched() } just Runs
            coEvery { remove(any(), any()) } coAnswers {
                items.value = items.value.filterNot { it.animeId == firstArg<Int>() && it.episode == secondArg<String>() }
            }
        }
        with(detailsNavigator) {
            every { getEpisodesDest(any(), any()) } answers { EpisodesKey(firstArg(), secondArg()) }
        }
    }

    private fun createViewModel() = WatchLaterViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        observeWatchLater = ObserveWatchLaterUseCase(repository),
        removeWatchLaterEpisode = RemoveWatchLaterEpisodeUseCase(repository),
        pruneWatchedWatchLater = PruneWatchedWatchLaterUseCase(repository),
        detailsNavigator = detailsNavigator,
    )

    @Test
    fun `prunes watched episodes on start`() {
        createViewModel()

        coVerify(exactly = 1) { repository.pruneWatched() }
    }

    @Test
    fun `state follows the stored list`() {
        val vm = createViewModel()
        assertTrue(vm.currentState.items.isEmpty())

        items.value = listOf(item(1, "1"), item(2, "3"))
        assertEquals(listOf(1 to "1", 2 to "3"), vm.currentState.items.map { it.animeId to it.episode })

        items.value = listOf(item(2, "3"))
        assertEquals(listOf(2 to "3"), vm.currentState.items.map { it.animeId to it.episode })
    }

    @Test
    fun `remove drops the episode from the store`() {
        items.value = listOf(item(1, "1"), item(2, "3"))
        val vm = createViewModel()

        vm.setEvent(WatchLaterState.Event.RemoveSelected(animeId = 1, episode = "1"))

        coVerify(exactly = 1) { repository.remove(1, "1") }
        assertEquals(listOf(2), vm.currentState.items.map { it.animeId })
    }

    @Test
    fun `selecting an item opens the episodes screen with the pending episode`() {
        createViewModel().setEvent(WatchLaterState.Event.ItemSelected(animeId = 5, episode = "7"))

        verify(exactly = 1) { nav.navigate(EpisodesKey(5, "7")) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(WatchLaterState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private fun item(animeId: Int, episode: String) = WatchLaterItem(
        animeId = animeId,
        episode = episode,
        animeTitle = "title $animeId",
        posterUrl = "",
        screenshotUrl = "",
        addedAt = 0L,
    )

    private data class EpisodesKey(val animeId: Int, val episode: String?) : NavKey
}
