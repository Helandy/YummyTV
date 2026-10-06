package su.afk.yummy.tv.feature.player.mapper

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.utils.activeBalancer
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbing
import su.afk.yummy.tv.feature.player.utils.activeDubbingEpisodes
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeEpisodeSource
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.activeQuality
import su.afk.yummy.tv.feature.player.utils.activeScreenshotUrl
import su.afk.yummy.tv.feature.player.utils.activeVideoId
import su.afk.yummy.tv.feature.player.utils.displayedBalancerIndices
import su.afk.yummy.tv.feature.player.utils.globalDubbingEpisodeNumbers
import su.afk.yummy.tv.feature.player.utils.globalDubbingNames
import su.afk.yummy.tv.feature.player.utils.globalDubbingSourceNames
import su.afk.yummy.tv.feature.player.utils.globalDubbingViews
import su.afk.yummy.tv.feature.player.utils.isBalancerAvailableForEpisode
import su.afk.yummy.tv.feature.player.utils.isDubbingAvailableForEpisode
import su.afk.yummy.tv.feature.player.utils.isFinalAvailableEpisode
import su.afk.yummy.tv.feature.player.utils.nextEpisodeOtherDubbingSource
import su.afk.yummy.tv.feature.player.utils.normalizedSourceSelection
import su.afk.yummy.tv.feature.player.utils.streamQualities

fun PlayerState.State.toPlayerPlaybackUiState(
    playerNamePrefix: String,
): PlayerPlaybackUiState {
    val selection = normalizedSourceSelection(this)
    val activeDubbing = activeDubbing(this)
    val activeDubbingName = activeDubbingName(this)
    val activeEpisodes = activeDubbingEpisodes(this)
    val globalDubbingNames = globalDubbingNames(this)
    val displayedDubbingNames = globalDubbingNames.ifEmpty {
        activeBalancer(this)?.dubbings?.map { it.name }.orEmpty()
    }
    val globalEpisodeNumbers = displayedDubbingNames.map { name ->
        globalDubbingEpisodeNumbers(this, name).ifEmpty {
            activeDubbing?.episodes?.map { it.number }.orEmpty()
        }
    }
    val globalViews = displayedDubbingNames.map { name ->
        globalDubbingViews(this, name).takeIf { it > 0 } ?: activeDubbing?.views ?: 0
    }
    val globalSourceNames = displayedDubbingNames.map { name ->
        globalDubbingSourceNames(this, name, playerNamePrefix)
    }
    val availableBalancerIndices = displayedBalancerIndices(this)
    val activeEpisode = activeEpisode(this)
    val dubbingAvailability = displayedDubbingNames.map { name ->
        isDubbingAvailableForEpisode(this, name, activeEpisode)
    }
    val balancerAvailability = availableBalancerIndices.map { index ->
        isBalancerAvailableForEpisode(this, index, activeDubbingName, activeEpisode)
    }
    val url = streamUrl.orEmpty()
    val qualities = streamQualities(this)
    val activeQuality = activeQuality(this)
    // Имена для показа: без общего префикса «Плеер», как во всех списках и кнопках UI.
    val balancerNames = availableBalancerIndices.map { index ->
        sourceGraph.balancers.getOrNull(index)?.name.orEmpty().removePrefix(playerNamePrefix)
    }
    val canChangePlayer = balancerNames.size > 1
    val canChangeDubbing = displayedDubbingNames.size > 1

    return PlayerPlaybackUiState(
        activeIframeUrl = activeIframeUrl(this),
        activeEpisode = activeEpisode,
        activeVideoId = activeVideoId(this),
        activeDubbing = activeDubbingName,
        activeBalancerName = activeBalancerName(this),
        activeScreenshotUrl = activeScreenshotUrl(this),
        activeSkips = activeEpisodeSource(this)?.skips ?: PlayerSkips.Empty,
        hasPrevEpisode = selection.episodeIndex > 0,
        hasNextEpisode = selection.episodeIndex < activeEpisodes.lastIndex,
        nextEpisodeDubbing = if (selection.episodeIndex < activeEpisodes.lastIndex) {
            null
        } else {
            nextEpisodeOtherDubbingSource(this)?.let { source ->
                sourceGraph.balancers[source.balancerIndex].dubbings[source.dubbingIndex].name
            }
        },
        finalEpisodeAction = if (isFinalAvailableEpisode(this, activeEpisode) && animeId > 0) {
            finalEpisodeAction
        } else {
            PlayerFinalEpisodeAction.None
        },
        dubbingNames = displayedDubbingNames.toImmutableList(),
        dubbingEpisodeCounts = globalEpisodeNumbers.map { it.distinct().size }.toImmutableList(),
        dubbingViews = globalViews.toImmutableList(),
        dubbingSourceNames = globalSourceNames.toImmutableList(),
        dubbingAvailability = dubbingAvailability.toImmutableList(),
        currentDubbingIndex = displayedDubbingNames.indexOf(activeDubbingName)
            .takeIf { it >= 0 }
            ?: selection.dubbingIndex,
        balancerNames = balancerNames.toImmutableList(),
        balancerAvailability = balancerAvailability.toImmutableList(),
        currentBalancerIndex = availableBalancerIndices.indexOf(selection.balancerIndex)
            .takeIf { it >= 0 }
            ?: 0,
        qualityLabels = qualities.keys.toImmutableList(),
        activeQuality = activeQuality,
        playbackUrl = activeQuality?.let(qualities::get) ?: url,
        canChangePlayer = canChangePlayer,
        canChangeDubbing = canChangeDubbing,
        showRecoveryHint = isPlaybackRecovering && showChangePlayerHint &&
            (canChangePlayer || canChangeDubbing),
        errorResumePositionMs = playbackPositionMs.takeIf { it > 0L }
            ?: resumeFromMs.takeIf { it > 0L }
            ?: 0L,
    )
}
