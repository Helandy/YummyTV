package su.afk.yummy.tv.core.utils.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class HttpHeaderUtilsTest : BaseUnitTest() {

    @Test
    fun `user agent is read whatever the header case`() {
        assertEquals("UA", mapOf("user-agent" to "UA").userAgentOrNull())
        assertEquals("UA", mapOf("USER-AGENT" to "UA").userAgentOrNull())
    }

    @Test
    fun `missing or blank user agent reads as null`() {
        assertNull(emptyMap<String, String>().userAgentOrNull())
        assertNull(mapOf("User-Agent" to "  ").userAgentOrNull())
    }

    @Test
    fun `missing user agent is added and other headers are kept`() {
        val headers = mapOf("Referer" to "https://r/").withUserAgentIfAbsent("UA")

        assertEquals(mapOf("Referer" to "https://r/", "User-Agent" to "UA"), headers)
    }

    @Test
    fun `existing user agent wins whatever the header case`() {
        val headers = mapOf("user-agent" to "Original", "X" to "1").withUserAgentIfAbsent("UA")

        assertEquals(mapOf("X" to "1", "User-Agent" to "Original"), headers)
    }

    @Test
    fun `blank user agent is replaced`() {
        val headers = mapOf("User-Agent" to "  ").withUserAgentIfAbsent("UA")

        assertEquals(mapOf("User-Agent" to "UA"), headers)
    }
}
