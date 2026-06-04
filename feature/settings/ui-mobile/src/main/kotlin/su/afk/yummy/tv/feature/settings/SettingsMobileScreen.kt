package su.afk.yummy.tv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileScreen
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
fun SettingsMobileScreen(

    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,

) {
    MobileScreen(title = stringResource(R.string.settings_mobile_title)) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.settings_mobile_theme)) }
            item {
                AppTheme.entries.forEach { theme ->
                    Button(
                        onClick = { onEvent(SettingsState.Event.AppThemeSelected(theme)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(theme.name) }
                }
            }
            item { Text(stringResource(R.string.settings_mobile_poster_quality)) }
            item {
                PosterQuality.entries.forEach { quality ->
                    Button(
                        onClick = { onEvent(SettingsState.Event.PosterQualitySelected(quality)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(quality.name) }
                }
            }
            item {
                ToggleRow(stringResource(R.string.settings_mobile_watch_next), state.watchNextEnabled) {
                    onEvent(SettingsState.Event.WatchNextToggled)
                }
            }
            item {
                ToggleRow(stringResource(R.string.settings_mobile_auto_skip), state.autoSkipOpeningsEndings) {
                    onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled)
                }
            }
            item { Text(stringResource(R.string.settings_mobile_default_player)) }
            item {
                PreferredPlayer.entries.forEach { player ->
                    Button(
                        onClick = { onEvent(SettingsState.Event.PreferredPlayerSelected(player)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(player.name) }
                }
            }
            item { Text(stringResource(R.string.settings_mobile_preview_cache)) }
            item {
                PreviewCacheSize.entries.forEach { size ->
                    Button(
                        onClick = { onEvent(SettingsState.Event.PreviewCacheSizeSelected(size)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(size.name) }
                }
            }
            item {
                OutlinedTextField(
                    value = state.yaniApplicationToken,
                    onValueChange = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
                    label = { Text(stringResource(R.string.settings_mobile_yani_token)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
