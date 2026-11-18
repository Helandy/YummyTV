package su.afk.yummy.tv.feature.library.mapper

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.domain.library.model.AnimeListExportResult
import su.afk.yummy.tv.domain.library.model.AnimeListImportResult
import su.afk.yummy.tv.feature.library.model.ListTransferExportSummaryUi
import su.afk.yummy.tv.feature.library.model.ListTransferImportSummaryUi

internal fun AnimeListImportResult.toSummaryUi(): ListTransferImportSummaryUi =
    ListTransferImportSummaryUi(
        addedCount = addedCount,
        updatedCount = updatedCount,
        skippedExistingCount = skippedExistingCount,
        failedCount = failedCount,
        unmatched = unmatched.toImmutableList(),
        wasCancelled = wasCancelled,
    )

internal fun AnimeListExportResult.toSummaryUi(savedPath: String? = null): ListTransferExportSummaryUi =
    ListTransferExportSummaryUi(
        format = format,
        exportedCount = exportedCount,
        skippedWithoutExternalIdCount = skippedWithoutExternalIdCount,
        skippedFavoriteOnlyCount = skippedFavoriteOnlyCount,
        savedPath = savedPath,
    )
