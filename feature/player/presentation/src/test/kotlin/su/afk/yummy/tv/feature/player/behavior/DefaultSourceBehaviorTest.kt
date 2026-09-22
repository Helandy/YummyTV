package su.afk.yummy.tv.feature.player.behavior

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackRetryHandler

class DefaultSourceBehaviorTest {

    private fun TestScope.setUp(): Pair<DefaultSourceBehavior, FakePlayerSourceHost> {
        val host = FakePlayerSourceHost(backgroundScope, onlineState("Kodik"))
        val behavior = DefaultSourceBehavior(
            retry = PlayerPlaybackRetryHandler(),
            analytics = PlayerAnalytics(NoOpAnalyticsTracker),
        )
        behavior.attach(host)
        return behavior to host
    }

    @Test
    fun `silent retry re-resolves the stream while keeping the frame`() = runTest {
        val (behavior, host) = setUp()

        assertTrue(behavior.onPlaybackError(playbackError()))
        assertTrue(host.state.isPlaybackRecovering)
        assertNull(host.state.playerError)

        runCurrent()
        assertEquals(1, host.state.retryKey)
        assertEquals(1, host.closedSessions)
        val request = host.loadRequests.single()
        assertTrue(request.forceRefresh)
        assertTrue(request.refreshSourcesOnFailure)
    }

    @Test
    fun `gives up after the retry budget is spent`() = runTest {
        val (behavior, host) = setUp()

        repeat(PlayerPlaybackRetryHandler.MAX_ATTEMPTS) {
            assertTrue(behavior.onPlaybackError(playbackError()))
            runCurrent()
        }

        assertFalse(behavior.onPlaybackError(playbackError()))
        assertEquals(PlayerPlaybackRetryHandler.MAX_ATTEMPTS, behavior.retryAttempts)
        assertEquals(PlayerPlaybackRetryHandler.MAX_ATTEMPTS, host.loadRequests.size)
    }

    @Test
    fun `successful start frees the retry budget`() = runTest {
        val (behavior, _) = setUp()
        repeat(PlayerPlaybackRetryHandler.MAX_ATTEMPTS) { behavior.onPlaybackError(playbackError()) }

        behavior.onPlaybackReady()

        assertEquals(0, behavior.retryAttempts)
        assertTrue(behavior.onPlaybackError(playbackError()))
    }

    @Test
    fun `retry for a previous episode is dropped`() = runTest {
        val (behavior, host) = setUp()
        behavior.onPlaybackError(playbackError())

        host.update { onlineState("Kodik", iframeUrl = "https://kodik.example/episode-2") }
        runCurrent()

        assertTrue(host.loadRequests.isEmpty())
    }

    @Test
    fun `offline playback is not handled`() = runTest {
        val (behavior, host) = setUp()
        assertTrue(behavior.handles(host.state))
        assertFalse(behavior.handles(host.state.copy(isOfflinePlayback = true)))
    }
}
