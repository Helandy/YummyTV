package su.afk.yummy.tv.feature.settings.mobile.view.category

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.permissions.rememberLocalNetworkAccessRequest
import su.afk.yummy.tv.core.designsystem.utils.addUrlLinks
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.hintText
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.common.utils.warningText
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileActionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow
import su.afk.yummy.tv.feature.settings.model.TorrServerCheckStatus
import su.afk.yummy.tv.feature.settings.presentation.R

/** Категория «Торренты»: тумблер, адрес TorrServer, индексатор раздач (JacRed/Jackett) и сортировка результатов. */
@Composable
internal fun SettingsMobileTorrentContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onPickerRequested: (SettingsMobilePicker) -> Unit,
) {
    val settings = state.torrent
    val onChange = { updated: TorrentSettings -> onEvent(SettingsState.Event.TorrentSettingsChanged(updated)) }
    // Local Network Protection (Android 16+): без разрешения сокеты к 192.168.x.x молча виснут до таймаута.
    val requestLocalNetworkAccess = rememberLocalNetworkAccessRequest()
    SettingsMobileSection {
        settings.warningText(state.torrServerCheck)?.let { warning ->
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        SettingsMobileToggleRow(
            label = stringResource(R.string.settings_torrent_enabled_label),
            hint = stringResource(
                if (settings.enabled) R.string.settings_torrent_enabled_on else R.string.settings_torrent_enabled_off,
            ),
            enabled = settings.enabled,
            onClick = {
                if (!settings.enabled) requestLocalNetworkAccess()
                onChange(settings.copy(enabled = !settings.enabled))
            },
        )
        val hint = stringResource(R.string.settings_torrent_hint)
        val linkStyle = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            text = buildAnnotatedString {
                append(hint)
                addUrlLinks(hint, linkStyle)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (!settings.enabled) return@SettingsMobileSection

        TorrentField(
            value = settings.torrServerUrl,
            label = stringResource(R.string.settings_torrent_server_label),
            placeholder = stringResource(R.string.settings_torrent_server_placeholder),
            keyboardType = KeyboardType.Uri,
            onValueChange = { onChange(settings.copy(torrServerUrl = it.trim())) },
        )
        TorrentField(
            value = settings.torrServerUser,
            label = stringResource(R.string.settings_torrent_user_label),
            onValueChange = { onChange(settings.copy(torrServerUser = it.trim())) },
        )
        TorrentField(
            value = settings.torrServerPassword,
            label = stringResource(R.string.settings_torrent_password_label),
            keyboardType = KeyboardType.Password,
            isSecret = true,
            onValueChange = { onChange(settings.copy(torrServerPassword = it)) },
        )
        if (state.torrServerCheckAvailable) {
            SettingsMobileActionRow(
                label = stringResource(R.string.settings_torrent_check_label),
                hint = state.torrServerCheck.hintText(),
                enabled = settings.torrServerUrl.isNotBlank() && state.torrServerCheck != TorrServerCheckStatus.Checking,
                onClick = {
                    requestLocalNetworkAccess()
                    onEvent(SettingsState.Event.TorrServerCheckRequested)
                },
            )
        }
        SettingsMobileToggleRow(
            label = stringResource(R.string.settings_torrent_jacred_label),
            hint = stringResource(
                if (settings.jacRedEnabled) R.string.settings_torrent_jacred_on else R.string.settings_torrent_jacred_off,
            ),
            enabled = settings.jacRedEnabled,
            onClick = { onChange(settings.copy(jacRedEnabled = !settings.jacRedEnabled)) },
        )
        if (settings.jacRedEnabled) {
            TorrentField(
                value = settings.jacRedUrl,
                label = stringResource(R.string.settings_torrent_jacred_url_label),
                placeholder = TorrentSettings.DEFAULT_INDEXER_URL,
                keyboardType = KeyboardType.Uri,
                onValueChange = { onChange(settings.copy(jacRedUrl = it.trim())) },
            )
        }
        SettingsMobileToggleRow(
            label = stringResource(R.string.settings_torrent_jackett_label),
            hint = stringResource(
                if (settings.jackettEnabled) R.string.settings_torrent_jackett_on else R.string.settings_torrent_jackett_off,
            ),
            enabled = settings.jackettEnabled,
            onClick = { onChange(settings.copy(jackettEnabled = !settings.jackettEnabled)) },
        )
        if (settings.jackettEnabled) {
            TorrentField(
                value = settings.jackettUrl,
                label = stringResource(R.string.settings_torrent_jackett_url_label),
                placeholder = TorrentSettings.JACKETT_URL_HINT,
                keyboardType = KeyboardType.Uri,
                onValueChange = { onChange(settings.copy(jackettUrl = it.trim())) },
            )
            TorrentField(
                value = settings.jackettApiKey,
                label = stringResource(R.string.settings_torrent_jackett_key_label),
                isSecret = true,
                onValueChange = { onChange(settings.copy(jackettApiKey = it.trim())) },
            )
        }
        SettingsMobileOptionRow(
            label = stringResource(R.string.settings_torrent_sort_title),
            value = settings.sort.label(),
            hint = settings.sort.hint(),
            onClick = { onPickerRequested(SettingsMobilePicker.TORRENT_SORT) },
        )
        SettingsMobileToggleRow(
            label = stringResource(R.string.settings_torrent_sort_reversed_label),
            hint = stringResource(
                if (settings.sortReversed) {
                    R.string.settings_torrent_sort_reversed_on
                } else {
                    R.string.settings_torrent_sort_reversed_off
                },
            ),
            enabled = settings.sortReversed,
            onClick = { onChange(settings.copy(sortReversed = !settings.sortReversed)) },
        )
    }
}

@Composable
private fun TorrentField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSecret: Boolean = false,
) {
    OutlinedTextField(
        shape = MaterialTheme.shapes.large,
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
    )
}
