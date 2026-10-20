package su.afk.yummy.tv.feature.player.mapper

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.common.player.playerDisplayName
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSourceOption
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeUnavailable
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.globalDubbingEpisodeNumbers
import su.afk.yummy.tv.feature.player.utils.globalDubbingViews
import su.afk.yummy.tv.feature.player.utils.nextEpisodeSourceOptions
import su.afk.yummy.tv.feature.player.utils.normalizedSourceSelection

fun PlayerState.State.toPlayerNextEpisodeUnavailable(): PlayerNextEpisodeUnavailable {
    val currentBalancerIndex = normalizedSourceSelection(this).balancerIndex
    val currentDubbing = activeDubbingName(this)
    // Одна строка на озвучку, как в меню озвучек; порядок групп — по лучшей паре.
    val options = nextEpisodeSourceOptions(this)
        .groupBy { source -> sourceGraph.balancers[source.balancerIndex].dubbings[source.dubbingIndex].name }
        .map { (dubbing, sources) ->
            val source = sources.firstOrNull { it.balancerIndex == currentBalancerIndex } ?: sources.first()
            PlayerNextEpisodeSourceOption(
                selection = PlayerSourceSelection(
                    balancerIndex = source.balancerIndex,
                    dubbingIndex = source.dubbingIndex,
                    episodeIndex = source.episodeIndex,
                ),
                dubbing = dubbing,
                views = globalDubbingViews(this, dubbing),
                episodeCount = globalDubbingEpisodeNumbers(this, dubbing).size,
                sourceNames = sources.joinToString(" • ") {
                    sourceGraph.balancers[it.balancerIndex].name.playerDisplayName()
                },
            )
        }
    return PlayerNextEpisodeUnavailable(
        episodeNumber = activeEpisode(this).toIntOrNull()?.plus(1),
        currentDubbing = currentDubbing,
        inCurrentDubbing = options.firstOrNull()?.dubbing == currentDubbing,
        options = options.toImmutableList(),
        canSubscribe = options.isEmpty() && finalEpisodeAction == PlayerFinalEpisodeAction.ManageSubscriptions,
    )
}
