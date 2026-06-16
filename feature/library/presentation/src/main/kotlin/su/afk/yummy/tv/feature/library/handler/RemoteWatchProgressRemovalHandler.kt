package su.afk.yummy.tv.feature.library.handler

import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import javax.inject.Inject

class RemoteWatchProgressRemovalHandler @Inject constructor(
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val removeWatchedVideos: RemoveWatchedVideosUseCase,
) {
    suspend fun removeAnimeWatchProgress(
        animeId: Int,
        knownVideoIds: List<Int>,
    ) {
        val animeVideoIds = runCatching { getAnimeVideos(animeId) }
            .getOrDefault(emptyList())
            .mapNotNull { it.id.takeIf { id -> id > 0 } }
        val remoteVideoIds = (knownVideoIds + animeVideoIds).distinct()
        runCatching { removeWatchedVideos(remoteVideoIds) }
    }
}
