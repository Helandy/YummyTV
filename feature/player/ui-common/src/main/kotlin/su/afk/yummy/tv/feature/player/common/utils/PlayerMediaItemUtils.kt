package su.afk.yummy.tv.feature.player.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.buildPlayerPlaybackKey
import su.afk.yummy.tv.feature.player.common.mediaMimeType
import su.afk.yummy.tv.feature.player.common.model.PlayerMediaItemMeta
import su.afk.yummy.tv.feature.player.common.playerAudioTrackPolicyFor
import su.afk.yummy.tv.feature.player.common.playerSilentReconnectEnabled
import su.afk.yummy.tv.feature.player.common.playerUseRotatingHlsCacheKeys
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.selectedAllohaSubtitle

@Composable
fun playerMediaItemMeta(ui: PlayerPlaybackUiState): PlayerMediaItemMeta {
    val subtitle = ui.activeEpisode.takeIf { it.isNotBlank() }?.let {
        stringResource(R.string.player_notification_episode, it)
    }
    val description = when {
        ui.activeBalancerName.isNotBlank() && ui.activeDubbing.isNotBlank() ->
            stringResource(
                R.string.player_notification_details,
                ui.activeDubbing,
                ui.activeBalancerName,
            )

        else -> ui.activeBalancerName.ifBlank { ui.activeDubbing }.takeIf { it.isNotBlank() }
    }
    val contentText = listOfNotNull(
        subtitle,
        ui.activeDubbing.takeIf { it.isNotBlank() },
        ui.activeBalancerName.takeIf { it.isNotBlank() },
    ).joinToString(separator = " • ")
    return PlayerMediaItemMeta(
        subtitle = subtitle,
        description = description,
        contentText = contentText,
    )
}

/** Ключ, при смене которого плеер заново готовит media item. */
fun playerPlaybackKey(state: PlayerState.State, url: String): String =
    buildPlayerPlaybackKey(
        url = url,
        retryKey = state.retryKey,
        headers = state.streamHeaders,
        // Side-loaded subtitles live on the MediaItem itself, so a different pick must produce a
        // different playback key for the player to re-prepare with it.
        offlineCacheKeySegment = state.offlineCacheKey.orEmpty() +
            "|sub=${state.selectedAllohaSubtitle()?.url.orEmpty()}",
    )

/** Ключ метаданных: их смена обновляет media item без переподготовки плеера. */
fun buildPlayerMediaItemKey(
    playbackKey: String,
    animeTitle: String,
    meta: PlayerMediaItemMeta,
    artworkUrl: String?,
): String = buildString {
    append(playbackKey)
    append('|').append(animeTitle)
    append('|').append(meta.subtitle.orEmpty())
    append('|').append(meta.description.orEmpty())
    append('|').append(meta.contentText)
    append('|').append(artworkUrl.orEmpty())
}

fun buildPlayerMediaItemConfig(
    playbackKey: String,
    mediaItemKey: String,
    url: String,
    state: PlayerState.State,
    playback: PlayerPlaybackUiState,
    meta: PlayerMediaItemMeta,
    durationMs: Long,
    playbackPositionMs: Long,
): PlayerMediaItemConfig {
    val episodeUrl = playback.activeIframeUrl
    val subtitle = state.selectedAllohaSubtitle()
    return PlayerMediaItemConfig(
        playbackKey = playbackKey,
        mediaItemKey = mediaItemKey,
        url = url,
        title = state.animeTitle,
        artist = meta.contentText,
        subtitle = meta.subtitle,
        description = meta.description,
        artworkUrl = state.artworkUrl,
        durationMs = durationMs,
        headers = state.streamHeaders,
        offlineCacheKey = state.offlineCacheKey,
        offlineCacheKeyScheme = state.offlineCacheKeyScheme,
        isOfflinePlayback = state.isOfflinePlayback,
        isLocalFile = state.isLocalFile,
        useRotatingHlsCacheKeys = playerUseRotatingHlsCacheKeys(
            isOfflinePlayback = state.isOfflinePlayback,
            episodeUrl = episodeUrl,
        ),
        audioTrackPolicy = playerAudioTrackPolicyFor(episodeUrl),
        playbackPositionMs = playbackPositionMs,
        resumeFromMs = state.resumeFromMs,
        subtitleUrl = subtitle?.url,
        subtitleMimeType = subtitle?.mediaMimeType(),
        subtitleLanguage = subtitle?.language,
        subtitleLabel = subtitle?.label,
        silentReconnectEnabled = playerSilentReconnectEnabled(
            episodeUrl = episodeUrl,
            isOfflinePlayback = state.isOfflinePlayback,
            isLocalFile = state.isLocalFile,
        ),
    )
}
