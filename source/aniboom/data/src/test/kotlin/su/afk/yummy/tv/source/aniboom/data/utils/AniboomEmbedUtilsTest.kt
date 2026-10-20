package su.afk.yummy.tv.source.aniboom.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AniboomEmbedUtilsTest : BaseUnitTest() {

    private fun fixture(): String =
        requireNotNull(javaClass.getResourceAsStream("/aniboom-embed.html")).readBytes().decodeToString()

    @Test
    fun `hls url is read from a real embed page`() {
        assertEquals(
            "https://poppy.ya-ligh123.site/lk/lk8qW4J5Mmo/master.m3u8",
            parseAniboomHlsUrl(fixture()),
        )
    }

    @Test
    fun `page without parameters or with broken ones has no hls`() {
        assertNull(parseAniboomHlsUrl("<html><body>nothing</body></html>"))
        assertNull(parseAniboomHlsUrl("""<video data-parameters="not json">"""))
        assertNull(parseAniboomHlsUrl("""<video data-parameters="{&quot;id&quot;:&quot;x&quot;}">"""))
    }

    @Test
    fun `non http link is rejected`() {
        val html = """<video data-parameters="{&quot;hls&quot;:&quot;{\&quot;src\&quot;:\&quot;javascript:alert(1)\&quot;}&quot;}">"""

        assertNull(parseAniboomHlsUrl(html))
    }
}
