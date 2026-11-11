package su.afk.yummy.tv.source.torrent.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class TorrentResultHashTest : BaseUnitTest() {

    private fun result(link: String) = TorrentResult("t", 1, 1, 1, "tr", link)

    @Test
    fun `hex hash is lowercased`() {
        val hash = "046080624672F569188F7613B915D3222D0A0FCD"

        assertEquals(hash.lowercase(), result("magnet:?xt=urn:btih:$hash&dn=x").infoHash)
    }

    @Test
    fun `base32 hash is converted to hex`() {
        // 20 байт 0x00..0x13 в base32.
        val hex = (0..19).joinToString("") { it.toString(16).padStart(2, '0') }
        val base32 = "aaaqeayeaudaocajbifqydiob4ibceqt"

        assertEquals(hex, result("magnet:?xt=urn:btih:$base32").infoHash)
    }

    @Test
    fun `non magnet or malformed link gives null`() {
        assertNull(result("https://tracker.example/file.torrent").infoHash)
        assertNull(result("magnet:?xt=urn:btih:abc").infoHash)
    }
}
