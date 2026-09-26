package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.model.ReleaseNotesStatus

/** Шторка «Что нового»: история изменений установленной и предыдущих версий. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReleaseNotesMobileSheet(
    status: ReleaseNotesStatus,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_mobile_release_notes_title),
    ) {
        when (status) {
            ReleaseNotesStatus.Idle, ReleaseNotesStatus.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            ReleaseNotesStatus.Error -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReleaseNotesMessage(stringResource(R.string.settings_mobile_release_notes_error))
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.settings_mobile_release_notes_retry))
                }
            }

            is ReleaseNotesStatus.Loaded -> if (status.items.isEmpty()) {
                ReleaseNotesMessage(
                    text = stringResource(R.string.settings_mobile_release_notes_empty),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(status.items, key = { "release_${it.version}_${it.isPrerelease}" }) { item ->
                        ReleaseNoteMobileItem(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseNotesMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}
