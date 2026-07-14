package su.afk.yummy.tv.feature.home

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.view.HomeDashboard
import su.afk.yummy.tv.feature.home.view.HomeError
import su.afk.yummy.tv.feature.home.view.HomeSupportPromptDialog

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun HomeTvScreenDefaultPreview() = ScreenPreviewTheme {
    HomeTvScreen(
        HomeState.State(isLoading = false, isContinueWatchingLoaded = true),
        emptyFlow()
    ) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun HomeTvScreenLoadingPreview() = ScreenPreviewTheme {
    HomeTvScreen(HomeState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun HomeTvScreenErrorPreview() = ScreenPreviewTheme {
    HomeTvScreen(
        HomeState.State(
            isLoading = false,
            isContinueWatchingLoaded = true,
            error = "Не удалось загрузить главную"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeTvScreen(
    state: HomeState.State,
    effect: Flow<HomeState.Effect>,
    onEvent: (HomeState.Event) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        onEvent(HomeState.Event.ScreenResumed)
    }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is HomeState.Effect.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, onEvent) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onEvent(HomeState.Event.ScreenResumed)
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
