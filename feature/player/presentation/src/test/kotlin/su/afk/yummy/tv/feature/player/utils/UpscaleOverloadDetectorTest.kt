package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class UpscaleOverloadDetectorTest : BaseUnitTest() {

    private val detector = UpscaleOverloadDetector().apply { reset(0L) }

    @Test
    fun `few dropped frames do not trigger`() {
        assertFalse(detector.onDroppedFrames(10, 6_000L))
        assertFalse(detector.onDroppedFrames(10, 8_000L))
    }

    @Test
    fun `many dropped frames inside window trigger`() {
        assertFalse(detector.onDroppedFrames(40, 6_000L))
        assertTrue(detector.onDroppedFrames(25, 9_000L))
    }

    @Test
    fun `frames outside window are not accumulated`() {
        assertFalse(detector.onDroppedFrames(40, 6_000L))
        assertFalse(detector.onDroppedFrames(40, 17_000L))
    }

    @Test
    fun `warmup ignores frames`() {
        assertFalse(detector.onDroppedFrames(100, 4_000L))
        detector.reset(20_000L)
        assertFalse(detector.onDroppedFrames(100, 22_000L))
        assertTrue(detector.onDroppedFrames(100, 26_000L))
    }

    @Test
    fun `triggers only once`() {
        assertTrue(detector.onDroppedFrames(70, 6_000L))
        assertFalse(detector.onDroppedFrames(70, 7_000L))
    }
}
