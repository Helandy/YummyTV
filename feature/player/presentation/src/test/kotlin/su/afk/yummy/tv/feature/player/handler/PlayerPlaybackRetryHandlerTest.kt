package su.afk.yummy.tv.feature.player.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerPlaybackRetryHandlerTest : BaseUnitTest() {

    @Test
    fun `retries are allowed until the budget is spent`() {
        val handler = PlayerPlaybackRetryHandler()

        repeat(PlayerPlaybackRetryHandler.MAX_ATTEMPTS) { attempt ->
            assertTrue(handler.canRetry())
            assertEquals(attempt + 1, handler.next())
        }

        assertFalse(handler.canRetry())
    }

    @Test
    fun `reset restores the full budget`() {
        val handler = PlayerPlaybackRetryHandler()
        repeat(PlayerPlaybackRetryHandler.MAX_ATTEMPTS) { handler.next() }

        handler.reset()

        assertEquals(0, handler.attempts)
        assertTrue(handler.canRetry())
    }
}
