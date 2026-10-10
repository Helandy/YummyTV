@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.ratingsByValue
import su.afk.yummy.tv.domain.account.model.topGenres
import su.afk.yummy.tv.feature.account.mobile.account.utils.isEmpty
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun AccountMobileStatsContent(
    profileSummary: UserProfileSummary?,
    stats: UserStats?,
) {
    if (profileSummary != null) {
        AccountMobileProfileSummaryPanel(summary = profileSummary, stats = stats)
    } else if (stats != null && !stats.isEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stats.lists.forEach { stat ->
                AccountMobileListStatCard(
                    stat = stat,
                    modifier = Modifier.fillMaxWidth(0.48f),
                )
            }
        }
    }
    val showSecondaryStats = profileSummary == null
    if (showSecondaryStats && stats?.genres?.isNotEmpty() == true) {
        AccountMobileStatSection(
            title = stringResource(R.string.account_stats_genres),
            icon = Icons.Filled.Category,
        ) {
            val topGenres = stats.topGenres
            val max = topGenres.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            topGenres.forEach { genre ->
                AccountMobileStatBar(
                    label = genre.title,
                    valueLabel = genre.count.toString(),
                    fraction = genre.count.toFloat() / max,
                )
            }
        }
    }
    if (showSecondaryStats && stats?.ratings?.isNotEmpty() == true) {
        AccountMobileStatSection(
            title = stringResource(R.string.account_stats_ratings),
            icon = Icons.Filled.Star,
        ) {
            val ratingsByValue = stats.ratingsByValue
            val max = stats.ratings.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            (1..10).forEach { rating ->
                val count = ratingsByValue[rating]?.count ?: 0
                AccountMobileStatBar(
                    label = rating.toString(),
                    valueLabel = count.toString(),
                    fraction = count.toFloat() / max,
                )
            }
        }
    }
}
