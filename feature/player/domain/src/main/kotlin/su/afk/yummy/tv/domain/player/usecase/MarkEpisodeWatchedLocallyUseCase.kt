package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import javax.inject.Inject

/**
 * Помечает серию просмотренной локально, без фактического воспроизведения.
 *
 * Отдельного флага "просмотрено" в хранилище нет: статус считается из позиции и длительности
 * (см. `isWatchedProgress`), поэтому отметка — это запись прогресса с позицией, равной
 * длительности. Ровно так же нормализует запись плеер, когда серия досмотрена до конца.
 */
class MarkEpisodeWatchedLocallyUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        animeId: Int,
        episode: String,
        videoId: Int,
        episodeUrl: String,
        durationMs: Long,
        animeTitle: String,
        posterUrl: String,
        playerName: String,
        dubbing: String,
        screenshotUrl: String,
    ) {
        if (animeId <= 0 || episode.isBlank() || durationMs <= 0L) return
        repository.save(
            animeId = animeId,
            episode = episode,
            videoId = videoId,
            episodeUrl = episodeUrl,
            positionMs = durationMs,
            durationMs = durationMs,
            updatedAt = clock.nowMillis(),
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )
    }
}
