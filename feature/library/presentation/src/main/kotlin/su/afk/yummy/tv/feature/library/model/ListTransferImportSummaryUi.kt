package su.afk.yummy.tv.feature.library.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.domain.library.model.UnmatchedExternalAnime

@Immutable
data class ListTransferImportSummaryUi(
    val addedCount: Int,
    val updatedCount: Int,
    val skippedExistingCount: Int,
    val failedCount: Int,
    val lookupFailedCount: Int = 0,
    val retryableCount: Int = 0,
    val unmatched: ImmutableList<UnmatchedExternalAnime> = persistentListOf(),
    val wasCancelled: Boolean = false,
) {
    val mutationFailedCount: Int
        get() = (failedCount - lookupFailedCount).coerceAtLeast(0)
}
