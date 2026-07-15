package su.afk.yummy.tv.feature.player.common

import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.common.service.PlayerAudioTrackPolicy

fun playerAudioTrackPolicyFor(episodeUrl: String): PlayerAudioTrackPolicy =
    if (episodeUrl.isAllohaPlayerUrl()) {
        PlayerAudioTrackPolicy.FirstAudioGroup
    } else {
        PlayerAudioTrackPolicy.Default
    }

fun playerUseRotatingHlsCacheKeys(isOfflinePlayback: Boolean, episodeUrl: String): Boolean =
    isOfflinePlayback && episodeUrl.isAllohaPlayerUrl()
