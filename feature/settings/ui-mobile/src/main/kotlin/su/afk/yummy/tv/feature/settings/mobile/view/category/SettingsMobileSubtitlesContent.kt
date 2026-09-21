package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.mobile.utils.toSubtitlePercentText
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSliderRow

@Composable
internal fun SettingsMobileSubtitlesContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onPickerRequested: (SettingsMobilePicker) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection(subtitle = stringResource(R.string.settings_subtitle_style_alloha_hint)) {
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_subtitle_size_title),
                valueText = state.subtitleStyle.textSize.toSubtitlePercentText(),
                value = state.subtitleStyle.textSize,
                valueRange = 50..200,
                enabled = true,
                onValueChange = {
                    onEvent(
                        SettingsState.Event.SubtitleStyleSelected(
                            state.subtitleStyle.copy(textSize = it),
                        ),
                    )
                },
            )
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_subtitle_offset_title),
                valueText = state.subtitleStyle.offset.toSubtitlePercentText(),
                value = state.subtitleStyle.offset,
                valueRange = 0..20,
                enabled = true,
                onValueChange = {
                    onEvent(
                        SettingsState.Event.SubtitleStyleSelected(
                            state.subtitleStyle.copy(offset = it),
                        ),
                    )
                },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_subtitle_color_title),
                value = state.subtitleStyle.textColor.label(),
                onClick = { onPickerRequested(SettingsMobilePicker.SUBTITLE_COLOR) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_subtitle_background_title),
                value = state.subtitleStyle.background.label(),
                onClick = { onPickerRequested(SettingsMobilePicker.SUBTITLE_BACKGROUND) },
            )
        }
    }
}
