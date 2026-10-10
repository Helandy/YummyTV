package su.afk.yummy.tv.data.player.extractor.rutube

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
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

class RutubeExtractorTest : BaseUnitTest() {

    private val http = FakePlayerHttpClient()
    private val analytics: AnalyticsTracker = mockk(relaxed = true)
    private val userAgents: BrowserUserAgentProvider = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { userAgents.userAgent } returns "test-agent"
    }

    private fun extractor() = RutubeExtractor(http, analytics, userAgents)

    private suspend fun extract(
        url: String = EMBED_URL,
        autoLabel: String = "auto",
    ) = extractor().extract(streamRequest(url, autoLabel), context)

    /** [m3u8] и [default] подставляются в JSON как есть: `"null"` в кавычках, `null` без кавычек. */
    private fun stubOptions(m3u8: String? = "\"$MASTER_URL\"", default: String? = null) {
        val fields = listOfNotNull(
            m3u8?.let { "\"m3u8\":$it" },
            default?.let { "\"default\":$it" },
        )
        http.get(
            "api/play/options/",
            httpResponse(200, """{"video_balancer":{${fields.joinToString(",")}}}"""),
        )
    }

    private fun stubMaster(vararg variants: Pair<String, String>) {
        val body = buildString {
            append("#EXTM3U\n")
            variants.forEach { (resolution, uri) ->
                append("#EXT-X-STREAM-INF:BANDWIDTH=1000,RESOLUTION=$resolution\n$uri\n")
            }
        }
        http.get("master.m3u8", httpResponse(200, body))
    }

    @Test
    fun `master variants become an ascending ladder with auto first`() = runTest {
        stubOptions()
        stubMaster("1280x720" to "720.m3u8", "640x360" to "360.m3u8")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("auto", "360p", "720p"), result.qualities!!.keys.toList())
        assertEquals("https://bl.rutube.ru/route/abc/720.m3u8", result.url)
        assertEquals(MASTER_URL, result.qualities!!["auto"])
    }

    @Test
    fun `video id is read from the embed path and used for the options request`() = runTest {
        stubOptions()
        stubMaster("1280x720" to "720.m3u8")

        extract()

        assertEquals(
            "https://rutube.ru/api/play/options/$VIDEO_ID/?no_404=true",
            http.callsTo("GET", "api/play/options/").single().url,
        )
    }

    @Test
    fun `url without a video id fails without requests`() = runTest {
        val result = extract("https://rutube.ru/play/embed/not-an-id")

        assertEquals(PlayerStreamResolveResult.Failed("Rutube: no stream found"), result)
        assertTrue(http.calls.isEmpty())
    }

    @Test
    fun `options error is a failure`() = runTest {
        http.get("api/play/options/", httpResponse(404, "not found"))

        assertEquals(PlayerStreamResolveResult.Failed("Rutube: no stream found"), extract())
    }

    @Test
    fun `missing balancer is a failure`() = runTest {
        http.get("api/play/options/", httpResponse(200, """{"title":"x"}"""))

        assertEquals(PlayerStreamResolveResult.Failed("Rutube: no stream found"), extract())
    }

    @Test
    fun `default balancer url is used when m3u8 is missing`() = runTest {
        stubOptions(m3u8 = null, default = "\"$MASTER_URL\"")
        stubMaster("1280x720" to "720.m3u8")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(MASTER_URL, result.qualities!!["auto"])
    }

    @Test
    fun `default balancer url is used when m3u8 is json null`() = runTest {
        stubOptions(m3u8 = "null", default = "\"$MASTER_URL\"")
        stubMaster("1280x720" to "720.m3u8")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(MASTER_URL, result.qualities!!["auto"])
    }

    @Test
    fun `balancer with only json null urls is a failure`() = runTest {
        stubOptions(m3u8 = "null", default = "null")

        assertEquals(PlayerStreamResolveResult.Failed("Rutube: no stream found"), extract())
    }

    @Test
    fun `unknown resolution is dropped from the ladder`() = runTest {
        stubOptions()
        stubMaster("1280x720" to "720.m3u8", "854x800" to "800.m3u8")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("auto", "720p"), result.qualities!!.keys.toList())
    }

    @Test
    fun `master failure falls back to a single auto stream`() = runTest {
        stubOptions()
        http.get("master.m3u8", httpResponse(500, "oops"))

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("auto"), result.qualities!!.keys.toList())
        assertEquals(MASTER_URL, result.url)
    }

    @Test
    fun `auto entry takes the requested label`() = runTest {
        stubOptions()
        stubMaster("1280x720" to "720.m3u8")

        val result = extract(autoLabel = "Авто") as PlayerStreamResolveResult.Stream

        assertEquals(listOf("Авто", "720p"), result.qualities!!.keys.toList())
    }

    @Test
    fun `root relative variant resolves against the master host`() = runTest {
        stubOptions()
        stubMaster("1280x720" to "/live/720.m3u8")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("https://bl.rutube.ru/live/720.m3u8", result.url)
    }

    @Test
    fun `absolute and protocol relative variants are kept`() = runTest {
        stubOptions()
        stubMaster("1280x720" to "//edge.rutube.ru/720.m3u8", "640x360" to "http://edge.rutube.ru/360.m3u8")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("https://edge.rutube.ru/360.m3u8", result.qualities!!["360p"])
        assertEquals("https://edge.rutube.ru/720.m3u8", result.qualities!!["720p"])
    }

    @Test
    fun `stream headers carry referer origin and user agent`() = runTest {
        stubOptions()
        stubMaster("1280x720" to "720.m3u8")

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(
            mapOf(
                "Referer" to EMBED_URL.replace("http://", "https://"),
                "Origin" to "https://rutube.ru",
                "User-Agent" to "test-agent",
            ),
            result.headers,
        )
    }

    @Test
    fun `supports rutube urls only`() {
        assertTrue(extractor().supports(EMBED_URL))
        assertFalse(extractor().supports("https://kodik.info/serial/1/x/720p"))
    }

    private companion object {
        const val VIDEO_ID = "0123456789abcdef0123456789abcdef"
        const val EMBED_URL = "https://rutube.ru/play/embed/$VIDEO_ID"
        const val MASTER_URL = "https://bl.rutube.ru/route/abc/master.m3u8"
    }
}
