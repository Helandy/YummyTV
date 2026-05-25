package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Marks a video as watched with the latest playback timing. */
class MarkVideoWatchedUseCase(private val repository: VideoWatchesRepository) {
    suspend operator fun invoke(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        repository.markWatched(videoId, timeSeconds, durationSeconds)
}
