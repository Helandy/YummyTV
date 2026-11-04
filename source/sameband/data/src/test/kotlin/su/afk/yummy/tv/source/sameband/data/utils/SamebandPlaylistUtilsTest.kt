package su.afk.yummy.tv.source.sameband.data.utils

import su.afk.yummy.tv.source.sameband.data.mapper.toEpisodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SamebandPlaylistUtilsTest {

    private val playlist = javaClass.classLoader!!.getResource("sameband-playlist.json")!!.readText()

    @Test
    fun `player and playlist urls are resolved against the site`() {
        val page = """<div><iframe src="/v/play/Grand_Blue_3.html" allowfullscreen></iframe></div>"""
        val player = """new Playerjs({id:"pl", file: "/v/list/Grand_Blue_3_list.txt", poster:""})"""

        assertEquals("https://sameband.studio/v/play/Grand_Blue_3.html", samebandPlayerUrl(page))
        assertEquals("https://sameband.studio/v/list/Grand_Blue_3_list.txt", samebandPlaylistUrl(player))
        assertNull(samebandPlayerUrl("<iframe src=\"https://youtube.com/embed/x\"></iframe>"))
        assertNull(samebandPlaylistUrl("nothing"))
    }

    @Test
    fun `qualities are ascending with absolute urls`() {
        val qualities = samebandQualities("[1080p]/v/a/0/index.m3u8,[480p]/v/a/2/index.m3u8,[720p]https://cdn/x/1/index.m3u8")

        assertEquals(listOf("480p", "720p", "1080p"), qualities.keys.toList())
        assertEquals("https://sameband.studio/v/a/2/index.m3u8", qualities["480p"])
        assertEquals("https://cdn/x/1/index.m3u8", qualities["720p"])
    }

    @Test
    fun `paths with spaces are kept whole and encoded`() {
        val file = "[480p]/v/anime/Grand Blue Season 3/Grand Blue Season 3 - 01_RUS_2/index.m3u8," +
            "[720p]/v/anime/Grand Blue Season 3/Grand Blue Season 3 - 01_RUS_1/index.m3u8," +
            "[1080p]/v/anime/Grand Blue Season 3/Grand Blue Season 3 - 01_RUS_0/index.m3u8"

        val qualities = samebandQualities(file)

        assertEquals(listOf("480p", "720p", "1080p"), qualities.keys.toList())
        assertEquals(
            "https://sameband.studio/v/anime/Grand%20Blue%20Season%203/Grand%20Blue%20Season%203%20-%2001_RUS_0/index.m3u8",
            qualities["1080p"],
        )
    }

    @Test
    fun `episodes come from titles and skip items without files`() {
        val episodes = parseSamebandPlaylist(playlist).toEpisodes()

        assertEquals(listOf(1, 2), episodes.map { it.number })
        assertEquals(listOf("480p", "720p", "1080p"), episodes[0].qualities.keys.toList())
        assertEquals("https://sameband.studio/v/a/1_0/index.m3u8", episodes[0].qualities["1080p"])
        assertEquals(listOf("480p", "1080p"), episodes[1].qualities.keys.toList())
    }

    @Test
    fun `episode number falls back to position and garbage playlist is empty`() {
        assertEquals(5, samebandEpisodeNumber("Фильм", 5))
        assertEquals(12, samebandEpisodeNumber("<i>12 серия</i>", 1))
        assertEquals(emptyList<Any>(), parseSamebandPlaylist("<html>blocked</html>"))
    }
}
