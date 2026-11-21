package su.afk.yummy.tv.feature.reviews.mobile.list

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mvi.collectAsEffect
import su.afk.yummy.tv.core.designsystem.paging.getOrNull
import su.afk.yummy.tv.feature.reviews.list.ReviewsListState
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewsPagedList
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewsSortSelector
import su.afk.yummy.tv.feature.reviews.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsListMobileScreen(
    state: ReviewsListState.State,
    effect: Flow<ReviewsListState.Effect>,
    onEvent: (ReviewsListState.Event) -> Unit
) {
    val context = LocalContext.current
    effect.collectAsEffect {
        if (it is ReviewsListState.Effect.ShowToast) {
            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    val sorts = state.availableSorts
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = sorts.indexOf(state.sort).coerceAtLeast(0),
        pageCount = sorts.size.coerceAtLeast(1),
        onPageSelected = { page ->
            sorts.getOrNull(page)?.let { onEvent(ReviewsListState.Event.SortSelected(it)) }
        },
    )

    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.reviews_title),
                onBack = { onEvent(ReviewsListState.Event.BackSelected) },
            )
        },
    ) {
        if (sorts.isNotEmpty()) {
            ReviewsSortSelector(
                sorts = sorts,
                selectedSort = state.sort,
                onSortSelected = { sort -> tabsState.selectPage(sorts.indexOf(sort)) },
                modifier = Modifier.fillMaxWidth(),
                transition = if (sorts.size > 1) tabsState::tabTransition else null,
            )
        }

        if (sorts.size > 1) {
            MobileSwipeableTabsPager(
                state = tabsState,
                modifier = Modifier.weight(1f),
                key = { page -> sorts[page].name },
                beyondViewportPageCount = 1,
            ) { page ->
                val sortFlow = state.reviewsBySort[sorts[page]] ?: state.reviews
                ReviewsPagedList(state, sortFlow.collectAsLazyPagingItems(), onEvent)
            }
        } else {
            ReviewsPagedList(state, state.reviews.collectAsLazyPagingItems(), onEvent)
        }
    }
}
