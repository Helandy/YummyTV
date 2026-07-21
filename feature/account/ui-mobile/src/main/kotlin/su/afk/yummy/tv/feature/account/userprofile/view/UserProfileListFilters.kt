package su.afk.yummy.tv.feature.account.userprofile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.userprofile.utils.tabCountLabel
import su.afk.yummy.tv.feature.account.userprofile.utils.uiMessage
import su.afk.yummy.tv.feature.account.view.AccountMobileEmptyText
import su.afk.yummy.tv.feature.account.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.view.toMobileListFilterUi

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UserProfileListFilters(
    selected: UserProfileState.ListFilter,
    counts: UserProfileCounts?,
    onSelected: (UserProfileState.ListFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UserProfileState.ListFilter.entries.forEach { filter ->
            val item = filter.toMobileListFilterUi(counts)
            val selectedFilter = filter == selected
            FilterChip(
                selected = selectedFilter,
                onClick = { onSelected(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = item.color.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(item.color),
                        )
                        Text(
                            text = item.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        item.count?.let { count ->
                            Badge(
                                modifier = Modifier.sizeIn(minWidth = 18.dp, minHeight = 18.dp),
                                containerColor = item.color.copy(
                                    alpha = if (selectedFilter) 0.28f else 0.18f,
                                ),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Text(
                                    text = count.tabCountLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

internal fun LazyListScope.userAnimeListItems(
    content: UserProfileState.PagedContent<UserAnimeListItem>,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    pagedStateItems(
        content = content,
        onRetry = { onEvent(UserProfileState.Event.RetryTabSelected) },
    )
    items(content.items, key = { "anime_${it.animeId}" }) { item ->
        UserAnimeListRow(
            item = item,
            onClick = { onEvent(UserProfileState.Event.AnimeSelected(item.animeId)) },
        )
    }
}

internal fun LazyListScope.collectionItems(
    content: LazyPagingItems<AnimeCollectionSummary>,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val onRetry = {
        onEvent(UserProfileState.Event.RetryTabSelected)
        content.retry()
    }
    pagedStateItems(content, onRetry)
    items(
        count = content.itemCount,
        key = content.itemKey { "collection_${it.id}" },
    ) { index ->
        content[index]?.let { item ->
            UserCollectionRow(
                item = item,
                onClick = {
                    onEvent(UserProfileState.Event.CollectionSelected(item.id))
                },
            )
        }
    }
    appendStateItem(content, onRetry)
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
                action = stringResource(R.string.user_profile_retry),
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
                action = stringResource(R.string.user_profile_retry),
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
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }

        is LoadState.Error -> item(key = "append_error") {
            UserProfileMessage(
                text = appendState.error.uiMessage(),
                action = stringResource(R.string.user_profile_retry),
                onAction = onRetry,
            )
        }

        is LoadState.NotLoading -> Unit
    }
}
