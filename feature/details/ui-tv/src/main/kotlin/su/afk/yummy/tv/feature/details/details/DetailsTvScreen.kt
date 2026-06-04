package su.afk.yummy.tv.feature.details.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.view.common.BalancerPickerOverlay
import su.afk.yummy.tv.feature.details.view.common.DetailsError

@Composable
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

    val error = state.error
    val details = state.details
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && details == null -> TvLoadingScreen()
            error != null && details == null -> DetailsError(
                message = error,
                onRetry = { onEvent(DetailsState.Event.RetrySelected) },
            )
            details != null -> DetailsBody(
                details = details,
                videosState = state.videosState,
                isWatchLoading = state.isWatchLaunchPending || state.videosState is VideosUiState.Loading,
                watchProgress = state.watchProgress,
                isInLibrary = state.isInLibrary,
                isFavorite = state.isFavorite,
                libraryList = state.libraryList,
                collections = state.collections,
                canSubscribe = state.isSignedIn && state.subscriptions.isNotEmpty(),
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
            )
            else -> TvLoadingScreen()
        }

        if (state.showPosterFullscreen && details != null) {
            val closeFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { closeFocusRequester.requestFocus() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = details.poster?.run { fullsize ?: big ?: medium ?: small },
                    contentDescription = details.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .focusRequester(closeFocusRequester)
                        .tvFocusableClick(
                            onClick = { onEvent(DetailsState.Event.PosterDismissed) },
                            shape = CircleShape,
                        )
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.details_close),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
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
