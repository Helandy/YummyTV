package su.afk.yummy.tv.feature.account.mobile.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.error.uiMessage
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
import su.afk.yummy.tv.core.designsystem.R as CoreR

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
                actions = {
                    IconButton(onClick = { onEvent(FriendsState.Event.UserSearchSelected) }) {
                        Icon(
                            imageVector = Icons.Filled.PersonSearch,
                            contentDescription = stringResource(R.string.account_user_search),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isOwnList) {
                OutlinedButton(
                    onClick = { onEvent(FriendsState.Event.MessagesSelected) },
                    modifier = Modifier
                        .mobileContentMaxWidth()
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.account_notification_type_message))
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val refreshError = (friends.loadState.refresh as? LoadState.Error)
                ?.takeIf { friends.itemCount == 0 }
                ?.error
                ?.uiMessage()
            if (refreshError != null) {
                // Как на остальных экранах: иконка, заголовок, пояснение и «Повторить», а не голая панель.
                MobileMessage(
                    title = refreshError.substringBefore('\n'),
                    description = refreshError.substringAfter('\n', "").ifBlank { null },
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(CoreR.string.common_retry),
                    onAction = friends::retry,
                )
            } else if (friends.loadState.refresh is LoadState.NotLoading && friends.itemCount == 0) {
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
    }
}
