package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.feature.player.common.PlayerSubtitleStyleOptions
import su.afk.yummy.tv.feature.player.common.icon
import su.afk.yummy.tv.feature.player.common.label
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Оформление субтитров в боковой панели: размер и отступ ползунками, цвет и фон чипами. Изменения
 * сразу видны на текущем кадре — оверлей субтитров читает стиль из того же стейта.
 */
@Composable
internal fun MobilePlayerSubtitleStyleSection(
    style: PlayerSubtitleStyleSettings,
    onStyleChange: (PlayerSubtitleStyleSettings) -> Unit,
) {
    MobilePlayerSettingsSection(
        title = stringResource(R.string.player_subtitle_style_title),
        icon = Icons.Filled.Palette,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MobilePlayerSidePanelLabel(stringResource(R.string.player_subtitle_size_title))
            // Ползунок в шагах по 10%: при шаге 1% полторы сотни делений сливаются в двойную полосу.
            val sizeStep = PlayerSubtitleStyleOptions.TEXT_SIZE_STEP
            val sizeRange = PlayerSubtitleStyleOptions.textSizeRange
            MobilePlayerSliderRow(
                value = (style.textSize + sizeStep / 2) / sizeStep,
                valueRange = sizeRange.first / sizeStep..sizeRange.last / sizeStep,
                valueLabel = { "${it * sizeStep}%" },
                onValueCommitted = { onStyleChange(style.copy(textSize = it * sizeStep)) },
            )
            MobilePlayerSidePanelLabel(stringResource(R.string.player_subtitle_offset_title))
            MobilePlayerSliderRow(
                value = style.offset,
                valueRange = PlayerSubtitleStyleOptions.offsetRange,
                valueLabel = { "$it%" },
                onValueCommitted = { onStyleChange(style.copy(offset = it)) },
            )
            MobilePlayerSidePanelLabel(stringResource(R.string.player_subtitle_color_title))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayerSubtitleTextColor.entries.forEach { color ->
                    MobilePlayerSubtitleColorSwatch(
                        color = Color(color.argb),
                        label = color.label(),
                        selected = color == style.textColor,
                        onClick = { onStyleChange(style.copy(textColor = color)) },
                    )
                }
            }
            MobilePlayerSidePanelLabel(stringResource(R.string.player_subtitle_background_title))
            MobilePlayerIconChoiceRow(
                options = PlayerSubtitleBackground.entries,
                selected = style.background,
                icon = { it.icon() },
                label = { it.label() },
                onSelected = { onStyleChange(style.copy(background = it)) },
            )
        }
    }
}

@Composable
private fun MobilePlayerSidePanelLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
