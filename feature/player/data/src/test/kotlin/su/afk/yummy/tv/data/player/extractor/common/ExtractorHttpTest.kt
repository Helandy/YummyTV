package su.afk.yummy.tv.data.player.extractor.common

import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.player.extractor.FakePlayerHttpClient
import su.afk.yummy.tv.data.player.extractor.httpResponse

class ExtractorHttpTest : BaseUnitTest() {

    private val http = FakePlayerHttpClient()

    @Test
    fun `non success body is returned when failures are not thrown`() = runTest {
        http.get("page", httpResponse(500, "oops"))

        assertEquals("oops", http.fetchText("https://h/page", emptyMap()))
    }

    @Test
    fun `non success throws with status and a trimmed body`() = runTest {
        http.get("page", httpResponse(502, "x".repeat(200)))

        val error = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                http.fetchText("https://h/page", emptyMap(), throwOnFailure = true)
            }
        }

        assertEquals("HTTP 502: ${"x".repeat(80)}", error.message)
    }

    @Test
    fun `299 is success and 300 is not`() = runTest {
        http.get("ok", httpResponse(299, "fine"))
        http.get("redirect", httpResponse(300, "moved"))

        assertEquals("fine", http.fetchText("https://h/ok", emptyMap(), throwOnFailure = true))
        assertTrue(
            runCatching {
                http.fetchText("https://h/redirect", emptyMap(), throwOnFailure = true)
            }.isFailure,
        )
    }

    @Test
    fun `headers are passed to the request`() = runTest {
        http.get("page", httpResponse(200, "ok"))

        http.fetchText("https://h/page", mapOf("Referer" to "https://r/"))

        assertEquals("https://r/", http.calls.single().headers["Referer"])
    }

    @Test
    fun `json body is parsed`() = runTest {
        http.get("api", httpResponse(200, """{"a":1}"""))

        assertEquals(1, http.fetchJson("https://h/api", emptyMap()).getInt("a"))
    }

    @Test
    fun `html body is a json error`() = runTest {
        http.get("api", httpResponse(200, "<html>"))

        val error = runCatching { http.fetchJson("https://h/api", emptyMap()) }.exceptionOrNull()

        assertTrue(error is JSONException)
    }
}
