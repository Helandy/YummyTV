package su.afk.yummy.tv.domain.account.repository

interface VideoWatchesRepository {
    suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean
    suspend fun removeWatched(videoId: Int): Boolean
}
