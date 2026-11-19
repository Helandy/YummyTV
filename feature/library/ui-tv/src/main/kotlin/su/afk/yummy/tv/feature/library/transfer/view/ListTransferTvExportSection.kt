package su.afk.yummy.tv.feature.library.transfer.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.TvFocusedGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.feature.library.model.ListTransferExportSummaryUi
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.transfer.ListTransferState
import su.afk.yummy.tv.feature.library.utils.hint
import su.afk.yummy.tv.feature.library.utils.label

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ListTransferTvExportSection(
    state: ListTransferState.State,
    onEvent: (ListTransferState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.exportSummary) {
        if (state.exportSummary != null) {
            val summaryIndex = EXPORT_HEADER_ITEMS_COUNT + ExternalListFormat.entries.size + 1
            if (summaryIndex < listState.layoutInfo.totalItemsCount) {
                listState.animateScrollToItem(summaryIndex)
            }
        }
    }

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
        CompositionLocalProvider(LocalBringIntoViewSpec provides TvFocusedGridBringIntoViewSpec) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(top = 2.dp, bottom = 6.dp),
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

                itemsIndexed(
                    items = ExternalListFormat.entries,
                    key = { _, format -> "export_format_${format.name}" },
                ) { index, format ->
                    ListTransferTvRadioRow(
                        label = format.label(),
                        hint = format.hint(),
                        selected = state.exportFormat == format,
                        enabled = !state.isBusy,
                        onClick = { onEvent(ListTransferState.Event.ExportFormatSelected(format)) },
                        modifier = Modifier
                            .tvWholeItemBringIntoView()
                            .then(
                                if (index == 0) {
                                    Modifier.onFocusChanged { focusState ->
                                        val isScrolled = listState.firstVisibleItemIndex > 0 ||
                                            listState.firstVisibleItemScrollOffset > 0
                                        if (focusState.hasFocus && isScrolled) {
                                            scope.launch { listState.animateScrollToItem(0) }
                                        }
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }

                item(key = "export_action") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvWholeItemBringIntoView(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ListTransferTvButton(
                            label = stringResource(R.string.library_transfer_export_save),
                            icon = Icons.Outlined.SaveAlt,
                            enabled = !state.isBusy,
                            primary = true,
                            retainFocusWhenDisabled = state.isExporting,
                            onClick = { onEvent(ListTransferState.Event.ExportSaveClicked) },
                        )
                        if (state.isExporting) {
                            ListTransferTvStatusRow(
                                text = stringResource(R.string.library_transfer_exporting),
                            )
                        }
                    }
                }

                val summary = state.exportSummary
                if (summary != null) {
                    item(key = "export_summary") {
                        TvExportResultBlock(
                            summary = summary,
                            modifier = Modifier.tvWholeItemBringIntoView(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvExportResultBlock(
    summary: ListTransferExportSummaryUi,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    ListTransferTvResultCard(modifier = modifier) {
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

private const val EXPORT_HEADER_ITEMS_COUNT = 2
