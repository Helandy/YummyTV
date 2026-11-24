package su.afk.yummy.tv.feature.library.mobile.transfer.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.feature.library.model.ListTransferExportSummaryUi
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.transfer.ListTransferState
import su.afk.yummy.tv.feature.library.utils.hint
import su.afk.yummy.tv.feature.library.utils.label

@Composable
internal fun ListTransferMobileExportCard(
    state: ListTransferState.State,
    onEvent: (ListTransferState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListTransferMobileSectionCard(
        title = stringResource(R.string.library_transfer_section_export),
        description = stringResource(R.string.library_transfer_export_description),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.library_transfer_export_format_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        ExternalListFormat.entries.forEach { format ->
            ListTransferMobileRadioRow(
                label = format.label(),
                hint = format.hint(),
                selected = state.exportFormat == format,
                enabled = !state.isBusy,
                onClick = { onEvent(ListTransferState.Event.ExportFormatSelected(format)) },
            )
        }

        if (state.isExporting) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.library_transfer_exporting),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onEvent(ListTransferState.Event.ExportSaveClicked) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.library_transfer_export_save))
                }
                OutlinedButton(
                    onClick = { onEvent(ListTransferState.Event.ExportShareClicked) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.library_transfer_export_share))
                }
            }
        }

        state.exportSummary?.let { summary ->
            ExportSummaryBlock(summary = summary)
        }
    }
}

@Composable
private fun ExportSummaryBlock(summary: ListTransferExportSummaryUi) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.library_transfer_export_done_title,
                    summary.exportedCount,
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            summary.savedPath?.let { path ->
                Text(
                    text = stringResource(R.string.library_transfer_export_saved_path, path),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (summary.skippedWithoutExternalIdCount > 0) {
                Text(
                    text = stringResource(
                        R.string.library_transfer_export_skipped_no_id,
                        summary.skippedWithoutExternalIdCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (summary.skippedFavoriteOnlyCount > 0) {
                Text(
                    text = stringResource(
                        R.string.library_transfer_export_skipped_favorites,
                        summary.skippedFavoriteOnlyCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
