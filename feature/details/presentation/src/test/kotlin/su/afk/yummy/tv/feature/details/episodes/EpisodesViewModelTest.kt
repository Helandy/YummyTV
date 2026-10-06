package su.afk.yummy.tv.feature.details.episodes

import androidx.navigation3.runtime.NavKey
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeEpisodeInfoUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeWatchProgressUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadStatusesUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.ObserveWatchLaterEpisodesUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.animeDetails
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.Effect
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.Event
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadEnqueueResult
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadHandler
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadPrepareResult
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchLaterHandler
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchedHandler
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator

class EpisodesViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val appLifecycleSettingsStore: AppLifecycleSettingsStore = mockk(relaxed = true)
    private val videoDownloadNavigator: IVideoDownloadNavigator = mockk()
    private val animeRepository: AnimeRepository = mockk()
    private val settingsStore: PlayerSettingsStore = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val playerNavigationHandler: DetailsPlayerNavigationHandler = mockk()
    private val downloadHandler: EpisodeDownloadHandler = mockk(relaxed = true)
    private val watchedHandler: EpisodeWatchedHandler = mockk()
    private val watchLaterHandler: EpisodeWatchLaterHandler = mockk()
    private val observeVideoDownloadStatuses: ObserveVideoDownloadStatusesUseCase = mockk()
    private val observeWatchLaterEpisodes: ObserveWatchLaterEpisodesUseCase = mockk()
    private val stringProvider: StringProvider = mockk()
    private val tracker: AnalyticsTracker = mockk(relaxed = true)

    private val session = MutableStateFlow(AccountSession(isAuthorized = false, userId = 0))
    private val watchLater = MutableStateFlow<Set<String>>(emptySet())
    private val playerKey: NavKey = mockk()
    private val navKey: NavKey = mockk()

    private val v1 = video(1, "1", "Anidub")
    private val v2 = video(2, "1", "Anilibria")
    private val v3 = video(3, "2", "Anidub")

    @Before
    fun setUp() {
        every { settingsStore.preferredPlayer } returns flowOf(PreferredPlayer.NONE)
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { accountRepository.observeSession() } returns session
        every { observeWatchLaterEpisodes(any()) } returns watchLater
        every { observeVideoDownloadStatuses(any()) } returns flowOf(emptyMap())
        every { animeRepository.observeWatchProgress(any()) } returns flowOf(emptyList())
        every { videoDownloadNavigator.getVideoDownloadDest() } returns navKey
        every {
            playerNavigationHandler.getPlayerDestination(any<AnimeVideo>(), any(), any(), any(), any(), any())
        } returns playerKey
        every { playerNavigationHandler.getDownloadedPlayerDestination(any()) } returns navKey
        coEvery { animeRepository.getAnimeDetails(ANIME_ID) } returns animeDetails(id = ANIME_ID, title = "Anime")
        coEvery { animeRepository.getAnimeVideos(ANIME_ID) } returns listOf(v1, v2, v3)
        coEvery { animeRepository.refreshAnimeVideos(ANIME_ID) } returns listOf(v1, v2, v3)
        coEvery { animeRepository.getAnimeEpisodeInfo(ANIME_ID) } returns emptyMap()
    }

    private fun createViewModel(pendingEpisode: String? = null) = EpisodesViewModel(
        animeId = ANIME_ID,
        pendingEpisode = pendingEpisode,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        appLifecycleSettingsStore = appLifecycleSettingsStore,
        videoDownloadNavigator = videoDownloadNavigator,
        getAnimeDetails = GetAnimeDetailsUseCase(animeRepository),
        getAnimeVideos = GetAnimeVideosUseCase(animeRepository),
        getAnimeEpisodeInfo = GetAnimeEpisodeInfoUseCase(animeRepository),
        refreshAnimeVideos = RefreshAnimeVideosUseCase(animeRepository),
        observeAnimeWatchProgress = ObserveAnimeWatchProgressUseCase(animeRepository),
        settingsStore = settingsStore,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        playerNavigationHandler = playerNavigationHandler,
        downloadHandler = downloadHandler,
        watchedHandler = watchedHandler,
        watchLaterHandler = watchLaterHandler,
        observeVideoDownloadStatuses = observeVideoDownloadStatuses,
        observeWatchLaterEpisodes = observeWatchLaterEpisodes,
        stringProvider = stringProvider,
        analytics = DetailsAnalytics(tracker),
    )

    private fun video(id: Int, episode: String, dubbing: String) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = "Kodik",
        playerId = 1,
        iframeUrl = "https://player/$id",
        durationSeconds = 1_400,
    )

    @Test
    fun `loads the videos grouped by episode`() {
        val state = createViewModel().currentState

        assertTrue(state.videosState is VideosUiState.Content)
        assertEquals(listOf("1", "2"), state.episodeGroups.map { it.episode })
    }

    @Test
    fun `empty answer is an empty state`() {
        coEvery { animeRepository.getAnimeVideos(ANIME_ID) } returns emptyList()

        assertEquals(VideosUiState.Empty, createViewModel().currentState.videosState)
    }

    @Test
    fun `failed load is an error state and retry recovers`() {
        coEvery { animeRepository.getAnimeVideos(ANIME_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals(VideosUiState.Error("boom"), vm.currentState.videosState)
        coEvery { animeRepository.getAnimeVideos(ANIME_ID) } returns listOf(v1)

        vm.setEvent(Event.RetryVideosSelected)

        assertTrue(vm.currentState.videosState is VideosUiState.Content)
    }

    @Test
    fun `watch later episodes and permission flag are mirrored`() {
        watchLater.value = setOf("1")
        val vm = createViewModel()

        assertEquals(setOf("1"), vm.currentState.watchLaterEpisodes)
    }

    @Test
    fun `pending episode with one candidate opens the player`() {
        createViewModel(pendingEpisode = "2")

        verify(exactly = 1) { nav.navigate(playerKey) }
    }

    @Test
    fun `pending episode with several candidates shows the dubbing picker`() {
        val vm = createViewModel(pendingEpisode = "1")

        assertNotNull(vm.currentState.pendingEpisodeDubbingSelection)
        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `signing in refreshes the videos from the network`() {
        createViewModel()

        session.value = AccountSession(isAuthorized = true, userId = 1)

        coVerify(atLeast = 1) { animeRepository.refreshAnimeVideos(ANIME_ID) }
    }

    @Test
    fun `episode selection offers the dubbings and the chosen one opens the player`() {
        val vm = createViewModel()

        vm.setEvent(Event.EpisodeSelected(v1))
        val selection = vm.currentState.pendingEpisodeDubbingSelection
        assertEquals(setOf("Anidub", "Anilibria"), selection?.options?.map { it.item.name }?.toSet())

        vm.setEvent(Event.EpisodeDubbingPickerDismissed)
        assertNull(vm.currentState.pendingEpisodeDubbingSelection)
    }

    @Test
    fun `confirmed balancer opens the player`() {
        val vm = createViewModel()

        vm.setEvent(Event.BalancerConfirmed(v1))

        verify(exactly = 1) { nav.navigate(playerKey) }
        assertNull(vm.currentState.pendingBalancerSelection)
    }

    @Test
    fun `description toggle expands and collapses`() {
        val vm = createViewModel()

        vm.setEvent(Event.EpisodeDescriptionToggled("1"))
        assertTrue("1" in vm.currentState.expandedEpisodeDescriptions)

        vm.setEvent(Event.EpisodeDescriptionToggled("1"))
        assertFalse("1" in vm.currentState.expandedEpisodeDescriptions)
    }

    @Test
    fun `episode actions open for the long pressed episode`() {
        watchLater.value = setOf("1")
        val vm = createViewModel()

        vm.setEvent(Event.EpisodeActionsRequested(listOf(v1, v2)))

        val action = vm.currentState.pendingEpisodeAction
        assertEquals("1", action?.episode)
        assertTrue(action?.isInWatchLater == true)
        assertFalse(action?.isWatched == true)

        vm.setEvent(Event.EpisodeActionsDismissed)
        assertNull(vm.currentState.pendingEpisodeAction)
    }

    @Test
    fun `marking an episode watched goes through the handler`() {
        coEvery { watchedHandler.markWatched(any(), any(), any(), any(), any(), any(), any()) } returns true
        val vm = createViewModel()
        vm.setEvent(Event.EpisodeActionsRequested(listOf(v1, v2)))

        vm.setEvent(Event.EpisodeWatchedToggled)

        coVerify(exactly = 1) { watchedHandler.markWatched(ANIME_ID, "1", any(), any(), any(), any(), false) }
        assertNull(vm.currentState.pendingEpisodeAction)
    }

    @Test
    fun `failed watched sync shows a toast`() = runTest {
        coEvery { watchedHandler.markWatched(any(), any(), any(), any(), any(), any(), any()) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.EpisodeActionsRequested(listOf(v1)))

        vm.setEvent(Event.EpisodeWatchedToggled)

        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `watch later toggle goes through the handler and confirms with a toast`() = runTest {
        coEvery { watchLaterHandler.toggle(any(), any(), any(), any()) } returns Unit
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.EpisodeActionsRequested(listOf(v1)))

        vm.setEvent(Event.EpisodeWatchLaterToggled)

        coVerify(exactly = 1) { watchLaterHandler.toggle(ANIME_ID, "1", false, any()) }
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `download dubbing picker is built by the handler and can be dismissed`() {
        val selection = EpisodesState.EpisodeDownloadDubbingSelection(episode = "1", options = persistentListOf())
        every { downloadHandler.dubbingSelection(any(), any(), any(), any(), any()) } returns selection
        val vm = createViewModel()

        vm.setEvent(Event.EpisodeDownloadSelected(listOf(v1)))
        assertEquals(selection, vm.currentState.pendingDownloadDubbingSelection)
        verify(exactly = 1) { downloadHandler.beginNewDownload() }

        vm.setEvent(Event.DownloadDubbingPickerDismissed)
        assertNull(vm.currentState.pendingDownloadDubbingSelection)
        verify(exactly = 1) { downloadHandler.dismissSourcePicker() }
    }

    @Test
    fun `prepared download shows the quality picker and the chosen quality is enqueued`() {
        val selection = EpisodesState.EpisodeDownloadQualitySelection(videoId = 1, episode = "1", options = persistentListOf())
        every { downloadHandler.downloadStatusKey(any()) } returns "key"
        coEvery { downloadHandler.prepare(v1) } returns EpisodeDownloadPrepareResult.Ready("key", selection)
        coEvery { downloadHandler.enqueue(any(), any(), any(), any(), any()) } returns EpisodeDownloadEnqueueResult.Success
        val vm = createViewModel()

        vm.setEvent(Event.DownloadBalancerSelected(v1))
        assertEquals(selection, vm.currentState.pendingDownloadQualitySelection)
        assertTrue(vm.currentState.resolvingDownloadKeys.isEmpty())

        vm.setEvent(Event.DownloadQualitySelected(EpisodesState.EpisodeDownloadQualityOption("720p", "url")))
        assertNull(vm.currentState.pendingDownloadQualitySelection)
    }

    @Test
    fun `failed preparation shows its message`() = runTest {
        every { downloadHandler.downloadStatusKey(any()) } returns "key"
        coEvery { downloadHandler.prepare(any()) } returns EpisodeDownloadPrepareResult.Failure("key", "no stream")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.DownloadBalancerSelected(v1))

        assertEquals(Effect.ShowToast("no stream"), effects.last())
        assertNull(vm.currentState.pendingDownloadQualitySelection)
    }

    @Test
    fun `quality picker dismissal clears the pending download`() {
        val vm = createViewModel()

        vm.setEvent(Event.DownloadQualityPickerDismissed)

        verify(exactly = 1) { downloadHandler.clearPending() }
    }

    @Test
    fun `downloaded episode is played or deleted from its action menu`() {
        every { downloadHandler.downloadDubbingName(any()) } answers { firstArg<AnimeVideo>().dubbing }
        coEvery { downloadHandler.delete(any()) } just Runs
        val vm = createViewModel()
        val download = EpisodesState.EpisodeDownloadUiState(
            downloadId = 9L,
            dubbing = "Anidub",
            playerName = "Kodik",
            qualityLabel = "720p",
            bytesDownloaded = 1L,
            status = EpisodesState.EpisodeDownloadUiStatus.Downloaded,
            progress = 1f,
            errorMessage = null,
        )

        vm.setEvent(Event.DownloadedEpisodeSelected(listOf(v1, v2), download))
        assertTrue(vm.currentState.pendingDownloadedEpisodeAction?.hasAlternativeDubbings == true)
        vm.setEvent(Event.PlayDownloadedEpisodeSelected)
        verify(exactly = 1) { playerNavigationHandler.getDownloadedPlayerDestination(9L) }

        vm.setEvent(Event.DownloadedEpisodeSelected(listOf(v1), download))
        vm.setEvent(Event.DeleteDownloadedEpisodeSelected)
        coVerify(exactly = 1) { downloadHandler.delete(9L) }
        assertNull(vm.currentState.pendingDownloadedEpisodeAction)
    }

    @Test
    fun `permission request is remembered and the downloads screen opens`() {
        val vm = createViewModel()

        vm.setEvent(Event.NotificationPermissionRequested)
        vm.setEvent(Event.OpenDownloadsScreenSelected)
        vm.setEvent(Event.BackSelected)

        coVerify(exactly = 1) { appLifecycleSettingsStore.markNotificationPermissionRequested() }
        verify(exactly = 1) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val ANIME_ID = 4
    }
}
