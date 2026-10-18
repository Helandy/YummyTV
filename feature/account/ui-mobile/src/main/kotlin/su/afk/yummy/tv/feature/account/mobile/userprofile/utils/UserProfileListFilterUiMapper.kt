package su.afk.yummy.tv.feature.account.mobile.userprofile.utils

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.feature.account.mobile.userprofile.model.UserProfileListFilterUi
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.common.utils.label

@Composable
internal fun UserProfileState.ListFilter.toMobileListFilterUi(
    counts: UserProfileCounts?,
): UserProfileListFilterUi =
    UserProfileListFilterUi(
        label = label(),
        count = count(counts),
        color = color(),
        icon = icon(),
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
    UserProfileState.ListFilter.WATCHING -> YummySemanticColors.StatusWatching
    UserProfileState.ListFilter.PLANNED -> YummySemanticColors.StatusPlanned
    UserProfileState.ListFilter.COMPLETED -> YummySemanticColors.StatusCompleted
    UserProfileState.ListFilter.DROPPED -> YummySemanticColors.StatusDropped
    UserProfileState.ListFilter.POSTPONED -> YummySemanticColors.StatusPostponed
    UserProfileState.ListFilter.FAVORITES -> YummySemanticColors.StatusFavorite
}

/** Те же иконки, что у тайтлов и в библиотеке ([CardListStatus.icon]); у «Любимого» своего статуса нет. */
private fun UserProfileState.ListFilter.icon(): ImageVector = when (this) {
    UserProfileState.ListFilter.WATCHING -> CardListStatus.WATCHING.icon()
    UserProfileState.ListFilter.PLANNED -> CardListStatus.PLANNED.icon()
    UserProfileState.ListFilter.COMPLETED -> CardListStatus.COMPLETED.icon()
    UserProfileState.ListFilter.DROPPED -> CardListStatus.DROPPED.icon()
    UserProfileState.ListFilter.POSTPONED -> CardListStatus.POSTPONED.icon()
    UserProfileState.ListFilter.FAVORITES -> Icons.Filled.Favorite
}
