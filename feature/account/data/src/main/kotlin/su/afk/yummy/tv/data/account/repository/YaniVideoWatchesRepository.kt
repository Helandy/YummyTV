package su.afk.yummy.tv.data.account.repository

import su.afk.yummy.tv.core.utils.coroutines.AppDispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.mapper.toVideoItemDto
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository

class YaniVideoWatchesRepository(
    private val api: YaniAccountApi,
    private val dispatchers: AppDispatchers,
) : VideoWatchesRepository {

    override suspend fun markWatched(
        videoId: Int,
        timeSeconds: Int,
        durationSeconds: Int,
        times: List<Int>
    ): Boolean =
        withContext(dispatchers.io) {
            api.markWatched(videoId, timeSeconds, durationSeconds, times)
        }

    override suspend fun syncWatched(videos: List<VideoWatchSyncItem>): Boolean =
        withContext(dispatchers.io) {
            val items = videos
                .filter { it.videoId > 0 && it.timeSeconds > 0 && it.dateSeconds > 0 }
                .distinctBy { it.videoId }
                .map { it.toVideoItemDto() }
            if (items.isEmpty()) true else api.syncWatched(items)
        }

    override suspend fun removeWatched(videoIds: List<Int>): Boolean =
        withContext(dispatchers.io) {
            val ids = videoIds.filter { it > 0 }.distinct()
            if (ids.isEmpty()) true else api.removeWatched(ids)
        }
}
