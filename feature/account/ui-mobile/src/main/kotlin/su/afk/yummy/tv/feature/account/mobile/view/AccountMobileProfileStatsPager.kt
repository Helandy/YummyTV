@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsPageModel
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsValueType
import su.afk.yummy.tv.feature.account.mobile.account.utils.averageRatingLabel
import su.afk.yummy.tv.feature.account.mobile.account.utils.genreCountSlices
import su.afk.yummy.tv.feature.account.mobile.account.utils.listDurationSlices
import su.afk.yummy.tv.feature.account.mobile.account.utils.ratingCountSlices
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
