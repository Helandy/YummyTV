package su.afk.yummy.tv.feature.player.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import javax.inject.Inject

class PlayerNavigator @Inject constructor() : IPlayerNavigator {
    override fun getPlayerDest(
        iframeUrl: String,
        animeTitle: String,
        episode: String,
        playerName: String,
        dubbing: String,
        episodeUrls: List<String>,
        episodeNumbers: List<String>,
        episodeVideoIds: List<Int>,
        currentEpisodeIndex: Int,
        screenshotUrls: List<String>,
        animeId: Int,
        posterUrl: String,
        allDubbingNames: List<String>,
        currentDubbingIndex: Int,
        allDubbingEpisodeUrls: List<List<String>>,
        allDubbingEpisodeNumbers: List<List<String>>,
        allDubbingEpisodeVideoIds: List<List<Int>>,
        allBalancerNames: List<String>,
        currentBalancerIndex: Int,
        allBalancerDubbingNames: List<List<String>>,
        allBalancerEpisodeUrls: List<List<List<String>>>,
        allBalancerEpisodeNumbers: List<List<List<String>>>,
        allBalancerEpisodeVideoIds: List<List<List<Int>>>,
        episodeSkips: List<su.afk.yummy.tv.feature.player.PlayerSkips>,
        allDubbingEpisodeSkips: List<List<su.afk.yummy.tv.feature.player.PlayerSkips>>,
        allBalancerEpisodeSkips: List<List<List<su.afk.yummy.tv.feature.player.PlayerSkips>>>,
    ): NavKey = PlayerDestination(
        iframeUrl, animeTitle, episode, playerName, dubbing,
        episodeUrls, episodeNumbers, episodeVideoIds, currentEpisodeIndex, screenshotUrls, animeId, posterUrl,
        allDubbingNames, currentDubbingIndex, allDubbingEpisodeUrls, allDubbingEpisodeNumbers, allDubbingEpisodeVideoIds,
        allBalancerNames, currentBalancerIndex, allBalancerDubbingNames,
        allBalancerEpisodeUrls, allBalancerEpisodeNumbers, allBalancerEpisodeVideoIds,
        episodeSkips, allDubbingEpisodeSkips, allBalancerEpisodeSkips,
    )
}
