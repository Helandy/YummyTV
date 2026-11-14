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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kLookupRequestDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamsRequestDto
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kApiException
import java.io.IOException

class Anime4kApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()

    private class FakeSigner(override val isAvailable: Boolean = true) : SiteRequestSigner {
        val calls = mutableListOf<Triple<String, String, String>>()

        override fun headers(method: String, path: String, query: String, body: ByteArray): Map<String, String> {
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
        return Anime4kApi(client, setOf(signer))
    }

    private val lookup = Anime4kLookupRequestDto(1735, "Наруто", listOf("Naruto"), 2002, "tv", 220)

    @Test
    fun `title request is a signed post to the title endpoint`() = runTest {
        val dto = api(body = """{"available":true,"episodes":[{"number":"1","season":"1","dubbings":["AlexFilm"]}]}""")
            .findTitle(lookup)

        val sent = requests.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertTrue(sent.url.toString().endsWith("/anime4k/title"))
        assertTrue(sent.body.toByteArray().decodeToString().contains("\"malId\":1735"))
        assertEquals(true, dto.available)
        assertEquals(listOf("AlexFilm"), dto.episodes.single().dubbings)
    }

    @Test
    fun `streams request carries the target in json body and the device header`() = runTest {
        val dto = api(body = """{"referer":"https://example.org/","files":[{"q":2160,"br":7500,"url":"https://v/x.mp4"}]}""")
            .getStreams(Anime4kStreamsRequestDto(1735, "1", "1", "AlexFilm"), deviceId = "dev123")

        val request = requests.single()
        val body = request.body.toByteArray().decodeToString()
        assertTrue(request.url.toString().endsWith("/anime4k/streams"))
        assertTrue(body.contains("\"team\":\"AlexFilm\""))
        assertFalse(body.contains("ticket"))
        assertEquals("dev123", request.headers["X-Device-Id"])
        assertEquals(2160, dto.files.single().q)
    }

    @Test
    fun `http statuses map to domain exceptions`() = runTest {
        val target = Anime4kStreamsRequestDto(1735, "1", "1", "AlexFilm")
        val rateLimited = runCatching { api(HttpStatusCode.TooManyRequests).getStreams(target, null) }.exceptionOrNull()
        val rejected = runCatching { api(HttpStatusCode.Unauthorized).getStreams(target, null) }.exceptionOrNull()
        val notFound = runCatching { api(HttpStatusCode.NotFound).getStreams(target, null) }.exceptionOrNull()
        val server = runCatching { api(HttpStatusCode.InternalServerError).getStreams(target, null) }.exceptionOrNull()

        assertTrue(rateLimited is Anime4kApiException.RateLimited)
        assertTrue(rejected is Anime4kApiException.Rejected)
        assertTrue(notFound is Anime4kApiException.NotFound)
        assertTrue(server is IOException)
    }

    @Test
    fun `every request is signed over method path and exact body`() = runTest {
        api(body = """{"available":false}""").findTitle(lookup)
        val streams = api(body = """{"referer":"r","files":[]}""")
        streams.getStreams(Anime4kStreamsRequestDto(1735, "1", "1", "AlexFilm"), null)

        val (titleMethod, titlePath, titleBody) = signer.calls[0]
        assertEquals("POST", titleMethod)
        assertEquals("/api/anime4k/title", titlePath)
        assertTrue(titleBody.contains("\"malId\":1735"))
        val (method, path, signedBody) = signer.calls[1]
        assertEquals("POST", method)
        assertEquals("/api/anime4k/streams", path)
        assertEquals(requests.last().body.toByteArray().decodeToString(), signedBody)
        assertEquals("sig", requests.last().headers["X-Sig"])
    }
}
