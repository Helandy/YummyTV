// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.data.player.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.data.player.mapper.toPlayerSourceVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.player.model.PlayerSourceData
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import javax.inject.Inject

class DefaultPlayerSourceRepository @Inject constructor(
    private val animeRepository: AnimeRepository,
) : PlayerSourceRepository {

    override suspend fun getSources(
        animeId: Int,
        forceRefreshVideos: Boolean,
    ): PlayerSourceData = coroutineScope {
        val videos = async {
            if (forceRefreshVideos) {
                animeRepository.refreshAnimeVideos(animeId)
            } else {
                animeRepository.getAnimeVideos(animeId)
            }
        }
        PlayerSourceData(
            videos = videos.await().map { it.toPlayerSourceVideo() },
        )
    }
}
