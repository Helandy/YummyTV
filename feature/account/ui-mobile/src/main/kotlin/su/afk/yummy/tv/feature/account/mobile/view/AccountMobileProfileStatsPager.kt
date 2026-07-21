@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatSlice
import su.afk.yummy.tv.feature.account.mobile.account.utils.averageRatingLabel
import su.afk.yummy.tv.feature.account.mobile.account.utils.genreCountSlices
import su.afk.yummy.tv.feature.account.mobile.account.utils.listDurationSlices
import su.afk.yummy.tv.feature.account.mobile.account.utils.positiveValueSum
import su.afk.yummy.tv.feature.account.mobile.account.utils.ratingCountSlices
import su.afk.yummy.tv.feature.account.mobile.account.utils.toProfileHoursLabel
import su.afk.yummy.tv.feature.account.mobile.account.utils.watchStatSlices

@Composable
internal fun AccountMobileProfileStatsPager(
    summary: UserProfileSummary,
    stats: UserStats?,
    modifier: Modifier = Modifier,
) {
    val pages = buildList {
        add(
            AccountMobileProfileStatsPageModel(
                title = stringResource(R.string.account_profile_watch_time_title),
                slices = summary.watchStatSlices(),
                valueType = AccountMobileProfileStatsValueType.DURATION,
            )
        )
        stats?.let {
            add(
                AccountMobileProfileStatsPageModel(
                    title = stringResource(R.string.account_profile_list_duration_title),
                    slices = it.listDurationSlices(),
                    valueType = AccountMobileProfileStatsValueType.DURATION,
                )
            )
            add(
                AccountMobileProfileStatsPageModel(
                    title = stringResource(R.string.account_profile_genres_title),
                    slices = it.genreCountSlices(),
                    valueType = AccountMobileProfileStatsValueType.COUNT,
                )
            )
            add(
                AccountMobileProfileStatsPageModel(
                    title = stringResource(
                        R.string.account_profile_ratings_title,
                        it.averageRatingLabel()
                    ),
                    slices = it.ratingCountSlices(),
                    valueType = AccountMobileProfileStatsValueType.COUNT,
                    compactLegend = true,
                )
            )
        }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> pages[page].title },
            pageSpacing = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
        ) { page ->
            AccountMobileProfileStatsPage(page = pages[page])
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 22.dp else 7.dp, 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                                }
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountMobileProfileStatsPage(
    page: AccountMobileProfileStatsPageModel,
    modifier: Modifier = Modifier,
) {
    val positiveSlices = page.slices.filter { it.value > 0L }
    val totalValue = positiveSlices.positiveValueSum()
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (page.slices.isEmpty()) {
            AccountMobileProfileStatsEmptyState()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccountMobileProfileStatsDonutChart(
                    slices = positiveSlices,
                    totalLabel = page.valueType.totalLabel(totalValue),
                    percentLabel = stringResource(R.string.account_profile_percent_full),
                )
                AccountMobileProfileStatsLegend(
                    slices = page.slices,
                    valueType = page.valueType,
                    compact = page.compactLegend,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AccountMobileProfileStatsDonutChart(
    slices: List<AccountMobileProfileStatSlice>,
    totalLabel: String,
    percentLabel: String,
    modifier: Modifier = Modifier,
) {
    val totalValue = slices.positiveValueSum()
    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (totalValue <= 0L) {
                drawCircle(color = Color.White.copy(alpha = 0.10f))
                return@Canvas
            }
            var startAngle = -90f
            slices.forEach { slice ->
                val sweepAngle = 360f * slice.value.toFloat() / totalValue.toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                )
                startAngle += sweepAngle
            }
        }
        Column(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AccountMobileProfileStatsLegend(
    slices: List<AccountMobileProfileStatSlice>,
    valueType: AccountMobileProfileStatsValueType,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        slices.forEach { slice ->
            Row(
                modifier = Modifier.widthIn(
                    min = if (compact) 72.dp else 92.dp,
                    max = if (compact) 92.dp else 150.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(slice.color),
                )
                Text(
                    text = slice.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = valueType.valueLabel(slice.value),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AccountMobileProfileStatsEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.account_profile_stats_empty),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class AccountMobileProfileStatsPageModel(
    val title: String,
    val slices: List<AccountMobileProfileStatSlice>,
    val valueType: AccountMobileProfileStatsValueType,
    val compactLegend: Boolean = false,
)

private enum class AccountMobileProfileStatsValueType {
    DURATION,
    COUNT,
}

@Composable
private fun AccountMobileProfileStatsValueType.totalLabel(value: Long): String =
    when (this) {
        AccountMobileProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        AccountMobileProfileStatsValueType.COUNT -> value.toString()
    }

@Composable
private fun AccountMobileProfileStatsValueType.valueLabel(value: Long): String =
    when (this) {
        AccountMobileProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        AccountMobileProfileStatsValueType.COUNT -> value.toString()
    }
