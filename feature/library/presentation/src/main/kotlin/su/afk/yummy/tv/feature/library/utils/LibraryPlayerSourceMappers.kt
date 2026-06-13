package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.player.PlayerVideoSource

internal fun AnimeVideo.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    iframeUrl = iframeUrl,
    views = views,
    skips = skips.toPlayerSkips(),
)

internal fun WatchProgressEntry.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = videoId,
    episode = episode,
    dubbing = dubbing,
    player = playerName,
    iframeUrl = episodeUrl,
)
