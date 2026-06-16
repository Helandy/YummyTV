package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkips
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerVideoSource

internal fun AnimeVideo.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    playerId = playerId,
    iframeUrl = iframeUrl,
    views = views,
    skips = skips.toPlayerSkips(),
)

private fun AnimeVideoSkips.toPlayerSkips(): PlayerSkips = PlayerSkips(
    opening = opening.toPlayerSkipSegment(),
    ending = ending.toPlayerSkipSegment(),
)

private fun AnimeVideoSkipSegment?.toPlayerSkipSegment(): PlayerSkipSegment? =
    this?.let { PlayerSkipSegment(startMs = it.startMs, endMs = it.endMs) }
