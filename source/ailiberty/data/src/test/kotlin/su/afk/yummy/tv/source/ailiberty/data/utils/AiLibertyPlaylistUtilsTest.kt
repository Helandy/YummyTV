package su.afk.yummy.tv.source.ailiberty.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiLibertyPlaylistUtilsTest {

    private val html = requireNotNull(javaClass.classLoader?.getResourceAsStream("ailiberty-release.html")).readBytes().decodeToString()

    @Test
    fun `qualities are absolute and ordered from worst to best`() {
        val qualities = requireNotNull(aiLibertyEpisodeQualities(html, 1))

        assertEquals(listOf("480p", "720p", "1080p"), qualities.keys.toList())
        assertEquals("https://ailiberty.top/video/playlists/a480.m3u8?expires=1&signature=x", qualities["480p"])
    }

    @Test
    fun `absolute links stay as they are and order is by height not by position`() {
        val qualities = requireNotNull(aiLibertyEpisodeQualities(html, 2))

        assertEquals(listOf("480p", "1080p"), qualities.keys.toList())
        assertEquals("https://cdn.example/b480.m3u8", qualities["480p"])
    }

    @Test
    fun `episode without a number in the title is found by position`() {
        val qualities = requireNotNull(aiLibertyEpisodeQualities(html, 3))

        assertEquals(listOf("720p"), qualities.keys.toList())
    }

    @Test
    fun `missing episode or playlist gives null`() {
        assertNull(aiLibertyEpisodeQualities(html, 4))
        assertNull(aiLibertyEpisodeQualities("<html>нет плеера</html>", 1))
    }

    @Test
    fun `release url is built from the slug`() {
        assertEquals("https://ailiberty.top/releases/psyren", aiLibertyReleaseUrl("psyren"))
    }
}
