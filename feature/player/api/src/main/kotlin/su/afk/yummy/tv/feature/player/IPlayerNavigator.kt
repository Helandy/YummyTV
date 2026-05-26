package su.afk.yummy.tv.feature.player

import androidx.navigation3.runtime.NavKey

interface IPlayerNavigator {
    fun getPlayerDest(
        iframeUrl: String,
        animeTitle: String,
        episode: String,
        playerName: String,
        dubbing: String = "",
        episodeUrls: List<String> = emptyList(),
        episodeNumbers: List<String> = emptyList(),
        episodeVideoIds: List<Int> = emptyList(),
        currentEpisodeIndex: Int = 0,
        screenshotUrls: List<String> = emptyList(),
        animeId: Int = 0,
        posterUrl: String = "",
        allDubbingNames: List<String> = emptyList(),
        currentDubbingIndex: Int = 0,
        allDubbingEpisodeUrls: List<List<String>> = emptyList(),
        allDubbingEpisodeNumbers: List<List<String>> = emptyList(),
        allDubbingEpisodeVideoIds: List<List<Int>> = emptyList(),
        allDubbingViews: List<Int> = emptyList(),
        allBalancerNames: List<String> = emptyList(),
        currentBalancerIndex: Int = 0,
        allBalancerDubbingNames: List<List<String>> = emptyList(),
        allBalancerEpisodeUrls: List<List<List<String>>> = emptyList(),
        allBalancerEpisodeNumbers: List<List<List<String>>> = emptyList(),
        allBalancerEpisodeVideoIds: List<List<List<Int>>> = emptyList(),
        allBalancerDubbingViews: List<List<Int>> = emptyList(),
        episodeSkips: List<PlayerSkips> = emptyList(),
        allDubbingEpisodeSkips: List<List<PlayerSkips>> = emptyList(),
        allBalancerEpisodeSkips: List<List<List<PlayerSkips>>> = emptyList(),
    ): NavKey
}
