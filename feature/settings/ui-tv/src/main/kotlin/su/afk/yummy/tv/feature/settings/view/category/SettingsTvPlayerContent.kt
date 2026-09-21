package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.utils.detailsText
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.utils.videoQualitySliderEntries
import su.afk.yummy.tv.feature.settings.view.DetailsButtonOrderResetRow
import su.afk.yummy.tv.feature.settings.view.SettingsBlockGap
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSectionTitle
import su.afk.yummy.tv.feature.settings.view.SettingsSliderRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
internal fun SettingsTvPlayerContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
    pickerRow: SettingsTvPickerRow,
) {
    pickerRow(
        SettingsTvPicker.PREFERRED_PLAYER,
        Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    val qualities = videoQualitySliderEntries
    val qualityIndex =
        qualities.indexOf(state.preferredVideoQuality).coerceAtLeast(0)
    SettingsSliderRow(
        label = stringResource(R.string.settings_preferred_video_quality_title),
        valueText = qualities[qualityIndex].label(),
        value = qualityIndex,
        valueRange = 0..qualities.lastIndex,
        enabled = true,
        onValueChange = {
            onEvent(
                SettingsState.Event.PreferredVideoQualitySelected(qualities[it]),
            )
        },
        modifier = Modifier
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    val profiles = PlayerBufferProfile.entries
    SettingsSliderRow(
        label = stringResource(R.string.settings_player_buffer_title),
        valueText = state.playerBufferProfile.detailsText(),
        value = profiles.indexOf(state.playerBufferProfile).coerceAtLeast(0),
        valueRange = 0..profiles.lastIndex,
        enabled = true,
        onValueChange = {
            onEvent(
                SettingsState.Event.PlayerBufferProfileSelected(profiles[it]),
            )
        },
        modifier = Modifier
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_tv_section_sound))
    ToggleRow(
        label = stringResource(R.string.settings_tv_advanced_volume_label),
        hint = if (state.advancedPlayerVolumeEnabled) {
            stringResource(R.string.settings_tv_advanced_volume_enabled)
        } else {
            stringResource(R.string.settings_tv_advanced_volume_disabled)
        },
        enabled = state.advancedPlayerVolumeEnabled,
        onClick = {
            onEvent(SettingsState.Event.AdvancedPlayerVolumeToggled)
        },
    )
    if (state.volumeStabilizationSupported) {
        SettingsDivider()
        ToggleRow(
            label = stringResource(R.string.settings_tv_volume_stabilization_label),
            hint = if (state.volumeStabilizationEnabled) {
                stringResource(R.string.settings_tv_volume_stabilization_enabled)
            } else {
                stringResource(R.string.settings_tv_volume_stabilization_disabled)
            },
            enabled = state.volumeStabilizationEnabled,
            onClick = {
                onEvent(SettingsState.Event.VolumeStabilizationToggled)
            },
        )
    }
    if (state.tvPlayerControlsTutorialDismissed) {
        SettingsDivider()
        DetailsButtonOrderResetRow(
            label = stringResource(R.string.settings_tv_controls_tutorial_reset),
            hint = stringResource(R.string.settings_tv_controls_tutorial_reset_hint),
            onReset = {
                onEvent(SettingsState.Event.TvPlayerControlsTutorialReset)
            },
        )
    }
}
