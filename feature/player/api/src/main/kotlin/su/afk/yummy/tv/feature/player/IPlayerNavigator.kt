package su.afk.yummy.tv.feature.player

import androidx.navigation3.runtime.NavKey

interface IPlayerNavigator {
    fun getPlayerDest(
        iframeUrl: String,
        animeTitle: String,
        episode: String,
        playerName: String,
        dubbing: String = "",
        selectedVideoId: Int = 0,
        selectedPlayerId: Int? = null,
        selectedScreenshotUrl: String = "",
        animeId: Int = 0,
        posterUrl: String = "",
        resumeFromMs: Long = 0L,
    ): NavKey
}
