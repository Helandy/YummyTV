package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.icon
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.common.utils.enabledHint
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.hint
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.view.SettingsBlockGap
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSectionTitle
import su.afk.yummy.tv.feature.settings.view.SettingsTvIconChoice
import su.afk.yummy.tv.feature.settings.view.SettingsTvIconChoiceRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
internal fun SettingsTvAppearanceContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
    pickerRow: SettingsTvPickerRow,
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_section_general_appearance))
    pickerRow(
        SettingsTvPicker.THEME,
        Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.BACKGROUND, Modifier)
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_seasonal_decorations),
        hint = if (state.seasonalDecorationsEnabled) {
            state.seasonalTheme.enabledHint()
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.seasonalDecorationsEnabled,
        onClick = { onEvent(SettingsState.Event.SeasonalDecorationsToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_section_posters))
    SettingsTvIconChoiceRow(
        label = stringResource(R.string.settings_poster_size_title),
        hint = state.posterCardSize.hint(),
        choices = PosterCardSize.entries.map { SettingsTvIconChoice(it, it.icon(), it.label()) },
        selected = state.posterCardSize,
        onSelected = { onEvent(SettingsState.Event.PosterCardSizeSelected(it)) },
        firstChoiceModifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    SettingsTvIconChoiceRow(
        label = stringResource(R.string.settings_title_card_layout_title),
        hint = state.titleCardLayout.hint(),
        choices = TitleCardLayout.entries.map { SettingsTvIconChoice(it, it.icon(), it.label()) },
        selected = state.titleCardLayout,
        onSelected = { onEvent(SettingsState.Event.TitleCardLayoutSelected(it)) },
        firstChoiceModifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.POSTER_QUALITY, Modifier)
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_show_catalog_title_year),
        hint = if (state.showCatalogTitleYear) {
            stringResource(R.string.settings_show_catalog_title_year_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.showCatalogTitleYear,
        onClick = { onEvent(SettingsState.Event.ShowCatalogTitleYearToggled) },
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
    SettingsBlockGap()
    SettingsSectionTitle(text = stringResource(R.string.settings_mobile_section_library))
    SettingsTvIconChoiceRow(
        label = stringResource(R.string.settings_library_continue_watching_card_size_title),
        hint = state.libraryContinueWatchingCardSize.hint(),
        choices = LibraryContinueWatchingCardSize.entries.map {
            SettingsTvIconChoice(it, it.icon(), it.label())
        },
        selected = state.libraryContinueWatchingCardSize,
        onSelected = { onEvent(SettingsState.Event.LibraryContinueWatchingCardSizeSelected(it)) },
        firstChoiceModifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    pickerRow(SettingsTvPicker.LIBRARY_TAB_ORDER, Modifier)
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
    }
}
