package su.afk.yummy.tv.feature.player.behavior

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.repository.AllohaPlaybackSessionRepository
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository
import su.afk.yummy.tv.domain.player.usecase.GetAllohaTrackPreferenceUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveAllohaAudioSelectionUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveAllohaSubtitleSelectionUseCase
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaRecoveryHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaSessionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaTrackPreferenceHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult

class AllohaSourceBehaviorTest : BaseUnitTest() {

    private val sessions: AllohaPlaybackSessionRepository = mockk(relaxed = true)
    private val trackPreferences: AllohaTrackPreferenceRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        with(sessions) {
            every { find(any()) } returns null
            every { activate(any()) } answers { firstArg() }
        }
        with(trackPreferences) {
            coEvery { get(any(), any(), any()) } returns null
        }
    }

    private fun TestScope.createBehavior(): Pair<AllohaSourceBehavior, PlayerSourceHostMock> {
        val harness = PlayerSourceHostMock(backgroundScope, onlineState("Alloha").copy(selectedQuality = "720p"))
        val behavior = AllohaSourceBehavior(
            session = PlayerAllohaSessionHandler(sessions),
            recovery = PlayerAllohaRecoveryHandler(),
            trackPreference = PlayerAllohaTrackPreferenceHandler(
                getPreference = GetAllohaTrackPreferenceUseCase(trackPreferences),
                saveAudio = SaveAllohaAudioSelectionUseCase(trackPreferences),
                saveSubtitle = SaveAllohaSubtitleSelectionUseCase(trackPreferences),
            ),
            analytics = PlayerAnalytics(mockk(relaxed = true)),
        )
        behavior.attach(harness.host)
        return behavior to harness
    }

    @Test
    fun `handles only online Alloha sources`() = runTest {
        val (behavior, harness) = createBehavior()
        assertTrue(behavior.handles(harness.state))
        assertFalse(behavior.handles(harness.state.copy(isOfflinePlayback = true)))
        assertFalse(behavior.handles(onlineState("Kodik")))
    }

    @Test
    fun `playback error opens a fresh session after a delay`() = runTest {
        val (behavior, harness) = createBehavior()

        assertTrue(behavior.onPlaybackError(playbackError(positionMs = 65_000L)))
        assertTrue(behavior.isRecovering)
        assertTrue(harness.state.isPlaybackRecovering)
        assertEquals(65_000L, harness.state.resumeFromMs)
        verify(exactly = 1) { harness.host.cancelStreamLoad() }
        assertTrue(behavior.keepsStreamWhileResolving())

        runCurrent()
        assertTrue(harness.loadRequests.isEmpty())

        advanceTimeBy(1_001L)
        val request = harness.loadRequests.single()
        assertTrue(request.forceFreshAllohaSession)
        assertFalse(request.refreshSourcesOnFailure)
        assertEquals("720p", request.selectedQualityOverride)
    }

    @Test
    fun `duplicate errors during recovery are ignored`() = runTest {
        val (behavior, harness) = createBehavior()
        behavior.onPlaybackError(playbackError())

        assertTrue(behavior.onPlaybackError(playbackError()))
        advanceTimeBy(1_001L)

        verify(exactly = 1) { harness.host.cancelStreamLoad() }
        assertEquals(1, harness.loadRequests.size)
    }

    @Test
    fun `recovery gives up with a real error after the attempt cap`() = runTest {
        val (behavior, harness) = createBehavior()
        behavior.onRetryRequested()
        runCurrent()

        repeat(PlayerAllohaRecoveryHandler.MAX_ATTEMPTS - 1) {
            assertTrue(behavior.retriesFailedResolve())
            advanceTimeBy(1_001L)
        }
        assertEquals(PlayerAllohaRecoveryHandler.MAX_ATTEMPTS, harness.loadRequests.size)

        assertTrue(behavior.retriesFailedResolve())

        assertFalse(behavior.isRecovering)
        assertFalse(harness.state.isPlaybackRecovering)
        assertTrue(harness.state.showChangePlayerHint)
        assertEquals(PlayerSourceHostMock.STREAM_ERROR, harness.state.playerError)
    }

    @Test
    fun `resolved stream completes the recovery`() = runTest {
        val (behavior, harness) = createBehavior()
        behavior.onPlaybackError(playbackError(positionMs = 10_000L))
        behavior.onPlaybackPositionChanged(12_000L)
        assertEquals(12_000L, behavior.recoveryResumePositionMs())

        val completed = behavior.onStreamResolved(PlayerStreamLoadResult.State(harness.state, false), failed = false)

        assertTrue(completed)
        assertFalse(behavior.isRecovering)
        assertNull(behavior.recoveryResumePositionMs())
        advanceTimeBy(1_001L)
        assertTrue(harness.loadRequests.isEmpty())
    }

    @Test
    fun `failed resolve is not retried outside of recovery`() = runTest {
        val (behavior, _) = createBehavior()
        assertFalse(behavior.retriesFailedResolve())
    }
}
