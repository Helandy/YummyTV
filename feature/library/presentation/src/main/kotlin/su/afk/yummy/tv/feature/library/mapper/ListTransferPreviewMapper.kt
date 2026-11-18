package su.afk.yummy.tv.feature.library.mapper

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.feature.library.model.ListTransferPreviewUi
import su.afk.yummy.tv.feature.library.model.ListTransferStatusCountUi

private val STATUS_DISPLAY_ORDER = listOf(
    UserAnimeList.WATCHING,
    UserAnimeList.PLANNED,
    UserAnimeList.COMPLETED,
    UserAnimeList.POSTPONED,
    UserAnimeList.DROPPED,
)

internal fun ParsedExternalList.toPreviewUi(): ListTransferPreviewUi {
    val countByStatus = entries.groupingBy { it.list }.eachCount()
    val orderedCounts = STATUS_DISPLAY_ORDER
        .mapNotNull { status ->
            val count = countByStatus[status] ?: 0
            count.takeIf { it > 0 }?.let { ListTransferStatusCountUi(list = status, count = it) }
        }
        .toImmutableList()

    return ListTransferPreviewUi(
        format = format,
        totalCount = entries.size,
        statusCounts = orderedCounts,
        ratedCount = entries.count { it.score != null },
        withWatchedEpisodesCount = entries.count { it.watchedEpisodes > 0 },
        skippedNonAnimeCount = skippedNonAnimeCount,
        skippedInvalidCount = skippedInvalidCount,
    )
}
