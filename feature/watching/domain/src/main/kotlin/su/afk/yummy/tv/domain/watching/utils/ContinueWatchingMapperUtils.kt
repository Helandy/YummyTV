package su.afk.yummy.tv.domain.watching.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomePoster

internal fun AnimeVideo.toContinueWatchingPlaybackVideo() = ContinueWatchingPlaybackVideo(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    playerId = playerId,
    iframeUrl = iframeUrl,
    views = views,
    skips = skips,
)

internal fun HomeContinueWatchingItem.toContinueWatchingPlaybackVideo() =
    ContinueWatchingPlaybackVideo(
        id = videoId,
        episode = episode,
        dubbing = dubbing,
        player = playerName,
        iframeUrl = episodeUrl,
    )

internal fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
