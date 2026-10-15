package su.afk.yummy.tv.source.anime4k.data.network

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.YUMMY_TV_API_BASE_URL
import su.afk.yummy.tv.core.testing.BaseUnitTest

class SiteApiConfigTest : BaseUnitTest() {

    @Test
    fun `empty remote values keep built in ones`() {
        val config = SiteApiConfig().apply { apply("", "  ") }

        assertEquals(YUMMY_TV_API_BASE_URL, config.baseUrl)
        assertEquals("builtin", config.effectiveToken("builtin"))
    }

    @Test
    fun `identical or insecure base url is ignored`() {
        val config = SiteApiConfig()

        config.apply("$YUMMY_TV_API_BASE_URL/", "")
        assertEquals(YUMMY_TV_API_BASE_URL, config.baseUrl)

        config.apply("http://new.example/api", "")
        assertEquals(YUMMY_TV_API_BASE_URL, config.baseUrl)
    }

    @Test
    fun `valid remote values replace built in ones`() {
        val config = SiteApiConfig().apply { apply(" https://new.example/api/ ", " token ") }

        assertEquals("https://new.example/api", config.baseUrl)
        assertEquals("token", config.effectiveToken("builtin"))
    }
}
