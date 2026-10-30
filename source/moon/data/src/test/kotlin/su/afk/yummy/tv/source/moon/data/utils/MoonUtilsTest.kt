package su.afk.yummy.tv.source.moon.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoonUtilsTest {

    private fun manifest(page: String) = moonScript(page)?.let(::moonManifest)

    @Test
    fun `hls manifest is found in the decoded script`() {
        val result = manifest(MoonTestPage.page("https://cdn.example/v/index.m3u8?t=1"))

        assertEquals("https://cdn.example/v/index.m3u8?t=1", result?.url)
        assertNull(result?.qualities)
    }

    @Test
    fun `labeled list plays the best quality up to 1080 and keeps the map ascending`() {
        val result = manifest(MoonTestPage.page("[1080p]https://c/1080.mp4,[480p]https://c/480.mp4,[2160p]https://c/2160.mp4"))

        assertEquals("https://c/1080.mp4", result?.url)
        assertEquals(listOf("480p", "1080p", "2160p"), result?.qualities?.keys?.toList())
    }

    @Test
    fun `single file url is accepted, junk is not`() {
        assertEquals("https://c/v.mp4", manifest(MoonTestPage.page("https://c/v.mp4"))?.url)
        assertNull(manifest(MoonTestPage.page("not a url")))
    }

    @Test
    fun `page without payload or with a broken one gives nothing`() {
        assertNull(moonScript("<html></html>"))
        assertNull(decodeMoonPayload("AAAA"))
        assertNull(decodeMoonPayload("***"))
    }

    @Test
    fun `page links are validated, the mirror host is kept and the query is dropped`() {
        assertEquals(MoonPage("https://moonanime.art", "abcd1234"), moonPage("https://moonanime.art/iframe/abcd1234/"))
        assertEquals(MoonPage("https://moonanime.art", "abcd1234"), moonPage("https://moonanime.art/iframe/abcd1234/?player=animeon.club&api=1"))
        assertEquals(MoonPage("https://moonanime.net", "28e91vdh5vgo"), moonPage("https://moonanime.net/iframe/28e91vdh5vgo/?player=animeon.club&api=1"))
        assertEquals("https://moonanime.net/iframe/28e91vdh5vgo/", moonPage("https://moonanime.net/iframe/28e91vdh5vgo/?x=1")?.url)
        assertNull(moonPage("https://evil.example/iframe/abcd1234/"))
        assertNull(moonPage("https://moonanime.art.evil.example/iframe/abcd1234/"))
        assertNull(moonPage("https://moonanime.art/iframe/../x"))
    }
}
