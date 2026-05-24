package su.afk.yummy.tv.feature.player.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.feature.player.PlayerSkips

@Serializable
data class PlayerDestination(
    val iframeUrl: String,
    val animeTitle: String,
    val episode: String,
    val playerName: String,
    val dubbing: String = "",
    val episodeUrls: List<String> = emptyList(),
    val episodeNumbers: List<String> = emptyList(),
    val episodeVideoIds: List<Int> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val screenshotUrls: List<String> = emptyList(),
    val animeId: Int = 0,
    val posterUrl: String = "",
    val allDubbingNames: List<String> = emptyList(),
    val currentDubbingIndex: Int = 0,
    val allDubbingEpisodeUrls: List<List<String>> = emptyList(),
    val allDubbingEpisodeNumbers: List<List<String>> = emptyList(),
    val allDubbingEpisodeVideoIds: List<List<Int>> = emptyList(),
    val allBalancerNames: List<String> = emptyList(),
    val currentBalancerIndex: Int = 0,
    val allBalancerDubbingNames: List<List<String>> = emptyList(),
    val allBalancerEpisodeUrls: List<List<List<String>>> = emptyList(),
    val allBalancerEpisodeNumbers: List<List<List<String>>> = emptyList(),
    val allBalancerEpisodeVideoIds: List<List<List<Int>>> = emptyList(),
    val episodeSkips: List<PlayerSkips> = emptyList(),
    val allDubbingEpisodeSkips: List<List<PlayerSkips>> = emptyList(),
    val allBalancerEpisodeSkips: List<List<List<PlayerSkips>>> = emptyList(),
) : NavKey
