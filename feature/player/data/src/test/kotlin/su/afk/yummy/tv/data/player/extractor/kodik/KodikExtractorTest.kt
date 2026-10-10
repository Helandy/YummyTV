package su.afk.yummy.tv.data.player.extractor.kodik

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.data.player.extractor.FakePlayerHttpClient
import su.afk.yummy.tv.data.player.extractor.httpResponse
import su.afk.yummy.tv.data.player.extractor.streamRequest
import su.afk.yummy.tv.data.player.extractor.stubAndroidBase64
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.data.player.network.PlayerHttpResponse
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.io.IOException

/**
 * Kodik: страница плеера → скрипт плеера (путь эндпоинта в `atob(...)`) → POST → `links`.
 * `android.util.Base64` подменён на `java.util.Base64` (`stubAndroidBase64`).
 */
class KodikExtractorTest : BaseUnitTest() {

    private val http = FakePlayerHttpClient()
    private val analytics: AnalyticsTracker = mockk(relaxed = true)
    private val userAgents: BrowserUserAgentProvider = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        stubAndroidBase64()
        every { userAgents.userAgent } returns "test-agent"
    }

    private fun extractor(client: PlayerHttpClient = http) = KodikExtractor(client, analytics, userAgents)

    private suspend fun extract(extractor: KodikExtractor = extractor()) =
        extractor.extract(streamRequest(IFRAME_URL), context)

    private fun pageHtml(
        urlParams: String? = URL_PARAMS,
        type: String? = "serial",
        hash: String? = "hash123",
        id: String? = "42",
        script: String? = """<script src="//kodik.info/assets/js/app.player_single.123.js"></script>""",
    ) = buildString {
        urlParams?.let { append("var urlParams = '$it';\n") }
        type?.let { append("videoInfo.type = '$it';\n") }
        hash?.let { append("videoInfo.hash = '$it';\n") }
        id?.let { append("videoInfo.id = '$it';\n") }
        script?.let { append(it) }
    }

    private fun stubPage(html: String = pageHtml(), headers: Map<String, List<String>> = emptyMap()) {
        http.get("kodik.info/serial", httpResponse(200, html, headers))
    }

    private fun stubScript(path: String? = "/ftor") {
        val body = path
            ?.let { java.util.Base64.getEncoder().encodeToString(it.toByteArray()) }
            ?.let { "var a = atob(\"$it\");" }
            ?: "no endpoint here"
        http.get("app.player_single", httpResponse(200, body))
    }

    private fun stubEndpoint(json: String, status: Int = 200) {
        http.post("kodik.info/", httpResponse(status, json))
    }

    @Test
    fun `missing page fields are reported one by one`() = runTest {
        val cases = listOf(
            pageHtml(urlParams = null) to "urlParams were not found",
            pageHtml(type = null) to "video type was not found",
            pageHtml(hash = null) to "video hash was not found",
            pageHtml(id = null) to "video id was not found",
            pageHtml(script = null) to "player script URL was not found",
        )

        cases.forEach { (html, reason) ->
            val client = FakePlayerHttpClient().apply { get("kodik.info/serial", httpResponse(200, html)) }

            val result = extract(extractor(client))

            assertEquals(PlayerStreamResolveResult.Failed(reason), result)
        }
    }

    @Test
    fun `non success page is a kodik block with the page message`() = runTest {
        http.get(
            "kodik.info/serial",
            httpResponse(403, """<div class="message">Видео недоступно в вашем регионе</div>"""),
        )

        val result = extract()

        assertEquals(
            PlayerStreamResolveResult.KodikBlocked(
                message = "Видео недоступно в вашем регионе",
                statusCode = 403,
            ),
            result,
        )
    }

    @Test
    fun `block without a message page keeps only the status`() = runTest {
        http.get("kodik.info/serial", httpResponse(503, "<html>down</html>"))

        val result = extract()

        assertEquals(PlayerStreamResolveResult.KodikBlocked(message = null, statusCode = 503), result)
    }

    @Test
    fun `plain links resolve to an ascending ladder and the best url`() = runTest {
        stubPage()
        stubScript()
        stubEndpoint(
            """{"links":{
                "360":[{"src":"//cdn.kodik.com/360.mp4"}],
                "720":[{"src":"//cdn.kodik.com/720.mp4"}],
                "480":[{"src":"//cdn.kodik.com/480.mp4"}]
            }}""",
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("360p", "480p", "720p"), result.qualities!!.keys.toList())
        assertEquals("https://cdn.kodik.com/720.mp4", result.url)
        assertEquals(mapOf("User-Agent" to "test-agent"), result.headers)
    }

    @Test
    fun `encoded link is decoded with the rot18 cipher`() = runTest {
        stubPage()
        stubScript()
        stubEndpoint("""{"links":{"720":[{"src":"$ROT_ENCODED_SRC"}]}}""")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(ROT_DECODED_URL, result.url)
    }

    @Test
    fun `endpoint response without links is a failure with the status`() = runTest {
        stubPage()
        stubScript()
        val body = """{"error":"nope"}"""
        stubEndpoint(body)

        val result = extract()

        assertEquals(
            PlayerStreamResolveResult.Failed(
                "no stream URL in endpoint response: HTTP 200, ${body.length} chars",
            ),
            result,
        )
    }

    @Test
    fun `post carries signed params, cookies and the referer`() = runTest {
        stubPage(headers = mapOf("Set-Cookie" to listOf("session=abc; Path=/", "k=v; HttpOnly")))
        stubScript()
        stubEndpoint("""{"links":{"720":[{"src":"//cdn/720.mp4"}]}}""")

        extract()

        val post = http.callsTo("POST").single()
        assertEquals("https://kodik.info/ftor", post.url)
        assertEquals("session=abc; k=v", post.headers["Cookie"])
        assertEquals("https://kodik.info/serial/1/hash123/720p", post.headers["Referer"])
        assertEquals("XMLHttpRequest", post.headers["X-Requested-With"])
        val body = post.body!!
        assertTrue(body.startsWith("d=kodik.info&d_sign=ds%2B1&pd=kodik.info&pd_sign=ps"))
        assertTrue("ref must stay as in the page", body.contains("&ref=https%3A%2F%2Fyani.tv%2F&ref_sign=rs"))
        assertTrue(body.endsWith("&type=serial&hash=hash123&id=42&info=%7B%7D"))
    }

    @Test
    fun `no cookies from the page means no cookie header`() = runTest {
        stubPage()
        stubScript()
        stubEndpoint("""{"links":{"720":[{"src":"//cdn/720.mp4"}]}}""")

        extract()

        assertFalse(http.callsTo("POST").single().headers.containsKey("Cookie"))
    }

    @Test
    fun `endpoint path comes from the player script and is cached per script url`() = runTest {
        stubPage()
        stubScript(path = "/gvi")
        stubEndpoint("""{"links":{"720":[{"src":"//cdn/720.mp4"}]}}""")
        val extractor = extractor()

        extract(extractor)
        extract(extractor)

        assertEquals(1, http.callsTo("GET", "app.player_single").size)
        assertTrue(http.callsTo("POST").all { it.url == "https://kodik.info/gvi" })
    }

    @Test
    fun `script without a usable endpoint falls back to the default path`() = runTest {
        stubPage()
        stubScript(path = null)
        stubEndpoint("""{"links":{"720":[{"src":"//cdn/720.mp4"}]}}""")

        extract()

        assertEquals("https://kodik.info/ftor", http.callsTo("POST").single().url)
    }

    @Test
    fun `endpoint path cache is dropped when the response has no stream`() = runTest {
        stubPage()
        stubScript()
        stubEndpoint("""{"links":{}}""")
        val extractor = extractor()

        extract(extractor)
        extract(extractor)

        assertEquals(2, http.callsTo("GET", "app.player_single").size)
    }

    @Test
    fun `hls quality is repaired when the better manifest exists`() = runTest {
        stubPage()
        stubScript()
        stubEndpoint("""{"links":{"720":[{"src":"//cdn/u/480.mp4:hls:manifest.m3u8"}]}}""")
        http.head("720.mp4:hls:manifest.m3u8", httpResponse(200))

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("720p"), result.qualities!!.keys.toList())
        assertEquals("https://cdn/u/720.mp4:hls:manifest.m3u8", result.url)
    }

    @Test
    fun `hls quality keeps the real label when the better manifest is missing`() = runTest {
        stubPage()
        stubScript()
        stubEndpoint("""{"links":{"720":[{"src":"//cdn/u/480.mp4:hls:manifest.m3u8"}]}}""")
        http.head("720.mp4:hls:manifest.m3u8", httpResponse(404))

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("480p"), result.qualities!!.keys.toList())
        assertEquals("https://cdn/u/480.mp4:hls:manifest.m3u8", result.url)
    }

    @Test
    fun `unexpected exception becomes a failure with its name`() = runTest {
        stubPage()
        stubScript()
        val throwing = object : PlayerHttpClient by http {
            override suspend fun postText(
                url: String,
                body: String,
                headers: Map<String, String>,
            ): PlayerHttpResponse = throw IOException("connection reset")
        }

        val result = extract(extractor(throwing))

        assertEquals(PlayerStreamResolveResult.Failed("IOException: connection reset"), result)
    }

    @Test
    fun `supports only kodik urls`() {
        assertTrue(extractor().supports("https://kodik.info/serial/1/x/720p"))
        assertFalse(extractor().supports("https://rutube.ru/play/embed/abc"))
    }

    private companion object {
        const val IFRAME_URL = "//kodik.info/serial/1/hash123/720p"
        const val URL_PARAMS =
            """{"d":"kodik.info","d_sign":"ds+1","pd":"kodik.info","pd_sign":"ps","ref":"https%3A%2F%2Fyani.tv%2F","ref_sign":"rs"}"""
        const val ROT_DECODED_URL =
            "https://cloud.kodik-storage.com/useruploads/abc/720.mp4:hls:manifest.m3u8"

        // ROT18 по буквам + base64 без padding; вектор посчитан независимой реализацией.
        const val ROT_ENCODED_SRC =
            "iPZ0kPU6Tg9rjO91HK5zj2Zxig1hlO9gGEltTuVdjA91k2DglFJaj2Nskg9pGuUdVhQeTu1eVLxwjPU6jENciEHtk3YcjBV1WI"
    }
}
