package su.afk.yummy.tv.data.player.network

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider

class PlayerHttpHeadersTest : BaseUnitTest() {

    @Test
    fun `stream headers hold only the user agent of the selected profile`() {
        val provider: BrowserUserAgentProvider = mockk()
        every { provider.userAgent } returns "Chrome/Win"

        assertEquals(mapOf("User-Agent" to "Chrome/Win"), provider.streamHeaders())
    }

    @Test
    fun `missing user agent is added and other headers are kept`() {
        val headers = mapOf("Referer" to "https://r/").withBrowserUserAgent("UA")

        assertEquals(mapOf("Referer" to "https://r/", "User-Agent" to "UA"), headers)
    }

    @Test
    fun `existing user agent wins whatever the header case`() {
        val headers = mapOf("user-agent" to "Original", "X" to "1").withBrowserUserAgent("UA")

        assertEquals(mapOf("X" to "1", "User-Agent" to "Original"), headers)
    }

    @Test
    fun `blank user agent is replaced`() {
        val headers = mapOf("User-Agent" to "  ").withBrowserUserAgent("UA")

        assertEquals(mapOf("User-Agent" to "UA"), headers)
    }
}
