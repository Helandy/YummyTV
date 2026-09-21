package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.utils.toNextEpisodeSwitchDelayText
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSliderRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow

@Composable
internal fun SettingsMobilePlaybackContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_ask_dubbing_on_watch_label),
                hint = if (state.askDubbingOnWatch) {
                    stringResource(R.string.settings_ask_dubbing_on_watch_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.askDubbingOnWatch,
                onClick = { onEvent(SettingsState.Event.AskDubbingOnWatchToggled) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_auto_skip_label),
                hint = if (state.autoSkipOpeningsEndings) {
                    stringResource(R.string.settings_auto_skip_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.autoSkipOpeningsEndings,
                onClick = { onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled) },
            )
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_mobile_auto_skip_delay_label),
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
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_mobile_show_opening_on_timeline_label),
                hint = if (state.showOpeningOnTimeline) {
                    stringResource(R.string.settings_mobile_show_opening_on_timeline_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.showOpeningOnTimeline,
                onClick = { onEvent(SettingsState.Event.ShowOpeningOnTimelineToggled) },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_auto_play_next_episode_label),
                hint = if (state.autoPlayNextEpisode) {
                    stringResource(R.string.settings_auto_play_next_episode_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.autoPlayNextEpisode,
                onClick = { onEvent(SettingsState.Event.AutoPlayNextEpisodeToggled) },
            )
            SettingsMobileSliderRow(
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
    }
}
