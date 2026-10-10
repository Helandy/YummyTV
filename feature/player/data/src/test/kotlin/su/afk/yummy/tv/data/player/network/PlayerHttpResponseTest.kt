package su.afk.yummy.tv.data.player.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerHttpResponseTest : BaseUnitTest() {

    private fun response(status: Int, headers: Map<String, List<String>> = emptyMap()) =
        PlayerHttpResponse(status, "body", headers)

    @Test
    fun `success is the 2xx range only`() {
        assertFalse(response(199).isSuccess)
        assertTrue(response(200).isSuccess)
        assertTrue(response(299).isSuccess)
        assertFalse(response(300).isSuccess)
        assertFalse(response(404).isSuccess)
    }

    @Test
    fun `set cookie keeps only name and value of every cookie`() {
        val response = response(
            200,
            mapOf("Set-Cookie" to listOf("session=abc; Path=/; HttpOnly", "lang=ru; Max-Age=60")),
        )

        assertEquals("session=abc; lang=ru", response.setCookieHeader)
    }

    @Test
    fun `set cookie lookup ignores header name case`() {
        val response = response(200, mapOf("set-cookie" to listOf("a=1; Secure")))

        assertEquals("a=1", response.setCookieHeader)
    }

    @Test
    fun `no cookies is an empty string`() {
        assertEquals("", response(200).setCookieHeader)
    }

    @Test
    fun `body bytes default to the utf8 body and decode with any charset`() {
        val utf8 = PlayerHttpResponse(200, "Привет", emptyMap())
        assertEquals("Привет", utf8.body(Charsets.UTF_8))

        val bytes = "Привет".toByteArray(charset("windows-1251"))
        val legacy = PlayerHttpResponse(200, "garbled", emptyMap(), bytes)
        assertEquals("Привет", legacy.body(charset("windows-1251")))
    }
}
