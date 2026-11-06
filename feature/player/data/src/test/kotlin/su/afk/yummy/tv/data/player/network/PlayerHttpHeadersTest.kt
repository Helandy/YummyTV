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
}
