package su.afk.yummy.tv.feature.playersetup.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState
import su.afk.yummy.tv.feature.playersetup.mobile.model.PlayerSetupMobilePicker
import su.afk.yummy.tv.feature.playersetup.mobile.model.PlayerSetupMobilePickerOption
import su.afk.yummy.tv.feature.playersetup.mobile.utils.setupHint
import su.afk.yummy.tv.feature.playersetup.mobile.utils.setupLabel
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobileOptionRow
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobilePickerSheet
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobileSection
import su.afk.yummy.tv.feature.playersetup.mobile.view.PlayerSetupMobileToggleRow
import su.afk.yummy.tv.feature.playersetup.presentation.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlayerSetupMobileScreen(
    state: PlayerSetupState.State,
    effect: Flow<PlayerSetupState.Effect>,
    onEvent: (PlayerSetupState.Event) -> Unit,
) {
    var activePicker by remember { mutableStateOf<PlayerSetupMobilePicker?>(null) }

    BaseScreen(isScroll = false) {
        Column(
            modifier = Modifier
                .mobileContentMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.player_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.player_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_general)) {
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_language_label),
                    value = state.contentLanguage.setupLabel(),
                    hint = stringResource(R.string.player_setup_language_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.LANGUAGE },
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_timings)) {
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_opening_timeline_label),
                    hint = stringResource(R.string.player_setup_opening_timeline_hint),
                    enabled = state.showOpeningOnTimeline,
                    onClick = { onEvent(PlayerSetupState.Event.ShowOpeningOnTimelineToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_auto_skip_label),
                    hint = stringResource(R.string.player_setup_auto_skip_hint),
                    enabled = state.autoSkipOpeningsEndings,
                    onClick = { onEvent(PlayerSetupState.Event.AutoSkipOpeningsEndingsToggled) },
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_episodes)) {
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_auto_play_label),
                    hint = stringResource(R.string.player_setup_auto_play_hint),
                    enabled = state.autoPlayNextEpisode,
                    onClick = { onEvent(PlayerSetupState.Event.AutoPlayNextEpisodeToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_suggest_next_label),
                    hint = stringResource(R.string.player_setup_suggest_next_hint),
                    enabled = state.suggestNextEpisodeOnWatched,
                    onClick = { onEvent(PlayerSetupState.Event.SuggestNextEpisodeOnWatchedToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_ask_dubbing_label),
                    hint = stringResource(R.string.player_setup_ask_dubbing_hint),
                    enabled = state.askDubbingOnWatch,
                    onClick = { onEvent(PlayerSetupState.Event.AskDubbingOnWatchToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_refresh_progress_label),
                    hint = stringResource(R.string.player_setup_refresh_progress_hint),
                    enabled = state.refreshContinueWatchingProgressOnLaunch,
                    onClick = { onEvent(PlayerSetupState.Event.RefreshContinueWatchingProgressToggled) },
                )
            }

            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_playback)) {
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_mobile_orientation_label),
                    value = state.playerOrientationMode.setupLabel(),
                    hint = stringResource(R.string.player_setup_mobile_orientation_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.ORIENTATION },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_mobile_pip_label),
                    hint = stringResource(R.string.player_setup_mobile_pip_hint),
                    enabled = state.pictureInPictureEnabled,
                    onClick = { onEvent(PlayerSetupState.Event.PictureInPictureToggled) },
                )
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_quality_label),
                    value = state.preferredVideoQuality.setupLabel(),
                    hint = stringResource(R.string.player_setup_quality_hint),
                    onClick = { activePicker = PlayerSetupMobilePicker.QUALITY },
                )
            }

            Button(
                onClick = { onEvent(PlayerSetupState.Event.DoneSelected) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(CoreR.string.common_done))
            }
        }
    }

    when (activePicker) {
        PlayerSetupMobilePicker.LANGUAGE -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_language_label),
            selectedValue = state.contentLanguage,
            options = YaniContentLanguage.entries.map {
                PlayerSetupMobilePickerOption(it, it.setupLabel())
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(PlayerSetupState.Event.ContentLanguageSelected(it))
                activePicker = null
            },
        )

        PlayerSetupMobilePicker.ORIENTATION -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_mobile_orientation_label),
            selectedValue = state.playerOrientationMode,
            options = PlayerOrientationMode.entries.map {
                PlayerSetupMobilePickerOption(it, it.setupLabel(), it.setupHint())
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(PlayerSetupState.Event.PlayerOrientationModeSelected(it))
                activePicker = null
            },
        )

        PlayerSetupMobilePicker.QUALITY -> PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_quality_label),
            selectedValue = state.preferredVideoQuality,
            options = PreferredVideoQuality.entries.map {
                PlayerSetupMobilePickerOption(it, it.setupLabel(), it.setupHint())
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(PlayerSetupState.Event.PreferredVideoQualitySelected(it))
                activePicker = null
            },
        )

        null -> Unit
    }
}
