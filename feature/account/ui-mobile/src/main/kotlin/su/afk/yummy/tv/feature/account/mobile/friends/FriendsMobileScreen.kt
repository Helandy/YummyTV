package su.afk.yummy.tv.feature.account.mobile.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.paging.getOrNull
import su.afk.yummy.tv.core.designsystem.paging.safeItemKey
import su.afk.yummy.tv.core.designsystem.time.rememberNowEpochSeconds
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.account.friends.FriendsState
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserFriendRow
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.appendStateItem
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.pagedStateItems
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
fun FriendsMobileScreen(
    state: FriendsState.State,
    effect: Flow<FriendsState.Effect>,
    onEvent: (FriendsState.Event) -> Unit,
) {
    val friends = state.friends.collectAsLazyPagingItems()
    val nowEpochSeconds = rememberNowEpochSeconds()

    BaseScreen(
        isScroll = false,
        contentModifier = Modifier.navigationBarsPadding(),
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.account_profile_social_friends),
                onBack = { onEvent(FriendsState.Event.BackSelected) },
            )
        },
    ) {
        if (friends.loadState.refresh is LoadState.NotLoading && friends.itemCount == 0) {
            MobileMessage(
                title = stringResource(R.string.friends_empty_title),
                description = stringResource(R.string.friends_empty_description),
                icon = Icons.Filled.Groups,
                actionLabel = stringResource(R.string.account_user_search),
                onAction = { onEvent(FriendsState.Event.UserSearchSelected) },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pagedStateItems(friends, onRetry = friends::retry)
                items(
                    count = friends.itemCount,
                    key = friends.safeItemKey("friend") { lazyKey("friend", it.id) },
                ) { index ->
                    friends.getOrNull(index)?.let { item ->
                        UserFriendRow(
                            item = item,
                            nowEpochSeconds = nowEpochSeconds,
                            onClick = { onEvent(FriendsState.Event.FriendSelected(item.id)) },
                        )
                    }
                }
                appendStateItem(friends, onRetry = friends::retry)
            }
        }
    }
}
