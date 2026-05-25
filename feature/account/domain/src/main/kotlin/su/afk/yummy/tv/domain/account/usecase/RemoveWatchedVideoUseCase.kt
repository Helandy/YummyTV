package su.afk.yummy.tv.domain.account

/** Removes the watched marker for a remote video. */
class RemoveWatchedVideoUseCase(private val repository: VideoWatchesRepository) {
    suspend operator fun invoke(videoId: Int): Boolean = repository.removeWatched(videoId)
}
