package su.afk.yummy.tv.feature.account.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

internal data class UserProfileListFilterUi(
    val label: String,
    val count: Int?,
    val color: Color,
)

@Composable
internal fun UserProfileState.ListFilter.toMobileListFilterUi(
    counts: UserProfileCounts?,
): UserProfileListFilterUi =
    UserProfileListFilterUi(
        label = label(),
        count = count(counts),
        color = color(),
    )

@Composable
private fun UserProfileState.ListFilter.label(): String = when (this) {
    UserProfileState.ListFilter.WATCHING -> stringResource(R.string.account_profile_list_watching)
    UserProfileState.ListFilter.PLANNED -> stringResource(R.string.account_profile_list_planned)
    UserProfileState.ListFilter.COMPLETED -> stringResource(R.string.account_profile_list_completed)
    UserProfileState.ListFilter.DROPPED -> stringResource(R.string.account_profile_list_dropped)
    UserProfileState.ListFilter.POSTPONED -> stringResource(R.string.account_profile_list_postponed)
    UserProfileState.ListFilter.FAVORITES -> stringResource(R.string.account_profile_list_favorite)
}

private fun UserProfileState.ListFilter.count(counts: UserProfileCounts?): Int? =
    counts?.let {
        when (this) {
            UserProfileState.ListFilter.WATCHING -> it.watching
            UserProfileState.ListFilter.PLANNED -> it.planned
            UserProfileState.ListFilter.COMPLETED -> it.completed
            UserProfileState.ListFilter.DROPPED -> it.dropped
            UserProfileState.ListFilter.POSTPONED -> it.postponed
            UserProfileState.ListFilter.FAVORITES -> it.favorite
        }
    }

private fun UserProfileState.ListFilter.color(): Color = when (this) {
    UserProfileState.ListFilter.WATCHING -> Color(0xFFFF6B6B)
    UserProfileState.ListFilter.PLANNED -> Color(0xFFA678E8)
    UserProfileState.ListFilter.COMPLETED -> Color(0xFF69D38B)
    UserProfileState.ListFilter.DROPPED -> Color(0xFF9CA3AF)
    UserProfileState.ListFilter.POSTPONED -> Color(0xFFFFC857)
    UserProfileState.ListFilter.FAVORITES -> Color(0xFFD86BFF)
}
