package su.afk.yummy.tv.feature.details.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.view.DetailsBody
import su.afk.yummy.tv.feature.details.details.view.LibraryListPickerOverlay
import su.afk.yummy.tv.feature.details.details.view.SubscriptionsPickerOverlay
import su.afk.yummy.tv.feature.details.view.common.BalancerPickerOverlay
import su.afk.yummy.tv.feature.details.view.common.DetailsError

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

    val error = state.error
    val details = state.details
    val screenState = when {
        details != null -> DetailsScreenState.Content
        error != null -> DetailsScreenState.Error
        else -> DetailsScreenState.Loading
    }
    Box(modifier = Modifier.fillMaxSize()) {
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
                )
            }
        }

        AnimatedVisibility(
            visible = state.showPosterFullscreen && details != null,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220)),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f, animationSpec = tween(180)),
        ) {
            val closeFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { closeFocusRequester.requestFocus() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = details?.poster?.run { fullsize ?: big ?: medium ?: small },
                    contentDescription = details?.title,
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

private enum class DetailsScreenState { Loading, Error, Content }
