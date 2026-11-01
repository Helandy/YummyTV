package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.mapper.toPlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
fun rememberPlayerPlaybackUiState(
    state: PlayerState.State,
    playerNamePrefix: String,
): PlayerPlaybackUiState {
    val connectionErrorMessage = stringResource(R.string.player_no_connection)
    return remember(state, playerNamePrefix, connectionErrorMessage) {
        state.toPlayerPlaybackUiState(playerNamePrefix, connectionErrorMessage)
    }
}
