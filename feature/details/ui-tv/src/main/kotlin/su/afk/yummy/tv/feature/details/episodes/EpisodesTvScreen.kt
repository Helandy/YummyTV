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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.episodes.view.EpisodeDubbingPickerOverlay
import su.afk.yummy.tv.feature.details.episodes.view.EpisodesSection
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
    val balancerPicker = state.pendingBalancerSelection
    val dubbingPicker = state.pendingEpisodeDubbingSelection
    fun dismissDubbingPicker() {
        onEvent(EpisodesState.Event.EpisodeDubbingPickerDismissed)
        restoreEpisodesFocusRequest += 1
    }
    fun handleBack() {
        if (dubbingPicker != null) {
            dismissDubbingPicker()
        } else if (balancerPicker != null) {
            dismissBalancerPicker()
        } else {
            onEvent(EpisodesState.Event.BackSelected)
        }
    }

    BackHandler(enabled = balancerPicker == null && dubbingPicker == null) {
        handleBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Back, Key.Escape -> {
                        handleBack()
                        true
                    }

                    else -> false
                }
            }
            .background(MaterialTheme.colorScheme.background),
    ) {
        EpisodesSection(
            state = state.videosState,
            watchProgress = state.watchProgress,
            restoreFocusRequest = restoreEpisodesFocusRequest,
            onVideoSelected = { video -> onEvent(EpisodesState.Event.TvEpisodeSelected(video)) },
        )

        BackHandler(enabled = balancerPicker != null) {
            handleBack()
        }
        if (balancerPicker != null) {
            BalancerPickerOverlay(
                picker = balancerPicker,
                onConfirmed = { option ->
                    onEvent(EpisodesState.Event.TvBalancerConfirmed(option.video))
                },
                onDismiss = ::dismissBalancerPicker,
            )
        }
        if (dubbingPicker != null) {
            EpisodeDubbingPickerOverlay(
                selection = dubbingPicker,
                onSelected = { option ->
                    onEvent(EpisodesState.Event.EpisodeDubbingSelected(option.video))
                },
                onDismiss = ::dismissDubbingPicker,
            )
        }
    }
}
