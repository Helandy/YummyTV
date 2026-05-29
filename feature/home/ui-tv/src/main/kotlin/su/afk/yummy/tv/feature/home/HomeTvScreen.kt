package su.afk.yummy.tv.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.view.HomeContent
import su.afk.yummy.tv.feature.home.view.HomeError

@Composable
fun HomeTvScreen(
    state: HomeState.State,
    effect: Flow<HomeState.Effect>,
    onEvent: (HomeState.Event) -> Unit,
) {
    val onItemSelected: (HomeFeedItem) -> Unit = remember(onEvent) {
        { item ->
            when (val action = item.action) {
                is HomeFeedItemAction.OpenSeries -> onEvent(HomeState.Event.AnimeSelected(action.seriesId))
                is HomeFeedItemAction.OpenVideo -> onEvent(HomeState.Event.VideoSelected(action.videoId))
                is HomeFeedItemAction.OpenCollection -> onEvent(HomeState.Event.CollectionSelected(action.collectionId))
            }
        }
    }
    val onItemFocused: (Int, Int?) -> Unit = remember(onEvent) {
        { displayId, animeId -> onEvent(HomeState.Event.ItemFocused(displayId, animeId)) }
    }
    val onHeroItemVisible: (Int) -> Unit = remember(onEvent) {
        { displayId -> onEvent(HomeState.Event.HeroItemVisible(displayId)) }
    }

    val error = state.error
    val feed = state.feed
    val isInitialContentReady = feed != null && state.isContinueWatchingLoaded
    when {
        error != null -> HomeError(
            message = error,
            onRetry = { onEvent(HomeState.Event.RetrySelected) },
        )
        state.isLoading || !isInitialContentReady -> TvLoadingScreen()
        else -> HomeContent(
            feed = feed,
            continueWatching = state.continueWatching,
            onContinueWatchingSelected = { entry ->
                onEvent(HomeState.Event.ContinueWatchingSelected(entry))
            },
            onItemSelected = onItemSelected,
            onItemFocused = onItemFocused,
            onHeroItemVisible = onHeroItemVisible,
            focusedItemId = state.focusedItemId,
            focusedPreview = state.focusedPreview,
            animePreviews = state.animePreviews,
            continueWatchingRestoreToken = state.continueWatchingRestoreToken,
        )
    }
}
