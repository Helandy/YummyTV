package su.afk.yummy.tv.feature.account.userprofile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.view.AccountMobileStatsTab

@Composable
internal fun UserProfileOverview(
    state: UserProfileState.State,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.isOverviewLoading && state.profile == null && state.stats == null ->
                AccountMobileLoadingIndicator()

            state.overviewError && state.profile == null && state.stats == null ->
                UserProfileMessage(
                    text = stringResource(R.string.user_profile_load_error),
                    action = stringResource(R.string.user_profile_retry),
                    onAction = { onEvent(UserProfileState.Event.RetryOverviewSelected) },
                )

            else -> AccountMobileStatsTab(
                profileSummary = state.profile,
                stats = state.stats,
                isLoading = state.isOverviewLoading,
            )
        }
    }
}
