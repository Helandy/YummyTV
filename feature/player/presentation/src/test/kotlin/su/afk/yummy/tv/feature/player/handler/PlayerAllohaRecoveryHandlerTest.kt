package su.afk.yummy.tv.feature.player.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerAllohaRecoveryHandlerTest : BaseUnitTest() {

    @Test
    fun `start remembers the position and quality and clamps a negative position`() {
        val handler = PlayerAllohaRecoveryHandler()

        handler.start(positionMs = -5L, selectedQuality = "1080p")

        assertTrue(handler.isRecovering)
        assertEquals(0L, handler.positionMs)
        assertEquals("1080p", handler.selectedQuality)
        assertEquals(0, handler.retryCount)
    }

    @Test
    fun `attempts are bounded`() {
        val handler = PlayerAllohaRecoveryHandler()
        handler.start(positionMs = 1_000L, selectedQuality = null)

        repeat(PlayerAllohaRecoveryHandler.MAX_ATTEMPTS) { handler.nextAttempt() }

        assertFalse(handler.canRetry())
    }

    @Test
    fun `complete returns the attempt count and resets the cycle`() {
        val handler = PlayerAllohaRecoveryHandler()
        handler.start(positionMs = 1_000L, selectedQuality = "720p")
        handler.nextAttempt()
        handler.nextAttempt()

        assertEquals(2, handler.complete())
        assertFalse(handler.isRecovering)
        assertNull(handler.selectedQuality)
        assertEquals(0L, handler.positionMs)
        assertTrue(handler.canRetry())
    }
}
