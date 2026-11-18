package su.afk.yummy.tv.feature.library.mobile.transfer.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.library.model.AnimeListImportProgress
import su.afk.yummy.tv.domain.library.model.ImportConflictPolicy
import su.afk.yummy.tv.feature.library.model.ListTransferImportSummaryUi
import su.afk.yummy.tv.feature.library.model.ListTransferPreviewUi
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.transfer.ListTransferState
import su.afk.yummy.tv.feature.library.utils.fractionOrNull
import su.afk.yummy.tv.feature.library.utils.hint
import su.afk.yummy.tv.feature.library.utils.label
import su.afk.yummy.tv.feature.library.utils.phaseLabel
import su.afk.yummy.tv.feature.library.utils.transferLabel

@Composable
internal fun ListTransferMobileImportCard(
    state: ListTransferState.State,
    onEvent: (ListTransferState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListTransferMobileSectionCard(
        title = stringResource(R.string.library_transfer_section_import),
        description = stringResource(R.string.library_transfer_import_description),
        modifier = modifier,
    ) {
        val pickLabel = stringResource(
            if (state.importPreview == null) {
                R.string.library_transfer_select_file
            } else {
                R.string.library_transfer_change_file
            },
        )
        OutlinedButton(
            onClick = { onEvent(ListTransferState.Event.PickImportFileClicked) },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.FileOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = pickLabel)
        }

        if (state.isParsingFile) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.library_transfer_parsing),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        state.importPreview?.let { preview ->
            ImportPreviewBlock(preview = preview)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Text(
                text = stringResource(R.string.library_transfer_conflict_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            ImportConflictPolicy.entries.forEach { policy ->
                ListTransferMobileRadioRow(
                    label = policy.label(),
                    hint = policy.hint(),
                    selected = state.importOptions.conflictPolicy == policy,
                    enabled = !state.isBusy,
                    onClick = { onEvent(ListTransferState.Event.ConflictPolicySelected(policy)) },
                )
            }

            ListTransferMobileSwitchRow(
                label = stringResource(R.string.library_transfer_option_scores_label),
                hint = stringResource(R.string.library_transfer_option_scores_hint),
                checked = state.importOptions.importScores,
                enabled = !state.isBusy,
                onCheckedChange = { onEvent(ListTransferState.Event.ImportScoresChanged(it)) },
            )
            ListTransferMobileSwitchRow(
                label = stringResource(R.string.library_transfer_option_episodes_label),
                hint = stringResource(R.string.library_transfer_option_episodes_hint),
                checked = state.importOptions.importWatchedEpisodesLocally,
                enabled = !state.isBusy,
                onCheckedChange = {
                    onEvent(ListTransferState.Event.ImportWatchedEpisodesChanged(it))
                },
            )

            if (state.isImporting) {
                ImportProgressBlock(
                    progress = state.importProgress,
                    isCancelRequested = state.isCancelRequested,
                    onCancel = { onEvent(ListTransferState.Event.CancelImportClicked) },
                )
            } else {
                Button(
                    onClick = { onEvent(ListTransferState.Event.StartImportClicked) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.library_transfer_start_import))
                }
            }
        }

        state.importSummary?.let { summary ->
            ImportSummaryBlock(
                summary = summary,
                isBusy = state.isBusy,
                showUnmatched = state.showUnmatchedDetails,
                onRetryFailed = { onEvent(ListTransferState.Event.RetryFailedImportClicked) },
                onToggleUnmatched = { onEvent(ListTransferState.Event.ToggleUnmatchedDetails) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportPreviewBlock(preview: ListTransferPreviewUi) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.library_transfer_preview_title, preview.totalCount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.library_transfer_preview_format,
                    preview.format.label(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (preview.statusCounts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    preview.statusCounts.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.secondaryContainer,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.library_transfer_status_count,
                                    item.list.transferLabel(),
                                    item.count,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.library_transfer_preview_scores_episodes,
                    preview.ratedCount,
                    preview.withWatchedEpisodesCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (preview.skippedNonAnimeCount > 0) {
                Text(
                    text = stringResource(
                        R.string.library_transfer_preview_skipped_non_anime,
                        preview.skippedNonAnimeCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (preview.skippedInvalidCount > 0) {
                Text(
                    text = stringResource(
                        R.string.library_transfer_preview_skipped_invalid,
                        preview.skippedInvalidCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImportProgressBlock(
    progress: AnimeListImportProgress?,
    isCancelRequested: Boolean,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (progress != null) {
            Text(
                text = progress.phaseLabel(),
                style = MaterialTheme.typography.bodyMedium,
            )
            val fraction = progress.fractionOrNull()
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        OutlinedButton(
            onClick = onCancel,
            enabled = !isCancelRequested,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.library_transfer_cancel))
        }
    }
}

@Composable
private fun ImportSummaryBlock(
    summary: ListTransferImportSummaryUi,
    isBusy: Boolean,
    showUnmatched: Boolean,
    onRetryFailed: () -> Unit,
    onToggleUnmatched: () -> Unit,
) {
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
                    if (summary.wasCancelled) {
                        R.string.library_transfer_import_cancelled_title
                    } else {
                        R.string.library_transfer_import_done_title
                    },
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.library_transfer_import_stats_main,
                    summary.addedCount,
                    summary.updatedCount,
                    summary.skippedExistingCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (summary.unmatched.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.library_transfer_import_stats_unmatched,
                        summary.unmatched.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (summary.lookupFailedCount > 0) {
                Text(
                    text = stringResource(
                        R.string.library_transfer_import_stats_lookup_failed,
                        summary.lookupFailedCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
            }
            if (summary.mutationFailedCount > 0) {
                Text(
                    text = stringResource(
                        R.string.library_transfer_import_stats_failed,
                        summary.mutationFailedCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
            }
            if (summary.retryableCount > 0) {
                OutlinedButton(
                    onClick = onRetryFailed,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.library_transfer_retry_failed,
                            summary.retryableCount,
                        ),
                    )
                }
            }
            if (summary.unmatched.isNotEmpty()) {
                TextButton(onClick = onToggleUnmatched) {
                    Text(
                        text = if (showUnmatched) {
                            stringResource(R.string.library_transfer_unmatched_hide)
                        } else {
                            stringResource(
                                R.string.library_transfer_unmatched_show,
                                summary.unmatched.size,
                            )
                        },
                    )
                }
                if (showUnmatched) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        summary.unmatched.forEach { item ->
                            val displayTitle = item.title.ifBlank {
                                stringResource(
                                    R.string.library_transfer_unmatched_untitled,
                                    item.externalId,
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.library_transfer_unmatched_item,
                                    item.externalId,
                                    displayTitle,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
