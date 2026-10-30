package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.NotificationPermissionGateHost
import su.afk.yummy.tv.core.designsystem.mobile.NotificationPermissionGateState
import su.afk.yummy.tv.core.designsystem.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.core.designsystem.mobile.rememberNotificationPermissionGranted
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.core.designsystem.R as CoreR

@Composable
internal fun SettingsMobileNotificationsContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    // Включение без системного разрешения сначала просит его: при отказе остаётся выключенным.
    // Выключение — только локальный флаг, воркер просто не показывает пуш.
    val notificationPermissionGate = rememberNotificationPermissionGate(runActionWhenDenied = false)
    var permissionCheck by remember { mutableIntStateOf(0) }
    val hasNotificationPermission = rememberNotificationPermissionGranted(permissionCheck)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            EpisodePushToggle(
                label = stringResource(R.string.settings_episode_push_label),
                hint = stringResource(R.string.settings_episode_push_hint),
                checked = state.episodePushEnabled && hasNotificationPermission,
                gate = notificationPermissionGate,
                onToggled = { enabled ->
                    onEvent(SettingsState.Event.EpisodePushToggled(enabled))
                    permissionCheck++
                },
            )
            EpisodePushToggle(
                label = stringResource(R.string.settings_library_episode_push_label),
                hint = stringResource(R.string.settings_library_episode_push_hint),
                checked = state.libraryEpisodePushEnabled && hasNotificationPermission,
                gate = notificationPermissionGate,
                onToggled = { enabled ->
                    onEvent(SettingsState.Event.LibraryEpisodePushToggled(enabled))
                    permissionCheck++
                },
            )
        }
    }
    NotificationPermissionGateHost(
        state = notificationPermissionGate,
        permissionWasRequested = state.notificationPermissionRequested,
        onPermissionRequested = { onEvent(SettingsState.Event.NotificationPermissionRequested) },
        explanationRes = CoreR.string.notification_permission_explanation_episode_push,
    )
}

/** Переключатель пуша: включение без системного разрешения сначала просит его. */
@Composable
private fun EpisodePushToggle(
    label: String,
    hint: String,
    checked: Boolean,
    gate: NotificationPermissionGateState,
    onToggled: (Boolean) -> Unit,
) {
    SettingsMobileToggleRow(
        label = label,
        hint = if (checked) hint else stringResource(R.string.settings_disabled),
        enabled = checked,
        onClick = {
            if (checked) {
                onToggled(false)
            } else {
                gate { onToggled(true) }
            }
        },
    )
}
