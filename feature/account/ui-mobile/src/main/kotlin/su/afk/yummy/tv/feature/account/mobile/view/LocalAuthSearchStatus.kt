package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.feature.account.presentation.R

/** Пустой список: либо ещё ищем, либо нет разрешения, либо ТВ так и не отозвался. */
@Composable
internal fun LocalAuthSearchStatus(
    isSearching: Boolean,
    isPermissionDenied: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isSearching) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                YummyLoadingIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.account_local_auth_mobile_searching),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = if (isPermissionDenied) {
                    stringResource(R.string.account_local_auth_mobile_permission_required)
                } else {
                    stringResource(R.string.account_local_auth_discovery_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_local_auth_mobile_search_again))
        }
        // Системный диалог второй раз уже не покажут — единственный путь остаётся через настройки.
        if (isPermissionDenied) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        R.string.account_local_network_permission_open_settings,
                    ),
                )
            }
        }
    }
}
