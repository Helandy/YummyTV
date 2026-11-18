package su.afk.yummy.tv.feature.library.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.domain.library.model.ExternalListFormat

@Immutable
data class ListTransferExportSummaryUi(
    val format: ExternalListFormat,
    val exportedCount: Int,
    val skippedWithoutExternalIdCount: Int,
    val skippedFavoriteOnlyCount: Int,
    val savedPath: String? = null,
)
