package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    adaptiveQuality: Boolean,
    adaptiveLimitQuality: String?,
    adaptiveLimitBest: Boolean,
    onAdaptiveLimitSelected: (String) -> Unit,
    onAdaptiveLimitBestSelected: () -> Unit,
    onAdaptiveQualityChange: (Boolean) -> Unit,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    selectedResizeMode: PlayerResizeMode,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (qualities.isNotEmpty()) {
            val currentIndex = qualities.indexOf(selectedQuality).coerceAtLeast(0)
            // При адаптации ползунок показывает и двигает лимит, а не текущее качество.
            val limitIndex = qualities.indexOf(adaptiveLimitQuality).coerceAtLeast(0)
            // В адаптиве к меткам добавляется «Лучшее»: лимит снят, настройка не прибивается к максимуму тайтла.
            val bestLabel = stringResource(R.string.player_quality_best)
            val sliderLabels = if (adaptiveQuality) qualities + bestLabel else qualities
            val bestIndex = qualities.size
            val qualityIndex = when {
                adaptiveQuality && adaptiveLimitBest -> bestIndex
                adaptiveQuality -> limitIndex
                else -> currentIndex
            }
            MobilePlayerSettingsSection(
                title = stringResource(R.string.player_quality_title),
                icon = Icons.Filled.HighQuality,
            ) {
                // Сверху переключатель: он решает, кто управляет качеством, а ползунок ниже —
                // лишь показывает/задаёт уровень. Текущее качество названо один раз.
                if (qualities.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.player_adaptive_quality_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (adaptiveQuality) {
                                    stringResource(
                                        R.string.player_adaptive_quality_current_limit,
                                        qualities.getOrNull(currentIndex).orEmpty(),
                                        if (adaptiveLimitBest) bestLabel else qualities.getOrNull(limitIndex).orEmpty(),
                                    )
                                } else {
                                    stringResource(R.string.player_adaptive_quality_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (adaptiveQuality) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Switch(checked = adaptiveQuality, onCheckedChange = onAdaptiveQualityChange)
                    }
                }
                MobilePlayerSliderRow(
                    value = qualityIndex,
                    valueRange = 0..(sliderLabels.size - 1).coerceAtLeast(0),
                    valueLabel = { index -> sliderLabels[index] },
                    onValueCommitted = { index ->
                        when {
                            adaptiveQuality && index == bestIndex -> onAdaptiveLimitBestSelected()
                            adaptiveQuality -> onAdaptiveLimitSelected(qualities[index])
                            else -> onQualitySelected(qualities[index])
                        }
                    },
                    tickLabels = sliderLabels,
                    enabled = true,
                    // Выбранное качество уже подсвечено в подписях под треком.
                    showValueLabel = false,
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
