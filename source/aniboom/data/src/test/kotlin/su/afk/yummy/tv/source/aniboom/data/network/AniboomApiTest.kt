package su.afk.yummy.tv.source.aniboom.data.network

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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomLookupRequestDto
import java.io.IOException

class AniboomApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()
    private val signed = mutableListOf<Triple<String, String, String>>()

    private val signer = object : SiteRequestSigner {
        override fun headers(method: String, path: String, body: ByteArray): Map<String, String> {
            signed += Triple(method, path, body.decodeToString())
            return mapOf("X-Sig" to "sig")
        }
    }

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}", signers: Set<SiteRequestSigner> = setOf(signer)): AniboomApi {
        val engine = MockEngine { request ->
            requests += request
            if (status.value in 200..299) respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            else respondError(status)
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return AniboomApi(client, signers, SiteApiConfig())
    }

    private val request = AniboomLookupRequestDto(1735, "Наруто", listOf("Naruto"), 2007, "tv", 220)

    @Test
    fun `title lookup is a signed post with the exact signed body`() = runTest {
        val dto = api(body = """{"available":true,"aniboomId":"0BzM07Pq71J","episodesAvailable":3,"dubbings":[{"title":"JAM CLUB","translation":16}]}""")
            .findTitle(request)

        val sent = requests.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertTrue(sent.url.toString().endsWith("/aniboom/title"))
        assertEquals("sig", sent.headers["X-Sig"])
        assertEquals(sent.body.toByteArray().decodeToString(), signed.single().third)
        assertEquals("/api/aniboom/title", signed.single().second)
        assertEquals("0BzM07Pq71J", dto.aniboomId)
        assertEquals(16, dto.dubbings.single().translation)
    }

    @Test
    fun `title lookup without signers still sends the request`() = runTest {
        api(signers = emptySet()).findTitle(request)

        assertEquals(1, requests.size)
        assertTrue(signed.isEmpty())
    }

    @Test
    fun `server error is an io exception`() = runTest {
        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { api(HttpStatusCode.Unauthorized).findTitle(request) } }
    }

    @Test
    fun `embed request carries the referer and returns the page text`() = runTest {
        val html = api(body = "<html>ok</html>").getEmbed("https://aniboom.one/embed/x?episode=1", "https://animego.me/")

        assertEquals("https://animego.me/", requests.single().headers["Referer"])
        assertEquals("<html>ok</html>", html)
    }

    @Test
    fun `missing embed is null and other errors throw`() = runTest {
        assertNull(api(HttpStatusCode.NotFound).getEmbed("https://aniboom.one/embed/x", "https://animego.me/"))
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { api(HttpStatusCode.BadGateway).getEmbed("https://aniboom.one/embed/x", "https://animego.me/") }
        }
    }
}
