package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.buildPlayerPlaybackKey
import su.afk.yummy.tv.feature.player.common.mediaMimeType
import su.afk.yummy.tv.feature.player.common.playerAudioTrackPolicyFor
import su.afk.yummy.tv.feature.player.common.playerSilentReconnectEnabled
import su.afk.yummy.tv.feature.player.common.playerUseRotatingHlsCacheKeys
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState

internal fun buildTvPlayerPlaybackKey(state: PlayerState.State, url: String): String =
    buildPlayerPlaybackKey(
        url = url,
        retryKey = state.retryKey,
        headers = state.streamHeaders,
        // Side-loaded subtitles live on the MediaItem itself, so a different pick must produce a
        // different playback key for the player to re-prepare with it.
        offlineCacheKeySegment = state.offlineCacheKey.orEmpty() +
                "|sub=${state.selectedAllohaSubtitle()?.url.orEmpty()}",
    )

internal fun buildTvMediaItemKey(
    playbackKey: String,
    animeTitle: String,
    playback: PlayerPlaybackUiState,
    artworkUrl: String?,
): String =
    "$playbackKey|$animeTitle|${playback.activeEpisode}|${playback.activeDubbing}|" +
            "${playback.activeBalancerName}|${artworkUrl.orEmpty()}"

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
    artworkUrl = state.artworkUrl,
    durationMs = durationMs,
    headers = state.streamHeaders,
    offlineCacheKey = state.offlineCacheKey,
    offlineCacheKeyScheme = state.offlineCacheKeyScheme,
    isOfflinePlayback = state.isOfflinePlayback,
    isLocalFile = state.isLocalFile,
    useRotatingHlsCacheKeys = playerUseRotatingHlsCacheKeys(
        isOfflinePlayback = state.isOfflinePlayback,
        episodeUrl = playback.activeIframeUrl,
    ),
    audioTrackPolicy = playerAudioTrackPolicyFor(playback.activeIframeUrl),
    playbackPositionMs = playbackPositionMs,
    resumeFromMs = state.resumeFromMs,
    subtitleUrl = state.selectedAllohaSubtitle()?.url,
    subtitleMimeType = state.selectedAllohaSubtitle()?.mediaMimeType(),
    subtitleLanguage = state.selectedAllohaSubtitle()?.language,
    subtitleLabel = state.selectedAllohaSubtitle()?.label,
    silentReconnectEnabled = playerSilentReconnectEnabled(
        episodeUrl = playback.activeIframeUrl,
        isOfflinePlayback = state.isOfflinePlayback,
        isLocalFile = state.isLocalFile,
    ),
)
