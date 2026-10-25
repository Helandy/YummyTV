package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.common.PlayerVolumeController
import su.afk.yummy.tv.feature.player.common.label
import su.afk.yummy.tv.feature.player.model.TvPlayerSettingsPage
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.isSelected
import su.afk.yummy.tv.feature.player.utils.speedLabel
import su.afk.yummy.tv.feature.player.utils.tvLabel
import su.afk.yummy.tv.feature.player.utils.tvPlayerScaleOptions

/** Корень меню настроек ТВ-плеера: пункты по секциям «Воспроизведение», «Субтитры», «Звук». */
@Composable
internal fun TvPlayerSettingsRoot(
    entryFocusRequester: FocusRequester,
    rowFocus: Map<TvPlayerSettingsPage, FocusRequester>,
    activeSpeed: Float,
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    subtitleStyle: PlayerSubtitleStyleSettings,
    showVolume: Boolean,
    volumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    showStabilization: Boolean,
    stabilizationEnabled: Boolean,
    onStabilizationChange: (Boolean) -> Unit,
    showAdaptiveQuality: Boolean,
    adaptiveQualityEnabled: Boolean,
    onAdaptiveQualityChange: (Boolean) -> Unit,
    onOpenPage: (TvPlayerSettingsPage) -> Unit,
) {
    @Composable
    fun PageRow(
        page: TvPlayerSettingsPage,
        label: String,
        value: String,
        modifier: Modifier = Modifier,
        valueColor: Color? = null,
    ) {
        TvPlayerSettingsMenuRow(
            label = label,
            value = value,
            valueColor = valueColor,
            onClick = { onOpenPage(page) },
            modifier = modifier.focusRequester(rowFocus.getValue(page)),
        )
    }

    TvPlayerSettingsTitle(stringResource(R.string.player_side_panel_title))

    TvPlayerSettingsHeader(stringResource(R.string.player_settings_section_playback))
    PageRow(
        page = TvPlayerSettingsPage.Speed,
        label = stringResource(R.string.player_speed_title),
        value = activeSpeed.speedLabel(),
        modifier = Modifier.focusRequester(entryFocusRequester),
    )
    PageRow(
        page = TvPlayerSettingsPage.Scale,
        label = stringResource(R.string.player_resize_title),
        value = tvPlayerScaleOptions.firstOrNull { it.isSelected(resizeMode, zoomLevel) }?.tvLabel().orEmpty(),
    )
    if (showAdaptiveQuality) {
        TvPlayerSettingsMenuRow(
            label = stringResource(R.string.player_adaptive_quality_title),
            value = stringResource(if (adaptiveQualityEnabled) R.string.player_toggle_on else R.string.player_toggle_off),
            toggleChecked = adaptiveQualityEnabled,
            onClick = { onAdaptiveQualityChange(!adaptiveQualityEnabled) },
            showChevron = false,
        )
    }

    TvPlayerSettingsHeader(stringResource(R.string.player_settings_section_subtitles))
    if (subtitleTrackNames.isNotEmpty()) {
        PageRow(
            page = TvPlayerSettingsPage.SubtitleTrack,
            label = stringResource(R.string.player_subtitle_track_title),
            value = subtitleTrackNames.getOrNull(selectedSubtitleTrackIndex).orEmpty(),
        )
    }
    PageRow(
        page = TvPlayerSettingsPage.SubtitleSize,
        label = stringResource(R.string.player_subtitle_size_title),
        value = "${subtitleStyle.textSize}%",
    )
    PageRow(
        page = TvPlayerSettingsPage.SubtitleOffset,
        label = stringResource(R.string.player_subtitle_offset_title),
        value = "${subtitleStyle.offset}%",
    )
    PageRow(
        page = TvPlayerSettingsPage.SubtitleColor,
        label = stringResource(R.string.player_subtitle_color_title),
        value = subtitleStyle.textColor.label(),
        valueColor = Color(subtitleStyle.textColor.argb),
    )
    PageRow(
        page = TvPlayerSettingsPage.SubtitleBackground,
        label = stringResource(R.string.player_subtitle_background_title),
        value = subtitleStyle.background.label(),
    )
    PageRow(
        page = TvPlayerSettingsPage.SubtitleEdge,
        label = stringResource(R.string.player_subtitle_edge_title),
        value = subtitleStyle.edge.label(),
    )

    if (showVolume || showStabilization) {
        TvPlayerSettingsHeader(stringResource(R.string.player_settings_section_sound))
    }
    if (showVolume) {
        TvPlayerSettingsStepperRow(
            label = stringResource(R.string.player_volume_title),
            value = "$volumePercent%",
            fraction = volumePercent.toFloat() / PlayerVolumeController.MAX_PERCENT,
            onStep = { direction ->
                onVolumeChange((volumePercent + direction).coerceIn(0, PlayerVolumeController.MAX_PERCENT))
            },
            onClick = {},
        )
    }
    if (showStabilization) {
        TvPlayerSettingsMenuRow(
            label = stringResource(R.string.player_volume_stabilization_title),
            value = stringResource(if (stabilizationEnabled) R.string.player_toggle_on else R.string.player_toggle_off),
            toggleChecked = stabilizationEnabled,
            onClick = { onStabilizationChange(!stabilizationEnabled) },
            showChevron = false,
        )
    }
}
