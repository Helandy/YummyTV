package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.feature.player.common.PlayerSpeedOptions
import su.afk.yummy.tv.feature.player.presentation.R

/** Качество, скорость и масштаб видео — верхние секции боковой панели настроек плеера. */
@Composable
internal fun MobilePlayerPlaybackSection(
    qualities: List<String>,
    selectedQuality: String?,
    onQualitySelected: (String) -> Unit,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    selectedResizeMode: PlayerResizeMode,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (qualities.isNotEmpty()) {
            val qualityIndex = qualities.indexOf(selectedQuality).coerceAtLeast(0)
            MobilePlayerSettingsSection(
                title = stringResource(R.string.player_quality_title),
                icon = Icons.Filled.HighQuality,
            ) {
                MobilePlayerSliderRow(
                    value = qualityIndex,
                    valueRange = 0..(qualities.size - 1).coerceAtLeast(0),
                    valueLabel = { index -> qualities[index] },
                    onValueCommitted = { index -> onQualitySelected(qualities[index]) },
                    tickLabels = qualities,
                )
            }
        }
        val speeds = PlayerSpeedOptions.values
        MobilePlayerSettingsSection(
            title = stringResource(R.string.player_speed_title),
            icon = Icons.Filled.Speed,
        ) {
            MobilePlayerSliderRow(
                value = PlayerSpeedOptions.nearestIndex(selectedSpeed),
                valueRange = speeds.indices,
                valueLabel = { index -> speeds[index].toSpeedLabel() },
                onValueCommitted = { index -> onSpeedSelected(speeds[index]) },
            )
        }
        MobilePlayerSettingsSection(
            title = stringResource(R.string.player_resize_title),
            icon = Icons.Filled.AspectRatio,
        ) {
            MobilePlayerScaleSelector(
                selectedMode = selectedResizeMode,
                onModeSelected = onResizeModeSelected,
            )
        }
    }
}

private fun Float.toSpeedLabel(): String = if (this % 1f == 0f) "${toInt()}x" else "${this}x"
