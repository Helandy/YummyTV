package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.buildPlayerPlaybackKey
import su.afk.yummy.tv.feature.player.common.playerAudioTrackPolicyFor
import su.afk.yummy.tv.feature.player.common.playerUseRotatingHlsCacheKeys
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState

internal fun buildTvPlayerPlaybackKey(state: PlayerState.State, url: String): String =
    buildPlayerPlaybackKey(
        url = url,
        retryKey = state.retryKey,
        headers = state.streamHeaders,
        offlineCacheKeySegment = state.offlineCacheKey.orEmpty(),
    )

internal fun buildTvMediaItemKey(
    playbackKey: String,
    animeTitle: String,
    playback: PlayerPlaybackUiState,
    durationMs: Long,
): String =
    "$playbackKey|$animeTitle|${playback.activeEpisode}|${playback.activeDubbing}|" +
            "${playback.activeBalancerName}|${playback.activeScreenshotUrl}|$durationMs"

internal fun buildTvPlayerMediaItemConfig(
    playbackKey: String,
    mediaItemKey: String,
    url: String,
    state: PlayerState.State,
    playback: PlayerPlaybackUiState,
    durationMs: Long,
    playbackPositionMs: Long,
): PlayerMediaItemConfig = PlayerMediaItemConfig(
    playbackKey = playbackKey,
    mediaItemKey = mediaItemKey,
    url = url,
    title = state.animeTitle,
    artist = listOf(playback.activeDubbing, playback.activeBalancerName)
        .filter(String::isNotBlank)
        .joinToString(" • "),
    subtitle = playback.activeEpisode.takeIf(String::isNotBlank),
    description = playback.activeBalancerName.takeIf(String::isNotBlank),
    artworkUrl = playback.activeScreenshotUrl.takeIf(String::isNotBlank),
    durationMs = durationMs,
    headers = state.streamHeaders,
    offlineCacheKey = state.offlineCacheKey,
    isOfflinePlayback = state.isOfflinePlayback,
    useRotatingHlsCacheKeys = playerUseRotatingHlsCacheKeys(
        isOfflinePlayback = state.isOfflinePlayback,
        episodeUrl = playback.activeIframeUrl,
    ),
    audioTrackPolicy = playerAudioTrackPolicyFor(playback.activeIframeUrl),
    playbackPositionMs = playbackPositionMs,
    resumeFromMs = state.resumeFromMs,
)
