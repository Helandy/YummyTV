@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.utils.isEmpty
import su.afk.yummy.tv.feature.account.mobile.utils.toDurationLabel

@Composable
internal fun AccountMobileStatsTab(
    stats: UserStats?,
    isLoading: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            isLoading && stats == null -> AccountMobileEmptyText(stringResource(R.string.account_loading))
            stats == null || stats.isEmpty() -> AccountMobileEmptyText(stringResource(R.string.account_stats_empty))
            else -> AccountMobileStatsContent(stats)
        }
    }
}

@Composable
private fun AccountMobileStatsContent(stats: UserStats) {
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
    if (stats.genres.isNotEmpty()) {
        AccountMobileStatSection(title = stringResource(R.string.account_stats_genres)) {
            val topGenres = stats.genres.sortedByDescending { it.count }.take(8)
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
    if (stats.ratings.isNotEmpty()) {
        AccountMobileStatSection(title = stringResource(R.string.account_stats_ratings)) {
            val byRating = stats.ratings.associateBy { it.rating }
            val max = stats.ratings.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            (1..10).forEach { rating ->
                val count = byRating[rating]?.count ?: 0
                AccountMobileStatBar(
                    label = rating.toString(),
                    valueLabel = count.toString(),
                    fraction = count.toFloat() / max,
                )
            }
        }
    }
    if (stats.types.isNotEmpty()) {
        AccountMobileStatSection(title = stringResource(R.string.account_stats_types)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats.types.sortedByDescending { it.count }.forEach { type ->
                    AccountMobileStatPill(
                        title = type.title.ifBlank { type.shortName },
                        value = type.count.toLong().toDurationLabel(),
                        modifier = Modifier.fillMaxWidth(0.48f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccountMobileEmptyText(text: String) {
    AccountMobileSurfacePanel {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
