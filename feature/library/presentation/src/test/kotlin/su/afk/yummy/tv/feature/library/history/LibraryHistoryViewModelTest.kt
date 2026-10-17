package su.afk.yummy.tv.feature.library.history

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
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.domain.library.usecase.GetWatchHistoryPageUseCase
import su.afk.yummy.tv.domain.player.usecase.GetMeaningfulVideoProgressUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.history.LibraryHistoryState.Event

class LibraryHistoryViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val getWatchHistoryPage: GetWatchHistoryPageUseCase = mockk()
    private val getMeaningfulVideoProgress: GetMeaningfulVideoProgressUseCase = mockk()

    private val userId = MutableStateFlow(USER_ID)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { settingsStore.yaniUserId } returns userId
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { detailsNavigator.getEpisodesDest(any(), any()) } returns navKey
        coEvery { getMeaningfulVideoProgress() } returns emptyList()
    }

    private fun createViewModel() = LibraryHistoryViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        settingsStore = settingsStore,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getWatchHistoryPage = getWatchHistoryPage,
        getMeaningfulVideoProgress = getMeaningfulVideoProgress,
    )

    @Test
    fun `mirrors the signed-in state`() {
        val vm = createViewModel()
        assertTrue(vm.currentState.isSignedIn)

        userId.value = 0

        assertFalse(vm.currentState.isSignedIn)
    }

    @Test
    fun `selecting an entry opens its episodes and details open the title`() {
        val vm = createViewModel()

        vm.setEvent(Event.EntrySelected(entry(animeId = 9, episode = "2")))
        vm.setEvent(Event.DetailsSelected(4))

        verify(exactly = 1) { detailsNavigator.getEpisodesDest(9, "2") }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(4) }
        verify(exactly = 2) { nav.navigate(navKey) }
    }

    @Test
    fun `local progress is reloaded on resume`() {
        val vm = createViewModel()
        coEvery { getMeaningfulVideoProgress() } returns listOf(progress(animeId = 9, episode = "2"))

        vm.setEvent(Event.ScreenResumed)

        assertEquals(1, vm.currentState.localProgress.size)
        coVerify(exactly = 2) { getMeaningfulVideoProgress() }
    }

    private fun entry(animeId: Int, episode: String) = WatchHistoryEntry(
        animeId = animeId,
        animeUrl = "",
        title = "",
        episode = episode,
        posterUrl = null,
        screenshotUrl = null,
        watchedAtSeconds = 0L,
        positionSeconds = 0,
        durationSeconds = 0,
    )

    private fun progress(animeId: Int, episode: String) = AnimeWatchProgress(
        animeId = animeId,
        episode = episode,
        episodeUrl = "",
        positionMs = 1_000L,
        durationMs = 10_000L,
        updatedAt = 0L,
    )

    private companion object {
        const val USER_ID = 7
    }
}
