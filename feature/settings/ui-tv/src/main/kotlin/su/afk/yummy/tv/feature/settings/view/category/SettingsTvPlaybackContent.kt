package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.utils.toNextEpisodeSwitchDelayText
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSliderRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
internal fun SettingsTvPlaybackContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
) {
    ToggleRow(
        label = stringResource(R.string.settings_ask_dubbing_on_watch_label),
        hint = if (state.askDubbingOnWatch) {
            stringResource(R.string.settings_ask_dubbing_on_watch_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.askDubbingOnWatch,
        onClick = {
            onEvent(SettingsState.Event.AskDubbingOnWatchToggled)
        },
        modifier = Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_auto_skip_label),
        hint = if (state.autoSkipOpeningsEndings) {
            stringResource(R.string.settings_auto_skip_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.autoSkipOpeningsEndings,
        onClick = { onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled) },
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    SettingsSliderRow(
        label = stringResource(R.string.settings_tv_auto_skip_delay_label),
        valueText = stringResource(
            R.string.settings_next_episode_switch_delay_seconds,
            state.autoSkipDelaySeconds,
        ),
        value = state.autoSkipDelaySeconds,
        valueRange = 1..15,
        enabled = state.autoSkipOpeningsEndings,
        onValueChange = {
            onEvent(SettingsState.Event.AutoSkipDelayChanged(it))
        },
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_tv_show_opening_on_timeline_label),
        hint = if (state.showOpeningOnTimeline) {
            stringResource(R.string.settings_tv_show_opening_on_timeline_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.showOpeningOnTimeline,
        onClick = { onEvent(SettingsState.Event.ShowOpeningOnTimelineToggled) },
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_auto_play_next_episode_label),
        hint = if (state.autoPlayNextEpisode) {
            stringResource(R.string.settings_auto_play_next_episode_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.autoPlayNextEpisode,
        onClick = {
            onEvent(SettingsState.Event.AutoPlayNextEpisodeToggled)
        },
    )
    SettingsDivider()
    SettingsSliderRow(
        label = stringResource(R.string.settings_next_episode_switch_delay_label),
        valueText = state.nextEpisodeSwitchDelaySeconds.toNextEpisodeSwitchDelayText(),
        value = state.nextEpisodeSwitchDelaySeconds,
        valueRange = 0..30,
        enabled = state.autoPlayNextEpisode,
        onValueChange = {
            onEvent(SettingsState.Event.NextEpisodeSwitchDelayChanged(it))
        },
    )
}
