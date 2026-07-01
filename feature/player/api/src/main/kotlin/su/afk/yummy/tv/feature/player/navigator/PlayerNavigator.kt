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
        selectedPlayerId: Int?,
        selectedScreenshotUrl: String,
        animeId: Int,
        posterUrl: String,
        resumeFromMs: Long,
    ): NavKey = PlayerDestination(
        iframeUrl = iframeUrl,
        animeTitle = animeTitle,
        episode = episode,
        playerName = playerName,
        dubbing = dubbing,
        animeId = animeId,
        posterUrl = posterUrl,
        selectedVideoId = selectedVideoId,
        selectedPlayerId = selectedPlayerId,
        selectedScreenshotUrl = selectedScreenshotUrl,
        resumeFromMs = resumeFromMs,
    )

    override fun getDownloadedPlayerDest(downloadId: Long): NavKey =
        PlayerDestination(
            iframeUrl = "",
            animeTitle = "",
            episode = "",
            playerName = "",
            downloadId = downloadId,
        )
}
