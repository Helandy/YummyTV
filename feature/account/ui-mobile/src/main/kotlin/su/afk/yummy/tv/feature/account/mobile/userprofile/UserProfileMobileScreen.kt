package su.afk.yummy.tv.feature.account.mobile.userprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileHeader
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileListFilters
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileOverview
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileTabs
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.collectionItems
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.friendItems
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.postItems
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.reviewItems
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.userAnimeListItems
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

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
    val tabs = UserProfileState.Tab.entries
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = tabs.indexOf(state.selectedTab).coerceAtLeast(0),
        pageCount = tabs.size,
        onPageSelected = { page ->
            tabs.getOrNull(page)?.let { onEvent(UserProfileState.Event.TabSelected(it)) }
        },
    )

    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
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
        UserProfileHeader(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        UserProfileTabs(
            selected = state.selectedTab,
            profile = state.profile,
            onSelected = { tab -> tabsState.selectPage(tabs.indexOf(tab)) },
        )
        MobileSwipeableTabsPager(
            state = tabsState,
            modifier = Modifier.weight(1f),
            key = { page -> tabs[page].name },
        ) { page ->
            if (tabs[page] == state.selectedTab) {
                UserProfileTabContent(
                    tab = tabs[page],
                    state = state,
                    onEvent = onEvent,
                )
            } else {
                MobileSectionLoading(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun UserProfileTabContent(
    tab: UserProfileState.Tab,
    state: UserProfileState.State,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val collections = if (tab == UserProfileState.Tab.COLLECTIONS) {
        state.collections.collectAsLazyPagingItems()
    } else {
        null
    }
    val posts = if (tab == UserProfileState.Tab.POSTS) {
        state.posts.collectAsLazyPagingItems()
    } else {
        null
    }
    val reviews = if (tab == UserProfileState.Tab.REVIEWS) {
        state.reviews.collectAsLazyPagingItems()
    } else {
        null
    }
    val friends = if (tab == UserProfileState.Tab.FRIENDS) {
        state.friends.collectAsLazyPagingItems()
    } else {
        null
    }

    LazyColumn(
        modifier = Modifier
            .mobileContentMaxWidth()
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (tab) {
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
