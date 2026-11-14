package su.afk.yummy.tv.source.anistar.data.network

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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException

class AnistarApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()
    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}"): AnistarApi {
        val engine = MockEngine { request ->
            requests += request
            if (status.value in 200..299) respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            else respondError(status)
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return AnistarApi(client, SiteApiConfig())
    }

    @Test
    fun `mirror request is a plain get without a signature`() = runTest {
        val dto = api(body = """{"mirror":"https://v31.astar.bz"}""").getMirror()

        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("/api/anistar/mirror", requests.single().url.encodedPath)
        assertEquals(null, requests.single().headers["X-Sig"])
        assertEquals("https://v31.astar.bz", dto.mirror)
    }

    @Test
    fun `server error is an io exception`() = runTest {
        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { api(HttpStatusCode.Unauthorized).getMirror() } }
        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { api(HttpStatusCode.BadGateway).getMirror() } }
    }

    @Test
    fun `player page is requested by news id on the given mirror`() = runTest {
        val html = api(body = "<html>pl</html>").getPlayerPage("https://v31.astar.bz", 2658)

        assertEquals("https://v31.astar.bz/test/player2/videoas_p2p_new.php?id=2658", requests.single().url.toString())
        assertEquals("<html>pl</html>", html)
    }

    @Test
    fun `player page error is an io exception`() = runTest {
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { api(HttpStatusCode.Forbidden).getPlayerPage("https://v31.astar.bz", 1) }
        }
    }
}
