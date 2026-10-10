package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.view.SettingsBlockGap
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSectionTitle
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
internal fun SettingsTvAppearanceContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
    pickerRow: SettingsTvPickerRow,
) {
    pickerRow(
        SettingsTvPicker.THEME,
        Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.BACKGROUND, Modifier)
    SettingsDivider()
    pickerRow(SettingsTvPicker.POSTER_SIZE, Modifier)
    SettingsDivider()
    pickerRow(SettingsTvPicker.CONTINUE_WATCHING_SIZE, Modifier)
    SettingsDivider()
    pickerRow(SettingsTvPicker.POSTER_QUALITY, Modifier)
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_show_top_title_year),
        hint = if (state.showTopTitleYear) {
            stringResource(R.string.settings_show_top_title_year_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.showTopTitleYear,
        onClick = { onEvent(SettingsState.Event.ShowTopTitleYearToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_show_library_title_year),
        hint = if (state.showLibraryTitleYear) {
            stringResource(R.string.settings_show_library_title_year_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.showLibraryTitleYear,
        onClick = { onEvent(SettingsState.Event.ShowLibraryTitleYearToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.LIBRARY_TAB_ORDER, Modifier)
    SettingsDivider()
    pickerRow(SettingsTvPicker.DETAILS_BUTTON_ORDER, Modifier)
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_section_new_episodes))
    ToggleRow(
        label = stringResource(R.string.settings_new_episodes_section),
        hint = if (state.newEpisodesSectionEnabled) {
            stringResource(R.string.settings_new_episodes_section_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.newEpisodesSectionEnabled,
        onClick = { onEvent(SettingsState.Event.NewEpisodesSectionToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    // Список источников без включённого блока ни на что не влияет.
    if (state.newEpisodesSectionEnabled) {
        SettingsDivider()
        pickerRow(SettingsTvPicker.NEW_EPISODES_SOURCES, Modifier)
        SettingsDivider()
        ToggleRow(
            label = stringResource(R.string.settings_new_episodes_hide_watched),
            hint = if (state.newEpisodesHideWatched) {
                stringResource(R.string.settings_new_episodes_hide_watched_enabled)
            } else {
                stringResource(R.string.settings_disabled)
            },
            enabled = state.newEpisodesHideWatched,
            onClick = { onEvent(SettingsState.Event.NewEpisodesHideWatchedToggled) },
            modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
        )
    }
}
