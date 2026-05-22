package su.afk.yummy.tv.feature.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.view.BalancerPickerOverlay
import su.afk.yummy.tv.feature.details.view.EpisodesSection

@Composable
fun EpisodesTvScreen(
    state: EpisodesState.State,
    effect: Flow<EpisodesState.Effect>,
    onEvent: (EpisodesState.Event) -> Unit,
) {
    BackHandler { onEvent(EpisodesState.Event.BackSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        EpisodesSection(
            state = state.videosState,
            watchProgress = state.watchProgress,
            onVideoSelected = { video -> onEvent(EpisodesState.Event.VideoSelected(video)) },
        )

        val balancerPicker = state.pendingBalancerSelection
        BackHandler(enabled = balancerPicker != null) {
            onEvent(EpisodesState.Event.BalancerPickerDismissed)
        }
        if (balancerPicker != null) {
            BalancerPickerOverlay(
                picker = balancerPicker,
                onConfirmed = { option -> onEvent(EpisodesState.Event.BalancerConfirmed(option.video)) },
                onDismiss = { onEvent(EpisodesState.Event.BalancerPickerDismissed) },
            )
        }
    }
}
