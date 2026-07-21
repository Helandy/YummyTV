package su.afk.yummy.tv.feature.account.userprofile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.account.mobile.utils.formatProfileDate
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.userprofile.utils.friendshipActionLabel
import su.afk.yummy.tv.feature.account.view.AccountMobileAvatar
import su.afk.yummy.tv.feature.account.view.AccountMobileSurfacePanel
import su.afk.yummy.tv.feature.account.view.UserProfileActions

@Composable
internal fun UserProfileHeader(
    state: UserProfileState.State,
    onEvent: (UserProfileState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    AccountMobileSurfacePanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AccountMobileAvatar(
                avatarUrl = profile?.avatarUrl.orEmpty(),
                nickname = profile?.nickname.orEmpty(),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profile?.nickname?.ifBlank {
                        stringResource(R.string.account_unknown_user)
                    } ?: stringResource(R.string.account_unknown_user),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile != null) {
                    Text(
                        text = stringResource(
                            R.string.user_profile_registered,
                            profile.registerDateSeconds.formatProfileDate(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (profile.about.isNotBlank()) {
                        Text(
                            text = profile.about,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (state.isOverviewLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        if (!state.isOwnProfile || state.userId > 0) {
            Spacer(Modifier.height(14.dp))
            UserProfileActions(
                showMessage = !state.isOwnProfile,
                showFriendship = !state.isOwnProfile,
                showComments = state.userId > 0,
                isAuthorized = state.isAuthorized,
                friendshipStatus = state.friendshipStatus,
                friendshipLoading = state.isFriendshipLoading,
                messageLabel = stringResource(R.string.user_profile_message),
                friendshipLabel = if (state.isAuthorized) {
                    state.friendshipStatus.friendshipActionLabel()
                } else {
                    stringResource(R.string.user_profile_login_to_friend)
                },
                commentsLabel = stringResource(R.string.user_profile_comments),
                onMessageClick = { onEvent(UserProfileState.Event.MessageSelected) },
                onFriendshipClick = {
                    onEvent(
                        if (state.isAuthorized) {
                            UserProfileState.Event.FriendshipActionSelected
                        } else {
                            UserProfileState.Event.LoginToFriendSelected
                        }
                    )
                },
                onCommentsClick = { onEvent(UserProfileState.Event.CommentsSelected) },
            )
        }
        if (state.friendshipError) {
            Text(
                text = stringResource(R.string.user_profile_friendship_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
