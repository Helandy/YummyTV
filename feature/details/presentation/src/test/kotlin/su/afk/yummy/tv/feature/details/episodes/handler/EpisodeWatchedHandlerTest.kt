package su.afk.yummy.tv.feature.details.episodes.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideosUseCase
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.ClearEpisodeWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.MarkEpisodeWatchedLocallyUseCase
import java.io.IOException

class EpisodeWatchedHandlerTest : BaseUnitTest() {

    private val markLocally: MarkEpisodeWatchedLocallyUseCase = mockk(relaxed = true)
    private val clearProgress: ClearEpisodeWatchProgressUseCase = mockk(relaxed = true)
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase = mockk()
    private val removeWatchedVideos: RemoveWatchedVideosUseCase = mockk()
    private val pendingMutationQueue: PendingMutationQueue = mockk()

    private val meta = EpisodeWatchedHandler.EpisodeMeta("Title", "poster", "shot")
    private val anilibria = video(id = 11, dubbing = "AniLibria", durationSeconds = null)
    private val dream = video(id = 12, dubbing = "Dream Cast", durationSeconds = 1_380)

    @Test
    fun `mark goes to the video with existing progress and its known duration`() = runTest {
        coEvery { saveVideoWatchProgress(any(), any(), any(), any()) } returns true
        val existing = progress(videoId = 11, durationMs = 1_500_000L)

        val ok = createHandler().markWatched(ANIME_ID, "3", listOf(dream, anilibria), "Dream Cast", existing, meta, isSignedIn = true)

        assertTrue(ok)
        coVerify { markLocally(ANIME_ID, "3", 11, anilibria.iframeUrl, 1_500_000L, "Title", "poster", "Kodik", "AniLibria", "shot") }
        // На сервер — у конца серии и без «просмотренных секунд».
        coVerify { saveVideoWatchProgress(videoId = 11, timeSeconds = 1_490, durationSeconds = 1_500, times = emptyList()) }
    }

    @Test
    fun `without progress the title's best dubbing and the episode duration are used`() = runTest {
        coEvery { saveVideoWatchProgress(any(), any(), any(), any()) } returns true

        createHandler().markWatched(ANIME_ID, "3", listOf(anilibria, dream), "Dream Cast", null, meta, isSignedIn = true)

        coVerify { markLocally(any(), any(), 12, any(), 1_380_000L, any(), any(), any(), "Dream Cast", any()) }
    }

    @Test
    fun `unknown duration falls back to a typical episode`() = runTest {
        createHandler().markWatched(ANIME_ID, "3", listOf(anilibria), "", null, meta, isSignedIn = false)

        coVerify { markLocally(any(), any(), 11, any(), 24 * 60 * 1_000L, any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { saveVideoWatchProgress(any(), any(), any(), any()) }
    }

    @Test
    fun `episode without videos is a no-op`() = runTest {
        assertTrue(createHandler().markWatched(ANIME_ID, "3", emptyList(), "", null, meta, isSignedIn = true))
        coVerify(exactly = 0) { markLocally(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `offline mark is queued and counts as success, a rejection does not`() = runTest {
        val offline = IOException("offline")
        coEvery { saveVideoWatchProgress(any(), any(), any(), any()) } throws offline
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), offline) } returns true
        val handler = createHandler()

        assertTrue(handler.markWatched(ANIME_ID, "3", listOf(dream), "", null, meta, isSignedIn = true))
        coVerify { pendingMutationQueue.enqueueOnNetworkFailure(PendingMutation.MarkWatched(12, 1_370, 1_380), offline) }

        val rejected = IllegalStateException("400")
        coEvery { saveVideoWatchProgress(any(), any(), any(), any()) } throws rejected
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), rejected) } returns false
        assertFalse(handler.markWatched(ANIME_ID, "3", listOf(dream), "", null, meta, isSignedIn = true))
    }

    @Test
    fun `unmark clears every dubbing of the episode on the server`() = runTest {
        coEvery { removeWatchedVideos(any()) } returns true

        val ok = createHandler().unmarkWatched(ANIME_ID, "3", listOf(anilibria, dream, video(id = 0, dubbing = "X")), isSignedIn = true)

        assertTrue(ok)
        coVerify { clearProgress(ANIME_ID, "3") }
        coVerify { removeWatchedVideos(listOf(11, 12)) }
    }

    @Test
    fun `guest unmark stays local`() = runTest {
        assertTrue(createHandler().unmarkWatched(ANIME_ID, "3", listOf(anilibria), isSignedIn = false))

        coVerify { clearProgress(ANIME_ID, "3") }
        coVerify(exactly = 0) { removeWatchedVideos(any()) }
    }

    private fun video(id: Int, dubbing: String, durationSeconds: Int? = null) = AnimeVideo(
        id = id,
        episode = "3",
        dubbing = dubbing,
        player = "Kodik",
        playerId = null,
        iframeUrl = "https://kodik.info/$id",
        durationSeconds = durationSeconds,
    )

    private fun progress(videoId: Int, durationMs: Long) = AnimeWatchProgress(
        animeId = ANIME_ID,
        episode = "3",
        videoId = videoId,
        episodeUrl = "",
        positionMs = 60_000L,
        durationMs = durationMs,
        updatedAt = 1L,
    )

    private fun createHandler() = EpisodeWatchedHandler(
        markEpisodeWatchedLocally = markLocally,
        clearEpisodeWatchProgress = clearProgress,
        saveVideoWatchProgress = saveVideoWatchProgress,
        removeWatchedVideos = removeWatchedVideos,
        pendingMutationQueue = pendingMutationQueue,
    )

    private companion object {
        const val ANIME_ID = 42
    }
}
