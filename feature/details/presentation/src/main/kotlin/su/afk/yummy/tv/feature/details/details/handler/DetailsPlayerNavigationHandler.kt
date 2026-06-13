package su.afk.yummy.tv.feature.details.details.handler

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.resolveDetailsPlayerSelection
import su.afk.yummy.tv.feature.details.utils.toPlayerVideoSource
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.getPlayerDest
import javax.inject.Inject

internal class DetailsPlayerNavigationHandler @Inject constructor(
    private val playerNavigator: IPlayerNavigator,
) {
    fun selectPlayer(
        video: AnimeVideo,
        allVideos: List<AnimeVideo>,
        preferredPlayer: PreferredPlayer,
    ): DetailsPlayerSelection =
        resolveDetailsPlayerSelection(
            video = video,
            allVideos = allVideos,
            preferredPlayer = preferredPlayer,
        )

    fun getPlayerDestination(
        video: AnimeVideo,
        allVideos: List<AnimeVideo>,
        animeTitle: String,
        animeId: Int,
        posterUrl: String,
        screenshotByEpisode: Map<String, String>,
    ): NavKey =
        getPlayerDestination(
            video = video.toPlayerVideoSource(),
            allVideos = allVideos,
            animeTitle = animeTitle,
            animeId = animeId,
            posterUrl = posterUrl,
            screenshotByEpisode = screenshotByEpisode,
        )

    fun getPlayerDestination(
        video: PlayerVideoSource,
        allVideos: List<AnimeVideo>,
        animeTitle: String,
        animeId: Int,
        posterUrl: String,
        screenshotByEpisode: Map<String, String>,
    ): NavKey =
        playerNavigator.getPlayerDest(
            video = video,
            allVideos = allVideos.map { it.toPlayerVideoSource() },
            animeTitle = animeTitle,
            animeId = animeId,
            posterUrl = posterUrl,
            screenshotByEpisode = screenshotByEpisode,
        )
}
