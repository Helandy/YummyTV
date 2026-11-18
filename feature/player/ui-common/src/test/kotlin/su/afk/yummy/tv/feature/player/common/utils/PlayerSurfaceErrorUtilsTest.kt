package su.afk.yummy.tv.feature.player.common.utils

import androidx.media3.common.PlaybackException
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/** Распознавание отказа прошивки сменить выходную поверхность декодера (Amlogic, Android 9). */
class PlayerSurfaceErrorUtilsTest : BaseUnitTest() {

    @Test
    fun `illegal argument thrown from setOutputSurface is a surface change failure`() {
        val cause = IllegalArgumentException().apply {
            stackTrace = arrayOf(StackTraceElement("android.media.MediaCodec", "setOutputSurface", "MediaCodec.java", 1979))
        }

        assertTrue(playbackError(cause).isOutputSurfaceChangeFailure())
    }

    @Test
    fun `illegal argument from another place is not a surface change failure`() {
        val cause = IllegalArgumentException().apply {
            stackTrace = arrayOf(StackTraceElement("androidx.media3.Foo", "bar", "Foo.java", 1))
        }

        assertFalse(playbackError(cause).isOutputSurfaceChangeFailure())
    }

    @Test
    fun `other exception type from setOutputSurface is not a surface change failure`() {
        val cause = IllegalStateException().apply {
            stackTrace = arrayOf(StackTraceElement("android.media.MediaCodec", "setOutputSurface", "MediaCodec.java", 1))
        }

        assertFalse(playbackError(cause).isOutputSurfaceChangeFailure())
    }

    // Конструктор PlaybackException читает SystemClock, которого в JVM-тестах нет, поэтому ошибка — мок.
    private fun playbackError(cause: Throwable): PlaybackException = mockk {
        every { this@mockk.cause } returns cause
        every { this@mockk.stackTrace } returns emptyArray()
    }
}
