package su.afk.yummy.tv.source.ashdi.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AshdiUtilsTest {

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    @Test
    fun `only ashdi vod pages are accepted`() {
        assertTrue(isAshdiVodUrl("https://ashdi.vip/vod/104245"))
        assertTrue(isAshdiVodUrl("https://ashdi.vip/vod/104245?player=animeon.club"))
        assertFalse(isAshdiVodUrl("https://moonanime.art/iframe/abc/?api=1"))
        assertFalse(isAshdiVodUrl("https://evil.example/ashdi.vip/vod/1"))
        assertFalse(isAshdiVodUrl("http://ashdi.vip/vod/1"))
    }

    @Test
    fun `master url comes from the playerjs file field and the geo stub gives none`() {
        assertEquals(
            "https://ashdi.vip/video28/1/serials/fanvox_sousou_no_frieren/frren_knczya_shlyaxu__01_104245/hls/BKaKlHKJjudZnhH+Aoo=/index.m3u8",
            ashdiMaster(resource("ashdi-page.html")),
        )
        assertNull(ashdiMaster("<html><h1>Плеєр недоступний для перегляду</h1></html>"))
    }

    @Test
    fun `geo stub is recognised and a normal player page is not`() {
        val stub = "<html><head><title>Змініть країну перегляду</title></head><body><h1>Плеєр недоступний для перегляду</h1>в цій країні</body></html>"

        assertTrue(ashdiRegionBlocked(stub))
        assertFalse(ashdiRegionBlocked(resource("ashdi-page.html")))
    }

    @Test
    fun `qualities go by ascending height with their own variant urls`() {
        val qualities = ashdiQualities(resource("ashdi-master.m3u8"))

        assertEquals(listOf("480p", "720p", "1080p"), qualities.keys.toList())
        assertTrue(qualities.getValue("720p").contains("/hls/720/"))
        assertEquals(emptyMap<String, String>(), ashdiQualities("not a playlist"))
    }
}
