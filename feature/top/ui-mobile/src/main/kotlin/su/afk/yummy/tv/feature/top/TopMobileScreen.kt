package su.afk.yummy.tv.feature.top

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.view.TopMobileGrid
import su.afk.yummy.tv.feature.top.view.TopMobileTypeTabs

private val topMobileTypes: List<AnimeTopType>
    get() = AnimeTopType.entries

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopMobileScreen(
    state: TopState.State,
    effect: Flow<TopState.Effect>,
    onEvent: (TopState.Event) -> Unit,
) {
    val items = state.items.collectAsLazyPagingItems()
    val pagerState = rememberPagerState(
        initialPage = state.selectedType.toTopTypePage(),
        pageCount = { topMobileTypes.size },
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.selectedType) {
        val targetPage = state.selectedType.toTopTypePage()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val selectedType = pagerState.currentPage.toTopType()
        if (selectedType != state.selectedType) {
            onEvent(TopState.Event.TypeSelected(selectedType))
        }
    }

    BaseScreen(
        isScroll = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            TopMobileTypeTabs(
                selectedType = pagerState.currentPage.toTopType(),
                onTypeSelected = { type ->
                    val targetPage = type.toTopTypePage()
                    if (pagerState.currentPage != targetPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                },
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )

            HorizontalPager(
                state = pagerState,
                key = { page -> page.toTopType().apiValue },
                modifier = Modifier.weight(1f),
            ) { page ->
                val pageType = page.toTopType()
                TopMobileGrid(
                    pagingItems = items,
                    isActive = pageType == state.selectedType,
                    showTitleYear = state.showTitleYear,
                    onAnimeSelected = { id -> onEvent(TopState.Event.AnimeSelected(id)) },
                    onRetry = {
                        onEvent(TopState.Event.RetrySelected)
                        items.retry()
                    },
                )
            }
        }
    }
}

private fun AnimeTopType.toTopTypePage(): Int =
    topMobileTypes.indexOf(this).coerceAtLeast(0)

private fun Int.toTopType(): AnimeTopType =
    topMobileTypes.getOrElse(this) { AnimeTopType.TV }
