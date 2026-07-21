@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.mobile.R

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
