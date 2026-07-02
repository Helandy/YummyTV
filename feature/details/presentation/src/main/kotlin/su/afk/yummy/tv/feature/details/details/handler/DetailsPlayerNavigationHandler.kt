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
