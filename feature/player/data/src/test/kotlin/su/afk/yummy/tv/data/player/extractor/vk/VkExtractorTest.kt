package su.afk.yummy.tv.data.player.extractor.vk

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
import java.nio.charset.Charset

class VkExtractorTest : BaseUnitTest() {

    private val httpClient: PlayerHttpClient = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val userAgents: BrowserUserAgentProvider = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { userAgents.userAgent } returns "test-agent"
    }

    private fun extractor() = VkExtractor(httpClient, analyticsTracker, userAgents)

    private fun request() = PlayerStreamRequest(iframeUrl = IFRAME_URL, autoQualityLabel = "auto")

    /** Страница-обёртка отдаётся как есть, страница `video_ext.php` — в windows-1251, как у VK. */
    private fun stubPages(videoExtHtml: String, videoExtStatus: Int = 200) {
        val bytes = videoExtHtml.toByteArray(Charset.forName("windows-1251"))
        coEvery { httpClient.getText(any(), any(), any()) } answers {
            val url = firstArg<String>()
            if ("video_ext.php" in url) {
                PlayerHttpResponse(videoExtStatus, String(bytes, Charsets.UTF_8), emptyMap(), bytes)
            } else {
                PlayerHttpResponse(200, "<html></html>", emptyMap())
            }
        }
    }

    @Test
    fun `not found message becomes an unavailable video`() = runTest {
        stubPages("""<div id="video_ext_msg">Видеофайл не найден.</div>""")

        val result = extractor().extract(request(), context)

        assertEquals(
            PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.VideoNotFound),
            result,
        )
    }

    @Test
    fun `another vk message is passed through as is`() = runTest {
        stubPages("""<div id="video_ext_msg">Видео доступно только друзьям.</div>""")

        val result = extractor().extract(request(), context)

        assertEquals(PlayerStreamResolveResult.Unavailable(message = "Видео доступно только друзьям."), result)
    }

    @Test
    fun `page without streams and without a vk message is a failure with a reason`() = runTest {
        stubPages("<html>nothing here</html>")

        val result = extractor().extract(request(), context)

        assertTrue(result is PlayerStreamResolveResult.Failed)
        assertTrue((result as PlayerStreamResolveResult.Failed).reason.orEmpty().startsWith("no stream URLs found"))
    }

    @Test
    fun `page with a stream url resolves to a stream`() = runTest {
        stubPages("""{"mp4_720":"https://cdn.okcdn.ru/video.720.mp4"}""")

        val result = extractor().extract(request(), context)

        assertTrue(result is PlayerStreamResolveResult.Stream)
        assertTrue((result as PlayerStreamResolveResult.Stream).url.endsWith("video.720.mp4"))
    }

    private companion object {
        const val IFRAME_URL = "//ru.yummyani.me/iframeVK.html?id=-227475776_456244574"
    }
}
