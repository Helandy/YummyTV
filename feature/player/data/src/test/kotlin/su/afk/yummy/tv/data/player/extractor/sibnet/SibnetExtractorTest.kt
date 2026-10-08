package su.afk.yummy.tv.data.player.extractor.sibnet

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.data.player.network.PlayerHttpResponse
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.model.PlayerStreamUnavailableCause

class SibnetExtractorTest : BaseUnitTest() {

    private val httpClient: PlayerHttpClient = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val userAgents: BrowserUserAgentProvider = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { userAgents.userAgent } returns "test-agent"
    }

    private fun extractor() = SibnetExtractor(httpClient, analyticsTracker, userAgents)

    private fun request() = PlayerStreamRequest(iframeUrl = IFRAME_URL, autoQualityLabel = "auto")

    private fun stubPage(status: Int, body: String) {
        coEvery { httpClient.getText(any(), any(), any()) } returns
            PlayerHttpResponse(status, body, emptyMap())
    }

    @Test
    fun `403 means access is forbidden for this network`() = runTest {
        stubPage(403, "<html><body><h1>403 Forbidden</h1>Request forbidden by administrative rules.</body></html>")

        val result = extractor().extract(request(), context)

        assertEquals(
            PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.AccessForbidden),
            result,
        )
    }

    @Test
    fun `other http errors stay a failure with the status in the reason`() = runTest {
        stubPage(500, "oops")

        val result = extractor().extract(request(), context)

        assertEquals(PlayerStreamResolveResult.Failed("IllegalStateException: HTTP 500"), result)
    }

    @Test
    fun `page without a source is a failure`() = runTest {
        stubPage(200, "<html>no player here</html>")

        val result = extractor().extract(request(), context)

        assertEquals(PlayerStreamResolveResult.Failed("MP4 source was not found"), result)
    }

    @Test
    fun `page with a player source resolves to a stream`() = runTest {
        stubPage(200, """<script>player.src([{src: "/v/abc/1.mp4", type: "video/mp4"}]);</script>""")

        val result = extractor().extract(request(), context)

        assertTrue(result is PlayerStreamResolveResult.Stream)
        assertTrue((result as PlayerStreamResolveResult.Stream).url.endsWith("/v/abc/1.mp4"))
    }

    private companion object {
        const val IFRAME_URL = "//video.sibnet.ru/shell.php?videoid=6181468"
    }
}
