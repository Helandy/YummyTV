@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.hasAny
import su.afk.yummy.tv.feature.account.mobile.account.utils.isEmpty

@Composable
internal fun AccountMobileStatsTab(
    profileSummary: UserProfileSummary?,
    stats: UserStats?,
    isLoading: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            isLoading && stats == null && profileSummary == null -> AccountMobileLoadingIndicator()
            stats == null && profileSummary == null -> AccountMobileEmptyText(stringResource(R.string.account_stats_empty))
            else -> AccountMobileStatsContent(profileSummary = profileSummary, stats = stats)
        }
    }
}

@Composable
private fun AccountMobileStatsContent(
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
    if (showSecondaryStats && stats?.ratings?.isNotEmpty() == true) {
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
}

@Composable
private fun AccountMobileProfileSummaryPanel(
    summary: UserProfileSummary,
    stats: UserStats?,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AccountMobileProfileStatsPager(summary = summary, stats = stats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountMobileDaysOnlineTile(daysOnline = summary.daysOnline)
                AccountMobileProfileHeatmap(
                    history = summary.watchHistory,
                    modifier = Modifier.weight(1f),
                )
            }
            if (summary.counts.hasAny()) {
                AccountMobileProfileListCounters(counts = summary.counts)
            }
            if (summary.about.isNotBlank()) {
                Text(
                    text = summary.about,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AccountMobileDaysOnlineTile(daysOnline: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.32f)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = daysOnline.coerceAtLeast(0).toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
        )
        Text(
            text = stringResource(R.string.account_profile_days_online),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun AccountMobileEmptyText(
    text: String,
    modifier: Modifier = Modifier,
) {
    AccountMobileSurfacePanel(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
