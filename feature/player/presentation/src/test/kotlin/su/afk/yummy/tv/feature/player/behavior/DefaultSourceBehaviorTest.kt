package su.afk.yummy.tv.feature.player.behavior

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackRetryHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult

class DefaultSourceBehaviorTest : BaseUnitTest() {

    private fun TestScope.createBehavior(): Pair<DefaultSourceBehavior, PlayerSourceHostMock> {
        val harness = PlayerSourceHostMock(backgroundScope, onlineState("Kodik"))
        val behavior = DefaultSourceBehavior(
            retry = PlayerPlaybackRetryHandler(),
            analytics = PlayerAnalytics(mockk(relaxed = true)),
        )
        behavior.attach(harness.host)
        return behavior to harness
    }

    @Test
    fun `silent retry re-resolves the stream while keeping the frame`() = runTest {
        val (behavior, harness) = createBehavior()

        assertTrue(behavior.onPlaybackError(playbackError()))
        assertTrue(harness.state.isPlaybackRecovering)
        assertNull(harness.state.playerError)

        runCurrent()
        assertEquals(0, harness.state.retryKey)
        verify(exactly = 1) { harness.host.closeSourceSessions() }
        val request = harness.loadRequests.single()
        assertTrue(request.forceRefresh)
        assertTrue(request.refreshSourcesOnFailure)
    }

    @Test
    fun `gives up after the retry budget is spent`() = runTest {
        val (behavior, harness) = createBehavior()

        repeat(PlayerPlaybackRetryHandler.MAX_ATTEMPTS) {
            assertTrue(behavior.onPlaybackError(playbackError()))
            runCurrent()
            behavior.onStreamResolved(PlayerStreamLoadResult.State(harness.state, false), failed = false)
        }

        assertFalse(behavior.onPlaybackError(playbackError()))
        assertEquals(PlayerPlaybackRetryHandler.MAX_ATTEMPTS, behavior.retryAttempts)
        assertEquals(PlayerPlaybackRetryHandler.MAX_ATTEMPTS, harness.loadRequests.size)
    }

    @Test
    fun `successful start frees the retry budget`() = runTest {
        val (behavior, _) = createBehavior()
        repeat(PlayerPlaybackRetryHandler.MAX_ATTEMPTS) { behavior.onPlaybackError(playbackError()) }

        behavior.onPlaybackReady()

        assertEquals(0, behavior.retryAttempts)
        assertTrue(behavior.onPlaybackError(playbackError()))
    }

    @Test
    fun `retry for a previous episode is dropped`() = runTest {
        val (behavior, harness) = createBehavior()
        behavior.onPlaybackError(playbackError())

        harness.update { onlineState("Kodik", iframeUrl = "https://kodik.example/episode-2") }
        runCurrent()

        assertTrue(harness.loadRequests.isEmpty())
    }

    @Test
    fun `offline playback is not handled`() = runTest {
        val (behavior, harness) = createBehavior()
        assertTrue(behavior.handles(harness.state))
        assertFalse(behavior.handles(harness.state.copy(isOfflinePlayback = true)))
    }
}
