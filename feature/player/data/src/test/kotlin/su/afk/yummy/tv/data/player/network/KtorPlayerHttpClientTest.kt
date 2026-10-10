package su.afk.yummy.tv.data.player.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/** `KtorPlayerHttpClient` поверх `MockEngine`: сеть не нужна, проверяем метод, заголовки и тело. */
class KtorPlayerHttpClientTest : BaseUnitTest() {

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "ok",
        headers: io.ktor.http.Headers = headersOf(),
    ): KtorPlayerHttpClient {
        val engine = MockEngine { request ->
            requests += request
            respond(content = body, status = status, headers = headers)
        }
        return KtorPlayerHttpClient(HttpClient(engine) { install(HttpTimeout) })
    }

    @Test
    fun `get sends the headers and maps status body and response headers`() = runTest {
        val client = client(
            body = "<html>",
            headers = headersOf("Set-Cookie", listOf("a=1; Path=/", "b=2")),
        )

        val response = client.getText("https://host/page", mapOf("Referer" to "https://r/"))

        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("https://r/", requests.single().headers["Referer"])
        assertEquals(200, response.statusCode)
        assertEquals("<html>", response.body)
        assertEquals("a=1; b=2", response.setCookieHeader)
    }

    @Test
    fun `post sends the form body`() = runTest {
        val client = client()

        client.postText("https://host/ftor", "a=1&b=2", mapOf("X-Requested-With" to "XMLHttpRequest"))

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("a=1&b=2", String(request.body.toByteArray()))
        assertEquals("XMLHttpRequest", request.headers["X-Requested-With"])
    }

    @Test
    fun `head returns no body`() = runTest {
        val client = client(body = "ignored")

        val response = client.head("https://host/file.mp4")

        assertEquals(HttpMethod.Head, requests.single().method)
        assertEquals(0, response.bodyBytes.size)
        assertTrue(response.isSuccess)
    }

    @Test
    fun `gzip body is decoded when the caller asks for gzip explicitly`() = runTest {
        val compressed = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write("<html>video</html>".toByteArray()) }
        }.toByteArray()
        val engine = MockEngine { request ->
            requests += request
            respond(
                content = compressed,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentEncoding, "gzip"),
            )
        }
        // Как общий клиент приложения (NetworkModule.provideHttpClient): экстракторы на него
        // полагаются, поэтому проверяем именно связку «явный gzip + плагин ContentEncoding».
        val client = KtorPlayerHttpClient(
            HttpClient(engine) {
                install(HttpTimeout)
                install(ContentEncoding) {
                    gzip()
                    deflate()
                }
            },
        )

        val response = client.getText(
            "https://host/page",
            mapOf("Accept-Encoding" to "gzip", "Accept-Language" to "ru-RU,ru;q=0.9"),
        )

        assertEquals("<html>video</html>", response.body)
        assertEquals("gzip", requests.single().headers["Accept-Encoding"])
        assertEquals("ru-RU,ru;q=0.9", requests.single().headers["Accept-Language"])
    }

    @Test
    fun `error status is returned and not thrown`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.Forbidden, "denied") }
        val client = KtorPlayerHttpClient(HttpClient(engine) { install(HttpTimeout) })

        val response = client.getText("https://host/page")

        assertEquals(403, response.statusCode)
        assertEquals("denied", response.body)
    }
}
