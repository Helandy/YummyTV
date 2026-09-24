package su.afk.yummy.tv.data.player.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.player.mapper.toPlayerSourceVideo
import su.afk.yummy.tv.data.player.mapper.toScreenshotByEpisode
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
        val details = async {
            runSuspendCatching { animeRepository.getAnimeDetails(animeId) }
                .getOrElse { error ->
                    null
                }
        }

        PlayerSourceData(
            videos = videos.await().map { it.toPlayerSourceVideo() },
            screenshotByEpisode = details.await()
                ?.screenshots
                .orEmpty()
                .toScreenshotByEpisode(),
        )
    }
}
