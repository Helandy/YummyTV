package su.afk.yummy.tv.source.animevost.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget

class AnimevostUtilsTest {

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    @Test
    fun `episode id is found by the number in the label`() {
        val page = resource("animevost-page.html")

        assertEquals("1070765499", animevostEpisodeId(page, 1))
        assertEquals("1613839688", animevostEpisodeId(page, 4))
        assertNull(animevostEpisodeId(page, 5))
        assertNull(animevostEpisodeId("<html>no data</html>", 1))
    }

    @Test
    fun `qualities go by ascending height`() {
        val qualities = animevostQualities(resource("animevost-frame.html"))

        assertEquals(listOf("480p", "720p"), qualities.keys.toList())
        assertEquals("https://vn5614.tigerlips.org/720/1070765499.mp4?md5=ZhAgKxiKdP8j0e1O4SUENw&time=1793230048&d=1", qualities["720p"])
        assertEquals(emptyMap<String, String>(), animevostQualities("<html></html>"))
    }

    @Test
    fun `source url round trips and rejects foreign shapes`() {
        val target = AnimevostTarget("tv", "3640-chitose-kun", 3)

        assertEquals("source://animevost/tv/3640-chitose-kun/3", target.toSourceUrl())
        assertEquals(target, parseAnimevostSourceUrl("source://animevost/tv/3640-chitose-kun/3"))
        assertNull(parseAnimevostSourceUrl("source://animevost/tv/chitose/3"))
        assertNull(parseAnimevostSourceUrl("source://animevost/tv/3640-chitose-kun/0"))
        assertNull(parseAnimevostSourceUrl("source://anidub/tv/3640-chitose-kun/3"))
    }

    @Test
    fun `title ref comes from the page url of any mirror host`() {
        assertEquals("tv" to "3640-chitose-kun", animevostTitleRef("https://mirror.example/tip/tv/3640-chitose-kun.html"))
        assertNull(animevostTitleRef("https://animevost.org/ongoing/"))
    }
}
