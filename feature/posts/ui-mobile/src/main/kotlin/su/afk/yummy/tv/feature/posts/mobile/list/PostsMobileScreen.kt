package su.afk.yummy.tv.feature.posts.mobile.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSortDropdown
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileHideOnScrollState
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTextTabs
import su.afk.yummy.tv.core.designsystem.paging.getOrNull
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.feature.posts.list.PostsListState
import su.afk.yummy.tv.feature.posts.mobile.view.PostsList
import su.afk.yummy.tv.feature.posts.presentation.R
import su.afk.yummy.tv.feature.posts.utils.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsMobileScreen(
    state: PostsListState.State,
    effect: Flow<PostsListState.Effect>,
    onEvent: (PostsListState.Event) -> Unit,
) {
    val sorts = PostSort.entries
    // Страница 0 — «Все», дальше по порядку категории.
    val selectedPage = state.selectedCategory
        ?.let { uri -> state.categories.indexOfFirst { it.uri == uri } + 1 }
        ?.coerceAtLeast(0)
        ?: 0
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = selectedPage,
        pageCount = state.categories.size + 1,
        onPageSelected = { page ->
            onEvent(PostsListState.Event.CategorySelected(state.categories.getOrNull(page - 1)?.uri))
        },
    )
    val chipsScrollState = rememberMobileHideOnScrollState()
    BaseScreen(
        isScroll = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .nestedScroll(chipsScrollState.nestedScrollConnection),
        ) {
            AnimatedVisibility(visible = chipsScrollState.isVisible) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MobileTextTabs(
                        titles = listOf(stringResource(R.string.posts_all)) + state.categories.map { it.title },
                        selectedIndex = selectedPage,
                        onSelected = tabsState::selectPage,
                        modifier = Modifier.weight(1f),
                        scrollable = true,
                        transition = tabsState::tabTransition,
                    )
                    MobileSortDropdown(
                        items = sorts,
                        selected = state.sort,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { onEvent(PostsListState.Event.SortSelected(it)) },
                        modifier = Modifier.padding(end = 8.dp),
                        compact = true,
                    )
                }
            }
            MobileSwipeableTabsPager(
                state = tabsState,
                modifier = Modifier.weight(1f),
                key = { page -> state.categories.getOrNull(page - 1)?.uri ?: "all" },
                // Соседние категории прогреваются заранее, чтобы при переходе не мелькал лоадер.
                beyondViewportPageCount = 1,
            ) { page ->
                // У каждой страницы своя лента: уходящая страница остаётся списком, а не спиннером.
                val category = state.categories.getOrNull(page - 1)?.uri
                val pagePosts = remember(category, state.sort, state.feeds) {
                    state.feeds?.get(category, state.sort) ?: state.posts
                }.collectAsLazyPagingItems()
                PostsList(pagePosts, onEvent)
            }
        }
    }
}
