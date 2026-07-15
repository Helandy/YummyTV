package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.toPlayerPlaybackUiState

@Composable
fun rememberPlayerPlaybackUiState(
    state: PlayerState.State,
    playerNamePrefix: String,
): PlayerPlaybackUiState = remember(state, playerNamePrefix) {
    state.toPlayerPlaybackUiState(playerNamePrefix)
}
