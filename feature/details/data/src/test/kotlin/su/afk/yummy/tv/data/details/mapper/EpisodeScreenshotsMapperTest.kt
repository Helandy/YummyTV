package su.afk.yummy.tv.data.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeScreenshotsMapperTest {

    @Test
    fun `tenrai frames are ordered by episode number and limited`() {
        val frames = mapOf("10" to "u10", "2" to "u2", "1" to "u1", "special" to "us", "3" to "u3")

        assertEquals(listOf("u1", "u2", "u3", "u10"), frames.toOrderedFrameUrls(limit = 4))
    }

    @Test
    fun `no frames give no urls`() {
        assertEquals(emptyList<String>(), emptyMap<String, String>().toOrderedFrameUrls(limit = 10))
    }
}
