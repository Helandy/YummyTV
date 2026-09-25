package su.afk.yummy.tv.feature.home

import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.feature.home.utils.hasInitialContent
import su.afk.yummy.tv.feature.home.utils.isFirstScreenSettled
import su.afk.yummy.tv.feature.home.view.HomeAnnouncementDialog
import su.afk.yummy.tv.feature.home.view.HomeDashboard
import su.afk.yummy.tv.feature.home.view.HomeError
import su.afk.yummy.tv.feature.home.view.HomeSupportPromptDialog
import su.afk.yummy.tv.feature.home.view.TvRecommendationActionsDialog

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
    var recommendationActionItem by remember { mutableStateOf<HomeFeedItem?>(null) }

    LaunchedEffect(Unit) {
        onEvent(HomeState.Event.ScreenResumed)
    }

    LaunchedEffect(Unit) {
        effect.collect { event ->
            when (event) {
                is HomeState.Effect.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is HomeState.Effect.OpenUri -> context.openExternalUri(event.uri)

                // На ТВ откат недоступен — показываем обычное уведомление.
                is HomeState.Effect.ShowRecommendationUndo -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currentOnEvent = rememberUpdatedState(onEvent)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnEvent.value(HomeState.Event.ScreenResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onItemSelected: (String, HomeFeedItem) -> Unit = remember(onEvent) {
        { _, item -> item.action.toHomeEventOrNull()?.let(onEvent) }
    }

    ReportDrawnWhen { state.isFirstScreenSettled() }

    val error = state.error
    val feed = state.feed
    when {
        error != null -> HomeError(
            message = error,
            onRetry = { onEvent(HomeState.Event.RetrySelected) },
        )

        feed == null || !state.hasInitialContent() -> TvLoadingScreen()
        else -> HomeDashboard(
            feed = feed,
            continueWatching = state.continueWatching,
            onContinueWatchingSelected = { entry ->
                onEvent(HomeState.Event.ContinueWatchingSelected(entry))
            },
            onItemSelected = onItemSelected,
            onRecommendationLongClick = { item -> recommendationActionItem = item },
        )
    }

    recommendationActionItem?.let { item ->
        TvRecommendationActionsDialog(
            title = item.title,
            onHide = {
                recommendationActionItem = null
                onEvent(HomeState.Event.RecommendationHideRequested(item.id))
            },
            onDismiss = { recommendationActionItem = null },
        )
    }

    if (state.supportPromptVisible) {
        HomeSupportPromptDialog(
            onDismiss = { onEvent(HomeState.Event.SupportPromptDismissed) },
        )
    }

    state.announcement?.let { announcement ->
        HomeAnnouncementDialog(
            title = announcement.title,
            message = announcement.message,
            buttonText = announcement.buttonText,
            onDismiss = { onEvent(HomeState.Event.AnnouncementDismissed) },
        )
    }
}
