package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.utils.label
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun LibraryListDialog(
    onSelected: (UserAnimeList) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        UserAnimeList.WATCHING,
        UserAnimeList.PLANNED,
        UserAnimeList.COMPLETED,
        UserAnimeList.POSTPONED,
        UserAnimeList.DROPPED,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_mobile_library_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    OutlinedButton(
                        onClick = { onSelected(option) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option.label())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_mobile_cancel))
            }
        },
    )
}
