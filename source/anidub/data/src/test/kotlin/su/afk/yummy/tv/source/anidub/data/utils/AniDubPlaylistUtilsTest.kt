package su.afk.yummy.tv.source.anidub.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AniDubPlaylistUtilsTest {

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private val page = resource("anidub-page.html")
    private val playlist = resource("anidub-playlist.html")
    private val playerHash = "s10/87925c141377bb17a5ecd1a06374b524"

    @Test
    fun `player is read from the iframe and urls are built from it`() {
        val player = requireNotNull(aniDubPlayer(page))

        assertEquals("https://player26.ladonyvesna2005.info", player.host)
        assertEquals(playerHash, player.hash)
        assertEquals("https://player26.ladonyvesna2005.info/index.php?v=/$playerHash&playlist", player.playlistUrl())
        assertEquals("https://player26.ladonyvesna2005.info/vid.php?v=/s11/abc12345", player.masterUrl("s11/abc12345"))
    }

    @Test
    fun `page without a ladonyvesna player has no player`() {
        assertNull(aniDubPlayer("<html><span data=\"https://video.sibnet.ru/shell.php?videoid=1\">Серия 1</span></html>"))
    }

    @Test
    fun `episode hash is taken from the playlist by number`() {
        assertEquals("s10/87925c141377bb17a5ecd1a06374b524", aniDubEpisodeHash(playlist, 1, playerHash))
        assertEquals("s11/8bb973f5692359d62675ab918277b609", aniDubEpisodeHash(playlist, 3, playerHash))
        assertNull(aniDubEpisodeHash(playlist, 4, playerHash))
    }

    @Test
    fun `without a playlist the only episode is the player itself`() {
        assertEquals(playerHash, aniDubEpisodeHash(null, 1, playerHash))
        assertNull(aniDubEpisodeHash(null, 2, playerHash))
        assertEquals(playerHash, aniDubEpisodeHash("<html>нет серий</html>", 1, playerHash))
    }

    @Test
    fun `single playlist entry is episode one whatever its label says`() {
        val movie = """{"episodes":[{"link":"/s6/58279c8819f24f35b53fc0cbadc3f502","key":"k","label":"0 серия"}]}"""

        assertEquals("s6/58279c8819f24f35b53fc0cbadc3f502", aniDubEpisodeHash(movie, 1, "s6/58279c8819f24f35b53fc0cbadc3f502"))
        assertNull(aniDubEpisodeHash(movie, 2, "s6/58279c8819f24f35b53fc0cbadc3f502"))
    }

    @Test
    fun `episode link that is not a hash is rejected`() {
        val broken = """{"link":"\/../../etc","label":"1 серия"}"""

        assertNull(aniDubEpisodeHash(broken, 1, playerHash))
    }

    @Test
    fun `fhd is labelled by the release quality and not by the lying RESOLUTION`() {
        val master = resource("anidub-master.m3u8")

        val qualities = aniDubQualities(master, aniDubReleaseHeight(playlist))

        assertEquals(listOf("480p", "1080p"), qualities.keys.toList())
        assertEquals(
            "https://cdn5.anivids.link/vod9/s10/87925c141377bb17a5ecd1a06374b524/fhd.mp4/chunk.m3u8?md5=A&expires=1793214437",
            qualities["1080p"],
        )
    }

    @Test
    fun `release height is read from the file name, unknown defaults to 1080p`() {
        assertEquals(1080, aniDubReleaseHeight(playlist))
        assertEquals(720, aniDubReleaseHeight("""event-filename="Show.s01e01.720p.WEBRip.mp4""""))
        assertNull(aniDubReleaseHeight("""event-filename="Show.s01e01.mp4""""))
        assertNull(aniDubReleaseHeight(null))
        assertEquals(listOf("480p", "1080p"), aniDubQualities(resource("anidub-master.m3u8"), null).keys.toList())
    }

    @Test
    fun `hd and sd keep their own labels and equal heights keep the older variant`() {
        val master = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720
            https://cdn/x/fhd.mp4/chunk.m3u8?a=1
            #EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720
            https://cdn/x/hd.mp4/chunk.m3u8?a=2
            #EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=854x480
            https://cdn/x/sd.mp4/chunk.m3u8?a=3
            #EXT-X-STREAM-INF:BANDWIDTH=500000,RESOLUTION=640x360
            https://cdn/x/other.mp4/chunk.m3u8?a=4
        """.trimIndent()

        val onlyHd = aniDubQualities(master, 720)
        assertEquals(listOf("480p", "720p"), onlyHd.keys.toList())
        assertEquals("https://cdn/x/fhd.mp4/chunk.m3u8?a=1", onlyHd["720p"])

        val full = aniDubQualities(master, 1080)
        assertEquals(listOf("480p", "720p", "1080p"), full.keys.toList())
        assertEquals("https://cdn/x/hd.mp4/chunk.m3u8?a=2", full["720p"])
    }

    @Test
    fun `master without variants gives no qualities`() {
        assertEquals(emptyMap<String, String>(), aniDubQualities("#EXTM3U\n#EXTINF:10,\nseg.ts", 1080))
    }

    @Test
    fun `host of a variant and the known bad node`() {
        assertEquals("cdn5.anivids.link", aniDubHost("https://CDN5.anivids.link:8443/vod9/x/fhd.mp4/chunk.m3u8?md5=A"))
        assertEquals("", aniDubHost("relative/path"))
        assertEquals(true, isAniDubBadNode("cdn1.anivids.link"))
        assertEquals(false, isAniDubBadNode("cdn5.anivids.link"))
    }

    @Test
    fun `page url is built from the slug`() {
        assertEquals("https://online.anidub.com/12254-blich.html", aniDubPageUrl("12254-blich"))
    }
}
