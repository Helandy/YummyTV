package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.R as UiR

@Composable
internal fun MobilePlayerSettingsDialog(
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
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(UiR.string.player_mobile_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                SelectionButton(
                    label = stringResource(UiR.string.player_mobile_dubbing),
                    value = dubbingNames.getOrElse(selectedDubbingIndex) { "-" },
                    items = dubbingNames,
                    onSelected = { selected -> onDubbingSelected(dubbingNames.indexOf(selected).coerceAtLeast(0)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectionButton(
                    label = stringResource(UiR.string.player_mobile_player),
                    value = balancerNames.getOrElse(selectedBalancerIndex) { "-" },
                    items = balancerNames,
                    onSelected = { selected -> onBalancerSelected(balancerNames.indexOf(selected).coerceAtLeast(0)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(UiR.string.player_mobile_done))
            }
        },
    )
}
