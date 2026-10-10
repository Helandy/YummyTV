package su.afk.yummy.tv.data.player.extractor.alloha

import android.os.Handler
import android.webkit.WebView
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack

/**
 * Состояние живой Alloha-сессии и ротация подписанных ссылок. Прокси (`startProxy`) не поднимаем:
 * он открывает реальный `ServerSocket`, поэтому `initialStream`/`playbackUrl`/`qualityUrls` здесь
 * не проверяются — только прямой поток (`directStream`), заголовки, мастер, срок и ротация.
 * `Handler` — мок: `post` исполняется сразу, отложенный коммит ротации перехватывается.
 */
class LiveAllohaStreamSessionTest : BaseUnitTest() {

    private val handler: Handler = mockk(relaxed = true)
    private val analytics: AnalyticsTracker = mockk(relaxed = true)
    private val timeout = slot<Runnable>()
    private var refreshCalls = 0

    @Before
    fun setUp() {
        every { handler.post(any()) } answers { firstArg<Runnable>().run(); true }
        every { handler.postDelayed(capture(timeout), any<Long>()) } returns true
    }

    private fun session(): LiveAllohaStreamSession =
        LiveAllohaStreamSession(handler, "https://alloha/1", analytics).also {
            it.attach(mockk<WebView>(relaxed = true), refresh = { refreshCalls++ }, release = {})
        }

    private fun track(id: String, default: Boolean = false, vararg ladder: Pair<String, String>) =
        AllohaParsedAudioTrack(
            track = AllohaAudioTrack(id = id, label = "Voice $id", isDefault = default),
            qualities = linkedMapOf(*ladder),
        )

    private fun sources(version: String = "v1") = AllohaParsedSources(
        audioTracks = listOf(
            track("a", default = true, "360p" to "https://cdn/$version/a360", "720p" to "https://cdn/$version/a720"),
            track("b", default = false, "480p" to "https://cdn/$version/b480", "720p" to "https://cdn/$version/b720"),
        ),
        subtitles = listOf(AllohaSubtitleTrack("Russian", "https://cdn/ru.vtt", "ru", "vtt")),
    )

    private fun LiveAllohaStreamSession.start(version: String = "v1") {
        initialize(sources(version), mapOf("User-Agent" to "UA", "Authorizations" to "token-$version"))
    }

    @Test
    fun `initialize picks the default dubbing and its best quality`() {
        val session = session().apply { start() }

        assertEquals("https://cdn/v1/a720", session.currentMasterUrl())
        val direct = session.directStream
        assertEquals("a", direct.selectedAllohaAudioId)
        assertEquals("https://cdn/v1/a720", direct.url)
        assertEquals(listOf("360p", "720p"), direct.qualities!!.keys.toList())
        assertEquals(listOf("Russian"), direct.allohaSubtitles.map { it.label })
    }

    @Test
    fun `header names are lowercased`() {
        val session = session().apply { start() }

        assertEquals(mapOf("user-agent" to "UA", "authorizations" to "token-v1"), session.currentHeaders())
    }

    fun `direct stream carries current headers for every quality`() {
        val session = session().apply { start() }

        val direct = session.directStream

        assertEquals(session.currentHeaders(), direct.headers)
        assertEquals(setOf("360p", "720p"), direct.qualityHeaders.keys)
        assertTrue(direct.qualityHeaders.values.all { it == session.currentHeaders() })
    }

    @Test
    fun `selecting a known quality changes the direct url, unknown is ignored`() {
        val session = session().apply { start() }

        session.selectQuality("360p")
        assertEquals("https://cdn/v1/a360", session.directStream.url)

        session.selectQuality("2160p")
        assertEquals("https://cdn/v1/a360", session.directStream.url)
    }

    @Test
    fun `preselect applies only an available quality`() {
        val session = session().apply { start() }

        session.preselectQuality("1080p")
        assertEquals("https://cdn/v1/a720", session.directStream.url)

        session.preselectQuality("360p")
        assertEquals("https://cdn/v1/a360", session.directStream.url)
    }

    @Test
    fun `switching dubbing swaps ladder and master`() {
        val session = session().apply { start() }

        session.selectAudioTrack("b")

        assertEquals("https://cdn/v1/b720", session.currentMasterUrl())
        assertEquals(listOf("480p", "720p"), session.directStream.qualities!!.keys.toList())
        assertEquals("b", session.directStream.selectedAllohaAudioId)
    }

    @Test
    fun `switching dubbing drops a quality the new dubbing lacks`() {
        val session = session().apply { start() }
        session.selectQuality("360p")

        session.selectAudioTrack("b")

        assertEquals("https://cdn/v1/b720", session.directStream.url)
    }

    @Test
    fun `switching to an unknown dubbing changes nothing`() {
        val session = session().apply { start() }

        session.selectAudioTrack("zzz")

        assertEquals("a", session.directStream.selectedAllohaAudioId)
        assertEquals("https://cdn/v1/a720", session.currentMasterUrl())
    }

    @Test
    fun `blank master url is ignored and headers merge outside rotation`() {
        val session = session().apply { start() }

        session.updateMasterUrl("  ")
        session.updateHeaders(mapOf("X-Token" to "t2"))

        assertEquals("https://cdn/v1/a720", session.currentMasterUrl())
        assertEquals("t2", session.currentHeaders()["x-token"])
        assertEquals("UA", session.currentHeaders()["user-agent"])
    }

    @Test
    fun `expiry is unknown until a config update sets it`() {
        val session = session().apply { start() }
        assertNull(session.expiresAtMs())

        val before = System.currentTimeMillis()
        session.updateExpiry(60)

        val expiresAt = session.expiresAtMs()!!
        assertTrue(expiresAt >= before + 60_000L)
        assertTrue(session.hasSeenConfigUpdate)
    }

    @Test
    fun `fallback expiry applies only when none is set and is not a config update`() {
        val session = session().apply { start() }

        session.ensureFallbackExpiry(30)
        val first = session.expiresAtMs()
        session.ensureFallbackExpiry(3_600)

        assertNotNull(first)
        assertEquals("a live expiry must not be overwritten", first, session.expiresAtMs())
        assertFalse(session.hasSeenConfigUpdate)
    }

    @Test
    fun `refresh stages a rotation once and asks the webview to reload`() {
        val session = session().apply { start() }

        session.refresh()
        session.refresh()

        assertTrue(session.isRotating)
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `live state is untouched while a rotation is staged`() {
        val session = session().apply { start() }
        session.refresh()

        session.initialize(sources("v2"), mapOf("authorizations" to "token-v2"))

        assertEquals("https://cdn/v1/a720", session.currentMasterUrl())
        assertEquals("token-v1", session.currentHeaders()["authorizations"])
        assertTrue(session.isRotating)
    }

    @Test
    fun `rotation commits on ready and master when no config update was ever seen`() {
        val session = session().apply { start() }
        session.refresh()

        session.initialize(sources("v2"), mapOf("authorizations" to "token-v2"))
        session.updateMasterUrl("https://cdn/v2/signed-master")

        assertFalse(session.isRotating)
        assertEquals("https://cdn/v2/signed-master", session.currentMasterUrl())
        assertEquals("token-v2", session.currentHeaders()["authorizations"])
    }

    @Test
    fun `rotation waits for the config update once one has been seen`() {
        val session = session().apply { start() }
        session.updateExpiry(60)
        session.refresh()

        session.initialize(sources("v2"), mapOf("authorizations" to "token-v2"))
        session.updateMasterUrl("https://cdn/v2/signed-master")
        assertTrue("still waiting for config_update", session.isRotating)

        session.updateExpiry(120)

        assertFalse(session.isRotating)
        assertEquals("https://cdn/v2/signed-master", session.currentMasterUrl())
    }

    @Test
    fun `fallback expiry does not complete a rotation that waits for a config update`() {
        val session = session().apply { start() }
        session.updateExpiry(60)
        session.refresh()
        session.initialize(sources("v2"), mapOf("authorizations" to "token-v2"))
        session.updateMasterUrl("https://cdn/v2/signed-master")

        session.ensureFallbackExpiry(30)

        assertTrue(session.isRotating)
    }

    @Test
    fun `timeout commits whatever arrived`() {
        val session = session().apply { start() }
        session.updateExpiry(60)
        session.refresh()
        session.initialize(sources("v2"), mapOf("authorizations" to "token-v2"))

        timeout.captured.run()

        assertFalse(session.isRotating)
        assertEquals("token-v2", session.currentHeaders()["authorizations"])
    }

    @Test
    fun `dubbing and quality survive a rotation`() {
        val session = session().apply { start() }
        session.selectAudioTrack("b")
        session.selectQuality("480p")
        session.refresh()

        session.initialize(sources("v2"), mapOf("authorizations" to "token-v2"))
        session.updateMasterUrl("https://cdn/v2/signed-master")

        val direct = session.directStream
        assertEquals("b", direct.selectedAllohaAudioId)
        assertEquals("https://cdn/v2/b480", direct.url)
    }

    @Test
    fun `close cancels the pending commit and releases through the handler`() {
        val session = session().apply { start() }
        session.refresh()

        session.close()

        assertFalse(session.isRotating)
        verify { handler.removeCallbacks(any()) }
    }
}
