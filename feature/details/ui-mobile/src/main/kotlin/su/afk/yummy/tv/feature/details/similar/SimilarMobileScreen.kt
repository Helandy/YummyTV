package su.afk.yummy.tv.feature.details.similar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.details.SimilarUiState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.similar.view.SimilarRecommendationsGrid
import su.afk.yummy.tv.feature.details.similar.view.SimilarSourceTabs

private const val SIMILAR_SOURCE_PAGE_COUNT = 2
private const val SIMILAR_SOURCE_SITE_PAGE = 0
private const val SIMILAR_SOURCE_AI_PAGE = 1

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SimilarMobileScreenDefaultPreview() = ScreenPreviewTheme {
    SimilarMobileScreen(SimilarState.State(), emptyFlow()) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SimilarMobileScreen(

    state: SimilarState.State,
    effect: Flow<SimilarState.Effect>,
    onEvent: (SimilarState.Event) -> Unit,

    ) {
    val pagerState = rememberPagerState(
        initialPage = state.fromAi.toSimilarSourcePage(),
        pageCount = { SIMILAR_SOURCE_PAGE_COUNT },
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.fromAi) {
        val targetPage = state.fromAi.toSimilarSourcePage()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val selectedFromAi = pagerState.currentPage.toSimilarSourceFromAi()
        if (selectedFromAi != state.fromAi) {
            onEvent(SimilarState.Event.SourceSelected(selectedFromAi))
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_similar),
                onBack = { onEvent(SimilarState.Event.BackSelected) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SimilarSourceTabs(
                fromAi = pagerState.currentPage.toSimilarSourceFromAi(),
                onSourceSelected = { fromAi ->
                    val targetPage = fromAi.toSimilarSourcePage()
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
                key = { page -> page },
                modifier = Modifier.weight(1f),
            ) { page ->
                val pageFromAi = page.toSimilarSourceFromAi()
                SimilarRecommendationsGrid(
                    similarState = if (pageFromAi == state.fromAi) {
                        state.similarState
                    } else {
                        SimilarUiState.Loading
                    },
                    onAnimeSelected = { id -> onEvent(SimilarState.Event.AnimeSelected(id)) },
                )
            }
        }
    }
}

private fun Boolean.toSimilarSourcePage(): Int =
    if (this) SIMILAR_SOURCE_AI_PAGE else SIMILAR_SOURCE_SITE_PAGE

private fun Int.toSimilarSourceFromAi(): Boolean =
    this == SIMILAR_SOURCE_AI_PAGE
