package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.feature.settings.presentation.R

/** Секция «Торренты»: тумблер, адрес TorrServer и индексатор раздач (JacRed/Jackett). */
@Composable
internal fun SettingsMobileTorrentSection(
    settings: TorrentSettings,
    onChange: (TorrentSettings) -> Unit,
) {
    SettingsMobileSection {
        SettingsMobileToggleRow(
            label = stringResource(R.string.settings_torrent_enabled_label),
            hint = stringResource(
                if (settings.enabled) R.string.settings_torrent_enabled_on else R.string.settings_torrent_enabled_off,
            ),
            enabled = settings.enabled,
            onClick = { onChange(settings.copy(enabled = !settings.enabled)) },
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
        val useJackett = settings.indexer == TorrentIndexer.JACKETT
        SettingsMobileToggleRow(
            label = stringResource(R.string.settings_torrent_jackett_label),
            hint = stringResource(
                if (useJackett) R.string.settings_torrent_jackett_on else R.string.settings_torrent_jackett_off,
            ),
            enabled = useJackett,
            onClick = {
                val next = if (useJackett) TorrentIndexer.JACRED else TorrentIndexer.JACKETT
                onChange(settings.copy(indexer = next))
            },
        )
        TorrentField(
            value = settings.indexerUrl,
            label = stringResource(R.string.settings_torrent_indexer_url_label),
            placeholder = TorrentSettings.DEFAULT_INDEXER_URL,
            keyboardType = KeyboardType.Uri,
            onValueChange = { onChange(settings.copy(indexerUrl = it.trim())) },
        )
        if (useJackett) {
            TorrentField(
                value = settings.jackettApiKey,
                label = stringResource(R.string.settings_torrent_jackett_key_label),
                isSecret = true,
                onValueChange = { onChange(settings.copy(jackettApiKey = it.trim())) },
            )
        }
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
