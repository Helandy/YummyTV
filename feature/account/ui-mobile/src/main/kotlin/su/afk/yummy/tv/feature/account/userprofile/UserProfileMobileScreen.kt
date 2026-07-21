package su.afk.yummy.tv.feature.account.userprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.userprofile.view.UserProfileHeader
import su.afk.yummy.tv.feature.account.userprofile.view.UserProfileListFilters
import su.afk.yummy.tv.feature.account.userprofile.view.UserProfileOverview
import su.afk.yummy.tv.feature.account.userprofile.view.UserProfileTabs
import su.afk.yummy.tv.feature.account.userprofile.view.collectionItems
import su.afk.yummy.tv.feature.account.userprofile.view.friendItems
import su.afk.yummy.tv.feature.account.userprofile.view.postItems
import su.afk.yummy.tv.feature.account.userprofile.view.reviewItems
import su.afk.yummy.tv.feature.account.userprofile.view.userAnimeListItems

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
                    state = state,
                    onEvent = onEvent,
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
