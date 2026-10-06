package su.afk.yummy.tv.feature.details.episodes

import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.error.isNetworkError
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideosUseCase
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import su.afk.yummy.tv.domain.player.usecase.ClearEpisodeWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.MarkEpisodeWatchedLocallyUseCase
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchedHandler
import java.io.IOException

/**
 * Ручная отметка серии просмотренной: локально это позиция, равная длительности, на сервере —
 * время у конца серии без добавления просмотренных секунд.
 */
class EpisodeWatchedHandlerTest : BaseUnitTest() {

    private val progressRepository: WatchProgressRepository = mockk(relaxed = true)
    private val watchesRepository: VideoWatchesRepository = mockk()
    private lateinit var clock: AppClock
    private val notifier: AccountMutationErrorRepository = mockk(relaxed = true)
    private lateinit var enqueued: MutableList<PendingMutation>
    private val queue: PendingMutationQueue = mockk()

    @Before
    fun setUp() {
        with(progressRepository) {
            coEvery { get(any(), any()) } returns null
            coEvery { allMeaningfulVideoProgress() } returns emptyList()
        }
        with(watchesRepository) {
            coEvery { markWatched(any(), any(), any(), any()) } returns true
            coEvery { removeWatched(any()) } returns true
        }
        clock = mockk<AppClock> { every { nowMillis() } returns 1_000L }
        enqueued = mutableListOf()
        with(queue) {
            coEvery { enqueueOnNetworkFailure(any(), any()) } coAnswers {
                secondArg<Throwable>().isNetworkError().also { queued ->
                    if (queued) enqueued += firstArg<PendingMutation>()
                }
            }
        }
    }

    private fun createHandler() = EpisodeWatchedHandler(
        markEpisodeWatchedLocally = MarkEpisodeWatchedLocallyUseCase(progressRepository, clock),
        clearEpisodeWatchProgress = ClearEpisodeWatchProgressUseCase(progressRepository),
        saveVideoWatchProgress = SaveVideoWatchProgressUseCase(watchesRepository),
        removeWatchedVideos = RemoveWatchedVideosUseCase(watchesRepository, notifier),
        pendingMutationQueue = queue,
    )

    private val meta = EpisodeWatchedHandler.EpisodeMeta(
        animeTitle = "Title",
        posterUrl = "poster",
        screenshotUrl = "shot",
    )

    private val videos = listOf(
        video(id = 1, dubbing = OTHER, durationSeconds = 1_500),
        video(id = 2, dubbing = BEST, durationSeconds = 1_500),
    )

    @Test
    fun `mark writes full position locally and end time to the server`() = runTest {
        val succeeded = createHandler().markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = true,
        )

        assertTrue(succeeded)
        // Озвучка по умолчанию для тайтла, а не первая в списке.
        coVerify(exactly = 1) { savedWith(progressRepository, videoId = 2, positionMs = 1_500_000L, durationMs = 1_500_000L) }
        // Фактически серия не просматривалась, поэтому spent_time расти не должен (times пуст).
        coVerify(exactly = 1) {
            watchesRepository.markWatched(videoId = 2, timeSeconds = 1_490, durationSeconds = 1_500, times = emptyList())
        }
    }

    @Test
    fun `mark prefers the video that already has progress`() = runTest {
        createHandler().markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = progress(videoId = 1, durationMs = 1_320_000L),
            meta = meta,
            isSignedIn = true,
        )

        // Длительность известной записи важнее длительности из списка серий.
        coVerify(exactly = 1) { savedWith(progressRepository, videoId = 1, positionMs = 1_320_000L, durationMs = 1_320_000L) }
    }

    @Test
    fun `mark falls back to a typical duration when the api reports none`() = runTest {
        createHandler().markWatched(
            animeId = 7,
            episode = "3",
            videos = listOf(video(id = 5, dubbing = BEST, durationSeconds = null)),
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = true,
        )

        coVerify(exactly = 1) { savedWith(progressRepository, videoId = 5, positionMs = any(), durationMs = 24 * 60 * 1_000L) }
    }

    @Test
    fun `mark keeps the local record when the server call fails`() = runTest {
        coEvery { watchesRepository.markWatched(any(), any(), any(), any()) } throws IllegalStateException("network")

        val succeeded = createHandler().markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = true,
        )

        assertFalse(succeeded)
        coVerify(exactly = 1) { savedWith(progressRepository) }
        // Ошибка не сетевая — повторять нечего, очередь остаётся пустой.
        assertTrue(enqueued.isEmpty())
    }

    @Test
    fun `mark queues the server call when the device is offline`() = runTest {
        coEvery { watchesRepository.markWatched(any(), any(), any(), any()) } throws IOException("offline")

        val succeeded = createHandler().markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = true,
        )

        // Локальная отметка уже проставлена, а доставку берёт на себя offline-очередь.
        assertTrue(succeeded)
        coVerify(exactly = 1) { savedWith(progressRepository) }
        assertEquals(listOf<PendingMutation>(PendingMutation.MarkWatched(2, 1_490, 1_500)), enqueued)
    }

    @Test
    fun `unmark queues the server call when the device is offline`() = runTest {
        coEvery { watchesRepository.removeWatched(any()) } throws IOException("offline")

        val succeeded = createHandler().unmarkWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            isSignedIn = true,
        )

        assertTrue(succeeded)
        coVerify(exactly = 1) { progressRepository.delete(7, "3") }
        assertEquals(listOf<PendingMutation>(PendingMutation.RemoveWatched(listOf(1, 2))), enqueued)
    }

    @Test
    fun `mark stays local when signed out`() = runTest {
        val succeeded = createHandler().markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = false,
        )

        assertTrue(succeeded)
        coVerify(exactly = 1) { savedWith(progressRepository) }
        coVerify(exactly = 0) { watchesRepository.markWatched(any(), any(), any(), any()) }
    }

    @Test
    fun `unmark clears the local record and every dubbing on the server`() = runTest {
        val succeeded = createHandler().unmarkWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            isSignedIn = true,
        )

        assertTrue(succeeded)
        coVerify(exactly = 1) { progressRepository.delete(7, "3") }
        coVerify(exactly = 1) { watchesRepository.removeWatched(listOf(1, 2)) }
    }

    @Test
    fun `unmark stays local when signed out`() = runTest {
        createHandler().unmarkWatched(animeId = 7, episode = "3", videos = videos, isSignedIn = false)

        coVerify(exactly = 1) { progressRepository.delete(7, "3") }
        coVerify(exactly = 0) { watchesRepository.removeWatched(any()) }
    }

    private fun video(id: Int, dubbing: String, durationSeconds: Int?) = AnimeVideo(
        id = id,
        episode = "3",
        dubbing = dubbing,
        player = "Плеер Kodik",
        playerId = null,
        iframeUrl = "https://example.test/$id",
        durationSeconds = durationSeconds,
    )

    private fun progress(videoId: Int, durationMs: Long) = AnimeWatchProgress(
        animeId = 7,
        episode = "3",
        videoId = videoId,
        episodeUrl = "https://example.test/$videoId",
        positionMs = 600_000L,
        durationMs = durationMs,
        updatedAt = 1L,
    )

    /** Сохранение прогресса с проверкой только тех полей, что заданы; остальные — любые. */
    private suspend fun MockKMatcherScope.savedWith(
        repository: WatchProgressRepository,
        videoId: Int? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
    ) = repository.save(
        animeId = any(),
        episode = any(),
        videoId = videoId ?: any(),
        episodeUrl = any(),
        positionMs = positionMs ?: any(),
        durationMs = durationMs ?: any(),
        updatedAt = any(),
        animeTitle = any(),
        posterUrl = any(),
        playerName = any(),
        dubbing = any(),
        screenshotUrl = any(),
    )

    private companion object {
        const val BEST = "AniLibria"
        const val OTHER = "AniDub"
    }
}
