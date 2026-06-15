package su.afk.yummy.tv.feature.player.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

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
    val selectedScreenshotUrl: String = "",
) : NavKey, AnalyticsDestination {
    override val screenName: String = "player"
    override val screenParams: Map<String, String>
        get() = if (animeId > 0) mapOf("anime_id" to animeId.toString()) else emptyMap()
}
