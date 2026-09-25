package su.afk.yummy.tv.feature.details.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.feature.details.details.model.DetailsScreenState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.details.view.DetailsBody
import su.afk.yummy.tv.feature.details.details.view.LibraryListPickerOverlay
import su.afk.yummy.tv.feature.details.details.view.SubscriptionsPickerOverlay
import su.afk.yummy.tv.feature.details.view.common.BalancerPickerOverlay
import su.afk.yummy.tv.feature.details.view.common.DetailsError
import su.afk.yummy.tv.feature.details.view.common.DubbingPickerOverlay

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun DetailsTvScreenDefaultPreview() = ScreenPreviewTheme {
    DetailsTvScreen(DetailsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun DetailsTvScreenLoadingPreview() = ScreenPreviewTheme {
    DetailsTvScreen(DetailsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun DetailsTvScreenErrorPreview() = ScreenPreviewTheme {
    DetailsTvScreen(
        DetailsState.State(isLoading = false, error = "Не удалось загрузить детали"),
        emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DetailsTvScreen(

    state: DetailsState.State,
    effect: Flow<DetailsState.Effect>,
    onEvent: (DetailsState.Event) -> Unit,

    ) {
    var restoreButtonFocusRequest by remember { mutableIntStateOf(0) }
    fun restoreButtonFocus() {
        restoreButtonFocusRequest += 1
    }

    fun dismissBalancerPicker() {
        onEvent(DetailsState.Event.BalancerPickerDismissed)
        restoreButtonFocus()
    }

    fun dismissLibraryListPicker() {
        onEvent(DetailsState.Event.LibraryListPickerDismissed)
        restoreButtonFocus()
    }

    fun dismissDubbingPicker() {
        onEvent(DetailsState.Event.DubbingPickerDismissed)
        restoreButtonFocus()
    }

    val error = state.error
    val details = state.details
    val screenState = when {
        details != null -> DetailsScreenState.Content
        error != null -> DetailsScreenState.Error
        else -> DetailsScreenState.Loading
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("details_root"),
    ) {
        Crossfade(
            targetState = screenState,
            animationSpec = tween(durationMillis = 260),
            label = "detailsScreenState",
        ) { target ->
            when (target) {
                DetailsScreenState.Loading -> TvLoadingScreen()
                DetailsScreenState.Error -> DetailsError(
                    message = error.orEmpty(),
                    onRetry = { onEvent(DetailsState.Event.RetrySelected) },
                )

                DetailsScreenState.Content -> if (details != null) DetailsBody(
                    details = details,
                    videosState = state.videosState,
                    isWatchLoading = state.isWatchLaunchPending || state.videosState is VideosUiState.Loading,
                    watchProgress = state.watchProgress,
                    isInLibrary = state.isInLibrary,
                    isFavorite = state.isFavorite,
                    libraryList = state.libraryList,
                    canSubscribe = state.isSignedIn,
                    detailsButtonOrder = state.detailsButtonOrder,
                    restoreButtonFocusRequest = restoreButtonFocusRequest,
                    onWatchSelected = { onEvent(DetailsState.Event.WatchSelected) },
                    onLibraryToggle = { onEvent(DetailsState.Event.LibraryToggled) },
                    onFavoriteToggle = { onEvent(DetailsState.Event.FavoriteToggled) },
                    onSubscriptionsSelected = { onEvent(DetailsState.Event.SubscriptionsSelected) },
                    onFullDetailsSelected = { onEvent(DetailsState.Event.FullDetailsSelected) },
                    onEpisodesSelected = { onEvent(DetailsState.Event.EpisodesSelected) },
                    onTrailersSelected = { onEvent(DetailsState.Event.TrailersSelected) },
                    onSimilarSelected = { onEvent(DetailsState.Event.SimilarSelected) },
                    onViewingOrderSelected = { onEvent(DetailsState.Event.ViewingOrderSelected) },
                    onScreenshotsSelected = { onEvent(DetailsState.Event.ScreenshotsSelected) },
                    onRatingScreenSelected = { onEvent(DetailsState.Event.RatingScreenSelected) },
                    onCollectionsSelected = { onEvent(DetailsState.Event.CollectionsSelected) },
                    onCommentsSelected = { onEvent(DetailsState.Event.CommentsSelected) },
                    onReviewsSelected = { onEvent(DetailsState.Event.ReviewsSelected) },
                    onBloggerVideosSelected = { onEvent(DetailsState.Event.BloggerVideosSelected) },
                )
            }
        }

        val balancerPicker = state.pendingBalancerSelection
        BackHandler(enabled = balancerPicker != null) {
            dismissBalancerPicker()
        }
        if (balancerPicker != null) {
            BalancerPickerOverlay(
                picker = balancerPicker,
                onConfirmed = { option -> onEvent(DetailsState.Event.BalancerConfirmed(option.video)) },
                onDismiss = ::dismissBalancerPicker,
            )
        }

        val dubbingPicker = state.pendingDubbingSelection
        BackHandler(enabled = dubbingPicker != null) {
            dismissDubbingPicker()
        }
        if (dubbingPicker != null) {
            DubbingPickerOverlay(
                selection = dubbingPicker,
                onSelected = { option -> onEvent(DetailsState.Event.DubbingSelected(option.video)) },
                onDismiss = ::dismissDubbingPicker,
            )
        }

        BackHandler(enabled = state.showLibraryListPicker) {
            dismissLibraryListPicker()
        }
        if (state.showLibraryListPicker) {
            LibraryListPickerOverlay(
                onConfirmed = { list ->
                    onEvent(DetailsState.Event.LibraryListSelected(list))
                    restoreButtonFocus()
                },
                onDismiss = ::dismissLibraryListPicker,
            )
        }

        BackHandler(enabled = state.showSubscriptionsPicker) {
            onEvent(DetailsState.Event.SubscriptionsDismissed)
            restoreButtonFocus()
        }
        if (state.showSubscriptionsPicker) {
            SubscriptionsPickerOverlay(
                subscriptions = state.subscriptions,
                isLoading = state.isSubscriptionsLoading,
                onToggle = { key -> onEvent(DetailsState.Event.SubscriptionToggled(key)) },
                onDismiss = {
                    onEvent(DetailsState.Event.SubscriptionsDismissed)
                    restoreButtonFocus()
                },
            )
        }
    }
}
