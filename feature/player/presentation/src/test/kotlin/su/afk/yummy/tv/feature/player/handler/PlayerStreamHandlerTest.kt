package su.afk.yummy.tv.feature.player.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.model.PlayerStreamUnavailableCause
import su.afk.yummy.tv.domain.player.usecase.GetWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.InvalidatePlayerStreamCacheUseCase
import su.afk.yummy.tv.domain.player.usecase.OpenAllohaStreamSessionUseCase
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.presentation.R

class PlayerStreamHandlerTest : BaseUnitTest() {

    private val getWatchProgress: GetWatchProgressUseCase = mockk()
    private val settingsStore: PlayerSettingsStore = mockk()
    private val resolvePlayerStream: ResolvePlayerStreamUseCase = mockk()
    private val invalidateCache: InvalidatePlayerStreamCacheUseCase = mockk(relaxed = true)
    private val openAllohaSession: OpenAllohaStreamSessionUseCase = mockk()
    private val strings: StringProvider = mockk()

    private val preferredQuality = MutableStateFlow(PreferredVideoQuality.P720)
    private val adaptiveQuality = MutableStateFlow(false)

    @Before
    fun setUp() {
        every { strings.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { strings.get(any<Int>(), *anyVararg()) } answers { "res${firstArg<Int>()}" }
        every { settingsStore.preferredVideoQuality } returns preferredQuality
        every { settingsStore.adaptiveVideoQualityEnabled } returns adaptiveQuality
        coEvery { getWatchProgress(ANIME_ID, "3") } returns null
    }

    @Test
    fun `preferred quality is picked exactly when available`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("360p", "720p", "1080p")

        val result = createHandler().resolve(state(KODIK_URL), pendingResumeMs = null) as PlayerStreamResult.Stream

        assertEquals("720p", result.selectedQuality)
        assertEquals("https://cdn/master.m3u8", result.url)
    }

    @Test
    fun `missing preferred quality falls back to the closest lower, then to the best`() = runTest {
        val handler = createHandler()
        coEvery { resolvePlayerStream(any()) } returns stream("360p", "720p")

        preferredQuality.value = PreferredVideoQuality.P1080
        assertEquals("720p", (handler.resolve(state(KODIK_URL), null) as PlayerStreamResult.Stream).selectedQuality)

        coEvery { resolvePlayerStream(any()) } returns stream("480p", "720p")
        preferredQuality.value = PreferredVideoQuality.P360
        assertEquals("720p", (handler.resolve(state(KODIK_URL), null) as PlayerStreamResult.Stream).selectedQuality)
    }

    @Test
    fun `best quality leaves the choice to the player`() = runTest {
        preferredQuality.value = PreferredVideoQuality.BEST
        coEvery { resolvePlayerStream(any()) } returns stream("720p", "1080p")

        val result = createHandler().resolve(state(KODIK_URL), null) as PlayerStreamResult.Stream

        assertNull(result.selectedQuality)
    }

    @Test
    fun `adaptive quality starts at the best level up to the policy ceiling and ignores the preferred one`() = runTest {
        adaptiveQuality.value = true
        preferredQuality.value = PreferredVideoQuality.P360
        coEvery { resolvePlayerStream(any()) } returns stream("360p", "720p", "1080p", "2160p")

        val result = createHandler().resolve(state(KODIK_URL), null) as PlayerStreamResult.Stream

        assertEquals("2160p", result.selectedQuality)
    }

    @Test
    fun `quality override wins only when the stream has it`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("360p", "720p", "1080p")
        val handler = createHandler()

        val kept = handler.resolve(state(KODIK_URL), null, selectedQualityOverride = "1080p") as PlayerStreamResult.Stream
        val ignored = handler.resolve(state(KODIK_URL), null, selectedQualityOverride = "2160p") as PlayerStreamResult.Stream

        assertEquals("1080p", kept.selectedQuality)
        assertEquals("720p", ignored.selectedQuality)
    }

    @Test
    fun `pending resume is consumed without reading local progress`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("720p")

        val result = createHandler().resolve(state(KODIK_URL), pendingResumeMs = 45_000L) as PlayerStreamResult.Stream

        assertEquals(45_000L, result.resumeFromMs)
        assertTrue(result.consumedPendingResume)
        coVerify(exactly = 0) { getWatchProgress(any(), any()) }
    }

    @Test
    fun `local progress resumes an unfinished episode but not a watched one`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns stream("720p")
        val handler = createHandler()

        coEvery { getWatchProgress(ANIME_ID, "3") } returns progress(positionMs = 120_000L)
        val unfinished = handler.resolve(state(KODIK_URL), null) as PlayerStreamResult.Stream
        coEvery { getWatchProgress(ANIME_ID, "3") } returns progress(positionMs = DURATION_MS)
        val watched = handler.resolve(state(KODIK_URL), null) as PlayerStreamResult.Stream

        assertEquals(120_000L, unfinished.resumeFromMs)
        assertFalse(unfinished.consumedPendingResume)
        assertEquals(0L, watched.resumeFromMs)
    }

    @Test
    fun `alloha plays the session stream and gets the selected quality`() = runTest {
        val session = allohaSession(stream("720p", "1080p"))

        val result = createHandler().resolve(state(ALLOHA_URL), null) as PlayerStreamResult.Stream

        assertSame(session, result.allohaSession)
        verify { session.selectQuality("720p") }
        coVerify(exactly = 0) { resolvePlayerStream(any()) }
    }

    @Test
    fun `failed source gives the generic stream error with the technical reason`() = runTest {
        coEvery { resolvePlayerStream(any()) } returns PlayerStreamResolveResult.Failed(reason = "http 500")

        val result = createHandler().resolve(state(KODIK_URL), null)

        assertEquals(
            PlayerStreamResult.PlayerError("res${R.string.player_stream_error}", PlayerStreamResult.REASON_FAILED, "http 500"),
            result,
        )
    }

    @Test
    fun `fresh alloha session is closed on a crash, a reused one is kept`() = runTest {
        val session = allohaSession(stream("720p"))
        every { session.selectQuality(any()) } throws IllegalStateException("webview died")
        val handler = createHandler()

        runCatching { handler.resolve(state(ALLOHA_URL), null, reuseAllohaPlaybackSession = true) }
        verify(exactly = 0) { session.close() }

        runCatching { handler.resolve(state(ALLOHA_URL), null, reuseAllohaPlaybackSession = false) }
        verify(exactly = 1) { session.close() }
    }

    @Test
    fun `source refusals become presentation errors`() = runTest {
        val handler = createHandler()

        coEvery { resolvePlayerStream(any()) } returns PlayerStreamResolveResult.KodikBlocked(message = "blocked")
        assertEquals(PlayerStreamResult.KodikBlocked("blocked"), handler.resolve(state(KODIK_URL), null))

        coEvery { resolvePlayerStream(any()) } returns
            PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.RegionBlocked)
        assertEquals(
            PlayerStreamResult.PlayerError(
                "res${R.string.player_region_blocked}",
                PlayerStreamResult.REASON_UNAVAILABLE,
                PlayerStreamUnavailableCause.RegionBlocked.name,
            ),
            handler.resolve(state(KODIK_URL), null),
        )

        coEvery { resolvePlayerStream(any()) } returns PlayerStreamResolveResult.Unsupported
        assertEquals(
            PlayerStreamResult.PlayerError("res${R.string.player_unsupported}", PlayerStreamResult.REASON_UNSUPPORTED),
            handler.resolve(state(KODIK_URL), null),
        )
    }

    @Test
    fun `playback error message lists the detail and a distinct code`() {
        val handler = createHandler()
        val base = "res${R.string.player_stream_error}"

        assertEquals("$base\nSource error\n4003", handler.playbackErrorMessage(" Source error ", "4003"))
        assertEquals("$base\n4003", handler.playbackErrorMessage("4003", "4003"))
        assertEquals(base, handler.playbackErrorMessage("  ", null))
    }

    @Test
    fun `cache invalidation goes to the repository`() {
        createHandler().invalidateCache(KODIK_URL)

        verify { invalidateCache(KODIK_URL) }
    }

    private fun allohaSession(initial: PlayerStreamResolveResult.Stream): AllohaStreamSession {
        val session: AllohaStreamSession = mockk(relaxed = true)
        every { session.initialStream } returns initial
        coEvery { openAllohaSession(any()) } returns session
        return session
    }

    private fun stream(vararg qualities: String) = PlayerStreamResolveResult.Stream(
        url = "https://cdn/master.m3u8",
        qualities = linkedMapOf(*qualities.map { it to "https://cdn/$it.m3u8" }.toTypedArray()),
    )

    private fun progress(positionMs: Long) = AnimeWatchProgress(
        animeId = ANIME_ID,
        episode = "3",
        episodeUrl = KODIK_URL,
        positionMs = positionMs,
        durationMs = DURATION_MS,
        updatedAt = 1L,
    )

    private fun state(iframeUrl: String) = PlayerState.State(
        animeId = ANIME_ID,
        sourceGraph = PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer(
                    name = "Player",
                    dubbings = listOf(
                        PlayerSourceDubbing(
                            name = "AniLibria",
                            episodes = listOf(PlayerSourceEpisode(id = 1, number = "3", iframeUrl = iframeUrl)),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun createHandler() = PlayerStreamHandler(
        getWatchProgress = getWatchProgress,
        settingsStore = settingsStore,
        resolvePlayerStream = resolvePlayerStream,
        invalidatePlayerStreamCache = invalidateCache,
        openAllohaStreamSession = openAllohaSession,
        strings = strings,
    )

    private companion object {
        const val ANIME_ID = 42
        const val DURATION_MS = 1_440_000L
        const val KODIK_URL = "https://kodik.info/seria/1"
        const val ALLOHA_URL = "https://alloha.tv/player/1"
    }
}
