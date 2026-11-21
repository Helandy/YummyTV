package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.time.rememberNowEpochSeconds
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileCollectionsGrid
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileCurrentListPill
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileListFilters
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileListsHidden
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileOverview
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.friendItems
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.postItems
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.reviewItems
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.userAnimeListItems
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

@Composable
internal fun UserProfileTabContent(
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

    if (collections != null) {
        UserProfileCollectionsGrid(collections = collections, onEvent = onEvent)
        return
    }
    val nowEpochSeconds = rememberNowEpochSeconds()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Чипы фильтров — первый элемент списка; когда они уехали, показываем, какой список листаем.
    val filtersScrolledAway by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .mobileContentMaxWidth()
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                UserProfileState.Tab.OVERVIEW -> item(key = "overview") {
                    UserProfileOverview(state = state, onEvent = onEvent)
                }

                UserProfileState.Tab.LISTS -> if (state.listsHidden) {
                    item(key = "lists_hidden") { UserProfileListsHidden() }
                } else {
                    item(key = "list_filters") {
                        UserProfileListFilters(
                            selected = state.selectedList,
                            counts = state.profile?.counts,
                            onSelected = {
                                onEvent(UserProfileState.Event.ListFilterSelected(it))
                            },
                        )
                    }
                    userAnimeListItems(
                        content = state.lists,
                        filter = state.selectedList,
                        showYear = state.showTitleYear,
                        isOwnProfile = state.isOwnProfile,
                        nowEpochSeconds = nowEpochSeconds,
                        onEvent = onEvent,
                    )
                }

                // Коллекции рисуются сеткой выше.
                UserProfileState.Tab.COLLECTIONS -> Unit

                UserProfileState.Tab.POSTS ->
                    posts?.let { postItems(it, onEvent) }

                UserProfileState.Tab.REVIEWS ->
                    reviews?.let { reviewItems(it, onEvent) }

                UserProfileState.Tab.FRIENDS ->
                    friends?.let { friendItems(it, nowEpochSeconds, onEvent) }
            }
        }
        AnimatedVisibility(
            visible = tab == UserProfileState.Tab.LISTS && filtersScrolledAway,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
        ) {
            UserProfileCurrentListPill(
                filter = state.selectedList,
                counts = state.profile?.counts,
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
            )
        }
    }
}
