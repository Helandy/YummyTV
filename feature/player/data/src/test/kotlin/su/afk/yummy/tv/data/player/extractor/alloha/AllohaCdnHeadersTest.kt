package su.afk.yummy.tv.data.player.extractor.alloha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AllohaCdnHeadersTest {

    private val chromeMac =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    @Test
    fun `origin referer and language go with the user agent`() {
        val headers = allohaCdnHeaders("https://alloha.yani.tv", chromeMac)

        assertEquals("https://alloha.yani.tv", headers["origin"])
        assertEquals("https://alloha.yani.tv/", headers["referer"])
        assertEquals(chromeMac, headers["user-agent"])
        assertEquals("ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7", headers["accept-language"])
        assertEquals("cors", headers["sec-fetch-mode"])
    }

    @Test
    fun `client hints are derived from the chrome user agent`() {
        val headers = allohaCdnHeaders("https://alloha.yani.tv", chromeMac)

        assertEquals(
            "\"Chromium\";v=\"133\", \"Google Chrome\";v=\"133\", \"Not_A Brand\";v=\"24\"",
            headers["sec-ch-ua"],
        )
        assertEquals("?0", headers["sec-ch-ua-mobile"])
        assertEquals("\"macOS\"", headers["sec-ch-ua-platform"])
    }

    @Test
    fun `platform follows the user agent`() {
        val windows = allohaCdnHeaders("https://a", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0.0.0")
        val linux = allohaCdnHeaders("https://a", "Mozilla/5.0 (X11; Linux x86_64) Chrome/130.0.0.0")

        assertEquals("\"Windows\"", windows["sec-ch-ua-platform"])
        assertEquals("\"Linux\"", linux["sec-ch-ua-platform"])
    }

    @Test
    fun `no client hints for a user agent that is not chrome`() {
        val headers = allohaCdnHeaders("https://a", "Mozilla/5.0 (X11; Linux x86_64; rv:159.0) Gecko/20100101 Firefox/159.0")

        assertFalse(headers.containsKey("sec-ch-ua"))
    }
}
