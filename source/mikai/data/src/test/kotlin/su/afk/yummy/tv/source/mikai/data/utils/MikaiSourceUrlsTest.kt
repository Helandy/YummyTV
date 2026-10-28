package su.afk.yummy.tv.source.mikai.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MikaiSourceUrlsTest {

    @Test
    fun `source url round trips and rejects foreign shapes`() {
        assertEquals("source://mikai/ashdi/221709", mikaiSourceUrl("221709"))
        assertEquals("221709", parseMikaiSourceUrl("source://mikai/ashdi/221709"))
        assertNull(parseMikaiSourceUrl("source://mikai/ashdi/abc"))
        assertNull(parseMikaiSourceUrl("source://mikai/ashdi/1/2"))
        assertNull(parseMikaiSourceUrl("source://animeon/ashdi/221709"))
    }

    @Test
    fun `vod id prefers the server id and falls back to the player link`() {
        assertEquals("1", mikaiVodId("1", "https://ashdi.vip/vod/2"))
        assertEquals("2", mikaiVodId(null, "https://ashdi.vip/vod/2?x=1"))
        assertEquals("2", mikaiVodId("tok-en", "https://ashdi.vip/vod/2"))
        assertNull(mikaiVodId(null, "https://fenixplay.xyz/vod/abc"))
        assertNull(mikaiVodId(null, null))
    }

    @Test
    fun `ashdi page url is built from the id`() {
        assertEquals("https://ashdi.vip/vod/221709", mikaiAshdiPageUrl("221709"))
    }

    @Test
    fun `fenix source url round trips and rejects foreign shapes`() {
        assertEquals("source://mikai/fenix/eyJ0.sig-1_x", mikaiFenixSourceUrl("eyJ0.sig-1_x"))
        assertEquals("eyJ0.sig-1_x", parseMikaiFenixSourceUrl("source://mikai/fenix/eyJ0.sig-1_x"))
        assertNull(parseMikaiFenixSourceUrl("source://mikai/fenix/a/b"))
        assertNull(parseMikaiFenixSourceUrl("source://mikai/fenix/"))
        assertNull(parseMikaiFenixSourceUrl("source://mikai/ashdi/1"))
        assertNull(parseMikaiSourceUrl("source://mikai/fenix/eyJ0.sig"))
    }

    @Test
    fun `fenix token prefers the server id and falls back to the link without the embed fragment`() {
        assertEquals("a.b", mikaiFenixToken("a.b", "https://fenixplay.xyz/vod/c.d"))
        assertEquals("c.d", mikaiFenixToken(null, "https://fenixplay.xyz/vod/c.d#embed=https%3A%2F%2Ffenixplay.xyz"))
        assertNull(mikaiFenixToken(null, "https://ashdi.vip/vod/1"))
        assertNull(mikaiFenixToken("bad/id", null))
    }

    @Test
    fun `fenix page url is built from the token`() {
        assertEquals("https://fenixplay.xyz/vod/a.b", mikaiFenixPageUrl("a.b"))
    }
}
