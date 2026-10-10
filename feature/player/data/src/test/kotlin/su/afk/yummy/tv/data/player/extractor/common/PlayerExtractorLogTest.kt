package su.afk.yummy.tv.data.player.extractor.common

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerExtractorLogTest : BaseUnitTest() {

    private val analytics: AnalyticsTracker = mockk()
    private val tag = slot<String>()
    private val error = slot<Throwable?>()
    private val message = slot<() -> String>()

    private fun logged(url: String, reason: String = "reason", throwable: Throwable? = null): String {
        every { analytics.log(capture(tag), captureNullable(error), capture(message)) } just runs
        analytics.logExtractorFailure("Kodik", url, reason, throwable)
        return message.captured()
    }

    @Test
    fun `message keeps host and file name and drops path and query`() {
        val text = logged("https://cdn.example/a/b/video.mp4?token=secret#frag")

        assertEquals("Kodik failed at https://cdn.example/.../video.mp4: reason", text)
        assertFalse(text.contains("secret"))
    }

    @Test
    fun `url without a file name keeps only the origin`() {
        assertEquals(
            "Kodik failed at https://cdn.example: reason",
            logged("https://cdn.example"),
        )
        assertEquals(
            "Kodik failed at https://cdn.example: reason",
            logged("https://cdn.example/dir/"),
        )
    }

    @Test
    fun `url that cannot be parsed loses its query anyway`() {
        val text = logged("https://cdn.example/a b/video.mp4?token=secret")

        assertFalse(text.contains("secret"))
    }

    @Test
    fun `entry goes under the extractor tag with the throwable`() {
        val failure = IllegalStateException("boom")

        logged("https://cdn.example/v.mp4", throwable = failure)

        assertEquals(PersistedLogTags.PLAYER_EXTRACTOR, tag.captured)
        assertSame(failure, error.captured)
    }
}
