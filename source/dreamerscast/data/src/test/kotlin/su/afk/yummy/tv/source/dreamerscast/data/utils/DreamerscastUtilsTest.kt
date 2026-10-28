package su.afk.yummy.tv.source.dreamerscast.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget

class DreamerscastUtilsTest {

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    @Test
    fun `playlist is decoded from the obfuscated playerjs, nested junk included`() {
        val playlist = requireNotNull(dreamerscastPlaylist(resource("dreamerscast-release.html")))

        assertEquals(listOf("Серия 1", "Серия 2", "Серия 3", "Серия 4", "Серия 5"), playlist.map { it.first })
        assertEquals(true, playlist[0].second.contains(" or https://play.dreamerscast.com/hls/312db18b-c489-4fa4-8ec2-8a79c698822c/"))
    }

    @Test
    fun `broken or missing playerjs gives no playlist`() {
        assertNull(dreamerscastPlaylist("<html>no player</html>"))
        assertNull(dreamerscastPlaylist("""<script>new Playerjs("#2!!!");</script>"""))
        assertNull(dreamerscastPlaylist("""<script>new Playerjs("#2YWJj");</script>"""))
    }

    @Test
    fun `hls master is taken from the dash-or-hls field by the episode number`() {
        val playlist = requireNotNull(dreamerscastPlaylist(resource("dreamerscast-release.html")))

        assertEquals(
            "https://play.dreamerscast.com/hls/312db18b-c489-4fa4-8ec2-8a79c698822c/88a30fac-2e1f-4390-9a46-6a9e81018ea3_,1080,720,low,aac,.mp4.urlset/master.m3u8",
            dreamerscastHls(playlist, 1),
        )
        assertEquals(true, dreamerscastHls(playlist, 5)?.endsWith("/master.m3u8"))
        assertNull(dreamerscastHls(playlist, 6))
    }

    @Test
    fun `an unnumbered title is matched by position, a foreign host is ignored`() {
        val hls = "https://play.dreamerscast.com/hls/a/b/master.m3u8"

        assertEquals(hls, dreamerscastHls(listOf("Фильм" to "https://play.dreamerscast.com/dash/a/b/manifest.mpd or $hls"), 1))
        assertNull(dreamerscastHls(listOf("Серия 1" to "https://evil.example/hls/a/master.m3u8"), 1))
        assertNull(dreamerscastHls(listOf("Серия 1" to "https://play.dreamerscast.com/dash/a/manifest.mpd"), 1))
    }

    @Test
    fun `qualities skip the audio-only track and go by ascending height`() {
        val qualities = dreamerscastQualities(resource("dreamerscast-master.m3u8"))

        assertEquals(listOf("480p", "720p", "1080p"), qualities.keys.toList())
        assertEquals(true, qualities.getValue("1080p").endsWith("index-f1-v1-f4-a1.m3u8"))
        assertEquals(emptyMap<String, String>(), dreamerscastQualities("not a playlist"))
    }

    @Test
    fun `source url round trips and slug comes from the page url`() {
        val target = DreamerscastTarget("618-tensei-goblin", 3)

        assertEquals("source://dreamerscast/618-tensei-goblin/3", target.toSourceUrl())
        assertEquals(target, parseDreamerscastSourceUrl("source://dreamerscast/618-tensei-goblin/3"))
        assertNull(parseDreamerscastSourceUrl("source://dreamerscast/tensei/3"))
        assertNull(parseDreamerscastSourceUrl("source://dreamerscast/618-tensei-goblin/0"))
        assertNull(parseDreamerscastSourceUrl("source://animebesst/618-tensei-goblin/3"))
        assertEquals("618-tensei-goblin", dreamerscastSlug("https://dreamerscast.com/home/release/618-tensei-goblin"))
        assertNull(dreamerscastSlug("https://dreamerscast.com/home/schedule"))
    }
}
