package su.afk.yummy.tv.source.torrent.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.data.source.TorrentTarget

class TorrentSourceUrlsTest : BaseUnitTest() {

    @Test
    fun `round trip keeps cyrillic spaces and special characters in queries`() {
        val target = TorrentTarget(42, "7", listOf("Клинок, рассекающий демонов & Ко", "Kimetsu no Yaiba"))

        val url = target.toSourceUrl()

        assertTrue(url.startsWith("source://torrent/42/7?q="))
        assertEquals(target, parseTorrentSourceUrl(url))
    }

    @Test
    fun `foreign or malformed urls are not parsed`() {
        assertFalse(isTorrentSourceUrl("source://anime4k/1/1/1/x"))
        assertNull(parseTorrentSourceUrl("source://anime4k/1/1/1/x"))
        assertNull(parseTorrentSourceUrl("source://torrent/abc/1?q=x"))
        assertNull(parseTorrentSourceUrl("source://torrent/1/1"))
    }

    @Test
    fun `base url gets http scheme and loses trailing slash`() {
        assertEquals("http://192.168.1.10:8090", normalizeBaseUrl(" 192.168.1.10:8090/ "))
        assertEquals("https://jac-red.ru", normalizeBaseUrl("https://jac-red.ru/"))
        assertEquals("", normalizeBaseUrl("  "))
    }

    @Test
    fun `basic auth header only with login`() {
        assertTrue(basicAuthHeaders("", "x").isEmpty())
        assertEquals("Basic dXNlcjpwYXNz", basicAuthHeaders("user", "pass")["Authorization"])
    }
}
