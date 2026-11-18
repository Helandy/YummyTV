package su.afk.yummy.tv.feature.library.transfer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
internal fun ListTransferTvImportSection(
    state: ListTransferState.State,
    initialFocusRequester: FocusRequester,
    onEvent: (ListTransferState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isImporting) {
        if (state.isImporting) {
            runCatching { cancelFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .background(colors.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.library_transfer_section_import),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
        )
        Text(
            text = stringResource(R.string.library_transfer_import_description),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "pick_file") {
                ListTransferTvButton(
                    label = stringResource(
                        if (state.importPreview == null) {
                            R.string.library_transfer_select_file
                        } else {
                            R.string.library_transfer_change_file
                        },
                    ),
                    icon = Icons.Outlined.FileOpen,
                    enabled = !state.isBusy,
                    primary = state.importPreview == null,
                    onClick = { onEvent(ListTransferState.Event.PickImportFileClicked) },
                    modifier = Modifier.focusRequester(initialFocusRequester),
                )
            }

            if (state.isParsingFile) {
                item(key = "parsing") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.library_transfer_parsing),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            val preview = state.importPreview
            if (preview != null) {
                item(key = "preview") {
                    TvImportPreviewSummary(preview = preview)
                }
                item(key = "conflict_header") {
                    Text(
                        text = stringResource(R.string.library_transfer_conflict_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(ImportConflictPolicy.entries, key = { "conflict_${it.name}" }) { policy ->
                    ListTransferTvRadioRow(
                        label = policy.label(),
                        hint = policy.hint(),
                        selected = state.importOptions.conflictPolicy == policy,
                        enabled = !state.isBusy,
                        onClick = {
                            onEvent(ListTransferState.Event.ConflictPolicySelected(policy))
                        },
                    )
                }
                item(key = "opt_scores") {
                    ListTransferTvToggleRow(
                        label = stringResource(R.string.library_transfer_option_scores_label),
                        hint = stringResource(R.string.library_transfer_option_scores_hint),
                        checked = state.importOptions.importScores,
                        enabled = !state.isBusy,
                        onToggle = {
                            onEvent(
                                ListTransferState.Event.ImportScoresChanged(
                                    !state.importOptions.importScores,
                                ),
                            )
                        },
                    )
                }
                item(key = "opt_episodes") {
                    ListTransferTvToggleRow(
                        label = stringResource(R.string.library_transfer_option_episodes_label),
                        hint = stringResource(R.string.library_transfer_option_episodes_hint),
                        checked = state.importOptions.importWatchedEpisodesLocally,
                        enabled = !state.isBusy,
                        onToggle = {
                            onEvent(
                                ListTransferState.Event.ImportWatchedEpisodesChanged(
                                    !state.importOptions.importWatchedEpisodesLocally,
                                ),
                            )
                        },
                    )
                }
                item(key = "import_action") {
                    if (state.isImporting) {
                        TvImportProgressBlock(
                            progress = state.importProgress,
                            isCancelRequested = state.isCancelRequested,
                            cancelFocusRequester = cancelFocusRequester,
                            onCancel = { onEvent(ListTransferState.Event.CancelImportClicked) },
                        )
                    } else {
                        ListTransferTvButton(
                            label = stringResource(R.string.library_transfer_start_import),
                            enabled = !state.isBusy,
                            primary = true,
                            onClick = { onEvent(ListTransferState.Event.StartImportClicked) },
                        )
                    }
                }
            }

            val summary = state.importSummary
            if (summary != null) {
                item(key = "import_summary") {
                    TvImportResultBlock(summary = summary)
                }
                if (summary.unmatched.isNotEmpty()) {
                    item(key = "unmatched_toggle") {
                        ListTransferTvButton(
                            label = if (state.showUnmatchedDetails) {
                                stringResource(R.string.library_transfer_unmatched_hide)
                            } else {
                                stringResource(
                                    R.string.library_transfer_unmatched_show,
                                    summary.unmatched.size,
                                )
                            },
                            onClick = { onEvent(ListTransferState.Event.ToggleUnmatchedDetails) },
                        )
                    }
                    if (state.showUnmatchedDetails) {
                        items(
                            items = summary.unmatched,
                            key = { "unmatched_${it.externalId}" },
                        ) { item ->
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvImportPreviewSummary(preview: ListTransferPreviewUi) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
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
                    Text(
                        text = stringResource(
                            R.string.library_transfer_status_count,
                            item.list.transferLabel(),
                            item.count,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSecondaryContainer,
                        modifier = Modifier
                            .background(colors.secondaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
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

@Composable
private fun TvImportProgressBlock(
    progress: AnimeListImportProgress?,
    isCancelRequested: Boolean,
    cancelFocusRequester: FocusRequester,
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
        ListTransferTvButton(
            label = stringResource(R.string.library_transfer_cancel),
            enabled = !isCancelRequested,
            onClick = onCancel,
            modifier = Modifier.focusRequester(cancelFocusRequester),
        )
    }
}

@Composable
private fun TvImportResultBlock(summary: ListTransferImportSummaryUi) {
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
                if (summary.wasCancelled) {
                    R.string.library_transfer_import_cancelled_title
                } else {
                    R.string.library_transfer_import_done_title
                },
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
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
        if (summary.failedCount > 0) {
            Text(
                text = stringResource(
                    R.string.library_transfer_import_stats_failed,
                    summary.failedCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }
    }
}
