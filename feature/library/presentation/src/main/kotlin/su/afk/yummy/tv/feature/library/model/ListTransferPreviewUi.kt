package su.afk.yummy.tv.feature.library.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.domain.library.model.ExternalListFormat

@Immutable
data class ListTransferPreviewUi(
    val format: ExternalListFormat,
    val totalCount: Int,
    val statusCounts: ImmutableList<ListTransferStatusCountUi> = persistentListOf(),
    val ratedCount: Int = 0,
    val withWatchedEpisodesCount: Int = 0,
    val skippedNonAnimeCount: Int = 0,
    val skippedInvalidCount: Int = 0,
)
