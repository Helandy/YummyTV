package su.afk.yummy.tv.source.amedia.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.data.dto.AmediaLookupRequestDto
import java.io.IOException

class AmediaApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()
    private val signed = mutableListOf<Triple<String, String, String>>()

    private val signer = object : SiteRequestSigner {
        override fun headers(method: String, path: String, body: ByteArray): Map<String, String> {
            signed += Triple(method, path, body.decodeToString())
            return mapOf("X-Sig" to "sig")
        }
    }

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}"): AmediaApi {
        val engine = MockEngine { request ->
            requests += request
            if (status.value in 200..299) respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            else respondError(status)
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return AmediaApi(client, setOf(signer), SiteApiConfig())
    }

    private val request = AmediaLookupRequestDto(59088, "Мстители", listOf("Revengers"), 2026, "tv", 12)

    @Test
    fun `title lookup is a signed post with the exact signed body`() = runTest {
        val dto = api(body = """{"available":true,"pageUrl":"https://amd.online/5753-x.html"}""").findTitle(request)

        val sent = requests.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertTrue(sent.url.toString().endsWith("/amedia/title"))
        assertEquals("sig", sent.headers["X-Sig"])
        assertEquals(sent.body.toByteArray().decodeToString(), signed.single().third)
        assertEquals("/api/amedia/title", signed.single().second)
        assertEquals("https://amd.online/5753-x.html", dto.pageUrl)
    }

    @Test
    fun `server error is an io exception`() = runTest {
        assertThrows(IOException::class.java) { runBlocking { api(HttpStatusCode.Unauthorized).findTitle(request) } }
        assertThrows(IOException::class.java) { runBlocking { api(HttpStatusCode.BadGateway).getVodPage(1) } }
    }

    @Test
    fun `site pages are plain unsigned gets`() = runTest {
        val html = api(body = "<html>vod</html>").getVodPage(47997)

        assertEquals("https://aser.pro/vod/47997", requests.single().url.toString())
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("<html>vod</html>", html)
        assertTrue(signed.isEmpty())
    }
}
