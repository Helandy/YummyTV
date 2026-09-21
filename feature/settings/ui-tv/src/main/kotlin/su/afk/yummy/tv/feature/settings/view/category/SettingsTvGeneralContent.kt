package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.view.SettingsBlockGap
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSectionTitle
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
internal fun SettingsTvGeneralContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
    pickerRow: SettingsTvPickerRow,
) {
    pickerRow(
        SettingsTvPicker.INTERFACE_MODE,
        Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.CONTENT_LANGUAGE, Modifier)
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_save_last_search_label),
        hint = if (state.saveLastSearchEnabled) {
            stringResource(R.string.settings_save_last_search_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.saveLastSearchEnabled,
        onClick = { onEvent(SettingsState.Event.SaveLastSearchToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_tv_beta_updates_label),
        hint = if (state.betaUpdatesEnabled) {
            stringResource(R.string.settings_tv_beta_updates_enabled)
        } else {
            stringResource(R.string.settings_tv_beta_updates_disabled)
        },
        enabled = state.betaUpdatesEnabled,
        onClick = { onEvent(SettingsState.Event.BetaUpdatesToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_tab_tv_home))
    ToggleRow(
        label = stringResource(R.string.settings_preview_channel_label),
        hint = if (state.isPreviewChannelBrowsable) {
            stringResource(R.string.settings_preview_channel_added)
        } else {
            stringResource(R.string.settings_preview_channel_add_hint)
        },
        enabled = state.isPreviewChannelBrowsable,
        onClick = {
            if (!state.isPreviewChannelBrowsable) {
                onEvent(SettingsState.Event.RequestPreviewChannelBrowsable)
            }
        },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_watch_next_label),
        hint = if (state.watchNextEnabled) {
            stringResource(R.string.settings_watch_next_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.watchNextEnabled,
        onClick = { onEvent(SettingsState.Event.WatchNextToggled) },
    )
}
