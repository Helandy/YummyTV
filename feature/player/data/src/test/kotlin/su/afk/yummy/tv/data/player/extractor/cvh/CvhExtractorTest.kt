package su.afk.yummy.tv.data.player.extractor.cvh

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
import su.afk.yummy.tv.domain.player.model.PlayerStreamUnavailableCause

/**
 * `CvhExtractor.extract` целиком: плейлист → видео → лестница mp4. Выбор элемента плейлиста и
 * регион-блок покрыты отдельно (`CvhPlaylistSelectionTest`, `CvhRegionBlockTest`).
 * Сценарии с ретраем идут в реальном времени (пауза 700 мс внутри `Dispatchers.IO`).
 */
class CvhExtractorTest : BaseUnitTest() {

    private val http = FakePlayerHttpClient()
    private val analytics: AnalyticsTracker = mockk(relaxed = true)
    private val userAgents: BrowserUserAgentProvider = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { userAgents.userAgent } returns "test-agent"
    }

    private fun extractor() = CvhExtractor(http, analytics, userAgents)

    private suspend fun extract(
        url: String = iframeUrl(),
        forceRefresh: Boolean = false,
    ) = extractor().extract(streamRequest(url, forceRefresh = forceRefresh), context)

    private fun iframeUrl(
        animeId: String? = "31240",
        episode: String = "2",
        dubbingCode: String = "AnilibriaTV",
    ) = buildString {
        append("//ru.yummyani.me/iframeCVH.html?dubbing_code=$dubbingCode&episode=$episode")
        animeId?.let { append("&anime_id=$it") }
    }

    private fun stubPlaylist(json: String) {
        http.get("sv/playlist", httpResponse(200, json))
    }

    private fun stubVideo(json: String) {
        http.get("sv/video/", httpResponse(200, json))
    }

    private fun playlist(vararg items: String, tags: String = "[]") =
        """{"isSerial":true,"tags":$tags,"items":[${items.joinToString(",")}]}"""

    private fun item(vkId: String, studio: String?, episode: Int = 2) =
        """{"vkId":"$vkId","voiceStudio":${studio?.let { "\"$it\"" } ?: "null"},"voiceType":"Озвучка","episode":$episode}"""

    @Test
    fun `missing anime id fails without any request`() = runTest {
        val result = extract(iframeUrl(animeId = null))

        assertEquals(PlayerStreamResolveResult.Failed("missing anime_id"), result)
        assertTrue(http.calls.isEmpty())
    }

    @Test
    fun `blocked playlist is unavailable by region`() = runTest {
        stubPlaylist("""{"tags":[5],"items":[]}""")

        val result = extract()

        assertEquals(
            PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.RegionBlocked),
            result,
        )
    }

    @Test
    fun `playlist without items is unavailable`() = runTest {
        stubPlaylist("""{"tags":[]}""")

        assertEquals(PlayerStreamResolveResult.Unavailable(), extract())
    }

    @Test
    fun `playlist without the requested episode is unavailable`() = runTest {
        stubPlaylist(playlist(item("a", "AnilibriaTV", episode = 7)))

        assertEquals(PlayerStreamResolveResult.Unavailable(), extract())
    }

    @Test
    fun `item without vk id is a failure`() = runTest {
        stubPlaylist(playlist("""{"vkId":"","voiceStudio":"AnilibriaTV","episode":2}"""))

        assertEquals(PlayerStreamResolveResult.Failed("playlist item has no vkId"), extract())
    }

    @Test
    fun `video response without sources is a failure`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo("""{"failoverHost":"vh.okcdn.ru"}""")

        assertEquals(PlayerStreamResolveResult.Failed("video response has no sources"), extract())
    }

    @Test
    fun `sources without mp4 links are a failure`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo("""{"sources":{"hlsUrl":"https://x/master.m3u8","mpegLowUrl":""}}""")

        assertEquals(PlayerStreamResolveResult.Failed("no mp4 qualities in sources"), extract())
    }

    @Test
    fun `mp4 ladder is ascending with the best quality as the url`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo(
            """{"sources":{
                "mpegFullHdUrl":"https://vh.okcdn.ru/f.mp4?s=1",
                "mpegLowUrl":"https://vh.okcdn.ru/l.mp4?s=1",
                "mpegHighUrl":"https://vh.okcdn.ru/h.mp4?s=1"
            }}""",
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals(listOf("360p", "720p", "1080p"), result.qualities!!.keys.toList())
        assertEquals("https://vh.okcdn.ru/f.mp4?s=1", result.url)
        assertEquals(mapOf("User-Agent" to "test-agent"), result.headers)
    }

    @Test
    fun `requests go to the playlist and video endpoints with api headers`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo("""{"sources":{"mpegLowUrl":"https://vh.okcdn.ru/l.mp4"}}""")

        extract()

        val playlistCall = http.callsTo("GET", "sv/playlist").single()
        assertEquals(
            "https://plapi.cdnvideohub.com/api/v1/player/sv/playlist?pub=745&id=31240&aggr=mali",
            playlistCall.url,
        )
        assertEquals("https://ru.yummyani.me/", playlistCall.headers["Referer"])
        assertEquals("application/json", playlistCall.headers["Accept"])
        assertEquals(
            "https://plapi.cdnvideohub.com/api/v1/player/sv/video/vk1",
            http.callsTo("GET", "sv/video/").single().url,
        )
    }

    @Test
    fun `failover host is reported when it differs from the link host`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo(
            """{"failoverHost":"vh2.okcdn.ru","sources":{"mpegLowUrl":"https://vh.okcdn.ru/l.mp4?s=1"}}""",
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("vh2.okcdn.ru", result.failoverHost)
        assertEquals("https://vh.okcdn.ru/l.mp4?s=1", result.url)
    }

    @Test
    fun `failover host is dropped when links already use it`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo(
            """{"failoverHost":"vh.okcdn.ru","sources":{"mpegLowUrl":"https://vh.okcdn.ru/l.mp4"}}""",
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertNull(result.failoverHost)
    }

    @Test
    fun `raw ip links are moved to the failover host even without a refresh`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo(
            """{"failoverHost":"vh2.okcdn.ru","sources":{"mpegLowUrl":"https://10.1.2.3/l.mp4?s=1"}}""",
        )

        val result = extract() as PlayerStreamResolveResult.Stream

        assertEquals("https://vh2.okcdn.ru/l.mp4?s=1", result.url)
    }

    @Test
    fun `named host links move to the failover host only on refresh`() = runTest {
        stubPlaylist(playlist(item("vk1", "AnilibriaTV")))
        stubVideo(
            """{"failoverHost":"vh2.okcdn.ru","sources":{"mpegLowUrl":"https://vh.okcdn.ru/l.mp4?s=1"}}""",
        )

        val normal = extract(forceRefresh = false) as PlayerStreamResolveResult.Stream
        val refreshed = extract(forceRefresh = true) as PlayerStreamResolveResult.Stream

        assertEquals("https://vh.okcdn.ru/l.mp4?s=1", normal.url)
        assertEquals("https://vh2.okcdn.ru/l.mp4?s=1", refreshed.url)
    }

    @Test
    fun `json null studio does not match a dubbing code of the same text`() = runTest {
        stubPlaylist(playlist(item("studio", "X"), item("subs", null)))
        stubVideo("""{"sources":{"mpegLowUrl":"https://vh.okcdn.ru/l.mp4"}}""")

        extract(iframeUrl(dubbingCode = "null"))

        assertTrue(http.callsTo("GET", "sv/video/").single().url.endsWith("/studio"))
    }

    @Test
    fun `non numeric episode falls back to the first one`() = runTest {
        stubPlaylist(playlist(item("ep1", "AnilibriaTV", episode = 1), item("ep2", "AnilibriaTV", episode = 2)))
        stubVideo("""{"sources":{"mpegLowUrl":"https://vh.okcdn.ru/l.mp4"}}""")

        extract(iframeUrl(episode = "abc"))

        assertTrue(http.callsTo("GET", "sv/video/").single().url.endsWith("/ep1"))
    }

    @Test
    fun `one failed request is retried`() = runTest {
        var playlistCalls = 0
        http.get("sv/playlist") {
            playlistCalls++
            if (playlistCalls == 1) {
                httpResponse(502, "bad gateway")
            } else {
                httpResponse(200, playlist(item("vk1", "AnilibriaTV")))
            }
        }
        stubVideo("""{"sources":{"mpegLowUrl":"https://vh.okcdn.ru/l.mp4"}}""")

        val result = extract()

        assertTrue(result is PlayerStreamResolveResult.Stream)
        assertEquals(2, playlistCalls)
    }

    @Test
    fun `request failing twice is reported with the http status`() = runTest {
        http.get("sv/playlist", httpResponse(500, "boom"))

        val result = extract()

        assertEquals(
            PlayerStreamResolveResult.Failed("IllegalStateException: HTTP 500: boom"),
            result,
        )
        assertEquals(2, http.callsTo("GET", "sv/playlist").size)
    }

    @Test
    fun `supports cvh iframe urls only`() {
        assertTrue(extractor().supports("//ru.yummyani.me/iframeCVH.html?anime_id=1"))
        assertTrue(extractor().supports("cvh"))
        assertFalse(extractor().supports("https://kodik.info/serial/1/x/720p"))
    }
}
