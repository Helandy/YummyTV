package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSliderRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.label

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
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_auto_manage_library_lists_label),
                hint = if (state.autoManageLibraryLists) {
                    stringResource(R.string.settings_auto_manage_library_lists_hint)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.autoManageLibraryLists,
                onClick = {
                    onEvent(SettingsState.Event.AutoManageLibraryListsToggled)
                },
            )
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_watched_short_label),
                valueLabel = { stringResource(R.string.settings_watched_minutes_value, it) },
                value = state.watchedThresholds.shortMinutes,
                valueRange = WatchedThresholds.SHORT_MINUTES_RANGE,
                enabled = true,
                onValueCommitted = {
                    onEvent(
                        SettingsState.Event.WatchedThresholdsChanged(
                            state.watchedThresholds.copy(shortMinutes = it),
                        ),
                    )
                },
            )
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_watched_medium_label),
                valueLabel = { stringResource(R.string.settings_watched_minutes_value, it) },
                value = state.watchedThresholds.mediumMinutes,
                valueRange = WatchedThresholds.MEDIUM_MINUTES_RANGE,
                enabled = true,
                onValueCommitted = {
                    onEvent(
                        SettingsState.Event.WatchedThresholdsChanged(
                            state.watchedThresholds.copy(mediumMinutes = it),
                        ),
                    )
                },
            )
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_watched_long_label),
                valueLabel = { stringResource(R.string.settings_watched_minutes_value, it) },
                value = state.watchedThresholds.longMinutes,
                valueRange = WatchedThresholds.LONG_MINUTES_RANGE,
                enabled = true,
                onValueCommitted = {
                    onEvent(
                        SettingsState.Event.WatchedThresholdsChanged(
                            state.watchedThresholds.copy(longMinutes = it),
                        ),
                    )
                },
            )
        }
    }
}
