package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSliderRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow

@Composable
internal fun SettingsMobileWatchProgressContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            SettingsMobileToggleRow(
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
            )
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_mobile_watched_short_label),
                valueText = stringResource(
                    R.string.settings_mobile_watched_minutes_value,
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
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_mobile_watched_medium_label),
                valueText = stringResource(
                    R.string.settings_mobile_watched_minutes_value,
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
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_mobile_watched_long_label),
                valueText = stringResource(
                    R.string.settings_mobile_watched_minutes_value,
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
            SettingsMobileToggleRow(
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
    }
}
