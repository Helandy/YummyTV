package su.afk.yummy.tv.feature.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.model.DetailsListCount

class DetailsRatingOverviewMapperTest : BaseUnitTest() {

    @Test
    fun `builds average, votes, full distribution and non-empty lists`() {
        val overview = detailsRatingOverview(
            summary = AnimeRatingSummary(
                distribution = listOf(AnimeRatingBucket(10, 3), AnimeRatingBucket(6, 1)),
            ),
            listStats = AnimeListStats(
                counts = mapOf(UserAnimeList.COMPLETED.id to 5, UserAnimeList.WATCHING.id to 2, UserAnimeList.DROPPED.id to 0),
            ),
        )

        requireNotNull(overview)
        assertEquals(9.0, overview.average!!, 0.001)
        assertEquals(4, overview.votes)
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 3), overview.distribution)
        assertEquals(
            listOf(DetailsListCount(UserAnimeList.WATCHING, 2), DetailsListCount(UserAnimeList.COMPLETED, 5)),
            overview.listCounts,
        )
    }

    @Test
    fun `nothing to show gives null`() {
        assertNull(detailsRatingOverview(summary = null, listStats = null))
        assertNull(detailsRatingOverview(summary = AnimeRatingSummary(), listStats = AnimeListStats()))
    }
}
