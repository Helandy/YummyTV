package su.afk.yummy.tv.feature.player.common.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerEndPromptUtilsTest : BaseUnitTest() {

    @Test
    fun `malformed container at the very end of the stream is the end of the episode`() {
        // Реальный отчёт: position_ms=1455060 при манифесте на 1455.1 с.
        assertTrue(isEndOfStreamError("ERROR_CODE_PARSING_CONTAINER_MALFORMED", 1_455_060L, 1_455_100L))
    }

    @Test
    fun `malformed container in the middle is a real error`() {
        assertFalse(isEndOfStreamError("ERROR_CODE_PARSING_CONTAINER_MALFORMED", 793_041L, 1_455_100L))
    }

    @Test
    fun `other error codes at the end are still errors`() {
        assertFalse(isEndOfStreamError("ERROR_CODE_DECODING_FAILED", 1_455_060L, 1_455_100L))
        assertFalse(isEndOfStreamError(null, 1_455_060L, 1_455_100L))
    }

    @Test
    fun `unknown duration is never the end`() {
        assertFalse(isEndOfStreamError("ERROR_CODE_PARSING_CONTAINER_MALFORMED", 0L, 0L))
        assertFalse(isEndOfStreamError("ERROR_CODE_PARSING_CONTAINER_MALFORMED", 0L, -1L))
    }
}
