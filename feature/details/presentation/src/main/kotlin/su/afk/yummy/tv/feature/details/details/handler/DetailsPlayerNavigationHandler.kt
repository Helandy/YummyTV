package su.afk.yummy.tv.feature.details.details.handler

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.feature.details.details.model.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.resolveDetailsPlayerSelection
import su.afk.yummy.tv.feature.details.mapper.toPlayerVideoSource
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.getPlayerDest
import javax.inject.Inject

internal class DetailsPlayerNavigationHandler @Inject constructor(
    private val playerNavigator: IPlayerNavigator,
    private val extraVideoRepositories: Set<@JvmSuppressWildcards ExtraVideoRepository> = emptySet(),
) {
    /** Источники-расширения могут запросить подтверждение до перехода в плеер; без них всегда `true`. */
    suspend fun confirmOpen(video: AnimeVideo): Boolean =
        extraVideoRepositories.all { it.confirmOpen(video) }

    fun selectPlayer(
        video: AnimeVideo,
        allVideos: List<AnimeVideo>,
        rememberedAnime4kDubbing: String? = null,
    ): DetailsPlayerSelection =
        resolveDetailsPlayerSelection(
            video = video,
            allVideos = allVideos,
            rememberedAnime4kDubbing = rememberedAnime4kDubbing,
        )

    fun getPlayerDestination(
        video: AnimeVideo,
        animeTitle: String,
        animeId: Int,
        posterUrl: String,
        screenshotByEpisode: Map<String, String>,
        resumeFromMs: Long = 0L,
    ): NavKey =
        getPlayerDestination(
            video = video.toPlayerVideoSource(),
            animeTitle = animeTitle,
            animeId = animeId,
            posterUrl = posterUrl,
            screenshotByEpisode = screenshotByEpisode,
            resumeFromMs = resumeFromMs,
        )

    fun getPlayerDestination(
        video: PlayerVideoSource,
        animeTitle: String,
        animeId: Int,
        posterUrl: String,
        screenshotByEpisode: Map<String, String>,
        resumeFromMs: Long = 0L,
    ): NavKey =
        playerNavigator.getPlayerDest(
            video = video,
            animeTitle = animeTitle,
            animeId = animeId,
            posterUrl = posterUrl,
            screenshotByEpisode = screenshotByEpisode,
            resumeFromMs = resumeFromMs,
        )

    fun getDownloadedPlayerDestination(downloadId: Long): NavKey =
        playerNavigator.getDownloadedPlayerDest(downloadId)
}
