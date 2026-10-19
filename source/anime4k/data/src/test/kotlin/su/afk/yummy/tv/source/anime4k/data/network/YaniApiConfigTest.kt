package su.afk.yummy.tv.source.anime4k.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.network.yani.YANI_BASE_URL
import su.afk.yummy.tv.core.network.yani.YaniApiConfig
import su.afk.yummy.tv.core.network.yani.YaniApiOverride
import su.afk.yummy.tv.core.testing.BaseUnitTest

class YaniApiConfigTest : BaseUnitTest() {

    @Test
    fun `empty insecure or built in value keeps built in host`() {
        val config = YaniApiConfig()

        listOf("", "  ", "http://mirror.example", "mirror.example", YANI_BASE_URL, "$YANI_BASE_URL/").forEach {
            config.apply(it)
            assertNull(it, config.override)
        }
    }

    @Test
    fun `https value overrides host and port`() {
        val config = YaniApiConfig()

        config.apply(" https://mirror.example:8443/ ")

        assertEquals(YaniApiOverride("mirror.example", 8443), config.override)
    }
}
