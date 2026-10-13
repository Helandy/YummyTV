package su.afk.yummy.tv.feature.player.handler

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState

class PlayerPlaybackProgressHandlerTest : BaseUnitTest() {

    private val progressHandler: PlayerProgressHandler = mockk(relaxed = true)
    private val analytics: PlayerAnalytics = mockk(relaxed = true)

    @Test
    fun `leaving the screen saves the played position with a forced sync`() {
        val request = createHandler().currentProgressSaveRequest(
            state(episodeIndex = 1).copy(playbackPositionMs = 90_000L, playbackDurationMs = DURATION_MS),
        )

        assertEquals(90_000L, request?.snapshot?.positionMs)
        assertEquals("2", request?.snapshot?.episode)
        assertEquals(ANIME_ID, request?.context?.animeId)
        assertTrue(request?.forceRemoteSync == true)
    }

    @Test
    fun `before playback started the resume position is saved`() {
        val request = createHandler().currentProgressSaveRequest(
            state(episodeIndex = 1).copy(playbackPositionMs = 0L, resumeFromMs = 30_000L),
        )

        assertEquals(30_000L, request?.snapshot?.positionMs)
    }

    @Test
    fun `incomplete source gives nothing to save`() {
        val state = state(episodeIndex = 0).copy(sourceGraph = PlayerSourceGraph())

        assertNull(createHandler().currentProgressSaveRequest(state))
    }

    @Test
    fun `continue target is created for every episode but the first`() {
        val handler = createHandler()

        assertNull(handler.continueTargetRequest(state(episodeIndex = 0)))
        assertEquals("2", handler.continueTargetRequest(state(episodeIndex = 1))?.snapshot?.episode)
    }

    @Test
    fun `watched episode is reported and saved once per source`() {
        val handler = createHandler()
        val state = state(episodeIndex = 1)

        val first = handler.watchedProgressRequest(state, DURATION_MS - 60_000L, DURATION_MS)
        val repeated = handler.watchedProgressRequest(state, DURATION_MS - 30_000L, DURATION_MS)

        assertNotNull(first)
        assertNull(repeated)
        verify(exactly = 1) { analytics.eventEpisodeCompleted(state, DURATION_MS - 60_000L, DURATION_MS) }
    }

    @Test
    fun `progress short of watched is neither reported nor saved`() {
        val request = createHandler().watchedProgressRequest(state(episodeIndex = 1), 60_000L, DURATION_MS)

        assertNull(request)
        verify(exactly = 0) { analytics.eventEpisodeCompleted(any(), any(), any()) }
    }

    @Test
    fun `full completion is reported once with clamped values`() {
        val handler = createHandler()
        val state = state(episodeIndex = 1)

        handler.reportEpisodeFullyCompleted(state, positionMs = -1L, durationMs = DURATION_MS)
        handler.reportEpisodeFullyCompleted(state, positionMs = DURATION_MS, durationMs = DURATION_MS)

        verify(exactly = 1) { analytics.eventEpisodeFullyCompleted(any(), any(), any()) }
        verify { analytics.eventEpisodeFullyCompleted(state, 0L, DURATION_MS) }
    }

    @Test
    fun `played tick is recorded for the active video`() {
        createHandler().recordWatchedTick(state(episodeIndex = 1), 5_000L, DURATION_MS)

        verify { progressHandler.recordWatchedSecond(VIDEO_ID_2, 5_000L, DURATION_MS) }
    }

    @Test
    fun `requests are executed by the progress handler`() = runTest {
        val handler = createHandler()
        val state = state(episodeIndex = 1)
        val save = handler.progressSaveRequest(state, handler.currentProgressSaveRequest(state)!!.snapshot)
        val target = handler.continueTargetRequest(state)!!

        handler.saveProgress(save.copy(forceRemoteSync = true))
        handler.saveContinueTarget(target)
        handler.suppressContinueWatchingDisplay(state)

        coVerify { progressHandler.saveProgress(save.context, save.snapshot, true) }
        coVerify { progressHandler.saveContinueTarget(target.context, target.snapshot) }
        coVerify { progressHandler.suppressContinueWatchingDisplay(save.context) }
    }

    private fun state(episodeIndex: Int) = PlayerState.State(
        animeId = ANIME_ID,
        animeTitle = "Title",
        sourceGraph = PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer(
                    name = "Kodik",
                    dubbings = listOf(
                        PlayerSourceDubbing(
                            name = "AniLibria",
                            episodes = listOf(
                                PlayerSourceEpisode(id = VIDEO_ID_1, number = "1", iframeUrl = "https://kodik/1"),
                                PlayerSourceEpisode(id = VIDEO_ID_2, number = "2", iframeUrl = "https://kodik/2"),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        sourceSelection = PlayerSourceSelection(episodeIndex = episodeIndex),
    )

    private fun createHandler() = PlayerPlaybackProgressHandler(progressHandler, analytics)

    private companion object {
        const val ANIME_ID = 42
        const val VIDEO_ID_1 = 101
        const val VIDEO_ID_2 = 102
        const val DURATION_MS = 1_440_000L
    }
}
