package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleEdge
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.common.PlayerSubtitleStyleOptions
import su.afk.yummy.tv.feature.player.common.icon
import su.afk.yummy.tv.feature.player.common.label
import su.afk.yummy.tv.feature.player.model.TvPlayerSettingsPage
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.isSelected
import su.afk.yummy.tv.feature.player.utils.speedLabel
import su.afk.yummy.tv.feature.player.utils.tvIcon
import su.afk.yummy.tv.feature.player.utils.tvLabel
import su.afk.yummy.tv.feature.player.utils.tvPlayerScaleOptions

/** Подменю: заголовок со стрелкой назад и список вариантов, текущий отмечен галочкой. */
@Composable
internal fun TvPlayerSettingsOptionsPage(
    page: TvPlayerSettingsPage,
    optionFocus: FocusRequester,
    speeds: List<Float>,
    activeSpeed: Float,
    onSpeedSelected: (index: Int) -> Unit,
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
    onZoomLevelSelected: (PlayerZoomLevel) -> Unit,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    onSubtitleTrackSelected: (index: Int) -> Unit,
    subtitleStyle: PlayerSubtitleStyleSettings,
    onSubtitleStyleChange: (PlayerSubtitleStyleSettings) -> Unit,
    onDone: () -> Unit,
) {
    val title: String
    val labels: List<String>
    var swatches: List<Color>? = null
    var icons: List<ImageVector>? = null
    val selectedIndex: Int
    val onSelect: (Int) -> Unit
    when (page) {
        TvPlayerSettingsPage.Speed -> {
            title = stringResource(R.string.player_speed_title)
            labels = speeds.map { it.speedLabel() }
            icons = speeds.map { Icons.Filled.Speed }
            selectedIndex = speeds.indexOf(activeSpeed)
            onSelect = onSpeedSelected
        }

        TvPlayerSettingsPage.Scale -> {
            title = stringResource(R.string.player_resize_title)
            labels = tvPlayerScaleOptions.map { it.tvLabel() }
            icons = tvPlayerScaleOptions.map { it.tvIcon() }
            selectedIndex = tvPlayerScaleOptions.indexOfFirst { it.isSelected(resizeMode, zoomLevel) }
            onSelect = { index ->
                val option = tvPlayerScaleOptions[index]
                val level = option.zoomLevel
                if (level != null) onZoomLevelSelected(level) else onResizeModeSelected(option.mode)
            }
        }

        TvPlayerSettingsPage.SubtitleTrack -> {
            title = stringResource(R.string.player_subtitle_track_title)
            labels = subtitleTrackNames
            selectedIndex = selectedSubtitleTrackIndex
            onSelect = onSubtitleTrackSelected
        }

        TvPlayerSettingsPage.SubtitleSize -> {
            val values = PlayerSubtitleStyleOptions.textSizeValues
            title = stringResource(R.string.player_subtitle_size_title)
            labels = values.map { "$it%" }
            icons = values.map { Icons.Filled.FormatSize }
            selectedIndex = values.indexOf(subtitleStyle.textSize)
            onSelect = { onSubtitleStyleChange(subtitleStyle.copy(textSize = values[it])) }
        }

        TvPlayerSettingsPage.SubtitleOffset -> {
            val values = PlayerSubtitleStyleOptions.offsetValues
            title = stringResource(R.string.player_subtitle_offset_title)
            labels = values.map { "$it%" }
            icons = values.map { Icons.Filled.VerticalAlignBottom }
            selectedIndex = values.indexOf(subtitleStyle.offset)
            onSelect = { onSubtitleStyleChange(subtitleStyle.copy(offset = values[it])) }
        }

        TvPlayerSettingsPage.SubtitleColor -> {
            val values = PlayerSubtitleTextColor.entries
            title = stringResource(R.string.player_subtitle_color_title)
            labels = values.map { it.label() }
            swatches = values.map { Color(it.argb) }
            selectedIndex = values.indexOf(subtitleStyle.textColor)
            onSelect = { onSubtitleStyleChange(subtitleStyle.copy(textColor = values[it])) }
        }

        TvPlayerSettingsPage.SubtitleBackground -> {
            val values = PlayerSubtitleBackground.entries
            title = stringResource(R.string.player_subtitle_background_title)
            labels = values.map { it.label() }
            icons = values.map { it.icon() }
            selectedIndex = values.indexOf(subtitleStyle.background)
            onSelect = { onSubtitleStyleChange(subtitleStyle.copy(background = values[it])) }
        }

        TvPlayerSettingsPage.SubtitleEdge -> {
            val values = PlayerSubtitleEdge.entries
            title = stringResource(R.string.player_subtitle_edge_title)
            labels = values.map { it.label() }
            icons = values.map { it.icon() }
            selectedIndex = values.indexOf(subtitleStyle.edge)
            onSelect = { onSubtitleStyleChange(subtitleStyle.copy(edge = values[it])) }
        }
    }
    // Значение могло прийти не из списка (размер с шагом 1% из экрана «Настройки») — тогда
    // фокус встаёт на первый вариант, галочки нет.
    val focusIndex = selectedIndex.takeIf { it in labels.indices } ?: 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp),
        )
        TvPlayerSettingsTitle(title)
    }
    labels.forEachIndexed { index, label ->
        PlayerSelectionItem(
            label = label,
            metaContent = {},
            selected = index == selectedIndex,
            enabled = true,
            swatchColor = swatches?.getOrNull(index),
            icon = icons?.getOrNull(index),
            modifier = if (index == focusIndex) Modifier.focusRequester(optionFocus) else Modifier,
            onExitDown = null,
            onClick = {
                if (index != selectedIndex) onSelect(index)
                onDone()
            },
        )
    }
}
