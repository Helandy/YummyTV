package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.settings.BuildConfig
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSliderRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.common.utils.toNextEpisodeSwitchDelayText

@Composable
internal fun SettingsTvPlaybackContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
) {
    ToggleRow(
        label = stringResource(R.string.settings_auto_skip_label),
        hint = if (state.autoSkipOpeningsEndings) {
            stringResource(R.string.settings_auto_skip_enabled)
        } else {
            stringResource(R.string.settings_disabled)
        },
        enabled = state.autoSkipOpeningsEndings,
        onClick = { onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled) },
        modifier = Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    SettingsSliderRow(
        label = stringResource(R.string.settings_auto_skip_delay_label),
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
        label = stringResource(R.string.settings_show_opening_on_timeline_label),
        hint = if (state.showOpeningOnTimeline) {
            stringResource(R.string.settings_show_opening_on_timeline_enabled)
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
    SettingsDivider()
    SettingsSliderRow(
        label = stringResource(R.string.settings_controls_auto_hide_label),
        valueText = stringResource(
            R.string.settings_next_episode_switch_delay_seconds,
            state.playerControlsAutoHideSeconds,
        ),
        value = state.playerControlsAutoHideSeconds.toControlsAutoHideSlider(),
        valueRange = 1..if (BuildConfig.DEBUG) CONTROLS_AUTO_HIDE_DEBUG_STEP else CONTROLS_AUTO_HIDE_MAX_STEP,
        enabled = true,
        onValueChange = {
            onEvent(SettingsState.Event.PlayerControlsAutoHideChanged(it.fromControlsAutoHideSlider()))
        },
    )
}

// В debug у слайдера есть лишний последний пункт, который означает 30000 с.
private const val DEBUG_CONTROLS_AUTO_HIDE_SECONDS = 30000
private const val CONTROLS_AUTO_HIDE_MAX_STEP = 10
private const val CONTROLS_AUTO_HIDE_DEBUG_STEP = CONTROLS_AUTO_HIDE_MAX_STEP + 1

private fun Int.toControlsAutoHideSlider(): Int =
    if (this >= DEBUG_CONTROLS_AUTO_HIDE_SECONDS) CONTROLS_AUTO_HIDE_DEBUG_STEP else coerceAtMost(CONTROLS_AUTO_HIDE_MAX_STEP)

private fun Int.fromControlsAutoHideSlider(): Int =
    if (this >= CONTROLS_AUTO_HIDE_DEBUG_STEP) DEBUG_CONTROLS_AUTO_HIDE_SECONDS else this
