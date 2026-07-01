package su.afk.yummy.tv.feature.player.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDestination(
    val iframeUrl: String,
    val animeTitle: String,
    val episode: String,
    val playerName: String,
    val dubbing: String = "",
    val animeId: Int = 0,
    val posterUrl: String = "",
    val selectedVideoId: Int = 0,
    val selectedPlayerId: Int? = null,
    val selectedScreenshotUrl: String = "",
    val resumeFromMs: Long = 0L,
    val downloadId: Long = 0L,
) : NavKey
