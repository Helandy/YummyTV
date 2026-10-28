package su.afk.yummy.tv.source.fenix.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.source.fenix.data.model.FenixPage

class FenixUtilsTest {

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private val embedPage = resource("fenix-embed-page.html")
    private val vodPage = resource("fenix-vod-page.html")

    private fun embed(ep: Int?, edition: String? = null) = FenixPage("/embed/abc.def", isVod = false, episode = ep, edition = edition)

    @Test
    fun `only fenix vod and embed pages are accepted, the query is parsed and the fragment dropped`() {
        assertEquals(FenixPage("/vod/a.b-c_d", isVod = true, episode = null, edition = null), parseFenixPage("https://fenixplay.xyz/vod/a.b-c_d#embed=https%3A%2F%2Fx&ep=1"))
        assertEquals(embed(2, "mini"), parseFenixPage("https://fenixplay.xyz/embed/abc.def?ep=2&edition=MINI"))
        assertEquals(embed(null), parseFenixPage("https://fenixplay.xyz/embed/abc.def?ep=0&edition="))
        assertNull(parseFenixPage("https://evil.example/fenixplay.xyz/vod/abc"))
        assertNull(parseFenixPage("https://fenixplay.xyz.evil.example/vod/abc"))
        assertNull(parseFenixPage("http://fenixplay.xyz/vod/abc"))
        assertNull(parseFenixPage("https://fenixplay.xyz/vod/a/b"))
        assertNull(parseFenixPage("https://tortuga.tw/vod/1"))
    }

    @Test
    fun `vod page url is built from a clean token only`() {
        assertEquals("https://fenixplay.xyz/vod/a.b", fenixVodPageUrl("a.b"))
        assertNull(fenixVodPageUrl("a/b"))
        assertNull(fenixVodPageUrl(""))
    }

    @Test
    fun `playlist and token are read from the page even with braces and quotes in strings`() {
        assertEquals(3, fenixPlaylist(embedPage)?.episodes?.size)
        assertEquals("media.tok.sig", fenixAccessToken(embedPage))
        assertNull(fenixPlaylist("<html>no player</html>"))
        assertNull(fenixAccessToken("<html>no player</html>"))
        assertNull(fenixPlaylist("window.FENIX_PLAYLIST = {\"episodes\": ["))
    }

    @Test
    fun `embed picks the episode by number and edition and the track by label ignoring case`() {
        val playlist = requireNotNull(fenixPlaylist(embedPage))

        assertEquals("https://media.fenixplay.xyz/anime/friren/ep002/fanvox/master.m3u8?v=2", fenixMasterUrl(playlist, embed(2), "fanvoxua"))
        assertEquals("https://media.fenixplay.xyz/anime/friren/ep001/orig/master.m3u8?v=1", fenixMasterUrl(playlist, embed(1), null))
        assertEquals("https://media.fenixplay.xyz/anime/friren/ep001/orig/master.m3u8?v=1", fenixMasterUrl(playlist, embed(1), "Unknown dub"))
        assertEquals("https://media.fenixplay.xyz/anime/friren/ep002/mini/master.m3u8?v=3", fenixMasterUrl(playlist, embed(2, "mini"), "FanVoxUa"))
    }

    @Test
    fun `embed without a known episode gives none`() {
        val playlist = requireNotNull(fenixPlaylist(embedPage))

        assertNull(fenixMasterUrl(playlist, embed(null), null))
        assertNull(fenixMasterUrl(playlist, embed(99), null))
        assertNull(fenixMasterUrl(playlist, embed(1, "mini"), null))
    }

    @Test
    fun `vod page plays the track whose vod url is the page path`() {
        val playlist = requireNotNull(fenixPlaylist(vodPage))

        assertEquals(
            "https://media.fenixplay.xyz/anime/naruto/ep001/qtv/master.m3u8?v=9",
            fenixMasterUrl(playlist, FenixPage("/vod/qtv.sig", isVod = true, episode = null, edition = null), null),
        )
        assertEquals(
            "https://media.fenixplay.xyz/anime/naruto/ep001/orig/master.m3u8?v=9",
            fenixMasterUrl(playlist, FenixPage("/vod/other.sig", isVod = true, episode = null, edition = null), null),
        )
    }

    @Test
    fun `token is appended to fenix hosts only`() {
        assertEquals("https://media.fenixplay.xyz/m.m3u8?v=1&token=t", signedFenixMaster("https://media.fenixplay.xyz/m.m3u8?v=1", "t"))
        assertEquals("https://media.fenixplay.xyz/m.m3u8?token=t", signedFenixMaster("https://media.fenixplay.xyz/m.m3u8", "t"))
        assertEquals("https://media.fenixplay.xyz/m.m3u8?token=old", signedFenixMaster("https://media.fenixplay.xyz/m.m3u8?token=old", "t"))
        assertNull(signedFenixMaster("https://evil.example/fenixplay.xyz/m.m3u8", "t"))
        assertNull(signedFenixMaster("https://fenixplay.xyz.evil.example/m.m3u8", "t"))
        assertNull(signedFenixMaster("http://media.fenixplay.xyz/m.m3u8", "t"))
        assertNotNull(signedFenixMaster("https://cf-media.fenixplay.xyz/m.m3u8", "t"))
    }
}
