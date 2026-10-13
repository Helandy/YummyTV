package su.afk.yummy.tv.feature.home.launch

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.watching.ContinueWatchingLaunch
import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.navigator.ContinueWatchingLaunchDestination
import su.afk.yummy.tv.feature.player.IPlayerNavigator

class ContinueWatchingLaunchViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val playerNavigator: IPlayerNavigator = mockk()
    private val observeContinueWatching: ObserveContinueWatchingUseCase = mockk()
    private val resolveLaunch: ResolveContinueWatchingLaunchUseCase = mockk()
    private val settingsStore: SettingsStore = mockk()

    private val playerKey: NavKey = mockk()
    private val detailsKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { nav.backStack } returns listOf(ContinueWatchingLaunchDestination(ANIME_ID))
        every { detailsNavigator.getDetailsDest(ANIME_ID) } returns detailsKey
        every { playerNavigator.getPlayerDest(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns playerKey
        every { settingsStore.refreshContinueWatchingProgressOnLaunch } returns flowOf(true)
        coEvery { resolveLaunch(any(), any()) } returns launch()
    }

    @Test
    fun `playable entry replaces the screen with the player`() {
        every { observeContinueWatching() } returns flowOf(listOf(entry(ANIME_ID, videoId = 7)))

        createViewModel()

        coVerify { resolveLaunch(match { it.animeId == ANIME_ID }, refreshProgressOnLaunch = true) }
        verify { nav.replace(playerKey) }
    }

    @Test
    fun `title missing from continue watching opens its details`() {
        every { observeContinueWatching() } returns flowOf(listOf(entry(animeId = 99, videoId = 7)))

        createViewModel()

        verify { nav.replace(detailsKey) }
        coVerify(exactly = 0) { resolveLaunch(any(), any()) }
    }

    @Test
    fun `entry without a playable target opens details`() {
        every { observeContinueWatching() } returns flowOf(listOf(entry(ANIME_ID, videoId = 0, episode = "", episodeUrl = "")))

        createViewModel()

        verify { nav.replace(detailsKey) }
    }

    @Test
    fun `failed launch opens details`() {
        every { observeContinueWatching() } returns flowOf(listOf(entry(ANIME_ID, videoId = 7)))
        coEvery { resolveLaunch(any(), any()) } throws IllegalStateException("network")

        createViewModel()

        verify { nav.replace(detailsKey) }
    }

    @Test
    fun `screen left during the launch is not replaced`() {
        val pending = CompletableDeferred<ContinueWatchingLaunch>()
        every { observeContinueWatching() } returns flowOf(listOf(entry(ANIME_ID, videoId = 7)))
        coEvery { resolveLaunch(any(), any()) } coAnswers { pending.await() }

        createViewModel()
        // Пока шёл запуск, пользователь ушёл назад.
        every { nav.backStack } returns listOf(mockk())
        pending.complete(launch())

        verify(exactly = 0) { nav.replace(any()) }
    }

    private fun entry(
        animeId: Int,
        videoId: Int,
        episode: String = "3",
        episodeUrl: String = "https://kodik.info/seria/7",
    ) = HomeContinueWatchingItem(
        animeId = animeId,
        animeTitle = "Title",
        description = "",
        poster = null,
        videoId = videoId,
        episode = episode,
        episodeUrl = episodeUrl,
        positionMs = 60_000L,
        durationMs = 1_440_000L,
        updatedAt = 1L,
        playerName = "Kodik",
        dubbing = "AniLibria",
        screenshotUrl = "",
    )

    private fun launch() = ContinueWatchingLaunch(
        animeId = ANIME_ID,
        animeTitle = "Title",
        posterUrl = "",
        video = ContinueWatchingPlaybackVideo(
            id = 7,
            episode = "3",
            dubbing = "AniLibria",
            player = "Kodik",
            iframeUrl = "https://kodik.info/seria/7",
        ),
        resumeFromMs = 60_000L,
    )

    private fun createViewModel() = ContinueWatchingLaunchViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        playerNavigator = playerNavigator,
        observeContinueWatching = observeContinueWatching,
        resolveContinueWatchingLaunch = resolveLaunch,
        settingsStore = settingsStore,
    )

    private companion object {
        const val ANIME_ID = 42
    }
}
