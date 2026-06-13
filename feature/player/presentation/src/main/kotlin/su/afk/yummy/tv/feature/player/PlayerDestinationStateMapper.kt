package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import javax.inject.Inject

internal class PlayerDestinationStateMapper @Inject constructor() {
    fun toState(
        dest: PlayerDestination,
        autoSkipOpeningsEndings: Boolean = false,
    ): PlayerState.State = PlayerState.State(
        iframeUrl = dest.iframeUrl,
        animeTitle = dest.animeTitle,
        episode = dest.episode,
        playerName = dest.playerName,
        dubbing = dest.dubbing,
        episodeUrls = dest.episodeUrls,
        episodeNumbers = dest.episodeNumbers,
        episodeVideoIds = dest.episodeVideoIds,
        screenshotUrls = dest.screenshotUrls,
        animeId = dest.animeId,
        posterUrl = dest.posterUrl,
        allDubbingNames = dest.allDubbingNames,
        allDubbingEpisodeUrls = dest.allDubbingEpisodeUrls,
        allDubbingEpisodeNumbers = dest.allDubbingEpisodeNumbers,
        allDubbingEpisodeVideoIds = dest.allDubbingEpisodeVideoIds,
        allDubbingViews = dest.allDubbingViews,
        allBalancerNames = dest.allBalancerNames,
        allBalancerDubbingNames = dest.allBalancerDubbingNames,
        allBalancerEpisodeUrls = dest.allBalancerEpisodeUrls,
        allBalancerEpisodeNumbers = dest.allBalancerEpisodeNumbers,
        allBalancerEpisodeVideoIds = dest.allBalancerEpisodeVideoIds,
        allBalancerDubbingViews = dest.allBalancerDubbingViews,
        episodeSkips = dest.episodeSkips,
        allDubbingEpisodeSkips = dest.allDubbingEpisodeSkips,
        allBalancerEpisodeSkips = dest.allBalancerEpisodeSkips,
        balancerIndex = dest.currentBalancerIndex,
        dubbingIndex = dest.currentDubbingIndex,
        episodeIndex = dest.currentEpisodeIndex,
        autoSkipOpeningsEndings = autoSkipOpeningsEndings,
    )
}
