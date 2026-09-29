package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemUpdater
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.common.utils.buildPlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.common.utils.buildPlayerMediaItemKey
import su.afk.yummy.tv.feature.player.common.utils.playerMediaItemMeta
import su.afk.yummy.tv.feature.player.common.utils.playerPlaybackKey
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState

/** Ключ воспроизведения текущего потока: пересчитывается только от того, что в него входит. */
@Composable
fun rememberPlayerPlaybackKey(state: PlayerState.State, url: String): String =
    remember(
        url,
        state.streamHeaders,
        state.offlineCacheKey,
        state.retryKey,
        // Side-loaded subtitles are part of the MediaItem, so a new pick has to rebuild the key.
        state.selectedAllohaSubtitleIndex,
    ) {
        playerPlaybackKey(state = state, url = url)
    }

/**
 * Отдаёт плееру media item текущего потока, общий для ТВ и мобилки: переподготовка при смене
 * [playbackKey], обновление метаданных медиа-сессии без переподготовки, затем playWhenReady.
 *
 * @param playbackPositionMs позиция, с которой стартует новый поток.
 */
@Composable
fun PlayerMediaItemEffect(
    player: Player?,
    playbackKey: String,
    state: PlayerState.State,
    playback: PlayerPlaybackUiState,
    durationMs: () -> Long,
    playbackPositionMs: () -> Long,
    shouldPlay: () -> Boolean,
) {
    val playbackConfig = rememberPlayerPlaybackConfig()
    val mediaItemUpdater = remember { PlayerMediaItemUpdater() }
    val meta = playerMediaItemMeta(playback)
    val mediaItemKey = remember(playbackKey, state.animeTitle, meta, state.artworkUrl) {
        buildPlayerMediaItemKey(
            playbackKey = playbackKey,
            animeTitle = state.animeTitle,
            meta = meta,
            artworkUrl = state.artworkUrl,
        )
    }
    val currentState by rememberUpdatedState(state)
    val currentPlayback by rememberUpdatedState(playback)
    val currentDuration by rememberUpdatedState(durationMs)
    val currentPosition by rememberUpdatedState(playbackPositionMs)
    val currentShouldPlay by rememberUpdatedState(shouldPlay)

    LaunchedEffect(player, playbackKey, mediaItemKey, playback.activeIframeUrl) {
        val activePlayer = player ?: return@LaunchedEffect
        mediaItemUpdater.update(
            player = activePlayer,
            playbackConfig = playbackConfig,
            config = buildPlayerMediaItemConfig(
                playbackKey = playbackKey,
                mediaItemKey = mediaItemKey,
                url = currentPlayback.playbackUrl,
                state = currentState,
                playback = currentPlayback,
                meta = meta,
                durationMs = currentDuration(),
                playbackPositionMs = currentPosition(),
            ),
        )
        activePlayer.playWhenReady = currentShouldPlay()
    }
}
