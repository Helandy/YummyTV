package su.afk.yummy.tv.source.anime4k.data.network

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
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamsRequestDto
import su.afk.yummy.tv.source.anime4k.data.signing.RequestSigner
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kApiException
import java.io.IOException

class Anime4kApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()

    private class FakeSigner(override val isAvailable: Boolean = true) : RequestSigner {
        val calls = mutableListOf<Triple<String, String, String>>()

        override fun headers(method: String, path: String, body: ByteArray): Map<String, String> {
            calls += Triple(method, path, body.decodeToString())
            return mapOf("X-Sig" to "sig", "X-Sig-Ts" to "1")
        }
    }

    private val signer = FakeSigner()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}"): Anime4kApi {
        val engine = MockEngine { request ->
            requests += request
            if (status.value in 200..299) {
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respondError(status)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return Anime4kApi(client, signer)
    }

    @Test
    fun `title request goes to the title endpoint and is parsed`() = runTest {
        val dto = api(body = """{"available":true,"episodes":[{"number":"1","season":"1","dubbings":["AlexFilm"]}]}""")
            .getTitle(1735)

        assertTrue(requests.single().url.toString().endsWith("/anime4k/title/1735"))
        assertEquals(true, dto?.available)
        assertEquals(listOf("AlexFilm"), dto?.episodes?.single()?.dubbings)
    }

    @Test
    fun `missing title is null`() = runTest {
        assertNull(api(HttpStatusCode.NotFound).getTitle(1))
    }

    @Test
    fun `ticket is posted with the device header`() = runTest {
        val dto = api(body = """{"ticket":"abc","notBeforeSec":20,"expiresInSec":300}""").requestTicket("dev123")

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.toString().endsWith("/anime4k/ticket"))
        assertEquals("dev123", request.headers["X-Device-Id"])
        assertEquals("abc", dto.ticket)
    }

    @Test
    fun `streams request carries the ticket in json body`() = runTest {
        val dto = api(body = """{"referer":"https://example.org/","files":[{"q":2160,"br":7500,"url":"https://v/x.mp4"}]}""")
            .getStreams(Anime4kStreamsRequestDto("abc", 1735, "1", "1", "AlexFilm"), deviceId = null)

        val body = requests.single().body.toByteArray().decodeToString()
        assertTrue(body.contains("\"ticket\":\"abc\""))
        assertTrue(body.contains("\"team\":\"AlexFilm\""))
        assertNull(requests.single().headers["X-Device-Id"])
        assertEquals(2160, dto.files.single().q)
    }

    @Test
    fun `http statuses map to domain exceptions`() = runTest {
        val rateLimited = runCatching { api(HttpStatusCode.TooManyRequests).requestTicket(null) }.exceptionOrNull()
        val rejected = runCatching { api(HttpStatusCode.Forbidden).requestTicket(null) }.exceptionOrNull()
        val server = runCatching { api(HttpStatusCode.InternalServerError).requestTicket(null) }.exceptionOrNull()

        assertTrue(rateLimited is Anime4kApiException.RateLimited)
        assertTrue(rejected is Anime4kApiException.Rejected)
        assertTrue(server is IOException)
    }

    @Test
    fun `every request is signed over method path and exact body`() = runTest {
        api(body = """{"available":false}""").getTitle(1735)
        api(body = """{"ticket":"abc","notBeforeSec":1,"expiresInSec":2}""").requestTicket("dev")
        val streams = api(body = """{"referer":"r","files":[]}""")
        streams.getStreams(Anime4kStreamsRequestDto("abc", 1735, "1", "1", "AlexFilm"), null)

        assertEquals(Triple("GET", "/api/anime4k/title/1735", ""), signer.calls[0])
        assertEquals(Triple("POST", "/api/anime4k/ticket", ""), signer.calls[1])
        val (method, path, signedBody) = signer.calls[2]
        assertEquals("POST", method)
        assertEquals("/api/anime4k/streams", path)
        assertEquals(requests.last().body.toByteArray().decodeToString(), signedBody)
        assertEquals("sig", requests.last().headers["X-Sig"])
    }
}
