@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toPersistentList
import su.afk.yummy.tv.core.designsystem.mobile.tabs.asMobileTabTransition
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.common.utils.averageRatingLabel
import su.afk.yummy.tv.feature.account.common.utils.genreAllSlices
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsChartType
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsPageModel
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsValueType
import su.afk.yummy.tv.feature.account.mobile.account.utils.listDurationSlices
import su.afk.yummy.tv.feature.account.mobile.account.utils.ratingCountSlices
import su.afk.yummy.tv.feature.account.mobile.account.utils.watchStatSlices
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun AccountMobileProfileStatsPager(
    summary: UserProfileSummary,
    stats: UserStats?,
    modifier: Modifier = Modifier,
    isStatsLoading: Boolean = false,
) {
    val pages = buildList {
        add(
            AccountMobileProfileStatsPageModel(
                title = stringResource(R.string.account_profile_watch_time_title),
                icon = Icons.Filled.Schedule,
                slices = summary.watchStatSlices(),
                valueType = AccountMobileProfileStatsValueType.DURATION,
                legendColumns = 1,
            ),
        )
        stats?.let {
            add(
                AccountMobileProfileStatsPageModel(
                    title = stringResource(R.string.account_profile_list_duration_title),
                    icon = Icons.Filled.BarChart,
                    slices = it.listDurationSlices(),
                    valueType = AccountMobileProfileStatsValueType.DURATION,
                    legendColumns = 1,
                ),
            )
            add(
                AccountMobileProfileStatsPageModel(
                    title = stringResource(R.string.account_profile_genres_title),
                    icon = Icons.Filled.Category,
                    slices = it.genreAllSlices(stringResource(R.string.account_profile_genres_other))
                        .toPersistentList(),
                    valueType = AccountMobileProfileStatsValueType.COUNT,
                    legendColumns = 1,
                ),
            )
            add(
                AccountMobileProfileStatsPageModel(
                    title = stringResource(
                        R.string.account_profile_ratings_title,
                        it.averageRatingLabel(),
                    ),
                    icon = Icons.Filled.Star,
                    slices = it.ratingCountSlices(),
                    valueType = AccountMobileProfileStatsValueType.COUNT,
                    legendColumns = 3,
                    chartType = AccountMobileProfileStatsChartType.HISTOGRAM,
                ),
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
            pageSpacing = 10.dp,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            AccountMobileProfileStatsPage(page = pages[page])
        }
        // Остальные страницы появятся, когда придёт статистика: до этого показываем загрузку.
        if (isStatsLoading && stats == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (pages.size > 1) {
            val statsTransition = pagerState.asMobileTabTransition()
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.forEachIndexed { index, _ ->
                    val fraction = statsTransition.selectionFraction(index)
                    val widthDp = INACTIVE_DOT_WIDTH_DP + (ACTIVE_DOT_WIDTH_DP - INACTIVE_DOT_WIDTH_DP) * fraction
                    Box(
                        modifier = Modifier
                            .size(widthDp.dp, DOT_HEIGHT)
                            .clip(CircleShape)
                            .background(lerp(inactiveColor, activeColor, fraction)),
                    )
                }
            }
        }
    }
}

private const val INACTIVE_DOT_WIDTH_DP = 7f
private const val ACTIVE_DOT_WIDTH_DP = 22f
private val DOT_HEIGHT = 7.dp
