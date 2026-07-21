package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.ProfileListPrivacy
import su.afk.yummy.tv.feature.account.mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfilePrivacySection(
    listPrivacy: ProfileListPrivacy,
    showShikimori: Boolean,
    showTelegram: Boolean,
    showVk: Boolean,
    showDiscord: Boolean,
    notifyTelegram: Boolean,
    notifyVk: Boolean,
    enabled: Boolean,
    onListPrivacyChanged: (ProfileListPrivacy) -> Unit,
    onShowShikimoriChanged: (Boolean) -> Unit,
    onShowTelegramChanged: (Boolean) -> Unit,
    onShowVkChanged: (Boolean) -> Unit,
    onShowDiscordChanged: (Boolean) -> Unit,
    onNotifyTelegramChanged: (Boolean) -> Unit,
    onNotifyVkChanged: (Boolean) -> Unit,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.profile_edit_privacy),
                style = MaterialTheme.typography.titleMedium
            )
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = it }) {
                OutlinedTextField(
                    value = listPrivacy.label(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_edit_lists_privacy)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ProfileListPrivacy.entries.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(value.label()) },
                            onClick = { expanded = false; onListPrivacyChanged(value) },
                        )
                    }
                }
            }
            ProfileSwitch(
                R.string.profile_edit_show_shikimori,
                showShikimori,
                enabled,
                onShowShikimoriChanged
            )
            ProfileSwitch(
                R.string.profile_edit_show_telegram,
                showTelegram,
                enabled,
                onShowTelegramChanged
            )
            ProfileSwitch(R.string.profile_edit_show_vk, showVk, enabled, onShowVkChanged)
            ProfileSwitch(
                R.string.profile_edit_show_discord,
                showDiscord,
                enabled,
                onShowDiscordChanged
            )
            Text(
                stringResource(R.string.profile_edit_notifications),
                style = MaterialTheme.typography.titleSmall
            )
            ProfileSwitch(
                R.string.profile_edit_notify_telegram,
                notifyTelegram,
                enabled,
                onNotifyTelegramChanged
            )
            ProfileSwitch(R.string.profile_edit_notify_vk, notifyVk, enabled, onNotifyVkChanged)
        }
    }
}

@Composable
private fun ProfileSwitch(
    labelRes: Int,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(labelRes), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ProfileListPrivacy.label() = stringResource(
    when (this) {
        ProfileListPrivacy.PUBLIC -> R.string.profile_privacy_public
        ProfileListPrivacy.FRIENDS -> R.string.profile_privacy_friends
        ProfileListPrivacy.AUTHORIZED -> R.string.profile_privacy_authorized
        ProfileListPrivacy.PRIVATE -> R.string.profile_privacy_private
    }
)
