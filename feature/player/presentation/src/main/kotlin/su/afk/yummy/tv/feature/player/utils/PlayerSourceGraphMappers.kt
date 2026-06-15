package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.domain.player.model.PlayerSourceBalancer as DomainPlayerSourceBalancer
import su.afk.yummy.tv.domain.player.model.PlayerSourceDubbing as DomainPlayerSourceDubbing
import su.afk.yummy.tv.domain.player.model.PlayerSourceEpisode as DomainPlayerSourceEpisode
import su.afk.yummy.tv.domain.player.model.PlayerSourceGraph as DomainPlayerSourceGraph
import su.afk.yummy.tv.domain.player.model.PlayerSourceSelection as DomainPlayerSourceSelection
import su.afk.yummy.tv.domain.player.model.PlayerSourceSkipSegment as DomainPlayerSourceSkipSegment
import su.afk.yummy.tv.domain.player.model.PlayerSourceSkips as DomainPlayerSourceSkips

internal fun DomainPlayerSourceGraph.toPresentationSourceGraph(): PlayerSourceGraph =
    PlayerSourceGraph(
        balancers = balancers.map { it.toPresentationSourceBalancer() },
        selection = selection.toPresentationSourceSelection(),
    )

private fun DomainPlayerSourceBalancer.toPresentationSourceBalancer(): PlayerSourceBalancer =
    PlayerSourceBalancer(
        name = name,
        dubbings = dubbings.map { it.toPresentationSourceDubbing() },
    )

private fun DomainPlayerSourceDubbing.toPresentationSourceDubbing(): PlayerSourceDubbing =
    PlayerSourceDubbing(
        name = name,
        episodes = episodes.map { it.toPresentationSourceEpisode() },
        views = views,
    )

private fun DomainPlayerSourceEpisode.toPresentationSourceEpisode(): PlayerSourceEpisode =
    PlayerSourceEpisode(
        id = id,
        number = number,
        iframeUrl = iframeUrl,
        screenshotUrl = screenshotUrl,
        skips = skips.toPresentationSkips(),
    )

private fun DomainPlayerSourceSelection.toPresentationSourceSelection(): PlayerSourceSelection =
    PlayerSourceSelection(
        balancerIndex = balancerIndex,
        dubbingIndex = dubbingIndex,
        episodeIndex = episodeIndex,
    )

private fun DomainPlayerSourceSkips.toPresentationSkips(): PlayerSkips =
    PlayerSkips(
        opening = opening.toPresentationSkipSegment(),
        ending = ending.toPresentationSkipSegment(),
    )

private fun DomainPlayerSourceSkipSegment?.toPresentationSkipSegment(): PlayerSkipSegment? =
    this?.let { PlayerSkipSegment(startMs = it.startMs, endMs = it.endMs) }
