package su.afk.yummy.tv.feature.player

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.feature.player.behavior.AllohaSourceBehavior
import su.afk.yummy.tv.feature.player.behavior.DefaultSourceBehavior
import su.afk.yummy.tv.feature.player.delegate.PlayerNavigationDelegate
import su.afk.yummy.tv.feature.player.delegate.PlayerOfflineSourceLoader
import su.afk.yummy.tv.feature.player.delegate.PlayerPreferencesBinder
import su.afk.yummy.tv.feature.player.handler.PlayerArtworkHandler
import su.afk.yummy.tv.feature.player.handler.PlayerDisplaySettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerFinalEpisodeActionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackProgressHandler
import su.afk.yummy.tv.feature.player.handler.PlayerProgressSaveRequest
import su.afk.yummy.tv.feature.player.handler.PlayerSourceGraphLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerSourceSelectionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceStreamHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.mapper.PlayerDestinationStateMapper
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.model.PlayerSkipType
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination

class PlayerViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val sourceStreamHandler: PlayerSourceStreamHandler = mockk()
    private val playbackProgressHandler: PlayerPlaybackProgressHandler = mockk(relaxed = true)
    private val preferences: PlayerPreferencesBinder = mockk(relaxed = true)
    private val displaySettings: PlayerDisplaySettingsHandler = mockk(relaxed = true)
    private val finalEpisodeActionHandler: PlayerFinalEpisodeActionHandler = mockk()
    private val artworkHandler: PlayerArtworkHandler = mockk()
    private val offlineSources: PlayerOfflineSourceLoader = mockk()
    private val navigation: PlayerNavigationDelegate = mockk(relaxed = true)
    private val strings: StringProvider = mockk()
    private val analytics: PlayerAnalytics = mockk(relaxed = true)
    private val allohaSource: AllohaSourceBehavior = mockk(relaxed = true)
    private val defaultSource: DefaultSourceBehavior = mockk(relaxed = true)

    private val streamState = PlayerState.State(
        streamUrl = STREAM_URL,
        selectedQuality = "720p",
        streamQualityMap = linkedMapOf("480p" to "url480", "720p" to STREAM_URL),
    )
    private val finalAction = PlayerFinalEpisodeAction.Loading

    @Before
    fun setUp() {
        every { strings.get(any<Int>()) } returns "stream error"
        coEvery { finalEpisodeActionHandler.resolve(any()) } returns finalAction
        coEvery { artworkHandler.resolve(any()) } returns null
        coEvery { sourceStreamHandler.loadSourceGraph(any(), any(), any(), any(), any(), any()) } returns
            PlayerSourceGraphLoadResult.Ignore
        every { sourceStreamHandler.preparingStreamLoad(any(), any()) } answers { firstArg() }
        every { sourceStreamHandler.preparingStreamResolve(any(), any()) } answers { firstArg() }
        every { sourceStreamHandler.playbackErrorMessage(any(), any()) } returns "friendly error"
        coEvery {
            sourceStreamHandler.resolveStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns PlayerStreamLoadResult.State(state = streamState, consumedDestinationResume = false)
    }

    private fun createViewModel(dest: PlayerDestination = dest()) = PlayerViewModel(
        dest = dest,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        sourceStreamHandler = sourceStreamHandler,
        playbackProgressHandler = playbackProgressHandler,
        preferences = preferences,
        displaySettings = displaySettings,
        finalEpisodeActionHandler = finalEpisodeActionHandler,
        artworkHandler = artworkHandler,
        destinationStateMapper = PlayerDestinationStateMapper(),
        sourceSelectionHandler = PlayerSourceSelectionHandler(),
        offlineSources = offlineSources,
        navigation = navigation,
        strings = strings,
        analytics = analytics,
        allohaSource = allohaSource,
        defaultSource = defaultSource,
    )

    private fun dest(downloadId: Long = 0L, localFileUri: String = "") = PlayerDestination(
        iframeUrl = "https://kodik.example/seria/1",
        animeTitle = "Anime",
        episode = "1",
        playerName = "Kodik",
        animeId = ANIME_ID,
        downloadId = downloadId,
        localFileUri = localFileUri,
    )

    @Test
    fun `starts from the destination and resolves the stream`() {
        val state = createViewModel().currentState

        assertEquals("Anime", state.animeTitle)
        assertEquals(ANIME_ID, state.animeId)
        assertEquals(STREAM_URL, state.streamUrl)
        assertEquals("720p", state.selectedQuality)
        assertFalse(state.showChangePlayerHint)
        verify(exactly = 1) { analytics.eventScreenOpened(ANIME_ID) }
        verify(exactly = 1) { allohaSource.attach(any()) }
        verify(exactly = 1) { defaultSource.attach(any()) }
        verify(exactly = 1) { preferences.bind(any()) }
    }

    @Test
    fun `final episode action is resolved for the title`() {
        val state = createViewModel().currentState

        assertEquals(finalAction, state.finalEpisodeAction)
        coVerify(exactly = 1) { finalEpisodeActionHandler.resolve(ANIME_ID) }
    }

    @Test
    fun `failed stream shows the player error and no stream`() {
        coEvery {
            sourceStreamHandler.resolveStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns PlayerStreamLoadResult.State(
            state = PlayerState.State(playerError = "no stream"),
            consumedDestinationResume = false,
        )

        val state = createViewModel().currentState

        assertNull(state.streamUrl)
        assertEquals("no stream", state.playerError)
    }

    @Test
    fun `downloaded destination loads the offline item`() {
        val item: VideoDownloadItem = mockk(relaxed = true)
        coEvery { offlineSources.findDownloaded(7L) } returns item
        every { offlineSources.downloaded(any(), item) } answers { firstArg<PlayerState.State>().copy(isOfflinePlayback = true) }

        val state = createViewModel(dest(downloadId = 7L)).currentState

        assertTrue(state.isOfflinePlayback)
        coVerify(exactly = 0) { sourceStreamHandler.resolveStream(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `missing download is reported by the offline loader`() {
        coEvery { offlineSources.findDownloaded(7L) } returns null
        every { offlineSources.missingDownload(any()) } answers { firstArg<PlayerState.State>().copy(playerError = "missing") }

        assertEquals("missing", createViewModel(dest(downloadId = 7L)).currentState.playerError)
    }

    @Test
    fun `local file destination is played offline`() {
        every { offlineSources.localFile(any(), "content://video", "Anime") } answers {
            firstArg<PlayerState.State>().copy(isLocalFile = true, isOfflinePlayback = true)
        }

        val state = createViewModel(dest(localFileUri = "content://video")).currentState

        assertTrue(state.isLocalFile)
    }

    @Test
    fun `unhandled playback error stops the stream and shows the message`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.PlaybackError(message = "boom", positionMs = 5_000L))

        assertNull(vm.currentState.streamUrl)
        assertEquals("friendly error", vm.currentState.playerError)
        assertFalse(vm.currentState.isPlaybackRecovering)
        verify(exactly = 1) { defaultSource.reset() }
    }

    @Test
    fun `playback error handled by the source behavior keeps the stream`() {
        every { defaultSource.handles(any()) } returns true
        every { defaultSource.onPlaybackError(any()) } returns true
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.PlaybackError(message = "boom"))

        assertEquals(STREAM_URL, vm.currentState.streamUrl)
        assertNull(vm.currentState.playerError)
    }

    @Test
    fun `playback ready is forwarded to the behaviors`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.PlaybackReady)

        verify(exactly = 1) { allohaSource.onPlaybackReady() }
        verify(exactly = 1) { defaultSource.onPlaybackReady() }
    }

    @Test
    fun `retry resolves the stream again and bumps the retry key`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.RetryStream)

        assertEquals(1, vm.currentState.retryKey)
        coVerify(exactly = 2) {
            sourceStreamHandler.resolveStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 1) { analytics.eventRetryStream(ANIME_ID) }
    }

    @Test
    fun `retry of a local file reloads it without touching the network`() {
        every { offlineSources.localFile(any(), any(), any()) } answers {
            firstArg<PlayerState.State>().copy(isLocalFile = true, isOfflinePlayback = true)
        }
        val vm = createViewModel(dest(localFileUri = "content://video"))

        vm.setEvent(PlayerState.Event.RetryStream)

        assertEquals(1, vm.currentState.retryKey)
        verify(exactly = 2) { offlineSources.localFile(any(), "content://video", "Anime") }
    }

    @Test
    fun `speed is clamped and the same speed is ignored`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.SpeedSelected(1.5f))
        vm.setEvent(PlayerState.Event.SpeedSelected(1.5f))
        assertEquals(1.5f, vm.currentState.selectedSpeed)
        verify(exactly = 1) { analytics.eventSpeedSelected(ANIME_ID, 1.5f) }

        vm.setEvent(PlayerState.Event.SpeedSelected(0f))
        assertEquals(0.1f, vm.currentState.selectedSpeed)
    }

    @Test
    fun `quality selection stores the quality and the resume position`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.QualitySelected("1080p", currentPosMs = 12_000L))

        assertEquals("1080p", vm.currentState.selectedQuality)
        assertEquals(12_000L, vm.currentState.resumeFromMs)
        verify(exactly = 1) { allohaSource.onQualitySelected("1080p") }
    }

    @Test
    fun `current quality selection changes nothing`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.QualitySelected("720p", currentPosMs = 12_000L))

        verify(exactly = 0) { allohaSource.onQualitySelected(any()) }
    }

    @Test
    fun `resize mode is applied through the display settings`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.ResizeModeSelected(PlayerResizeMode.ZOOM))

        verify(exactly = 1) { displaySettings.selectResizeMode(any(), PlayerResizeMode.ZOOM) }
    }

    @Test
    fun `position updates are stored and only played ones are counted as watched`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.PlaybackPositionChanged(positionMs = 5_000L, durationMs = 60_000L))
        assertEquals(5_000L, vm.currentState.playbackPositionMs)
        assertEquals(60_000L, vm.currentState.playbackDurationMs)
        verify(exactly = 0) { playbackProgressHandler.recordWatchedTick(any(), any(), any()) }

        vm.setEvent(PlayerState.Event.PlaybackPositionChanged(positionMs = 6_000L, durationMs = 60_000L, isPlayed = true))
        verify(exactly = 1) { playbackProgressHandler.recordWatchedTick(any(), 6_000L, 60_000L) }
    }

    @Test
    fun `position of a foreign source is ignored`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.PlaybackPositionChanged(5_000L, 60_000L, episodeUrl = "https://other"))

        assertEquals(0L, vm.currentState.playbackPositionMs)
    }

    @Test
    fun `progress snapshot is saved in the background`() {
        val request: PlayerProgressSaveRequest = mockk()
        every { playbackProgressHandler.progressSaveRequest(any(), any()) } returns request
        val snapshot = PlayerProgressSnapshot("1", "url", 1, "Kodik", "Dub", "", 1_000L, 60_000L)
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.SaveProgress(snapshot))

        coVerify(exactly = 1) { playbackProgressHandler.saveProgress(request) }
    }

    @Test
    fun `failed progress save does not break the screen`() {
        coEvery { playbackProgressHandler.saveProgress(any()) } throws IllegalStateException("room")
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.SaveProgress(PlayerProgressSnapshot("1", "url", 1, "Kodik", "Dub", "", 1L, 2L)))

        assertEquals(STREAM_URL, vm.currentState.streamUrl)
        verify(exactly = 0) { errorHandler.parse(any(), any(), any(), any()) }
    }

    @Test
    fun `skip segment is tracked`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.SkipSegmentSelected(PlayerSkipType.Opening, 0L, 90_000L))

        verify(exactly = 1) { analytics.eventSkipSegmentSelected(any(), PlayerSkipType.Opening, 0L, 90_000L) }
    }

    @Test
    fun `navigation events go through the delegate`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.Back)
        vm.setEvent(PlayerState.Event.OpenDetails)
        vm.setEvent(PlayerState.Event.RateTitle)
        vm.setEvent(PlayerState.Event.ManageSubscriptions)
        vm.setEvent(PlayerState.Event.TvAppBackgrounded)

        verify(exactly = 1) { navigation.back(any(), any()) }
        verify(exactly = 1) { navigation.openDetails(any()) }
        verify(exactly = 1) { navigation.openRating(any()) }
        verify(exactly = 1) { navigation.openSubscriptions(any()) }
        verify(exactly = 1) { navigation.returnToDetailsAfterTvBackground(any(), any()) }
    }

    @Test
    fun `tutorials are dismissed through the preferences`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.MobileGestureTutorialDismissed)
        vm.setEvent(PlayerState.Event.TvControlsTutorialDismissed)

        verify(exactly = 1) { preferences.dismissMobileGestureTutorial(any()) }
        verify(exactly = 1) { preferences.dismissTvControlsTutorial(any()) }
    }

    @Test
    fun `destination with the same content is not reloaded`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.NavigateToDestination(dest()))

        coVerify(exactly = 1) {
            sourceStreamHandler.resolveStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `new destination closes the old sessions and loads the new episode`() {
        val vm = createViewModel()

        vm.setEvent(PlayerState.Event.NavigateToDestination(dest().copy(episode = "2", iframeUrl = "https://kodik.example/seria/2")))

        assertEquals("Anime", vm.currentState.animeTitle)
        verify(atLeast = 1) { allohaSource.close(any()) }
        coVerify(exactly = 2) {
            sourceStreamHandler.resolveStream(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private companion object {
        const val ANIME_ID = 42
        const val STREAM_URL = "https://cdn.example/master.m3u8"
    }
}
