package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.mobile.utils.newEpisodesSourcesValue
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileNavigationRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow
import su.afk.yummy.tv.feature.settings.presentation.R

@Composable
internal fun SettingsMobileAppearanceContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onPickerRequested: (SettingsMobilePicker) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_tab_theme),
                value = state.appTheme.label(),
                hint = state.appTheme.hint(),
                onClick = { onPickerRequested(SettingsMobilePicker.THEME) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_tab_background),
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
                label = stringResource(R.string.settings_poster_quality_title),
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
        SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_library)) {
            SettingsMobileNavigationRow(
                label = stringResource(R.string.settings_library_tabs_order),
                hint = stringResource(R.string.settings_library_tabs_order_hint),
                onClick = { onEvent(SettingsState.Event.LibraryTabOrderSelected) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_tab_details)) {
            SettingsMobileNavigationRow(
                label = stringResource(R.string.settings_details_buttons_order),
                hint = stringResource(R.string.settings_details_buttons_order_hint),
                onClick = { onEvent(SettingsState.Event.DetailsButtonOrderSelected) },
            )
        }
        SettingsMobileSection(title = stringResource(R.string.settings_section_new_episodes)) {
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_new_episodes_section),
                hint = if (state.newEpisodesSectionEnabled) {
                    stringResource(R.string.settings_new_episodes_section_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.newEpisodesSectionEnabled,
                onClick = { onEvent(SettingsState.Event.NewEpisodesSectionToggled) },
            )
            // Список источников без включённого блока ни на что не влияет.
            if (state.newEpisodesSectionEnabled) {
                SettingsMobileOptionRow(
                    label = stringResource(R.string.settings_new_episodes_sources_title),
                    value = state.newEpisodesSources.newEpisodesSourcesValue(),
                    onClick = { onPickerRequested(SettingsMobilePicker.NEW_EPISODES_SOURCES) },
                )
                SettingsMobileToggleRow(
                    label = stringResource(R.string.settings_new_episodes_hide_watched),
                    hint = if (state.newEpisodesHideWatched) {
                        stringResource(R.string.settings_new_episodes_hide_watched_enabled)
                    } else {
                        stringResource(R.string.settings_disabled)
                    },
                    enabled = state.newEpisodesHideWatched,
                    onClick = { onEvent(SettingsState.Event.NewEpisodesHideWatchedToggled) },
                )
            }
        }
    }
}
