package su.afk.yummy.tv.source.aniboom.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniboomPlaylistUtilsTest {

    private val masterUrl = "https://cdn.example/er/ER1/master.m3u8"

    private val master = """
        #EXTM3U
        #EXT-X-VERSION:7
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="group_A1",NAME="audio_1",DEFAULT=YES,URI="media_1.m3u8"
        #EXT-X-STREAM-INF:BANDWIDTH=596406,RESOLUTION=640x360,CODECS="avc1.640028,mp4a.40.2",AUDIO="group_A1"
        media_0.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=1596399,RESOLUTION=1280x720,CODECS="avc1.640028,mp4a.40.2",AUDIO="group_A1"
        media_4.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=946399,RESOLUTION=854x480,CODECS="avc1.640028,mp4a.40.2",AUDIO="group_A1"
        media_2.m3u8
    """.trimIndent()

    @Test
    fun `qualities are ascending and cap the height on the same master`() {
        val qualities = aniboomQualities(master, masterUrl)

        assertEquals(listOf("360p", "480p", "720p"), qualities.keys.toList())
        assertEquals("$masterUrl#yummy-max-height=720", qualities.getValue("720p"))
    }

    @Test
    fun `single variant or foreign text gives no qualities`() {
        val single = master.lines().take(5).joinToString("\n")
        assertTrue(aniboomQualities(single, masterUrl).isEmpty())
        assertTrue(aniboomQualities("<html></html>", masterUrl).isEmpty())
    }
}
