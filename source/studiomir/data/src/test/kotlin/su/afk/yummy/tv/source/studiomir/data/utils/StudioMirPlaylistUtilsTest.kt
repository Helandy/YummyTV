package su.afk.yummy.tv.source.studiomir.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioMirPlaylistUtilsTest {

    private fun page() = requireNotNull(javaClass.classLoader?.getResourceAsStream("studiomir-page.html")).readBytes().decodeToString()

    @Test
    fun `variants of an episode go in ascending quality`() {
        val q = studioMirEpisodeQualities(page(), 3)

        assertEquals(listOf("720p", "1080p"), q.keys.toList())
        assertTrue(q.getValue("720p").endsWith("_720.m3u8"))
        assertTrue(q.getValue("1080p").contains("/encoded/107/3/"))
    }

    @Test
    fun `trailer is not an episode and unknown episode is empty`() {
        assertEquals(emptyMap<String, String>(), studioMirEpisodeQualities(page(), 0))
        assertEquals(emptyMap<String, String>(), studioMirEpisodeQualities(page(), 99))
    }

    @Test
    fun `page without player gives nothing`() {
        assertEquals(emptyMap<String, String>(), studioMirEpisodeQualities("<html>нет плеера</html>", 1))
    }

    @Test
    fun `single link without quality label becomes auto`() {
        val html = """new Playerjs({id:"x", file: [{"title":"Серия 1","file":"https://a/b.m3u8"}] })"""

        assertEquals(mapOf("Auto" to "https://a/b.m3u8"), studioMirEpisodeQualities(html, 1))
    }
}
