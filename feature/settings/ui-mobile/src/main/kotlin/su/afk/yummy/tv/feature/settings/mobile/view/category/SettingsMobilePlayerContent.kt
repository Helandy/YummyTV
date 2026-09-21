package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.utils.detailsText
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.mobile.utils.videoQualitySliderEntries
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileActionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSliderRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow

@Composable
internal fun SettingsMobilePlayerContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onPickerRequested: (SettingsMobilePicker) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_mobile_default_player),
                value = state.preferredPlayer.label(),
                hint = state.preferredPlayer.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.PLAYER) },
            )
            val videoQualityIndex = videoQualitySliderEntries
                .indexOf(state.preferredVideoQuality).coerceAtLeast(0)
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_preferred_video_quality_title),
                valueText = videoQualitySliderEntries[videoQualityIndex].label(),
                value = videoQualityIndex,
                valueRange = 0..videoQualitySliderEntries.lastIndex,
                enabled = true,
                onValueChange = {
                    onEvent(
                        SettingsState.Event.PreferredVideoQualitySelected(
                            videoQualitySliderEntries[it],
                        ),
                    )
                },
            )
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_player_buffer_title),
                valueText = state.playerBufferProfile.detailsText(),
                value = PlayerBufferProfile.entries.indexOf(state.playerBufferProfile)
                    .coerceAtLeast(0),
                valueRange = 0..PlayerBufferProfile.entries.lastIndex,
                enabled = true,
                onValueChange = {
                    onEvent(
                        SettingsState.Event.PlayerBufferProfileSelected(
                            PlayerBufferProfile.entries[it],
                        ),
                    )
                },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_player_orientation_title),
                value = state.playerOrientationMode.label(),
                hint = state.playerOrientationMode.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.PLAYER_ORIENTATION) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_player_advanced)) {
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_picture_in_picture_label),
                hint = if (state.pictureInPictureEnabled) {
                    stringResource(R.string.settings_picture_in_picture_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.pictureInPictureEnabled,
                onClick = { onEvent(SettingsState.Event.PictureInPictureToggled) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_mobile_advanced_volume_label),
                hint = if (state.advancedPlayerVolumeEnabled) {
                    stringResource(R.string.settings_mobile_advanced_volume_enabled)
                } else {
                    stringResource(R.string.settings_mobile_advanced_volume_disabled)
                },
                enabled = state.advancedPlayerVolumeEnabled,
                onClick = { onEvent(SettingsState.Event.AdvancedPlayerVolumeToggled) },
            )
            if (state.volumeStabilizationSupported) {
                SettingsMobileToggleRow(
                    label = stringResource(R.string.settings_mobile_volume_stabilization_label),
                    hint = if (state.volumeStabilizationEnabled) {
                        stringResource(R.string.settings_mobile_volume_stabilization_enabled)
                    } else {
                        stringResource(R.string.settings_mobile_volume_stabilization_disabled)
                    },
                    enabled = state.volumeStabilizationEnabled,
                    onClick = { onEvent(SettingsState.Event.VolumeStabilizationToggled) },
                )
            }
            SettingsMobileActionRow(
                label = stringResource(R.string.settings_player_gesture_tutorial_reset),
                hint = if (state.mobilePlayerGestureTutorialDismissed) {
                    stringResource(R.string.settings_player_gesture_tutorial_reset_hint)
                } else {
                    stringResource(R.string.settings_player_gesture_tutorial_reset_done)
                },
                enabled = state.mobilePlayerGestureTutorialDismissed,
                onClick = {
                    onEvent(SettingsState.Event.MobilePlayerGestureTutorialReset)
                },
            )
        }
    }
}
