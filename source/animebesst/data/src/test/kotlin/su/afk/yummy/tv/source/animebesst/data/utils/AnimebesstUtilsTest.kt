package su.afk.yummy.tv.source.animebesst.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget

class AnimebesstUtilsTest {

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    @Test
    fun `player url comes from the video list, a range label covers both episodes`() {
        val page = resource("animebesst-page.html")

        assertEquals("https://plus4.aff1best.org/vod/96032", animebesstPlayerUrl(page, 1))
        assertEquals("https://plus4.aff1best.org/vod/96312", animebesstPlayerUrl(page, 3))
        assertEquals("https://plus4.aff1best.org/vod/96312", animebesstPlayerUrl(page, 4))
        assertNull(animebesstPlayerUrl(page, 5))
        assertNull(animebesstPlayerUrl(page, 9))
        assertNull(animebesstPlayerUrl("<html>no list</html>", 1))
    }

    @Test
    fun `master url comes from the playerjs file field`() {
        assertEquals(
            "https://plus4.aff1best.org/content/vod/serials/kikansha_no_mahou_wa_tokubetsu_desu/s02/kikansha_no_mahou_wa_tokubetsu_desu_1/hls/index.m3u8",
            animebesstMaster(resource("animebesst-player.html")),
        )
        assertNull(animebesstMaster("<html></html>"))
    }

    @Test
    fun `relative variant urls are resolved against the master and sorted by height`() {
        val masterUrl = "https://plus4.aff1best.org/content/vod/serials/x/hls/index.m3u8"

        val qualities = animebesstQualities(resource("animebesst-master.m3u8"), masterUrl)

        assertEquals(listOf("720p", "1080p"), qualities.keys.toList())
        assertEquals("https://plus4.aff1best.org/content/vod/serials/x/hls/720/index.m3u8", qualities["720p"])
        assertEquals(emptyMap<String, String>(), animebesstQualities("not a playlist", masterUrl))
    }

    @Test
    fun `source url round trips with hosts and rejects foreign shapes`() {
        val target = AnimebesstTarget("957-kikansha", 3, listOf("anime1.best", "ab2.animebesst.org"))

        assertEquals("source://animebesst/957-kikansha/3?h=anime1.best,ab2.animebesst.org", target.toSourceUrl())
        assertEquals(target, parseAnimebesstSourceUrl("source://animebesst/957-kikansha/3?h=anime1.best,ab2.animebesst.org"))
        assertNull(parseAnimebesstSourceUrl("source://animebesst/957-kikansha/3"))
        assertNull(parseAnimebesstSourceUrl("source://animebesst/957-kikansha/3?h=bad/host"))
        assertNull(parseAnimebesstSourceUrl("source://animebesst/kikansha/3?h=anime1.best"))
        assertNull(parseAnimebesstSourceUrl("source://animebesst/957-kikansha/0?h=anime1.best"))
        assertNull(parseAnimebesstSourceUrl("source://animevost/957-kikansha/3?h=anime1.best"))
    }

    @Test
    fun `title ref gives the slug and hosts main first, foreign and repeated mirrors dropped`() {
        val (slug, hosts) = requireNotNull(
            animebesstTitleRef(
                "https://anime1.best/anime1-online/957-kikansha.html",
                listOf(
                    "https://ab2.animebesst.org/anime1-online/957-kikansha.html",
                    "https://ab2.animebesst.org/anime1-online/957-kikansha.html",
                    "https://other.example/anime1-online/111-other.html",
                    "garbage",
                ),
            ),
        )

        assertEquals("957-kikansha", slug)
        assertEquals(listOf("anime1.best", "ab2.animebesst.org"), hosts)
        assertNull(animebesstTitleRef("https://anime1.best/", emptyList()))
    }
}
