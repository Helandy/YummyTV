package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileChipTab
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileChipTabs
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.error.uiMessage
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.feature.account.mobile.userprofile.utils.toMobileListFilterUi
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileEmptyText
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.common.utils.isEmpty
import su.afk.yummy.tv.feature.account.common.utils.label

/** Фильтры списков — те же компактные вкладки-чипы, что в библиотеке. */
@Composable
internal fun UserProfileListFilters(
    selected: UserProfileState.ListFilter,
    counts: UserProfileCounts?,
    onSelected: (UserProfileState.ListFilter) -> Unit,
) {
    MobileChipTabs(
        tabs = UserProfileState.ListFilter.entries.map { filter ->
            val item = filter.toMobileListFilterUi(counts)
            MobileChipTab(
                key = filter,
                title = item.label,
                icon = item.icon,
                count = item.count,
                color = item.color,
            )
        },
        selectedKey = selected,
        onSelected = onSelected,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

internal fun LazyListScope.userAnimeListItems(
    content: UserProfileState.PagedContent<UserAnimeListItem>,
    filter: UserProfileState.ListFilter,
    showYear: Boolean,
    isOwnProfile: Boolean,
    nowEpochSeconds: Long,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    pagedStateItems(
        content = content,
        onRetry = { onEvent(UserProfileState.Event.RetryTabSelected) },
    )
    items(content.items, key = { "anime_${it.animeId}" }) { item ->
        UserAnimeListRow(
            item = item,
            filter = filter,
            showYear = showYear,
            isOwnProfile = isOwnProfile,
            nowEpochSeconds = nowEpochSeconds,
            onClick = { onEvent(UserProfileState.Event.AnimeSelected(item.animeId)) },
        )
    }
}

internal fun LazyListScope.postItems(
    content: LazyPagingItems<UserPostSummary>,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val onRetry = {
        onEvent(UserProfileState.Event.RetryTabSelected)
        content.retry()
    }
    pagedStateItems(content, onRetry)
    items(
        count = content.itemCount,
        key = content.itemKey { "post_${it.id}" },
    ) { index ->
        content[index]?.let { item ->
            UserPostRow(
                item = item,
                onClick = { onEvent(UserProfileState.Event.PostSelected(item.id)) },
            )
        }
    }
    appendStateItem(content, onRetry)
}

internal fun LazyListScope.reviewItems(
    content: LazyPagingItems<UserReviewSummary>,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val onRetry = {
        onEvent(UserProfileState.Event.RetryTabSelected)
        content.retry()
    }
    pagedStateItems(content, onRetry)
    items(
        count = content.itemCount,
        key = content.itemKey { "review_${it.id}" },
    ) { index ->
        content[index]?.let { item ->
            UserReviewRow(
                item = item,
                onClick = { onEvent(UserProfileState.Event.ReviewSelected(item.id)) },
            )
        }
    }
    appendStateItem(content, onRetry)
}

internal fun LazyListScope.friendItems(
    content: LazyPagingItems<UserFriend>,
    nowEpochSeconds: Long,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val onRetry = {
        onEvent(UserProfileState.Event.RetryTabSelected)
        content.retry()
    }
    pagedStateItems(content, onRetry)
    items(
        count = content.itemCount,
        key = content.itemKey { "friend_${it.id}" },
    ) { index ->
        content[index]?.let { item ->
            UserFriendRow(
                item = item,
                nowEpochSeconds = nowEpochSeconds,
                onClick = { onEvent(UserProfileState.Event.FriendSelected(item.id)) },
            )
        }
    }
    appendStateItem(content, onRetry)
}

internal fun <T> LazyListScope.pagedStateItems(
    content: UserProfileState.PagedContent<T>,
    onRetry: () -> Unit,
) {
    when {
        content.isLoading -> item(key = "loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                AccountMobileLoadingIndicator()
            }
        }

        content.error && content.items.isEmpty() -> item(key = "error") {
            UserProfileMessage(
                text = stringResource(R.string.user_profile_section_error),
                action = stringResource(CoreR.string.common_retry),
                onAction = onRetry,
            )
        }

        content.loaded && content.items.isEmpty() -> item(key = "empty") {
            AccountMobileEmptyText(
                text = stringResource(R.string.user_profile_section_empty),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

internal fun <T : Any> LazyListScope.pagedStateItems(
    content: LazyPagingItems<T>,
    onRetry: () -> Unit,
) {
    val refreshState = content.loadState.refresh
    when {
        refreshState is LoadState.Loading -> item(key = "loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                AccountMobileLoadingIndicator()
            }
        }

        refreshState is LoadState.Error && content.itemCount == 0 -> item(key = "error") {
            UserProfileMessage(
                text = refreshState.error.uiMessage(),
                action = stringResource(CoreR.string.common_retry),
                onAction = onRetry,
            )
        }

        refreshState !is LoadState.Loading && content.itemCount == 0 -> item(key = "empty") {
            AccountMobileEmptyText(
                text = stringResource(R.string.user_profile_section_empty),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

internal fun <T : Any> LazyListScope.appendStateItem(
    content: LazyPagingItems<T>,
    onRetry: () -> Unit,
) {
    when (val appendState = content.loadState.append) {
        is LoadState.Loading -> item(key = "loading_more") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                YummyLoadingIndicator()
            }
        }

        is LoadState.Error -> item(key = "append_error") {
            UserProfileMessage(
                text = appendState.error.uiMessage(),
                action = stringResource(CoreR.string.common_retry),
                onAction = onRetry,
            )
        }

        is LoadState.NotLoading -> Unit
    }
}
