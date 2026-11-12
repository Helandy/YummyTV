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

    @Test
    fun `last failure survives the cycle and is cleared by reset`() {
        val handler = PlayerAllohaRecoveryHandler()
        handler.start(positionMs = 1_000L, selectedQuality = null)
        handler.lastFailure = "ERROR_CODE_IO_BAD_HTTP_STATUS http=403"
        handler.nextAttempt()

        assertEquals("ERROR_CODE_IO_BAD_HTTP_STATUS http=403", handler.lastFailure)

        handler.reset()

        assertNull(handler.lastFailure)
    }

    private fun handlerAt(time: LongArray) = PlayerAllohaRecoveryHandler().apply { clock = { time[0] } }

    @Test
    fun `an error right after a recovery at the same place continues the attempt count`() {
        val time = longArrayOf(0L)
        val handler = handlerAt(time)
        handler.start(positionMs = 455_000L, selectedQuality = null)
        handler.nextAttempt()
        handler.complete()

        time[0] = 2_000L
        handler.start(positionMs = 455_800L, selectedQuality = null)

        assertEquals(1, handler.retryCount)
    }

    @Test
    fun `repeated failures at one place run out of attempts`() {
        val time = longArrayOf(0L)
        val handler = handlerAt(time)

        repeat(PlayerAllohaRecoveryHandler.MAX_ATTEMPTS) {
            handler.start(positionMs = 455_000L, selectedQuality = null)
            handler.nextAttempt()
            handler.complete()
            time[0] += 2_000L
        }
        handler.start(positionMs = 455_000L, selectedQuality = null)

        assertFalse(handler.canRetry())
    }

    @Test
    fun `a failure later or elsewhere starts a fresh budget`() {
        val time = longArrayOf(0L)
        val handler = handlerAt(time)
        handler.start(positionMs = 455_000L, selectedQuality = null)
        handler.nextAttempt()
        handler.complete()

        time[0] = 120_000L
        handler.start(positionMs = 455_000L, selectedQuality = null)
        assertEquals(0, handler.retryCount)

        handler.nextAttempt()
        handler.complete()
        time[0] += 2_000L
        handler.start(positionMs = 900_000L, selectedQuality = null)
        assertEquals(0, handler.retryCount)
    }

    @Test
    fun `reset forgets the streak`() {
        val time = longArrayOf(0L)
        val handler = handlerAt(time)
        handler.start(positionMs = 455_000L, selectedQuality = null)
        handler.nextAttempt()
        handler.complete()
        handler.reset()

        time[0] = 1_000L
        handler.start(positionMs = 455_000L, selectedQuality = null)

        assertEquals(0, handler.retryCount)
    }
}
