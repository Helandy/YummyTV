package su.afk.yummy.tv.data.player.extractor.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class UrlNormalizationTest : BaseUnitTest() {

    @Test
    fun `protocol-relative url becomes https`() {
        assertEquals(
            "https://cdn.example.com/a.m3u8",
            normalizeUrlScheme("//cdn.example.com/a.m3u8"),
        )
    }

    @Test
    fun `plain http is upgraded to https`() {
        assertEquals(
            "https://cdn.example.com/a.m3u8",
            normalizeUrlScheme("http://cdn.example.com/a.m3u8"),
        )
    }

    @Test
    fun `https passes through unchanged`() {
        assertEquals(
            "https://cdn.example.com/a.m3u8",
            normalizeUrlScheme("https://cdn.example.com/a.m3u8"),
        )
    }

    @Test
    fun `bare host is prefixed with https`() {
        assertEquals("https://cdn.example.com/a.m3u8", normalizeUrlScheme("cdn.example.com/a.m3u8"))
    }

    @Test
    fun `hasKnownUrlScheme recognizes protocol-relative and absolute urls only`() {
        assertEquals(true, "//cdn.example.com".hasKnownUrlScheme())
        assertEquals(true, "http://cdn.example.com".hasKnownUrlScheme())
        assertEquals(true, "https://cdn.example.com".hasKnownUrlScheme())
        assertEquals(false, "/relative/path".hasKnownUrlScheme())
        assertEquals(false, "cdn.example.com".hasKnownUrlScheme())
    }

    @Test
    fun `resolveRelativeUrl resolves against base url`() {
        val resolved = resolveRelativeUrl(
            raw = "/api/video/abc",
            baseUrl = "https://player.aksor.tv/embed",
            fallback = { "unused" },
        )
        assertEquals("https://player.aksor.tv/api/video/abc", resolved)
    }

    @Test
    fun `resolveRelativeUrl falls back when base url is blank`() {
        val resolved =
            resolveRelativeUrl(raw = "abc", baseUrl = "", fallback = { "https://fallback/abc" })
        assertEquals("https://fallback/abc", resolved)
    }

    @Test
    fun `resolveRelativeUrl falls back when base url is malformed`() {
        val resolved = resolveRelativeUrl(
            raw = "abc",
            baseUrl = "not a url",
            fallback = { "https://fallback/abc" },
        )
        assertEquals("https://fallback/abc", resolved)
    }

    // Sibnet's own normalizeUrl deliberately does NOT catch URL-resolution failures (pre-existing
    // behaviour, preserved as-is rather than "fixed" as part of this cleanup) - callers that need
    // that must go through java.net.URL directly rather than resolveRelativeUrl's catching variant.
    @Test
    fun `URL resolution without a catching helper throws on a malformed base`() {
        assertThrows(java.net.MalformedURLException::class.java) {
            java.net.URL(java.net.URL("not a url"), "abc")
        }
    }

    @Test
    fun `decodeUnicodeEscapes decodes hex escapes and common JS escape sequences`() {
        assertEquals("a/b", decodeUnicodeEscapes("a\\u002fb"))
        assertEquals("a&b", decodeUnicodeEscapes("a\\u0026b"))
        assertEquals("a-b", decodeUnicodeEscapes("a\\u002Db"))
        assertEquals("plain text", decodeUnicodeEscapes("plain text"))
    }
}
