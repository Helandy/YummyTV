package su.afk.yummy.tv.data.player.extractor.zedfilm

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.data.player.extractor.FakePlayerHttpClient
import su.afk.yummy.tv.data.player.extractor.httpResponse
import su.afk.yummy.tv.data.player.extractor.httpResponseBytes
import su.afk.yummy.tv.data.player.extractor.streamRequest
import su.afk.yummy.tv.data.player.extractor.stubAndroidBase64
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.data.player.network.PlayerHttpResponse
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.io.IOException

/** Zedfilm разбирает только статическую страницу iframe: WebView-фолбэка нет, провал — это `Failed`. */
class ZedfilmExtractorTest : BaseUnitTest() {

    private val http = FakePlayerHttpClient()
    private val analytics: AnalyticsTracker = mockk(relaxed = true)
    private val userAgents: BrowserUserAgentProvider = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        stubAndroidBase64()
        every { userAgents.userAgent } returns "test-agent"
    }

    private fun extractor() = ZedfilmExtractor(http, analytics, userAgents)

    private suspend fun extract(autoLabel: String = "auto") =
        extractor().extract(streamRequest(PLAYER_URL, autoLabel), context)

    private fun stubHtml(html: String) {
        http.get("zedfilm.ru", httpResponse(200, html))
    }

    private fun videoInit(json: String): String =
        "video_Init('${java.util.Base64.getEncoder().encodeToString(json.toByteArray())}');"

    private suspend fun stream(html: String): PlayerStreamResolveResult.Stream {
        stubHtml(html)
        return extract() as PlayerStreamResolveResult.Stream
    }

    @Test
    fun `video init url wins over the fallback`() = runTest {
        val result = stream(
            videoInit("""{"url":"https://hlamer.ru/v/720.mpd","url2":"https://hlamer.ru/v/720.mp4"}"""),
        )

        assertEquals("https://hlamer.ru/v/720.mpd", result.url)
        assertNull("one candidate is not a ladder", result.qualities)
    }

    @Test
    fun `video init falls back to the second url`() = runTest {
        val result = stream(videoInit("""{"url":"","url2":"https://hlamer.ru/v/480.mp4"}"""))

        assertEquals("https://hlamer.ru/v/480.mp4", result.url)
    }

    @Test
    fun `video init with the null text url falls back too`() = runTest {
        val result = stream(videoInit("""{"url":"null","url2":"https://hlamer.ru/v/480.mp4"}"""))

        assertEquals("https://hlamer.ru/v/480.mp4", result.url)
    }

    @Test
    fun `page candidates form an ascending ladder`() = runTest {
        val result = stream(
            """<source src="https://cdn.example/v/360.mp4" type="video/mp4">
               <script>var player = {file: "https://cdn.example/v/720.mp4"};</script>""",
        )

        assertEquals(listOf("360p", "720p"), result.qualities!!.keys.toList())
        assertEquals("https://cdn.example/v/720.mp4", result.url)
    }

    @Test
    fun `escaped urls in a script are unescaped`() = runTest {
        val result = stream("""var cfg = {"file":"https:\/\/cdn.example\/v\/720.mp4?a=1&b=2"};""")

        assertEquals("https://cdn.example/v/720.mp4?a=1&b=2", result.url)
    }

    @Test
    fun `urls without a host or scheme are completed`() = runTest {
        val rootRelative = stream("""<source src="/media/720.mp4">""")
        assertEquals("https://hlamer.ru/media/720.mp4", rootRelative.url)

        val protocolRelative = stream("""<source src="//cdn.example/720.mp4">""")
        assertEquals("https://cdn.example/720.mp4", protocolRelative.url)

        val plainHttp = stream("""<source src="http://cdn.example/720.mp4">""")
        assertEquals("https://cdn.example/720.mp4", plainHttp.url)
    }

    @Test
    fun `1440p is not read as 144p`() = runTest {
        val result = stream(
            """<source src="https://cdn.example/v/1440p/index.m3u8">
               <source src="https://cdn.example/v/720p/index.m3u8">""",
        )

        assertEquals(listOf("720p", "1440p"), result.qualities!!.keys.toList())
    }

    @Test
    fun `quality is not taken from the host name`() = runTest {
        val result = stream("""<source src="https://s240.cdn.example/video/index.m3u8">""")

        assertEquals("https://s240.cdn.example/video/index.m3u8", result.url)
        assertNull(result.qualities)
    }

    @Test
    fun `legitimate urls containing ima or ads inside words are kept`() = runTest {
        val result = stream(
            """<source src="https://cdn.example/animation/360.mp4">
               <source src="https://cdn.example/uploads/720.mp4">""",
        )

        assertEquals(listOf("360p", "720p"), result.qualities!!.keys.toList())
    }

    @Test
    fun `advertising streams are ignored`() = runTest {
        val result = stream(
            """<source src="https://ads.example.com/promo/360.mp4">
               <source src="https://pubads.g.doubleclick.net/ad/480.mp4">
               <source src="https://cdn.example/v/720.mp4">""",
        )

        assertEquals("https://cdn.example/v/720.mp4", result.url)
        assertNull(result.qualities)
    }

    @Test
    fun `auto entry takes the requested label`() = runTest {
        stubHtml(
            """<source src="https://cdn.example/index.m3u8">
               <source src="https://cdn.example/v/720.mp4">""",
        )

        val result = extract(autoLabel = "Авто") as PlayerStreamResolveResult.Stream

        assertEquals(listOf("Авто", "720p"), result.qualities!!.keys.toList())
    }

    @Test
    fun `windows 1251 page is decoded`() = runTest {
        http.get(
            "zedfilm.ru",
            httpResponseBytes(
                text = """<title>Фильм</title><source src="https://cdn.example/v/720.mp4">""",
                charset = charset("windows-1251"),
            ),
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("https://cdn.example/v/720.mp4", result.url)
    }

    @Test
    fun `stream headers carry referer origin and user agent`() = runTest {
        val result = stream("""<source src="https://cdn.example/v/720.mp4">""")

        assertEquals(
            mapOf(
                "Referer" to PLAYER_URL,
                "Origin" to "https://hlamer.ru",
                "User-Agent" to "test-agent",
            ),
            result.headers,
        )
    }

    @Test
    fun `iframe page is requested with the site referer`() = runTest {
        stubHtml("""<source src="https://cdn.example/v/720.mp4">""")

        extract()

        val call = http.callsTo("GET").single()
        assertEquals(PLAYER_URL, call.url)
        assertEquals("https://yani.tv/", call.headers["Referer"])
    }

    @Test
    fun `iframe page is requested with russian language and gzip`() = runTest {
        stubHtml("""<source src="https://cdn.example/v/720.mp4">""")

        extract()

        val headers = http.callsTo("GET").single().headers
        assertEquals("ru-RU,ru;q=0.9,en;q=0.8", headers["Accept-Language"])
        assertEquals("gzip", headers["Accept-Encoding"])
    }

    @Test
    fun `language and encoding never leak into stream headers`() = runTest {
        val result = stream("""<source src="https://cdn.example/v/720.mp4">""")

        assertFalse(result.headers.containsKey("Accept-Language"))
        assertFalse(result.headers.containsKey("Accept-Encoding"))
    }

    @Test
    fun `page without any stream is a failure`() = runTest {
        stubHtml("<html><body>player is rendered by a script</body></html>")

        assertEquals(PlayerStreamResolveResult.Failed("Zedfilm: no stream found"), extract())
    }

    @Test
    fun `page that cannot be loaded is a failure`() = runTest {
        val throwing = object : PlayerHttpClient by http {
            override suspend fun getText(
                url: String,
                headers: Map<String, String>,
                followRedirects: Boolean,
            ): PlayerHttpResponse = throw IOException("connection reset")
        }

        val result = ZedfilmExtractor(throwing, analytics, userAgents)
            .extract(streamRequest(PLAYER_URL), context)

        assertEquals(PlayerStreamResolveResult.Failed("Zedfilm: no stream found"), result)
    }

    @Test
    fun `supports zedfilm and hlamer urls`() {
        assertTrue(extractor().supports("https://zedfilm.ru/embed/1"))
        assertTrue(extractor().supports("https://hlamer.ru/embed/1"))
        assertFalse(extractor().supports("https://kodik.info/serial/1/x/720p"))
    }

    private companion object {
        const val PLAYER_URL = "https://zedfilm.ru/embed/1"
    }
}
