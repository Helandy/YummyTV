package su.afk.yummy.tv.feature.playersetup.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState
import su.afk.yummy.tv.feature.playersetup.presentation.R
import su.afk.yummy.tv.feature.playersetup.tv.utils.next
import su.afk.yummy.tv.feature.playersetup.tv.utils.setupLabel
import su.afk.yummy.tv.feature.playersetup.tv.view.PlayerSetupTvButton
import su.afk.yummy.tv.feature.playersetup.tv.view.PlayerSetupTvChoiceRow
import su.afk.yummy.tv.feature.playersetup.tv.view.PlayerSetupTvToggleRow

@Composable
fun PlayerSetupTvScreen(
    state: PlayerSetupState.State,
    effect: Flow<PlayerSetupState.Effect>,
    onEvent: (PlayerSetupState.Event) -> Unit,
) {
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 32.dp)
            .widthIn(max = 880.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.player_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.player_setup_subtitle),
            modifier = Modifier.padding(bottom = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PlayerSetupTvToggleRow(
            label = stringResource(R.string.player_setup_opening_timeline_label),
            hint = stringResource(R.string.player_setup_opening_timeline_hint),
            enabled = state.showOpeningOnTimeline,
            onClick = { onEvent(PlayerSetupState.Event.ShowOpeningOnTimelineToggled) },
            modifier = Modifier.focusRequester(firstRowFocusRequester),
        )
        PlayerSetupTvToggleRow(
            label = stringResource(R.string.player_setup_auto_skip_label),
            hint = stringResource(R.string.player_setup_auto_skip_hint),
            enabled = state.autoSkipOpeningsEndings,
            onClick = { onEvent(PlayerSetupState.Event.AutoSkipOpeningsEndingsToggled) },
        )
        PlayerSetupTvToggleRow(
            label = stringResource(R.string.player_setup_auto_play_label),
            hint = stringResource(R.string.player_setup_auto_play_hint),
            enabled = state.autoPlayNextEpisode,
            onClick = { onEvent(PlayerSetupState.Event.AutoPlayNextEpisodeToggled) },
        )
        PlayerSetupTvToggleRow(
            label = stringResource(R.string.player_setup_suggest_next_label),
            hint = stringResource(R.string.player_setup_suggest_next_hint),
            enabled = state.suggestNextEpisodeOnWatched,
            onClick = { onEvent(PlayerSetupState.Event.SuggestNextEpisodeOnWatchedToggled) },
        )
        PlayerSetupTvToggleRow(
            label = stringResource(R.string.player_setup_ask_dubbing_label),
            hint = stringResource(R.string.player_setup_ask_dubbing_hint),
            enabled = state.askDubbingOnWatch,
            onClick = { onEvent(PlayerSetupState.Event.AskDubbingOnWatchToggled) },
        )
        PlayerSetupTvToggleRow(
            label = stringResource(R.string.player_setup_refresh_progress_label),
            hint = stringResource(R.string.player_setup_refresh_progress_hint),
            enabled = state.refreshContinueWatchingProgressOnLaunch,
            onClick = { onEvent(PlayerSetupState.Event.RefreshContinueWatchingProgressToggled) },
        )
        PlayerSetupTvChoiceRow(
            label = stringResource(R.string.player_setup_language_label),
            hint = stringResource(R.string.player_setup_language_hint),
            value = state.contentLanguage.setupLabel(),
            onCycle = {
                onEvent(PlayerSetupState.Event.ContentLanguageSelected(state.contentLanguage.next()))
            },
        )
        PlayerSetupTvChoiceRow(
            label = stringResource(R.string.player_setup_quality_label),
            hint = stringResource(R.string.player_setup_quality_hint),
            value = state.preferredVideoQuality.setupLabel(),
            onCycle = {
                onEvent(PlayerSetupState.Event.PreferredVideoQualitySelected(state.preferredVideoQuality.next()))
            },
        )

        PlayerSetupTvButton(
            text = stringResource(R.string.player_setup_done),
            onClick = { onEvent(PlayerSetupState.Event.DoneSelected) },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
