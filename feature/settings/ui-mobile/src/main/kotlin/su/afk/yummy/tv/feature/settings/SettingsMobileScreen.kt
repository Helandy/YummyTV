package su.afk.yummy.tv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileScreen
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize

@Composable
fun SettingsMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    MobileScreen(title = "Настройки") { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Тема") }
            item {
                AppTheme.entries.forEach { theme ->
                    Button(
                        onClick = { onEvent(SettingsState.Event.AppThemeSelected(theme)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(theme.name) }
                }
            }
            item { Text("Качество постеров") }
            item {
                PosterQuality.entries.forEach { quality ->
                    Button(
                        onClick = { onEvent(SettingsState.Event.PosterQualitySelected(quality)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(quality.name) }
                }
            }
            item {
                ToggleRow("Watch Next", state.watchNextEnabled) { onEvent(SettingsState.Event.WatchNextToggled) }
            }
            item {
                ToggleRow("Автопропуск опенингов/эндингов", state.autoSkipOpeningsEndings) {
                    onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled)
                }
            }
            item { Text("Плеер по умолчанию") }
            item {
                PreferredPlayer.entries.forEach { player ->
                    Button(
                        onClick = { onEvent(SettingsState.Event.PreferredPlayerSelected(player)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(player.name) }
                }
            }
            item { Text("Кэш превью") }
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
                    label = { Text("Yani application token") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
