package su.afk.yummy.tv.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.view.HomeDashboard
import su.afk.yummy.tv.feature.home.view.HomeError
import su.afk.yummy.tv.feature.home.view.HomeSupportPromptDialog

@Composable
fun HomeTvScreen(
    state: HomeState.State,
    effect: Flow<HomeState.Effect>,
    onEvent: (HomeState.Event) -> Unit,
) {
    LaunchedEffect(Unit) {
        onEvent(HomeState.Event.ScreenResumed)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, onEvent) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onEvent(HomeState.Event.ScreenResumed)
                onEvent(HomeState.Event.RefreshRequested)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onItemSelected: (String, HomeFeedItem) -> Unit = remember(onEvent) {
        { _, item ->
            when (val action = item.action) {
                is HomeFeedItemAction.OpenSeries -> onEvent(
                    HomeState.Event.AnimeSelected(seriesId = action.seriesId),
                )

                is HomeFeedItemAction.OpenVideo -> Unit
                is HomeFeedItemAction.OpenCollection -> onEvent(
                    HomeState.Event.CollectionSelected(collectionId = action.collectionId),
                )
            }
        }
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
        else -> HomeDashboard(
            feed = feed,
            continueWatching = state.continueWatching,
            onContinueWatchingSelected = { entry ->
                onEvent(HomeState.Event.ContinueWatchingSelected(entry))
            },
            onItemSelected = onItemSelected,
        )
    }

    if (state.supportPromptVisible) {
        HomeSupportPromptDialog(
            onDismiss = { onEvent(HomeState.Event.SupportPromptDismissed) },
        )
    }
}
