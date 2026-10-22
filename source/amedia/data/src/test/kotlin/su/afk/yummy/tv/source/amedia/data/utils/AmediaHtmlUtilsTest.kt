package su.afk.yummy.tv.source.amedia.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode

class AmediaHtmlUtilsTest : BaseUnitTest() {

    private val titlePage = """
        <video-player
        id="myPlayer"
        data-title-id="59088"
        data-publisher-id="1080"
        data-aggregator="mali"
        season="4"
        ></video-player>
        <a data-text="2" data-vid="2" data-vlnk="https://aser.pro/vod/48111" href='https://amd.online/5753-x/episode/2/seriya-onlayn.html'  class='BatcoH-prevnext'>
        <a data-vid="1" data-vlnk="https://aser.pro/vod/47997" href="https://amd.online/5753-x/episode/1/seriya-onlayn.html" class="BatcoH sel-active">1</a>
        <a data-vid="2" data-vlnk="https://aser.pro/vod/48111" href="https://amd.online/5753-x/episode/2/seriya-onlayn.html" class="BatcoH">2</a>
        <a data-vid="3" data-vlnk="https://aser.pro/vod/48250" href="https://amd.online/5753-x/episode/3/seriya-onlayn.html" class="BatcoH">3</a>
        <a data-vid="4" data-vlnk="/index.php?do=nz&amp;id=5753" href="https://amd.online/5753-x/episode/4/seriya-onlayn.html" class="BatcoH">4</a>
    """.trimIndent()

    @Test
    fun `search links are page urls without episode pages and duplicates`() {
        val html = """
            <a href="https://amd.online/1304-tokijskie-mstiteli-2.html">a</a>
            <a href="https://amd.online/5753-x/episode/1/seriya-onlayn.html">e</a>
            <a href="https://amd.online/1304-tokijskie-mstiteli-2.html">again</a>
            <a href="https://amd.online/843-tokiyskie-mstiteli.html">b</a>
        """.trimIndent()

        assertEquals(
            listOf("https://amd.online/1304-tokijskie-mstiteli-2.html", "https://amd.online/843-tokiyskie-mstiteli.html"),
            parseAmediaSearchLinks(html),
        )
    }

    @Test
    fun `mal id comes only from the mali aggregator tag`() {
        assertEquals(59088, parseAmediaMalId(titlePage))
        assertNull(parseAmediaMalId(titlePage.replace("mali", "kinopoisk")))
        assertNull(parseAmediaMalId("<div></div>"))
    }

    @Test
    fun `released episodes are parsed and unreleased are skipped`() {
        val episodes = parseAmediaEpisodes(titlePage)

        assertEquals(
            listOf(AmediaEpisode(2, 48111), AmediaEpisode(1, 47997), AmediaEpisode(2, 48111), AmediaEpisode(3, 48250)),
            episodes,
        )
    }

    @Test
    fun `vod file is the hls master playlist`() {
        val html = """new Playerjs({
            id: "videoplayer47997",
            file: "https://aser.pro/content/stream/x/001_47997/hls/index.m3u8"
        });"""

        assertEquals("https://aser.pro/content/stream/x/001_47997/hls/index.m3u8", parseAmediaVodFile(html))
        assertNull(parseAmediaVodFile("<html></html>"))
        assertNull(parseAmediaVodFile("""Playerjs({file: "http://aser.pro/x/index.m3u8"})"""))
    }

    @Test
    fun `master playlist variants resolve relative to the master url`() {
        val master = """
            #EXTM3U
            #EXT-X-STREAM-INF:RESOLUTION=1280x720,BANDWIDTH=1046000
            ./720/index.m3u8
            #EXT-X-STREAM-INF:RESOLUTION=640x360,BANDWIDTH=464000
            ./360/index.m3u8
        """.trimIndent()

        assertEquals(
            mapOf(720 to "https://aser.pro/a/hls/720/index.m3u8", 360 to "https://aser.pro/a/hls/360/index.m3u8"),
            parseAmediaMasterPlaylist(master, "https://aser.pro/a/hls/index.m3u8"),
        )
        assertEquals(
            emptyMap<Int, String>(),
            parseAmediaMasterPlaylist("#EXT-X-STREAM-INF:RESOLUTION=1x720\nhttp://evil/720.m3u8", "https://aser.pro/a/hls/index.m3u8"),
        )
        assertEquals(emptyMap<Int, String>(), parseAmediaMasterPlaylist("#EXTM3U", "https://aser.pro/a/hls/index.m3u8"))
    }
}
