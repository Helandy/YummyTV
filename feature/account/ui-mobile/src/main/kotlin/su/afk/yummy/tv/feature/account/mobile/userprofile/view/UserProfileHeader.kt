package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.feature.account.mobile.userprofile.utils.friendshipActionLabel
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileHeroActionButton
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileProfileHero
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileProfileIdentity
import su.afk.yummy.tv.feature.account.mobile.view.UserProfileActions
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

@Composable
internal fun UserProfileHeader(
    state: UserProfileState.State,
    onEvent: (UserProfileState.Event) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    AccountMobileProfileHero(
        bannerUrl = profile?.bannerUrl.orEmpty(),
        bannerWideUrl = profile?.bannerWideUrl,
        avatarUrl = profile?.avatarUrl.orEmpty(),
        nickname = profile?.nickname.orEmpty(),
        isLoading = profile == null && state.isOverviewLoading,
        modifier = modifier,
        // Шапка и прилипающие вкладки — один блок одного цвета, без стыка.
        bottomCorner = 0.dp,
        navigationAction = {
            AccountMobileHeroActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(CoreR.string.common_back),
                onClick = onBack,
            )
        },
    ) {
        // Пока профиль грузится, имени ещё нет: запасное «Неизвестный пользователь»
        // показываем только по итогам запроса, иначе оно мелькает при каждом открытии.
        AccountMobileProfileIdentity(
            nickname = when {
                profile != null -> profile.nickname.ifBlank { stringResource(R.string.account_unknown_user) }
                !state.isOverviewLoading -> stringResource(R.string.account_unknown_user)
                else -> null
            },
            summary = profile,
        )
        if (profile == null && state.isOverviewLoading) {
            YummyLoadingIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        if (!state.isOwnProfile) {
            Spacer(Modifier.height(8.dp))
            UserProfileActions(
                isAuthorized = state.isAuthorized,
                friendshipStatus = state.friendshipStatus,
                friendshipLoading = state.isFriendshipLoading,
                messageLabel = stringResource(R.string.user_profile_message),
                friendshipLabel = if (state.isAuthorized) {
                    state.friendshipStatus.friendshipActionLabel()
                } else {
                    stringResource(R.string.user_profile_login_to_friend)
                },
                onMessageClick = { onEvent(UserProfileState.Event.MessageSelected) },
                onFriendshipClick = {
                    onEvent(
                        if (state.isAuthorized) {
                            UserProfileState.Event.FriendshipActionSelected
                        } else {
                            UserProfileState.Event.LoginToFriendSelected
                        },
                    )
                },
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
