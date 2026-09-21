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
internal fun SettingsMobileAppearanceContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onPickerRequested: (SettingsMobilePicker) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_mobile_theme),
                value = state.appTheme.label(),
                hint = state.appTheme.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.THEME) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_mobile_background),
                value = state.backgroundStyle.label(),
                hint = state.backgroundStyle.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.BACKGROUND) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_mobile_poster_size),
                value = state.posterCardSize.label(),
                hint = state.posterCardSize.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.POSTER_SIZE) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_library_continue_watching_card_size_title),
                value = state.libraryContinueWatchingCardSize.label(),
                hint = state.libraryContinueWatchingCardSize.hint(),
                onClick = {
                    onPickerRequested(SettingsMobilePicker.LIBRARY_CONTINUE_WATCHING_SIZE)
                },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_mobile_poster_quality),
                value = state.posterQuality.label(),
                hint = state.posterQuality.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.POSTER_QUALITY) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_show_top_title_year),
                hint = if (state.showTopTitleYear) {
                    stringResource(R.string.settings_show_top_title_year_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.showTopTitleYear,
                onClick = { onEvent(SettingsState.Event.ShowTopTitleYearToggled) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_show_library_title_year),
                hint = if (state.showLibraryTitleYear) {
                    stringResource(R.string.settings_show_library_title_year_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.showLibraryTitleYear,
                onClick = { onEvent(SettingsState.Event.ShowLibraryTitleYearToggled) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_details)) {
            SettingsMobileNavigationRow(
                label = stringResource(R.string.settings_details_buttons_order),
                hint = stringResource(R.string.settings_details_buttons_order_hint),
                onClick = { onEvent(SettingsState.Event.DetailsButtonOrderSelected) },
            )
        }
    }
}
