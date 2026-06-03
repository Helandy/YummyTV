package su.afk.yummy.tv.feature.details.episodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.view.common.BalancerPickerOverlay

@Composable
fun EpisodesTvScreen(
    state: EpisodesState.State,
    effect: Flow<EpisodesState.Effect>,
    onEvent: (EpisodesState.Event) -> Unit,
) {
    var restoreEpisodesFocusRequest by remember { mutableIntStateOf(0) }
    fun dismissBalancerPicker() {
        onEvent(EpisodesState.Event.BalancerPickerDismissed)
        restoreEpisodesFocusRequest += 1
    }

    BackHandler { onEvent(EpisodesState.Event.BackSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        EpisodesSection(
            state = state.videosState,
            watchProgress = state.watchProgress,
            restoreFocusRequest = restoreEpisodesFocusRequest,
            onVideoSelected = { video -> onEvent(EpisodesState.Event.VideoSelected(video)) },
        )

        val balancerPicker = state.pendingBalancerSelection
        BackHandler(enabled = balancerPicker != null) {
            dismissBalancerPicker()
        }
        if (balancerPicker != null) {
            BalancerPickerOverlay(
                picker = balancerPicker,
                onConfirmed = { option -> onEvent(EpisodesState.Event.BalancerConfirmed(option.video)) },
                onDismiss = ::dismissBalancerPicker,
            )
        }
    }
}
