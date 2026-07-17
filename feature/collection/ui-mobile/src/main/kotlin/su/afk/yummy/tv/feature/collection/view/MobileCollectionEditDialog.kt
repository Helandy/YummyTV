package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.collection.mobile.R

@Composable
internal fun MobileCollectionEditDialog(
    title: String,
    description: String,
    isPublic: Boolean,
    isUpdating: Boolean,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPublicChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text(stringResource(R.string.collection_edit_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    singleLine = true,
                    label = { Text(stringResource(R.string.collection_create_title_label)) },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    minLines = 3,
                    maxLines = 5,
                    label = { Text(stringResource(R.string.collection_create_description_label)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.collection_create_public_label))
                    Switch(
                        checked = isPublic,
                        onCheckedChange = onPublicChanged,
                        enabled = !isUpdating,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = title.isNotBlank() && !isUpdating) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.collection_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text(stringResource(R.string.collection_create_cancel))
            }
        },
    )
}
