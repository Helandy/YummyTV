package su.afk.yummy.tv.source.amedia.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import io.ktor.http.HttpMethod
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.IOException

class AmediaApiTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = ""): AmediaApi {
        val engine = MockEngine { request ->
            requests += request
            if (status.value in 200..299) respond(body, status) else respondError(status)
        }
        return AmediaApi(HttpClient(engine))
    }

    @Test
    fun `site errors are io exceptions`() = runTest {
        assertThrows(IOException::class.java) { runBlocking { api(HttpStatusCode.BadGateway).getVodPage(1) } }
        assertThrows(IOException::class.java) { runBlocking { api(HttpStatusCode.NotFound).getPlaylist("https://aser.pro/a/index.m3u8") } }
    }

    @Test
    fun `site pages are plain unsigned gets`() = runTest {
        val html = api(body = "<html>vod</html>").getVodPage(47997)

        assertEquals("https://aser.pro/vod/47997", requests.single().url.toString())
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("<html>vod</html>", html)
        assertEquals(null, requests.single().headers["X-Sig"])
    }
}
