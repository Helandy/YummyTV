package su.afk.yummy.tv.data.account.repository

import su.afk.yummy.tv.data.account.dto.YaniPostVideoItemDto
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.RemoteWatchState
import su.afk.yummy.tv.domain.account.VideoWatchesRepository

class YaniVideoWatchesRepository(
    private val api: YaniAccountApi,
) : VideoWatchesRepository {

    override suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        api.markWatched(videoId, timeSeconds, durationSeconds)

    override suspend fun removeWatched(videoId: Int): Boolean =
        api.removeWatched(videoId)

    override suspend fun syncWatched(states: List<RemoteWatchState>): Boolean =
        api.syncWatched(states.map { YaniPostVideoItemDto(it.videoId, it.timeSeconds) })
}
