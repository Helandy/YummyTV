package su.afk.yummy.tv.feature.details.mobile.similar

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mvi.collectAsEffect
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.details.model.SimilarUiState
import su.afk.yummy.tv.feature.details.mobile.similar.utils.SIMILAR_SOURCE_PAGE_COUNT
import su.afk.yummy.tv.feature.details.mobile.similar.utils.toSimilarSourceFromAi
import su.afk.yummy.tv.feature.details.mobile.similar.utils.toSimilarSourcePage
import su.afk.yummy.tv.feature.details.mobile.similar.view.MobileSimilarRecommendationVisibilityButton
import su.afk.yummy.tv.feature.details.mobile.similar.view.SimilarRecommendationsGrid
import su.afk.yummy.tv.feature.details.mobile.similar.view.SimilarSourceTabs
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.similar.SimilarState

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
    val context = LocalContext.current
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = state.fromAi.toSimilarSourcePage(),
        pageCount = SIMILAR_SOURCE_PAGE_COUNT,
        onPageSelected = { page ->
            val selectedFromAi = page.toSimilarSourceFromAi()
            if (selectedFromAi != state.fromAi) {
                onEvent(SimilarState.Event.SourceSelected(selectedFromAi))
            }
        },
    )

    effect.collectAsEffect { event ->
        when (event) {
            is SimilarState.Effect.ShowToast ->
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_similar),
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
                fromAi = state.fromAi,
                onSourceSelected = { fromAi -> tabsState.selectPage(fromAi.toSimilarSourcePage()) },
                transition = tabsState::tabTransition,
            )

            MobileSimilarRecommendationVisibilityButton(
                ignored = state.isRecommendationIgnored,
                enabled = !state.isRecommendationMutationPending,
                onClick = {
                    onEvent(SimilarState.Event.RecommendationVisibilityToggled)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            MobileSwipeableTabsPager(
                state = tabsState,
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
                    onVote = { id, vote ->
                        onEvent(SimilarState.Event.VoteSelected(id, vote))
                    },
                    pendingVoteAnimeIds = state.pendingVoteAnimeIds,
                    showVoting = !pageFromAi,
                    onRetry = { onEvent(SimilarState.Event.RetrySelected) },
                )
            }
        }
    }
}
