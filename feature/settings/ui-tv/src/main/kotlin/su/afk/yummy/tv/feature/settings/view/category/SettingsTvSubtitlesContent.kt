package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.utils.toSubtitlePercentText
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSectionTitle
import su.afk.yummy.tv.feature.settings.view.SettingsSliderRow

@Composable
internal fun SettingsTvSubtitlesContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
    pickerRow: SettingsTvPickerRow,
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_subtitle_style_title))
    Text(
        text = stringResource(R.string.settings_subtitle_style_alloha_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    SettingsSliderRow(
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
        modifier = Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    SettingsSliderRow(
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
    SettingsDivider()
    pickerRow(SettingsTvPicker.SUBTITLE_COLOR, Modifier)
    SettingsDivider()
    pickerRow(SettingsTvPicker.SUBTITLE_BACKGROUND, Modifier)
}
