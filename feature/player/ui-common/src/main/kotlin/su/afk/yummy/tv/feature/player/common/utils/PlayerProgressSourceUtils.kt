package su.afk.yummy.tv.feature.player.common.utils

import su.afk.yummy.tv.feature.player.common.model.PlayerProgressSource
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState

/** Источник прогресса для активной серии: по нему репортёр знает, к какой серии относится позиция. */
fun PlayerPlaybackUiState.toProgressSource(): PlayerProgressSource =
    PlayerProgressSource(
        episodeUrl = activeIframeUrl,
        episode = activeEpisode,
        videoId = activeVideoId,
        playerName = activeBalancerName,
        dubbing = activeDubbing,
        screenshotUrl = activeScreenshotUrl,
    )
