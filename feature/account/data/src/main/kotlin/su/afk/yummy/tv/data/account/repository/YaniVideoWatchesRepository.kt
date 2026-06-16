package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.dto.YaniPostVideoItemDto
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository

class YaniVideoWatchesRepository(
    private val api: YaniAccountApi,
) : VideoWatchesRepository {

    override suspend fun markWatched(
        videoId: Int,
        timeSeconds: Int,
        durationSeconds: Int
    ): Boolean =
        withContext(Dispatchers.IO) {
            api.markWatched(videoId, timeSeconds, durationSeconds)
        }

    override suspend fun syncWatched(videos: List<VideoWatchSyncItem>): Boolean =
        withContext(Dispatchers.IO) {
            val items = videos
                .filter { it.videoId > 0 && it.timeSeconds > 0 && it.dateSeconds > 0 }
                .distinctBy { it.videoId }
                .map {
                    YaniPostVideoItemDto(
                        videoId = it.videoId,
                        time = it.timeSeconds,
                        date = it.dateSeconds,
                    )
                }
            if (items.isEmpty()) true else api.syncWatched(items)
        }

    override suspend fun removeWatched(videoIds: List<Int>): Boolean =
        withContext(Dispatchers.IO) {
            val ids = videoIds.filter { it > 0 }.distinct()
            if (ids.isEmpty()) true else api.removeWatched(ids)
        }
}
