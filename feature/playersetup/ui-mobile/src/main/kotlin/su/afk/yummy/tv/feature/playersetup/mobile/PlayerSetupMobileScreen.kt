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
import androidx.compose.material3.TextButton
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
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState
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
    var showQualityPicker by remember { mutableStateOf(false) }

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
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.player_setup_mobile_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Только три ключевые опции: остальное (язык, ориентация, PiP, таймлайн, выбор озвучки)
            // остаётся на умолчаниях и меняется в настройках.
            PlayerSetupMobileSection(title = stringResource(R.string.player_setup_mobile_section_main)) {
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_adaptive_quality_label),
                    hint = stringResource(R.string.player_setup_adaptive_quality_hint),
                    enabled = state.adaptiveVideoQualityEnabled,
                    onClick = { onEvent(PlayerSetupState.Event.AdaptiveVideoQualityToggled) },
                )
                PlayerSetupMobileOptionRow(
                    label = stringResource(R.string.player_setup_quality_label),
                    value = state.preferredVideoQuality.setupLabel(),
                    hint = stringResource(R.string.player_setup_quality_hint),
                    onClick = { showQualityPicker = true },
                    enabled = !state.adaptiveVideoQualityEnabled,
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_auto_skip_label),
                    hint = stringResource(R.string.player_setup_auto_skip_hint),
                    enabled = state.autoSkipOpeningsEndings,
                    onClick = { onEvent(PlayerSetupState.Event.AutoSkipOpeningsEndingsToggled) },
                )
                PlayerSetupMobileToggleRow(
                    label = stringResource(R.string.player_setup_auto_play_label),
                    hint = stringResource(R.string.player_setup_auto_play_hint),
                    enabled = state.autoPlayNextEpisode,
                    onClick = { onEvent(PlayerSetupState.Event.AutoPlayNextEpisodeToggled) },
                )
            }

            Button(
                onClick = { onEvent(PlayerSetupState.Event.DoneSelected) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(CoreR.string.common_done))
            }
            TextButton(
                onClick = { onEvent(PlayerSetupState.Event.MoreSettingsSelected) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.player_setup_mobile_more_settings))
            }
        }
    }

    if (showQualityPicker) {
        PlayerSetupMobilePickerSheet(
            title = stringResource(R.string.player_setup_quality_label),
            selectedValue = state.preferredVideoQuality,
            options = PreferredVideoQuality.entries.map {
                PlayerSetupMobilePickerOption(it, it.setupLabel(), it.setupHint())
            },
            onDismiss = { showQualityPicker = false },
            onSelected = {
                onEvent(PlayerSetupState.Event.PreferredVideoQualitySelected(it))
                showQualityPicker = false
            },
        )
    }
}
