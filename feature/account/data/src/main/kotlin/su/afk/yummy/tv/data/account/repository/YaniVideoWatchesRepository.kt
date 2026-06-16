package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository

class YaniVideoWatchesRepository(
    private val api: YaniAccountApi,
) : VideoWatchesRepository {

    override suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        withContext(Dispatchers.IO) {
            api.markWatched(videoId, timeSeconds, durationSeconds)
        }

    override suspend fun removeWatched(videoId: Int): Boolean =
        withContext(Dispatchers.IO) {
            api.removeWatched(videoId)
        }
}
