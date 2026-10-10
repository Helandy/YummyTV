package su.afk.yummy.tv.feature.settings.view

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.utils.system.CacheStorageEntry
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.cacheStorageFolderLabel

@Composable
internal fun CacheStorageTvDialog(
    entries: List<CacheStorageEntry>,
    totalBytes: Long,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { closeFocusRequester.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cache_storage_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CacheStorageRow(
                    label = stringResource(R.string.settings_cache_storage_total),
                    size = Formatter.formatShortFileSize(context, totalBytes),
                    emphasized = true,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_cache_storage_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    entries.forEach { entry ->
                        CacheStorageRow(
                            label = cacheStorageFolderLabel(entry.id),
                            size = Formatter.formatShortFileSize(context, entry.sizeBytes),
                            emphasized = false,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh, enabled = !isLoading) {
                Text(
                    stringResource(
                        if (isLoading) {
                            R.string.settings_cache_storage_loading
                        } else {
                            R.string.settings_cache_storage_refresh
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(closeFocusRequester),
            ) {
                Text(stringResource(CoreR.string.common_close))
            }
        },
    )
}

@Composable
private fun CacheStorageRow(
    label: String,
    size: String,
    emphasized: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = size,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
