package su.afk.yummy.tv.feature.player.common

import su.afk.yummy.tv.core.utils.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.common.service.PlayerAudioTrackPolicy

fun playerAudioTrackPolicyFor(episodeUrl: String): PlayerAudioTrackPolicy =
    if (episodeUrl.isAllohaPlayerUrl()) {
        PlayerAudioTrackPolicy.FirstAudioGroup
    } else {
        PlayerAudioTrackPolicy.Default
    }

fun playerUseRotatingHlsCacheKeys(isOfflinePlayback: Boolean, episodeUrl: String): Boolean =
    isOfflinePlayback && episodeUrl.isAllohaPlayerUrl()

/**
 * «Тихое переподключение под буфер» ([PlayerLoadErrorHandlingPolicy]) — только для сетевых
 * не-Alloha источников. Alloha исключена (свой fresh-session recovery), офлайн/локальные файлы —
 * там перерезолва нет и обрываться нечему.
 */
fun playerSilentReconnectEnabled(
    episodeUrl: String,
    isOfflinePlayback: Boolean,
    isLocalFile: Boolean,
): Boolean = !isOfflinePlayback && !isLocalFile && !episodeUrl.isAllohaPlayerUrl()
