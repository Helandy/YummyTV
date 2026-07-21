package su.afk.yummy.tv.feature.account.userprofile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

@Composable
internal fun FriendshipStatus.friendshipActionLabel(): String = stringResource(
    when (this) {
        FriendshipStatus.NONE -> R.string.user_profile_add_friend
        FriendshipStatus.FOLLOWERS,
        FriendshipStatus.REQUESTS -> R.string.user_profile_accept_friend

        FriendshipStatus.FOLLOWING,
        FriendshipStatus.SENT_REQUESTS -> R.string.user_profile_cancel_friend_request

        FriendshipStatus.FRIENDS -> R.string.user_profile_remove_friend
    }
)

@Composable
internal fun UserProfileState.Tab.label(): String = when (this) {
    UserProfileState.Tab.OVERVIEW -> stringResource(R.string.user_profile_tab_overview)
    UserProfileState.Tab.LISTS -> stringResource(R.string.user_profile_tab_lists)
    UserProfileState.Tab.COLLECTIONS -> stringResource(R.string.user_profile_tab_collections)
    UserProfileState.Tab.POSTS -> stringResource(R.string.user_profile_tab_posts)
    UserProfileState.Tab.REVIEWS -> stringResource(R.string.user_profile_tab_reviews)
    UserProfileState.Tab.FRIENDS -> stringResource(R.string.user_profile_tab_friends)
}

internal fun UserProfileState.Tab.count(profile: UserProfileSummary?): Int? {
    if (profile == null) return null
    return when (this) {
        UserProfileState.Tab.OVERVIEW -> null
        UserProfileState.Tab.LISTS -> with(profile.counts) {
            watching + planned + completed + dropped + postponed + favorite
        }

        UserProfileState.Tab.COLLECTIONS -> profile.socialCounts.collections
        UserProfileState.Tab.POSTS -> profile.socialCounts.posts
        UserProfileState.Tab.REVIEWS -> profile.socialCounts.reviews
        UserProfileState.Tab.FRIENDS -> profile.socialCounts.friends
    }
}
