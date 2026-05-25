package su.afk.yummy.tv.domain.account

interface VideoWatchesRepository {
    suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean
    suspend fun removeWatched(videoId: Int): Boolean
    suspend fun syncWatched(states: List<RemoteWatchState>): Boolean
}
