package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.tabularNumerals
import su.afk.yummy.tv.feature.player.common.PlayerVolumeController
import su.afk.yummy.tv.feature.player.presentation.R
import kotlin.math.roundToInt

/**
 * Громкость плеера (независимая от системной, шаг 1%) и стабилизация громкости в боковой панели.
 * Ползунок применяется на лету: [PlayerVolumeController] сам дебаунсит запись.
 */
@Composable
internal fun MobilePlayerVolumeSection(
    showVolume: Boolean,
    volumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    showStabilization: Boolean,
    stabilizationEnabled: Boolean,
    onStabilizationChange: (Boolean) -> Unit,
) {
    MobilePlayerSettingsSection(
        title = stringResource(R.string.player_volume_title),
        icon = Icons.AutoMirrored.Filled.VolumeUp,
    ) {
        if (showVolume) {
            val icon = when {
                volumePercent <= 0 -> Icons.AutoMirrored.Filled.VolumeOff
                volumePercent < PlayerVolumeController.MAX_PERCENT -> Icons.AutoMirrored.Filled.VolumeDown
                else -> Icons.AutoMirrored.Filled.VolumeUp
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { onVolumeChange(it.roundToInt()) },
                    valueRange = 0f..PlayerVolumeController.MAX_PERCENT.toFloat(),
                    steps = PlayerVolumeController.MAX_PERCENT - 1,
                    colors = SliderDefaults.colors(
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$volumePercent%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium.tabularNumerals(),
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 44.dp),
                )
            }
        }
        if (showStabilization) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.player_volume_stabilization_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.player_volume_stabilization_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = stabilizationEnabled, onCheckedChange = onStabilizationChange)
            }
        }
    }
}
