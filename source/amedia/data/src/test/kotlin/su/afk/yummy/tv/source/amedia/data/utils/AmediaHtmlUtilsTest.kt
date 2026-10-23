package su.afk.yummy.tv.source.amedia.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AmediaHtmlUtilsTest : BaseUnitTest() {

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
