package su.afk.yummy.tv.source.aniboom.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException

class AniboomApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}"): AniboomApi {
        val engine = MockEngine { request ->
            requests += request
            if (status.value in 200..299) respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            else respondError(status)
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return AniboomApi(client)
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
