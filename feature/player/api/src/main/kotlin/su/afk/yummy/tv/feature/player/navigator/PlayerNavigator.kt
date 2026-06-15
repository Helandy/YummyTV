package su.afk.yummy.tv.feature.player.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.player.IPlayerNavigator

class PlayerNavigator : IPlayerNavigator {
    override fun getPlayerDest(
        iframeUrl: String,
        animeTitle: String,
        episode: String,
        playerName: String,
        dubbing: String,
        selectedVideoId: Int,
        selectedScreenshotUrl: String,
        animeId: Int,
        posterUrl: String,
    ): NavKey = PlayerDestination(
        iframeUrl = iframeUrl,
        animeTitle = animeTitle,
        episode = episode,
        playerName = playerName,
        dubbing = dubbing,
        animeId = animeId,
        posterUrl = posterUrl,
        selectedVideoId = selectedVideoId,
        selectedScreenshotUrl = selectedScreenshotUrl,
    )
}
