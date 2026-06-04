@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserRatingStat
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun RatingStats(ratings: List<UserRatingStat>) {
    StatSection(title = stringResource(R.string.account_stats_ratings)) {
        val byRating = ratings.associateBy { it.rating }
        val max = ratings.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        (1..10).forEach { rating ->
            val count = byRating[rating]?.count ?: 0
            StatBar(label = rating.toString(), valueLabel = count.toString(), fraction = count.toFloat() / max)
        }
    }
}
