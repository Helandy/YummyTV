package su.afk.yummy.tv.feature.account.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTitleListCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.feature.account.account.mobile.utils.formatProfileDate
import su.afk.yummy.tv.feature.account.account.mobile.utils.formatUserListDate
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.view.AccountMobileAvatar
import su.afk.yummy.tv.feature.account.view.AccountMobileEmptyText
import su.afk.yummy.tv.feature.account.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.view.AccountMobileStatsTab
import su.afk.yummy.tv.feature.account.view.AccountMobileSurfacePanel
import su.afk.yummy.tv.feature.account.view.toMobileListFilterUi

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UserProfileMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        UserProfileMobileScreen(UserProfileState.State(isOverviewLoading = false), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun UserProfileMobileScreenLoadingPreview() = ScreenPreviewTheme {
    UserProfileMobileScreen(UserProfileState.State(isOverviewLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun UserProfileMobileScreenErrorPreview() = ScreenPreviewTheme {
    UserProfileMobileScreen(
        UserProfileState.State(isOverviewLoading = false, overviewError = true),
        emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserProfileMobileScreen(
    state: UserProfileState.State,
    effect: Flow<UserProfileState.Effect>,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val listState = rememberLazyListState()
    val collections = if (state.selectedTab == UserProfileState.Tab.COLLECTIONS) {
        state.collections.collectAsLazyPagingItems()
    } else {
        null
    }
    val posts = if (state.selectedTab == UserProfileState.Tab.POSTS) {
        state.posts.collectAsLazyPagingItems()
    } else {
        null
    }
    val reviews = if (state.selectedTab == UserProfileState.Tab.REVIEWS) {
        state.reviews.collectAsLazyPagingItems()
    } else {
        null
    }
    val friends = if (state.selectedTab == UserProfileState.Tab.FRIENDS) {
        state.friends.collectAsLazyPagingItems()
    } else {
        null
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = state.profile?.nickname?.ifBlank {
                    stringResource(R.string.user_profile_title)
                } ?: stringResource(R.string.user_profile_title),
                onBack = { onEvent(UserProfileState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                UserProfileHeader(
                    profile = state.profile,
                    isLoading = state.isOverviewLoading,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item(key = "tabs") {
                UserProfileTabs(
                    selected = state.selectedTab,
                    profile = state.profile,
                    onSelected = { onEvent(UserProfileState.Event.TabSelected(it)) },
                )
            }
            when (state.selectedTab) {
                UserProfileState.Tab.OVERVIEW -> item(key = "overview") {
                    UserProfileOverview(state = state, onEvent = onEvent)
                }

                UserProfileState.Tab.LISTS -> {
                    item(key = "list_filters") {
                        UserProfileListFilters(
                            selected = state.selectedList,
                            counts = state.profile?.counts,
                            onSelected = {
                                onEvent(UserProfileState.Event.ListFilterSelected(it))
                            },
                        )
                    }
                    userAnimeListItems(state.lists, onEvent)
                }

                UserProfileState.Tab.COLLECTIONS ->
                    collections?.let { collectionItems(it, onEvent) }

                UserProfileState.Tab.POSTS ->
                    posts?.let { postItems(it, onEvent) }

                UserProfileState.Tab.REVIEWS ->
                    reviews?.let { reviewItems(it, onEvent) }

                UserProfileState.Tab.FRIENDS ->
                    friends?.let { friendItems(it, onEvent) }
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    profile: UserProfileSummary?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
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
                } else if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UserProfileTabs(
    selected: UserProfileState.Tab,
    profile: UserProfileSummary?,
    onSelected: (UserProfileState.Tab) -> Unit,
) {
    val tabs = UserProfileState.Tab.entries
    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selected),
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        tabs.forEach { tab ->
            val count = tab.count(profile)
            Tab(
                selected = tab == selected,
                onClick = { onSelected(tab) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = tab.label(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (count != null) {
                            Badge(
                                modifier = Modifier.sizeIn(minWidth = 18.dp, minHeight = 18.dp),
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

@Composable
private fun UserProfileOverview(
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UserProfileListFilters(
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

private fun androidx.compose.foundation.lazy.LazyListScope.userAnimeListItems(
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

private fun androidx.compose.foundation.lazy.LazyListScope.collectionItems(
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
        content[index]?.let { item -> UserCollectionRow(item = item) }
    }
    appendStateItem(content, onRetry)
}

private fun androidx.compose.foundation.lazy.LazyListScope.postItems(
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
        content[index]?.let { item -> UserPostRow(item = item) }
    }
    appendStateItem(content, onRetry)
}

private fun androidx.compose.foundation.lazy.LazyListScope.reviewItems(
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
                onClick = { onEvent(UserProfileState.Event.AnimeSelected(item.animeId)) },
            )
        }
    }
    appendStateItem(content, onRetry)
}

private fun androidx.compose.foundation.lazy.LazyListScope.friendItems(
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

private fun <T> androidx.compose.foundation.lazy.LazyListScope.pagedStateItems(
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

private fun <T : Any> androidx.compose.foundation.lazy.LazyListScope.pagedStateItems(
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

private fun <T : Any> androidx.compose.foundation.lazy.LazyListScope.appendStateItem(
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

@Composable
private fun UserAnimeListRow(item: UserAnimeListItem, onClick: () -> Unit) {
    val rating = item.userRating
        ?.takeIf { it in 1..10 }
        ?.toDouble()
    val addedDate = item.updatedAtSeconds
        ?.formatUserListDate()
        ?.takeIf { it.isNotBlank() }

    MobileTitleListCard(
        title = item.title.ifBlank { stringResource(R.string.user_profile_untitled) },
        posterUrl = item.posterUrl,
        dateText = addedDate,
        rating = rating,
        modifier = Modifier
            .padding(horizontal = 16.dp),
        onClick = onClick,
    )
}

@Composable
private fun UserCollectionRow(item: AnimeCollectionSummary) {
    UserProfileMediaRow(
        imageUrl = item.posterUrl,
        title = item.title,
        subtitle = item.description,
        onClick = null,
    )
}

@Composable
private fun UserPostRow(item: UserPostSummary) {
    UserProfileMediaRow(
        imageUrl = item.previewImageUrl,
        title = item.title,
        subtitle = item.contentPreview.ifBlank { item.categoryTitle },
        onClick = null,
    )
}

@Composable
private fun UserReviewRow(item: UserReviewSummary, onClick: () -> Unit) {
    UserProfileMediaRow(
        imageUrl = item.animePosterUrl,
        title = item.animeTitle.ifBlank { stringResource(R.string.user_profile_review) },
        subtitle = item.textPreview,
        onClick = onClick.takeIf { item.animeId > 0 },
    )
}

@Composable
private fun UserFriendRow(item: UserFriend, onClick: () -> Unit) {
    AccountMobileSurfacePanel(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.nickname.ifBlank { stringResource(R.string.account_unknown_user) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.status.isNotBlank()) {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileMediaRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    AccountMobileSurfacePanel(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(clickModifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title.ifBlank { stringResource(R.string.user_profile_untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileMessage(
    text: String,
    action: String,
    onAction: () -> Unit,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(onClick = onAction, label = { Text(action) })
        }
    }
}

@Composable
private fun UserProfileState.Tab.label(): String = when (this) {
    UserProfileState.Tab.OVERVIEW -> stringResource(R.string.user_profile_tab_overview)
    UserProfileState.Tab.LISTS -> stringResource(R.string.user_profile_tab_lists)
    UserProfileState.Tab.COLLECTIONS -> stringResource(R.string.user_profile_tab_collections)
    UserProfileState.Tab.POSTS -> stringResource(R.string.user_profile_tab_posts)
    UserProfileState.Tab.REVIEWS -> stringResource(R.string.user_profile_tab_reviews)
    UserProfileState.Tab.FRIENDS -> stringResource(R.string.user_profile_tab_friends)
}

private fun UserProfileState.Tab.count(profile: UserProfileSummary?): Int? {
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

private fun Int.tabCountLabel(): String =
    if (this > 999) "999+" else coerceAtLeast(0).toString()

private fun Throwable.uiMessage(): String = message ?: localizedMessage ?: toString()
