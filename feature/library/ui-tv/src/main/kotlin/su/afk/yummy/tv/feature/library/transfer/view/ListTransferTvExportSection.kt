package su.afk.yummy.tv.feature.library.transfer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
internal fun ListTransferTvExportSection(
    state: ListTransferState.State,
    onEvent: (ListTransferState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.library_transfer_section_export),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
        )

        // На стандартном ТВ-экране (960×540dp) радиокнопки, кнопка сохранения и итог экспорта
        // не помещаются в обычную Column — без прокрутки нижняя кнопка схлопывалась по высоте.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentPadding = PaddingValues(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "export_description") {
                Text(
                    text = stringResource(R.string.library_transfer_export_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            item(key = "export_format_label") {
                Text(
                    text = stringResource(R.string.library_transfer_export_format_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            items(ExternalListFormat.entries, key = { "export_format_${it.name}" }) { format ->
                ListTransferTvRadioRow(
                    label = format.label(),
                    hint = format.hint(),
                    selected = state.exportFormat == format,
                    enabled = !state.isBusy,
                    onClick = { onEvent(ListTransferState.Event.ExportFormatSelected(format)) },
                )
            }

            item(key = "export_action") {
                if (state.isExporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.library_transfer_exporting),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    ListTransferTvButton(
                        label = stringResource(R.string.library_transfer_export_save),
                        icon = Icons.Outlined.SaveAlt,
                        enabled = !state.isBusy,
                        primary = true,
                        onClick = { onEvent(ListTransferState.Event.ExportSaveClicked) },
                    )
                }
            }

            val summary = state.exportSummary
            if (summary != null) {
                item(key = "export_summary") {
                    TvExportResultBlock(summary = summary)
                }
            }
        }
    }
}

@Composable
private fun TvExportResultBlock(summary: ListTransferExportSummaryUi) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
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
