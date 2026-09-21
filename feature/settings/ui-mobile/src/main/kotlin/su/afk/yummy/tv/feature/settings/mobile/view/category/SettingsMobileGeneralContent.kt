package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileNavigationRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow

@Composable
internal fun SettingsMobileGeneralContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onPickerRequested: (SettingsMobilePicker) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_interface_type),
                value = state.interfaceMode.label(),
                hint = state.interfaceMode.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.INTERFACE_MODE) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_content_language_title),
                value = state.contentLanguage.label(),
                onClick = { onPickerRequested(SettingsMobilePicker.CONTENT_LANGUAGE) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_save_last_search_label),
                hint = if (state.saveLastSearchEnabled) {
                    stringResource(R.string.settings_save_last_search_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.saveLastSearchEnabled,
                onClick = { onEvent(SettingsState.Event.SaveLastSearchToggled) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_devices)) {
            SettingsMobileNavigationRow(
                label = stringResource(R.string.settings_mobile_login_on_tv),
                hint = stringResource(R.string.settings_mobile_login_on_tv_hint),
                onClick = { onEvent(SettingsState.Event.LoginOnTvSelected) },
            )
        }
    }
}
