package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem

interface VideoWatchesRepository {
    suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean
    suspend fun syncWatched(videos: List<VideoWatchSyncItem>): Boolean
    suspend fun removeWatched(videoIds: List<Int>): Boolean
}
