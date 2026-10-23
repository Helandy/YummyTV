package su.afk.yummy.tv.source.amedia.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource

class AmediaHtmlUtilsTest : BaseUnitTest() {

    private val titlePage = """
        <a data-text="2" data-vid="2" data-vlnk="https://aser.pro/vod/48111" href='https://amd.online/5753-x/episode/2/seriya-onlayn.html'  class='BatcoH-prevnext'>
        <a data-vid="1" data-vlnk="https://aser.pro/vod/47997" href="https://amd.online/5753-x/episode/1/seriya-onlayn.html" class="BatcoH sel-active">1</a>
        <a data-vid="2" data-vlnk="https://aser.pro/vod/48111" href="https://amd.online/5753-x/episode/2/seriya-onlayn.html" class="BatcoH">2</a>
        <a data-vid="3" data-vlnk="https://aser.pro/vod/48250" href="https://amd.online/5753-x/episode/3/seriya-onlayn.html" class="BatcoH">3</a>
        <a data-vid="4" data-vlnk="/index.php?do=nz&amp;id=5753" href="https://amd.online/5753-x/episode/4/seriya-onlayn.html" class="BatcoH">4</a>
    """.trimIndent()

    @Test
    fun `released episodes are parsed and unreleased are skipped`() {
        val episodes = parseAmediaEpisodes(titlePage)

        assertEquals(
            listOf(
                AmediaEpisode(2, AmediaEpisodeSource.Vod(48111)), AmediaEpisode(1, AmediaEpisodeSource.Vod(47997)),
                AmediaEpisode(2, AmediaEpisodeSource.Vod(48111)), AmediaEpisode(3, AmediaEpisodeSource.Vod(48250)),
            ),
            episodes,
        )
    }

    @Test
    fun `episode where the site embedded rutube keeps the embed`() {
        val embed = "https://rutube.ru/play/embed/be753fcea5b1c586363cf8a220c47167"
        val html = """
            <a data-vid="1" data-vlnk="$embed" href="https://amd.online/1489-x/episode/1/seriya-onlayn.html" class="BatcoH">1</a>
            <a data-vid="2" data-vlnk="https://aser.pro/vod/30155" href="https://amd.online/1489-x/episode/2/seriya-onlayn.html" class="BatcoH">2</a>
            <a data-vid="3" data-vlnk="https://rutube.ru/play/embed/" href="x" class="BatcoH">3</a>
            <a data-vid="4" data-vlnk="https://evil.example/play/embed/abc" href="x" class="BatcoH">4</a>
        """.trimIndent()

        assertEquals(
            listOf(AmediaEpisode(1, AmediaEpisodeSource.Rutube(embed)), AmediaEpisode(2, AmediaEpisodeSource.Vod(30155))),
            parseAmediaEpisodes(html),
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
