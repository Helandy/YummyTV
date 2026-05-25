package su.afk.yummy.tv.domain.account

/** Marks a video as watched with the latest playback timing. */
class MarkVideoWatchedUseCase(private val repository: VideoWatchesRepository) {
    suspend operator fun invoke(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        repository.markWatched(videoId, timeSeconds, durationSeconds)
}
