package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSliderRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
internal fun SettingsTvWatchProgressContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
) {
    ToggleRow(
        label = stringResource(R.string.settings_suggest_next_episode_on_watched_label),
        hint = if (state.suggestNextEpisodeOnWatched) {
            stringResource(R.string.settings_suggest_next_episode_on_watched_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.suggestNextEpisodeOnWatched,
        onClick = {
            onEvent(SettingsState.Event.SuggestNextEpisodeOnWatchedToggled)
        },
        modifier = Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    SettingsSliderRow(
        label = stringResource(R.string.settings_tv_watched_short_label),
        valueText = stringResource(
            R.string.settings_tv_watched_minutes_value,
            state.watchedThresholds.shortMinutes,
        ),
        value = state.watchedThresholds.shortMinutes,
        valueRange = WatchedThresholds.SHORT_MINUTES_RANGE,
        enabled = true,
        onValueChange = {
            onEvent(
                SettingsState.Event.WatchedThresholdsChanged(
                    state.watchedThresholds.copy(shortMinutes = it),
                ),
            )
        },
    )
    SettingsDivider()
    SettingsSliderRow(
        label = stringResource(R.string.settings_tv_watched_medium_label),
        valueText = stringResource(
            R.string.settings_tv_watched_minutes_value,
            state.watchedThresholds.mediumMinutes,
        ),
        value = state.watchedThresholds.mediumMinutes,
        valueRange = WatchedThresholds.MEDIUM_MINUTES_RANGE,
        enabled = true,
        onValueChange = {
            onEvent(
                SettingsState.Event.WatchedThresholdsChanged(
                    state.watchedThresholds.copy(mediumMinutes = it),
                ),
            )
        },
    )
    SettingsDivider()
    SettingsSliderRow(
        label = stringResource(R.string.settings_tv_watched_long_label),
        valueText = stringResource(
            R.string.settings_tv_watched_minutes_value,
            state.watchedThresholds.longMinutes,
        ),
        value = state.watchedThresholds.longMinutes,
        valueRange = WatchedThresholds.LONG_MINUTES_RANGE,
        enabled = true,
        onValueChange = {
            onEvent(
                SettingsState.Event.WatchedThresholdsChanged(
                    state.watchedThresholds.copy(longMinutes = it),
                ),
            )
        },
    )
    SettingsDivider()
    ToggleRow(
        label = stringResource(R.string.settings_refresh_continue_watching_progress_label),
        hint = if (state.refreshContinueWatchingProgressOnLaunch) {
            stringResource(R.string.settings_refresh_continue_watching_progress_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.refreshContinueWatchingProgressOnLaunch,
        onClick = {
            onEvent(
                SettingsState.Event.RefreshContinueWatchingProgressOnLaunchToggled,
            )
        },
    )
}
