package su.afk.yummy.tv.feature.player.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerSourceGraph

@Serializable
data class PlayerDestination(
    @Deprecated("Use sourceGraph instead.")
    val iframeUrl: String,
    val animeTitle: String,
    @Deprecated("Use sourceGraph instead.")
    val episode: String,
    @Deprecated("Use sourceGraph instead.")
    val playerName: String,
    @Deprecated("Use sourceGraph instead.")
    val dubbing: String = "",
    @Deprecated("Use sourceGraph instead.")
    val episodeUrls: List<String> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val episodeNumbers: List<String> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val episodeVideoIds: List<Int> = emptyList(),
    @Deprecated("Use sourceGraph.selection instead.")
    val currentEpisodeIndex: Int = 0,
    @Deprecated("Use sourceGraph instead.")
    val screenshotUrls: List<String> = emptyList(),
    val animeId: Int = 0,
    val posterUrl: String = "",
    val sourceGraph: PlayerSourceGraph = PlayerSourceGraph(),
    @Deprecated("Use sourceGraph instead.")
    val allDubbingNames: List<String> = emptyList(),
    @Deprecated("Use sourceGraph.selection instead.")
    val currentDubbingIndex: Int = 0,
    @Deprecated("Use sourceGraph instead.")
    val allDubbingEpisodeUrls: List<List<String>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allDubbingEpisodeNumbers: List<List<String>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allDubbingEpisodeVideoIds: List<List<Int>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allDubbingViews: List<Int> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allBalancerNames: List<String> = emptyList(),
    @Deprecated("Use sourceGraph.selection instead.")
    val currentBalancerIndex: Int = 0,
    @Deprecated("Use sourceGraph instead.")
    val allBalancerDubbingNames: List<List<String>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allBalancerEpisodeUrls: List<List<List<String>>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allBalancerEpisodeNumbers: List<List<List<String>>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allBalancerEpisodeVideoIds: List<List<List<Int>>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allBalancerDubbingViews: List<List<Int>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val episodeSkips: List<PlayerSkips> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allDubbingEpisodeSkips: List<List<PlayerSkips>> = emptyList(),
    @Deprecated("Use sourceGraph instead.")
    val allBalancerEpisodeSkips: List<List<List<PlayerSkips>>> = emptyList(),
) : NavKey
