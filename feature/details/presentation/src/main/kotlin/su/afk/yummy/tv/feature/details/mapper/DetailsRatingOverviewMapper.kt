package su.afk.yummy.tv.feature.details.mapper

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.model.DetailsListCount
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.feature.details.utils.weightedAverage

private val listOrder = listOf(
    UserAnimeList.WATCHING,
    UserAnimeList.PLANNED,
    UserAnimeList.COMPLETED,
    UserAnimeList.POSTPONED,
    UserAnimeList.DROPPED,
)

/** Собирает сводку для карточки рейтинга; `null`, если показывать нечего. */
internal fun detailsRatingOverview(
    summary: AnimeRatingSummary?,
    listStats: AnimeListStats?,
): DetailsRatingOverview? {
    val countsByRating = summary?.distribution.orEmpty().associate { it.rating to it.count }
    val overview = DetailsRatingOverview(
        average = summary?.weightedAverage(),
        votes = countsByRating.values.sum(),
        distribution = if (summary == null) {
            persistentListOf()
        } else {
            (1..10).map { countsByRating[it] ?: 0 }.toImmutableList()
        },
        listCounts = listOrder
            .map { DetailsListCount(it, listStats?.counts?.get(it.id) ?: 0) }
            .filter { it.count > 0 }
            .toImmutableList(),
    )
    return overview.takeUnless { it.isEmpty }
}
