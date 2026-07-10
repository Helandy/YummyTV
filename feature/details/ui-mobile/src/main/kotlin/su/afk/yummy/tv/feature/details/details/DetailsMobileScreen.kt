package su.afk.yummy.tv.feature.details.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.details.details.view.DetailsDescriptionSection
import su.afk.yummy.tv.feature.details.details.view.DetailsMobileHero
import su.afk.yummy.tv.feature.details.details.view.DetailsPickerSheets
import su.afk.yummy.tv.feature.details.details.view.DetailsSecondaryActions
import su.afk.yummy.tv.feature.details.details.view.PosterDialog
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DetailsMobileScreen(
    state: DetailsState.State,
    effect: Flow<DetailsState.Effect>,
    onEvent: (DetailsState.Event) -> Unit,
) {
    val details = state.details
    val emptyText = stringResource(R.string.details_mobile_empty)
    val error = state.error.takeIf { details == null }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    if (details == null) {
        BaseScreen(
            isScroll = false,
            customTopBar = {
                MobileTopBar(
                    title = stringResource(R.string.details_mobile_title),
                    onBack = { onEvent(DetailsState.Event.BackSelected) },
                )
            },
            isLoading = state.isLoading,
            error = error?.let { ErrorItem(title = it, message = it) },
            onRetry = { onEvent(DetailsState.Event.RetrySelected) },
            isEmpty = true,
            emptyContent = { MobileMessage(title = emptyText) },
            errorContent = error?.let { message ->
                { _, retry ->
                    MobileMessage(
                        title = message,
                        actionLabel = stringResource(R.string.details_mobile_retry),
                        onAction = retry,
                    )
                }
            },
        ) {}
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        ) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item(key = "hero") {
                        DetailsMobileHero(
                            state = state,
                            details = details,
                            onBack = { onEvent(DetailsState.Event.BackSelected) },
                            onPosterClick = { onEvent(DetailsState.Event.PosterClicked) },
                            onTitleClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(2)
                                }
                            },
                            onWatchSelected = { onEvent(DetailsState.Event.WatchSelected) },
                            onLibraryToggle = { onEvent(DetailsState.Event.LibraryToggled) },
                            onFavoriteToggle = { onEvent(DetailsState.Event.FavoriteToggled) },
                        )
                    }
                    item(key = "actions") {
                        DetailsSecondaryActions(
                            state = state,
                            details = details,
                            onSubscriptionsSelected = { onEvent(DetailsState.Event.SubscriptionsRouteSelected) },
                            onFullDetailsSelected = { onEvent(DetailsState.Event.FullDetailsSelected) },
                            onEpisodesSelected = { onEvent(DetailsState.Event.EpisodesSelected) },
                            onTrailersSelected = { onEvent(DetailsState.Event.TrailersSelected) },
                            onSimilarSelected = { onEvent(DetailsState.Event.SimilarSelected) },
                            onViewingOrderSelected = { onEvent(DetailsState.Event.ViewingOrderSelected) },
                            onScreenshotsSelected = { onEvent(DetailsState.Event.ScreenshotsSelected) },
                            onRatingScreenSelected = { onEvent(DetailsState.Event.RatingScreenSelected) },
                            onCollectionsSelected = { onEvent(DetailsState.Event.CollectionsSelected) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item(key = "description") {
                        DetailsDescriptionSection(
                            description = details.description,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item(key = "comments") {
                        Button(
                            onClick = { onEvent(DetailsState.Event.CommentsSelected) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.details_mobile_show_comments))
                        }
                    }
                    state.error?.let { error ->
                        item(key = "soft_error") {
                            MobileMessage(
                                title = error,
                                actionLabel = stringResource(R.string.details_mobile_retry),
                                onAction = { onEvent(DetailsState.Event.RetrySelected) },
                            )
                        }
                    }
                }
                DetailsPickerSheets(
                    state = state,
                    onLibraryListSelected = { onEvent(DetailsState.Event.LibraryListSelected(it)) },
                    onLibraryDismiss = { onEvent(DetailsState.Event.LibraryListPickerDismissed) },
                    onSubscriptionToggle = { onEvent(DetailsState.Event.SubscriptionToggled(it)) },
                    onSubscriptionsDismiss = { onEvent(DetailsState.Event.SubscriptionsDismissed) },
                    onBalancerConfirmed = { onEvent(DetailsState.Event.BalancerConfirmed(it)) },
                    onBalancerDismiss = { onEvent(DetailsState.Event.BalancerPickerDismissed) },
                )

                if (state.showPosterFullscreen) {
                    PosterDialog(
                        details = details,
                        onDismiss = { onEvent(DetailsState.Event.PosterDismissed) },
                    )
                }
            }
        }
    }
}
