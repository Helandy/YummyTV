package su.afk.yummy.tv.data.player.extractor.aksor

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
import su.afk.yummy.tv.data.player.extractor.streamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

class AksorExtractorTest : BaseUnitTest() {

    private val http = FakePlayerHttpClient()
    private val analytics: AnalyticsTracker = mockk(relaxed = true)
    private val userAgents: BrowserUserAgentProvider = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { userAgents.userAgent } returns "test-agent"
    }

    private fun extractor() = AksorExtractor(http, analytics, userAgents)

    private suspend fun extract(url: String = PLAYER_URL) =
        extractor().extract(streamRequest(url), context)

    @Test
    fun `api qualities are labelled and ordered`() = runTest {
        http.get(
            "player.aksor.tv/api/video/$HASH",
            httpResponse(
                200,
                """{"qualities":{"q4k":"https://c/4k.mp4","q720":"https://c/720.mp4","q360":"https://c/360.mp4",
                    "q1080":"null","q2k":""}}""",
            ),
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("360p", "720p", "4K"), result.qualities!!.keys.toList())
        assertEquals("https://c/4k.mp4", result.url)
    }

    @Test
    fun `spaces in stream urls are percent encoded`() = runTest {
        http.get(
            "player.aksor.tv/api/video/$HASH",
            httpResponse(200, """{"qualities":{"q720":"https://c/JAM CLUB/720.mp4"}}"""),
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("https://c/JAM%20CLUB/720.mp4", result.url)
    }

    @Test
    fun `api request carries the player url as referer`() = runTest {
        http.get("player.aksor.tv/api/video/$HASH", httpResponse(200, """{"qualities":{"q720":"https://c/720.mp4"}}"""))

        val result = extract() as PlayerStreamResolveResult.Stream

        val call = http.callsTo("GET").single()
        assertEquals("https://player.aksor.tv/api/video/$HASH", call.url)
        assertEquals(PLAYER_URL, call.headers["Referer"])
        assertEquals("application/json", call.headers["Accept"])
        assertEquals(mapOf("Referer" to PLAYER_URL, "User-Agent" to "test-agent"), result.headers)
    }

    @Test
    fun `broken api falls back to the meta video url`() = runTest {
        http.get("player.aksor.tv/api/video/$HASH", httpResponse(404, "<html>not found</html>"))
        http.get(
            "player.aksor.tv/video/$HASH",
            httpResponse(200, """<meta name="video_url" content="https://c/JAM CLUB/m.mp4">"""),
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("https://c/JAM%20CLUB/m.mp4", result.url)
        assertNull(result.qualities)
    }

    @Test
    fun `template meta video url is ignored`() = runTest {
        http.get("player.aksor.tv/api/video/$HASH", httpResponse(404, "nope"))
        http.get(
            "player.aksor.tv/video/$HASH",
            httpResponse(200, """<meta name="video_url" content="{{ video.url }}">"""),
        )

        assertEquals(PlayerStreamResolveResult.Failed("Aksor: no stream found"), extract())
    }

    @Test
    fun `api path found in a player script is queried for the hash`() = runTest {
        http.get("player.aksor.tv/api/video/$HASH", httpResponse(404, "nope"))
        http.get("player.aksor.tv/video/$HASH", httpResponse(200, """<script src="/static/app.js"></script>"""))
        http.get("static/app.js", httpResponse(200, """const base = "https://api.aksor.tv/api";"""))
        http.get(
            "api.aksor.tv/api/video/$HASH",
            httpResponse(200, """{"qualities":{"q480":"https://c/480.mp4"}}"""),
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("https://c/480.mp4", result.url)
    }

    @Test
    fun `no api path anywhere is a failure`() = runTest {
        http.get("player.aksor.tv/api/video/$HASH", httpResponse(404, "nope"))
        http.get("player.aksor.tv/video/$HASH", httpResponse(200, """<script src="/static/app.js"></script>"""))
        http.get("static/app.js", httpResponse(200, "console.log(1)"))

        assertEquals(PlayerStreamResolveResult.Failed("Aksor: no stream found"), extract())
    }

    @Test
    fun `player url without a hash fails without requests`() = runTest {
        val result = extract("https://player.aksor.tv/")

        assertEquals(PlayerStreamResolveResult.Failed("Aksor: no stream found"), result)
        assertTrue(http.calls.isEmpty())
    }

    @Test
    fun `hash is the segment after video`() = runTest {
        http.get("player.aksor.tv/api/video/$HASH", httpResponse(200, """{"qualities":{"q720":"https://c/720.mp4"}}"""))

        extract("//player.aksor.tv/video/$HASH/extra")

        assertEquals("https://player.aksor.tv/api/video/$HASH", http.callsTo("GET").single().url)
    }

    @Test
    fun `supports aksor urls only`() {
        assertTrue(extractor().supports(PLAYER_URL))
        assertFalse(extractor().supports("https://rutube.ru/play/embed/abc"))
    }

    private companion object {
        const val HASH = "abc123"
        const val PLAYER_URL = "https://player.aksor.tv/video/$HASH"
    }
}
