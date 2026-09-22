package su.afk.yummy.tv.feature.player.behavior

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.AllohaTrackPreference
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository
import su.afk.yummy.tv.domain.player.session.AllohaPlaybackSessionManager
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaRecoveryHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaSessionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaTrackPreferenceHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult

class AllohaSourceBehaviorTest {

    private fun TestScope.setUp(): Pair<AllohaSourceBehavior, FakePlayerSourceHost> {
        val host = FakePlayerSourceHost(backgroundScope, onlineState("Alloha").copy(selectedQuality = "720p"))
        val behavior = AllohaSourceBehavior(
            session = PlayerAllohaSessionHandler(NoOpSessionManager),
            recovery = PlayerAllohaRecoveryHandler(),
            trackPreference = PlayerAllohaTrackPreferenceHandler(EmptyTrackPreferenceRepository),
            analytics = PlayerAnalytics(NoOpAnalyticsTracker),
        )
        behavior.attach(host)
        return behavior to host
    }

    @Test
    fun `handles only online Alloha sources`() = runTest {
        val (behavior, host) = setUp()
        assertTrue(behavior.handles(host.state))
        assertFalse(behavior.handles(host.state.copy(isOfflinePlayback = true)))
        assertFalse(behavior.handles(onlineState("Kodik")))
    }

    @Test
    fun `playback error opens a fresh session after a delay`() = runTest {
        val (behavior, host) = setUp()

        assertTrue(behavior.onPlaybackError(playbackError(positionMs = 65_000L)))
        assertTrue(behavior.isRecovering)
        assertTrue(host.state.isPlaybackRecovering)
        assertEquals(65_000L, host.state.resumeFromMs)
        assertEquals(1, host.cancelledLoads)
        assertTrue(behavior.keepsStreamWhileResolving())

        runCurrent()
        assertTrue(host.loadRequests.isEmpty())

        advanceTimeBy(1_001L)
        val request = host.loadRequests.single()
        assertTrue(request.forceFreshAllohaSession)
        assertFalse(request.refreshSourcesOnFailure)
        assertEquals("720p", request.selectedQualityOverride)
    }

    @Test
    fun `duplicate errors during recovery are ignored`() = runTest {
        val (behavior, host) = setUp()
        behavior.onPlaybackError(playbackError())

        assertTrue(behavior.onPlaybackError(playbackError()))
        advanceTimeBy(1_001L)

        assertEquals(1, host.cancelledLoads)
        assertEquals(1, host.loadRequests.size)
    }

    @Test
    fun `recovery gives up with a real error after the attempt cap`() = runTest {
        val (behavior, host) = setUp()
        behavior.onRetryRequested()
        runCurrent()

        repeat(PlayerAllohaRecoveryHandler.MAX_ATTEMPTS - 1) {
            assertTrue(behavior.retriesFailedResolve())
            advanceTimeBy(1_001L)
        }
        assertEquals(PlayerAllohaRecoveryHandler.MAX_ATTEMPTS, host.loadRequests.size)

        assertTrue(behavior.retriesFailedResolve())

        assertFalse(behavior.isRecovering)
        assertFalse(host.state.isPlaybackRecovering)
        assertTrue(host.state.showChangePlayerHint)
        assertEquals(FakePlayerSourceHost.STREAM_ERROR, host.state.playerError)
    }

    @Test
    fun `resolved stream completes the recovery`() = runTest {
        val (behavior, host) = setUp()
        behavior.onPlaybackError(playbackError(positionMs = 10_000L))
        behavior.onPlaybackPositionChanged(12_000L)
        assertEquals(12_000L, behavior.recoveryResumePositionMs())

        val completed = behavior.onStreamResolved(PlayerStreamLoadResult.State(host.state, false), failed = false)

        assertTrue(completed)
        assertFalse(behavior.isRecovering)
        assertNull(behavior.recoveryResumePositionMs())
        advanceTimeBy(1_001L)
        assertTrue(host.loadRequests.isEmpty())
    }

    @Test
    fun `failed resolve is not retried outside of recovery`() = runTest {
        val (behavior, _) = setUp()
        assertFalse(behavior.retriesFailedResolve())
    }

    private object NoOpSessionManager : AllohaPlaybackSessionManager {
        override fun find(sourceKey: String): AllohaStreamSession? = null
        override fun activate(session: AllohaStreamSession): AllohaStreamSession = session
        override fun release(session: AllohaStreamSession, immediately: Boolean) = Unit
        override fun closeActive() = Unit
    }

    private object EmptyTrackPreferenceRepository : AllohaTrackPreferenceRepository {
        override suspend fun get(animeId: Int, dubbing: String, player: String): AllohaTrackPreference? = null
        override suspend fun save(preference: AllohaTrackPreference) = Unit
    }
}
