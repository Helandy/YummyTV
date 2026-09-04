package su.afk.yummy.tv.feature.details.episodes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.storage.outbox.MarkWatchedPayload
import su.afk.yummy.tv.core.storage.outbox.PendingMutationEntry
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
import su.afk.yummy.tv.core.storage.outbox.PendingMutationTypes
import su.afk.yummy.tv.core.storage.outbox.RemoveWatchedPayload
import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorEvent
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
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
class EpisodeWatchedHandlerTest {

    private val progressRepository = FakeWatchProgressRepository()
    private val watchesRepository = FakeVideoWatchesRepository()
    private val outbox = FakePendingMutationOutbox()
    private val syncScheduler = FakePendingMutationSyncScheduler()

    private val handler = EpisodeWatchedHandler(
        markEpisodeWatchedLocally = MarkEpisodeWatchedLocallyUseCase(progressRepository),
        clearEpisodeWatchProgress = ClearEpisodeWatchProgressUseCase(progressRepository),
        saveVideoWatchProgress = SaveVideoWatchProgressUseCase(watchesRepository),
        removeWatchedVideos = RemoveWatchedVideosUseCase(watchesRepository, NoopNotifier),
        pendingMutationOutbox = outbox,
        pendingMutationSyncScheduler = syncScheduler,
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
        val succeeded = handler.markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = true,
        )

        assertTrue(succeeded)
        val saved = progressRepository.saved.single()
        // Озвучка по умолчанию для тайтла, а не первая в списке.
        assertEquals(2, saved.videoId)
        assertEquals(1_500_000L, saved.durationMs)
        assertEquals(saved.durationMs, saved.positionMs)

        val request = watchesRepository.marked.single()
        assertEquals(2, request.videoId)
        assertEquals(1_500, request.durationSeconds)
        assertEquals(1_490, request.timeSeconds)
        // Фактически серия не просматривалась, поэтому spent_time расти не должен.
        assertTrue(request.times.isEmpty())
    }

    @Test
    fun `mark prefers the video that already has progress`() = runTest {
        handler.markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = progress(videoId = 1, durationMs = 1_320_000L),
            meta = meta,
            isSignedIn = true,
        )

        val saved = progressRepository.saved.single()
        assertEquals(1, saved.videoId)
        // Длительность известной записи важнее длительности из списка серий.
        assertEquals(1_320_000L, saved.durationMs)
    }

    @Test
    fun `mark falls back to a typical duration when the api reports none`() = runTest {
        handler.markWatched(
            animeId = 7,
            episode = "3",
            videos = listOf(video(id = 5, dubbing = BEST, durationSeconds = null)),
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = true,
        )

        assertEquals(24 * 60 * 1_000L, progressRepository.saved.single().durationMs)
    }

    @Test
    fun `mark keeps the local record when the server call fails`() = runTest {
        watchesRepository.failMark = true

        val succeeded = handler.markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = true,
        )

        assertFalse(succeeded)
        assertEquals(1, progressRepository.saved.size)
        // Ошибка не сетевая — повторять нечего, очередь остаётся пустой.
        assertTrue(outbox.enqueued.isEmpty())
        assertEquals(0, syncScheduler.flushes)
    }

    @Test
    fun `mark queues the server call when the device is offline`() = runTest {
        watchesRepository.markError = IOException("offline")

        val succeeded = handler.markWatched(
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
        assertEquals(1, progressRepository.saved.size)
        val queued = outbox.enqueued.single()
        assertEquals(PendingMutationTypes.MARK_WATCHED, queued.type)
        assertEquals(MarkWatchedPayload(2, 1_490, 1_500), MarkWatchedPayload.decode(queued.payload))
        assertEquals(1, syncScheduler.flushes)
    }

    @Test
    fun `unmark queues the server call when the device is offline`() = runTest {
        watchesRepository.removeError = IOException("offline")

        val succeeded = handler.unmarkWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            isSignedIn = true,
        )

        assertTrue(succeeded)
        assertEquals(7 to "3", progressRepository.deleted.single())
        val queued = outbox.enqueued.single()
        assertEquals(PendingMutationTypes.REMOVE_WATCHED, queued.type)
        assertEquals(
            RemoveWatchedPayload(listOf(1, 2)),
            RemoveWatchedPayload.decode(queued.payload),
        )
        assertEquals(1, syncScheduler.flushes)
    }

    @Test
    fun `mark stays local when signed out`() = runTest {
        val succeeded = handler.markWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            bestDubbing = BEST,
            existing = null,
            meta = meta,
            isSignedIn = false,
        )

        assertTrue(succeeded)
        assertEquals(1, progressRepository.saved.size)
        assertTrue(watchesRepository.marked.isEmpty())
    }

    @Test
    fun `unmark clears the local record and every dubbing on the server`() = runTest {
        val succeeded = handler.unmarkWatched(
            animeId = 7,
            episode = "3",
            videos = videos,
            isSignedIn = true,
        )

        assertTrue(succeeded)
        assertEquals(7 to "3", progressRepository.deleted.single())
        assertEquals(listOf(1, 2), watchesRepository.removed.single())
    }

    @Test
    fun `unmark stays local when signed out`() = runTest {
        handler.unmarkWatched(animeId = 7, episode = "3", videos = videos, isSignedIn = false)

        assertEquals(7 to "3", progressRepository.deleted.single())
        assertTrue(watchesRepository.removed.isEmpty())
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

    private companion object {
        const val BEST = "AniLibria"
        const val OTHER = "AniDub"
    }
}

private class FakeWatchProgressRepository : WatchProgressRepository {
    val saved = mutableListOf<AnimeWatchProgress>()
    val deleted = mutableListOf<Pair<Int, String>>()

    override suspend fun get(animeId: Int, episode: String): AnimeWatchProgress? = null

    override suspend fun save(
        animeId: Int,
        episode: String,
        videoId: Int,
        episodeUrl: String,
        positionMs: Long,
        durationMs: Long,
        updatedAt: Long,
        animeTitle: String,
        posterUrl: String,
        playerName: String,
        dubbing: String,
        screenshotUrl: String,
    ) {
        saved += AnimeWatchProgress(
            animeId = animeId,
            episode = episode,
            videoId = videoId,
            episodeUrl = episodeUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = updatedAt,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )
    }

    override suspend fun saveContinueTarget(
        animeId: Int,
        episode: String,
        videoId: Int,
        episodeUrl: String,
        updatedAt: Long,
        animeTitle: String,
        posterUrl: String,
        playerName: String,
        dubbing: String,
        screenshotUrl: String,
    ) = Unit

    override suspend fun delete(animeId: Int, episode: String) {
        deleted += animeId to episode
    }

    override suspend fun suppressContinueWatchingDisplay(animeId: Int, suppressedAt: Long) = Unit

    override suspend fun allMeaningfulVideoProgress(): List<AnimeWatchProgress> = emptyList()
}

private class FakeVideoWatchesRepository : VideoWatchesRepository {
    data class MarkRequest(
        val videoId: Int,
        val timeSeconds: Int,
        val durationSeconds: Int,
        val times: List<Int>,
    )

    val marked = mutableListOf<MarkRequest>()
    val removed = mutableListOf<List<Int>>()
    var failMark = false
    var markError: Throwable? = null
    var removeError: Throwable? = null

    override suspend fun markWatched(
        videoId: Int,
        timeSeconds: Int,
        durationSeconds: Int,
        times: List<Int>,
    ): Boolean {
        marked += MarkRequest(videoId, timeSeconds, durationSeconds, times)
        markError?.let { throw it }
        if (failMark) throw IllegalStateException("network")
        return true
    }

    override suspend fun syncWatched(videos: List<VideoWatchSyncItem>): Boolean = true

    override suspend fun removeWatched(videoIds: List<Int>): Boolean {
        removed += videoIds
        removeError?.let { throw it }
        return true
    }
}

private class FakePendingMutationOutbox : PendingMutationOutbox {
    data class Enqueued(val type: String, val payload: String)

    val enqueued = mutableListOf<Enqueued>()

    override suspend fun enqueue(type: String, payloadJson: String) {
        enqueued += Enqueued(type, payloadJson)
    }

    override suspend fun pending(): List<PendingMutationEntry> = emptyList()

    override suspend fun remove(id: Long) = Unit

    override suspend fun recordAttemptFailure(id: Long) = Unit

    override fun observeCount(): Flow<Int> = flowOf(enqueued.size)
}

private class FakePendingMutationSyncScheduler : PendingMutationSyncScheduler {
    var flushes = 0

    override fun scheduleFlush() {
        flushes++
    }
}

private object NoopNotifier : AccountMutationErrorNotifier {
    override val events: SharedFlow<AccountMutationErrorEvent> = MutableSharedFlow()
    override suspend fun notify(event: AccountMutationErrorEvent) = Unit
}
