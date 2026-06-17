package su.afk.yummy.tv.data.player.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.player.model.PlayerSourceData
import su.afk.yummy.tv.domain.player.model.PlayerSourceSkipSegment
import su.afk.yummy.tv.domain.player.model.PlayerSourceSkips
import su.afk.yummy.tv.domain.player.model.PlayerSourceVideo
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
            runCatching { animeRepository.getAnimeDetails(animeId) }
                .getOrElse { error ->
                    if (error is CancellationException) throw error
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

private fun AnimeVideo.toPlayerSourceVideo(): PlayerSourceVideo =
    PlayerSourceVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = playerId,
        iframeUrl = iframeUrl,
        views = views,
        skips = skips.toPlayerSourceSkips(),
    )

private fun AnimeVideoSkips.toPlayerSourceSkips(): PlayerSourceSkips =
    PlayerSourceSkips(
        opening = opening.toPlayerSourceSkipSegment(),
        ending = ending.toPlayerSourceSkipSegment(),
    )

private fun AnimeVideoSkipSegment?.toPlayerSourceSkipSegment(): PlayerSourceSkipSegment? =
    this?.let { PlayerSourceSkipSegment(startMs = it.startMs, endMs = it.endMs) }

private fun List<AnimeScreenshot>.toScreenshotByEpisode(): Map<String, String> =
    mapNotNull { screenshot ->
        val episode = screenshot.episode ?: return@mapNotNull null
        val url = screenshot.small ?: screenshot.full ?: return@mapNotNull null
        episode to url
    }.toMap()
