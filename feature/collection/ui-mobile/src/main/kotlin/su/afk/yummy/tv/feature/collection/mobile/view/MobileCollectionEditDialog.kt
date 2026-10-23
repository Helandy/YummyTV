package su.afk.yummy.tv.feature.collection.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.collection.presentation.R

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
    MobileAlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text(stringResource(R.string.collection_edit_dialog_title)) },
        confirmText = stringResource(R.string.collection_save),
        onConfirm = onConfirm,
        confirmEnabled = title.isNotBlank() && !isUpdating,
        confirmLoading = isUpdating,
        dismissText = stringResource(CoreR.string.common_cancel),
        onDismiss = onDismiss,
        dismissEnabled = !isUpdating,
        content = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large,
                    value = title,
                    onValueChange = onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    singleLine = true,
                    label = { Text(stringResource(R.string.collection_title_label)) },
                )
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large,
                    value = description,
                    onValueChange = onDescriptionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    minLines = 3,
                    maxLines = 5,
                    label = { Text(stringResource(R.string.collection_description_label)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.collection_public_label))
                    Switch(
                        checked = isPublic,
                        onCheckedChange = onPublicChanged,
                        enabled = !isUpdating,
                    )
                }
            }
        },
    )
}
