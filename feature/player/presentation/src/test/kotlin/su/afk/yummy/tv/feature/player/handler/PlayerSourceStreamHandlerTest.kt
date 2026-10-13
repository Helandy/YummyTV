package su.afk.yummy.tv.feature.player.handler

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.usecase.GetPlayerSourceGraphUseCase
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.domain.player.model.PlayerSourceBalancer as DomainBalancer
import su.afk.yummy.tv.domain.player.model.PlayerSourceDubbing as DomainDubbing
import su.afk.yummy.tv.domain.player.model.PlayerSourceEpisode as DomainEpisode
import su.afk.yummy.tv.domain.player.model.PlayerSourceGraph as DomainGraph

class PlayerSourceStreamHandlerTest : BaseUnitTest() {

    private val streamHandler: PlayerStreamHandler = mockk()
    private val getPlayerSourceGraph: GetPlayerSourceGraphUseCase = mockk()
    private val analytics: PlayerAnalytics = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { streamHandler.playbackErrorMessage(any(), any()) } answers { "error: ${firstArg<String>()}" }
    }

    // ---------------- source graph ----------------

    @Test
    fun `loaded graph is returned with the caller's stream instructions`() = runTest {
        coEvery { getPlayerSourceGraph(any(), true) } returns domainGraph()

        val result = createHandler().loadSourceGraph(
            state(),
            forceRefreshVideos = true,
            loadStreamAfterRefresh = true,
            resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
        )

        result as PlayerSourceGraphLoadResult.SourceGraph
        assertEquals("Kodik", result.sourceGraph.balancers.single().name)
        assertTrue(result.loadStreamAfterRefresh)
        assertEquals(PlayerStreamResumeMode.SelectedSourceOnly, result.resumeMode)
        // Принудительное обновление /videos не повторяется при отказе потока.
        assertEquals(false, result.refreshStreamOnFailure)
    }

    @Test
    fun `failed or empty graph loads the stream only when asked`() = runTest {
        val handler = createHandler()
        coEvery { getPlayerSourceGraph(any(), any()) } throws IllegalStateException("api")

        assertEquals(PlayerSourceGraphLoadResult.Ignore, handler.loadSourceGraph(state()))
        assertEquals(
            PlayerSourceGraphLoadResult.LoadStream(PlayerStreamResumeMode.PreserveCurrent, refreshSourcesOnFailure = true),
            handler.loadSourceGraph(state(), loadStreamOnFailure = true),
        )

        coEvery { getPlayerSourceGraph(any(), any()) } returns DomainGraph()
        assertEquals(PlayerSourceGraphLoadResult.Ignore, handler.loadSourceGraph(state()))
    }

    @Test
    fun `local playback without a title skips the graph`() = runTest {
        val handler = createHandler()
        val local = state().copy(animeId = 0)

        assertEquals(PlayerSourceGraphLoadResult.Ignore, handler.loadSourceGraph(local))
        assertEquals(
            PlayerSourceGraphLoadResult.LoadStream(PlayerStreamResumeMode.PreserveCurrent, refreshSourcesOnFailure = true),
            handler.loadSourceGraph(local, loadStreamAfterRefresh = true),
        )
    }

    @Test
    fun `applied graph keeps the active video instead of its stale selection`() {
        val state = state(episodeIndex = 1).copy(streamUrl = STREAM, playbackPositionMs = 50_000L)
        // Граф пришёл с выбором первой серии, а играет вторая.
        val fresh = graph().copy(selection = PlayerSourceSelection(episodeIndex = 0))

        val applied = createHandler().applySourceGraph(state, fresh)

        assertEquals(PlayerSourceSelection(episodeIndex = 1), applied.sourceSelection)
        assertEquals(STREAM, applied.streamUrl)
    }

    @Test
    fun `applied graph with another iframe drops the old stream but keeps the position`() {
        val state = state(episodeIndex = 1).copy(streamUrl = STREAM, playbackPositionMs = 50_000L)
        val moved = graph(iframePrefix = "https://kodik/new")

        val applied = createHandler().applySourceGraph(state, moved)

        assertNull(applied.streamUrl)
        assertEquals(50_000L, applied.playbackPositionMs)
    }

    // ---------------- preparing ----------------

    @Test
    fun `explicit episode switch resets the position, reload of the same source keeps it`() {
        val handler = createHandler()
        val state = state().copy(
            streamUrl = STREAM,
            playbackPositionMs = 50_000L,
            resumeFromMs = 40_000L,
            dubbingResumeMs = 30_000L,
            allohaAudioTracks = listOf(AllohaAudioTrack("a", "RU")),
            playerError = "old",
        )

        val switched = handler.preparingStreamLoad(state, PlayerStreamResumeMode.SelectedSourceOnly)
        val reloaded = handler.preparingStreamLoad(state, PlayerStreamResumeMode.PreserveCurrent)

        assertNull(switched.streamUrl)
        assertNull(switched.playerError)
        assertTrue(switched.allohaAudioTracks.isEmpty())
        assertEquals(0L, switched.playbackPositionMs)
        assertEquals(-1L, switched.dubbingResumeMs)
        assertEquals(50_000L, reloaded.playbackPositionMs)
        assertEquals(30_000L, reloaded.dubbingResumeMs)
    }

    @Test
    fun `in-place recovery keeps the current stream and only clears errors`() {
        val handler = createHandler()
        val state = state().copy(streamUrl = STREAM, playerError = "old", kodikBlockedError = "blocked")

        val recovering = handler.preparingStreamResolve(state, preserveCurrentStream = true)
        val fresh = handler.preparingStreamResolve(state)

        assertEquals(STREAM, recovering.streamUrl)
        assertNull(recovering.playerError)
        assertNull(recovering.kodikBlockedError)
        assertNull(fresh.streamUrl)
    }

    // ---------------- resolve ----------------

    @Test
    fun `resolved stream is merged into the state`() = runTest {
        val session: AllohaStreamSession = mockk()
        coEvery { streamHandler.resolve(any(), 20_000L, any(), any(), any()) } returns PlayerStreamResult.Stream(
            url = STREAM,
            headers = mapOf("Referer" to "r"),
            qualities = linkedMapOf("720p" to "u"),
            selectedQuality = "720p",
            resumeFromMs = 20_000L,
            consumedPendingResume = true,
            allohaSession = session,
            failoverHost = "cdn2",
        )
        val state = state().copy(dubbingResumeMs = 20_000L)

        val result = createHandler().resolveStream(
            state,
            pendingResume = 20_000L,
            destinationResumeMs = 20_000L,
            resumeMode = PlayerStreamResumeMode.PreserveCurrent,
            refreshSourcesOnFailure = true,
        ) as PlayerStreamLoadResult.State

        assertEquals(STREAM, result.state.streamUrl)
        assertEquals("720p", result.state.selectedQuality)
        assertEquals("cdn2", result.state.streamFailoverHost)
        assertEquals(20_000L, result.state.resumeFromMs)
        assertEquals(-1L, result.state.dubbingResumeMs)
        assertTrue(result.consumedDestinationResume)
        assertSame(session, result.allohaSession)
        verify { analytics.eventStreamStarted(state) }
    }

    @Test
    fun `first failure asks for fresh sources, the final one shows the error`() = runTest {
        coEvery { streamHandler.resolve(any(), any(), any(), any(), any()) } returns
            PlayerStreamResult.PlayerError(message = "unavailable", reason = PlayerStreamResult.REASON_UNAVAILABLE)
        val handler = createHandler()

        val first = handler.resolveStream(state(), null, null, PlayerStreamResumeMode.SelectedSourceOnly, refreshSourcesOnFailure = true)
        val last = handler.resolveStream(state(), null, null, PlayerStreamResumeMode.SelectedSourceOnly, refreshSourcesOnFailure = false)

        assertEquals(PlayerStreamLoadResult.RefreshSources(PlayerStreamResumeMode.SelectedSourceOnly), first)
        assertEquals("unavailable", (last as PlayerStreamLoadResult.State).state.playerError)
        verify(exactly = 2) {
            analytics.eventStreamResolveFailed(any(), PlayerStreamResult.REASON_UNAVAILABLE, any(), any(), any(), any())
        }
    }

    @Test
    fun `kodik block is shown in its own error`() = runTest {
        coEvery { streamHandler.resolve(any(), any(), any(), any(), any()) } returns PlayerStreamResult.KodikBlocked("blocked")

        val result = createHandler().resolveStream(
            state(), null, null, PlayerStreamResumeMode.PreserveCurrent, refreshSourcesOnFailure = false,
        ) as PlayerStreamLoadResult.State

        assertEquals("blocked", result.state.kodikBlockedError)
        assertNull(result.state.playerError)
    }

    @Test
    fun `crash while resolving becomes a stream error`() = runTest {
        coEvery { streamHandler.resolve(any(), any(), any(), any(), any()) } throws IllegalStateException("boom")

        val result = createHandler().resolveStream(
            state().copy(streamUrl = STREAM), null, null, PlayerStreamResumeMode.PreserveCurrent, refreshSourcesOnFailure = false,
        ) as PlayerStreamLoadResult.State

        assertEquals("error: boom", result.state.playerError)
        assertNull(result.state.streamUrl)
        verify {
            analytics.eventStreamResolveFailed(any(), PlayerStreamResult.REASON_EXCEPTION, any(), any(), any(), any())
        }
    }

    private fun state(episodeIndex: Int = 0) = PlayerState.State(
        animeId = ANIME_ID,
        sourceGraph = graph(),
        sourceSelection = PlayerSourceSelection(episodeIndex = episodeIndex),
    )

    private fun graph(iframePrefix: String = "https://kodik/ep") = PlayerSourceGraph(
        balancers = listOf(
            PlayerSourceBalancer(
                name = "Kodik",
                dubbings = listOf(
                    PlayerSourceDubbing(
                        name = "AniLibria",
                        episodes = listOf(
                            PlayerSourceEpisode(id = 1, number = "1", iframeUrl = "$iframePrefix/1"),
                            PlayerSourceEpisode(id = 2, number = "2", iframeUrl = "$iframePrefix/2"),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun domainGraph() = DomainGraph(
        balancers = listOf(
            DomainBalancer(
                name = "Kodik",
                dubbings = listOf(
                    DomainDubbing(name = "AniLibria", episodes = listOf(DomainEpisode(id = 1, number = "1", iframeUrl = "u"))),
                ),
            ),
        ),
    )

    private fun createHandler() = PlayerSourceStreamHandler(streamHandler, getPlayerSourceGraph, analytics)

    private companion object {
        const val ANIME_ID = 42
        const val STREAM = "https://cdn/master.m3u8"
    }
}
