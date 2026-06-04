package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.R as UiR

@Composable
internal fun MobilePlayerControls(
    qualities: List<String>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    speeds: List<Float>,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    dubbingNames: List<String>,
    selectedDubbingIndex: Int,
    onDubbingSelected: (Int) -> Unit,
    balancerNames: List<String>,
    selectedBalancerIndex: Int,
    onBalancerSelected: (Int) -> Unit,
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = hasPrevEpisode, onClick = onPrevEpisode, modifier = Modifier.weight(1f)) {
                Text(stringResource(UiR.string.player_mobile_back))
            }
            Button(enabled = hasNextEpisode, onClick = onNextEpisode, modifier = Modifier.weight(1f)) {
                Text(stringResource(UiR.string.player_mobile_forward))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SelectionButton(
                label = stringResource(UiR.string.player_mobile_quality),
                value = selectedQuality,
                items = qualities,
                onSelected = onQualitySelected,
                modifier = Modifier.weight(1f),
            )
            SelectionButton(
                label = stringResource(UiR.string.player_mobile_speed),
                value = "${selectedSpeed}x",
                items = speeds.map { "${it}x" },
                onSelected = { label -> label.removeSuffix("x").toFloatOrNull()?.let(onSpeedSelected) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SelectionButton(
                label = stringResource(UiR.string.player_mobile_dubbing),
                value = dubbingNames.getOrElse(selectedDubbingIndex) { "-" },
                items = dubbingNames,
                onSelected = { selected -> onDubbingSelected(dubbingNames.indexOf(selected).coerceAtLeast(0)) },
                modifier = Modifier.weight(1f),
            )
            SelectionButton(
                label = stringResource(UiR.string.player_mobile_player),
                value = balancerNames.getOrElse(selectedBalancerIndex) { "-" },
                items = balancerNames,
                onSelected = { selected -> onBalancerSelected(balancerNames.indexOf(selected).coerceAtLeast(0)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
