package su.afk.yummy.tv.feature.details.episodes

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.animeDetails
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState.Event
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsViewModel

class EpisodeDubbingsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val repository: AnimeRepository = mockk()
    private val settingsStore: PlayerSettingsStore = mockk()
    private val playerNavigationHandler: DetailsPlayerNavigationHandler = mockk()
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val playerKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { settingsStore.preferredPlayer } returns MutableStateFlow(PreferredPlayer.NONE)
        every {
            playerNavigationHandler.getPlayerDestination(any<AnimeVideo>(), any(), any(), any(), any(), any())
        } returns playerKey
        coEvery { repository.getAnimeDetails(ANIME_ID) } returns animeDetails(id = ANIME_ID, title = "Anime")
        coEvery { repository.getAnimeVideos(ANIME_ID) } returns listOf(
            video(1, dubbing = "Anidub", episode = "1"),
            video(2, dubbing = "Anilibria", episode = "1"),
            video(3, dubbing = "Anidub", episode = "2"),
        )
    }

    private fun createViewModel(episode: String = "1") = EpisodeDubbingsViewModel(
        animeId = ANIME_ID,
        episode = episode,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        getAnimeDetails = GetAnimeDetailsUseCase(repository),
        getAnimeVideos = GetAnimeVideosUseCase(repository),
        settingsStore = settingsStore,
        playerNavigationHandler = playerNavigationHandler,
        analytics = DetailsAnalytics(tracker),
    )

    private fun video(id: Int, dubbing: String, episode: String) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = "Kodik",
        playerId = 1,
        iframeUrl = "https://player/$id",
        durationSeconds = 1_400,
    )

    @Test
    fun `lists the dubbings that have the episode`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("1", state.episode)
        assertEquals(setOf("Anidub", "Anilibria"), state.dubbings.map { it.name }.toSet())
    }

    @Test
    fun `episode without dubbings has an empty list`() {
        assertEquals(emptyList<String>(), createViewModel("9").currentState.dubbings.map { it.name })
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { repository.getAnimeVideos(ANIME_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("boom", vm.currentState.error)
        coEvery { repository.getAnimeVideos(ANIME_ID) } returns listOf(video(1, "Anidub", "1"))

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(listOf("Anidub"), vm.currentState.dubbings.map { it.name })
    }

    @Test
    fun `dubbing with a single balancer opens the player`() {
        every { playerNavigationHandler.selectPlayer(any(), any(), any()) } answers {
            DetailsPlayerSelection.Navigate(firstArg())
        }
        val vm = createViewModel()

        vm.setEvent(Event.DubbingSelected("Anidub"))

        verify(timeout = 2_000, exactly = 1) { nav.navigate(playerKey) }
        verify(exactly = 1) {
            playerNavigationHandler.getPlayerDestination(
                match<AnimeVideo> { it.id == 1 },
                "Anime",
                ANIME_ID,
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `several balancers show the picker that can be dismissed`() {
        val picker = BalancerPickerState(episodeNumber = "1", options = persistentListOf())
        every { playerNavigationHandler.selectPlayer(any(), any(), any()) } returns
            DetailsPlayerSelection.ShowPicker(picker)
        val vm = createViewModel()

        vm.setEvent(Event.DubbingSelected("Anidub"))
        assertNotNull(vm.currentState.pendingBalancerSelection)

        vm.setEvent(Event.BalancerPickerDismissed)
        assertNull(vm.currentState.pendingBalancerSelection)
    }

    @Test
    fun `confirmed balancer opens the player and closes the picker`() {
        val vm = createViewModel()

        vm.setEvent(Event.BalancerConfirmed(video(2, "Anilibria", "1")))

        assertNull(vm.currentState.pendingBalancerSelection)
        verify(timeout = 2_000, exactly = 1) { nav.navigate(playerKey) }
    }

    @Test
    fun `unknown dubbing does nothing`() {
        val vm = createViewModel()

        vm.setEvent(Event.DubbingSelected("Nobody"))

        verify(exactly = 0) { nav.navigate(any()) }
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
