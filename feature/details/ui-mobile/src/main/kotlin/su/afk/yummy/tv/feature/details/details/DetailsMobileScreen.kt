package su.afk.yummy.tv.feature.details.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.details.details.view.DetailsDescriptionSection
import su.afk.yummy.tv.feature.details.details.view.DetailsMobileHero
import su.afk.yummy.tv.feature.details.details.view.DetailsPickerSheets
import su.afk.yummy.tv.feature.details.details.view.DetailsSecondaryActions
import su.afk.yummy.tv.feature.details.details.view.PosterDialog
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
fun DetailsMobileScreen(
    state: DetailsState.State,
    effect: Flow<DetailsState.Effect>,
    onEvent: (DetailsState.Event) -> Unit,
) {
    val details = state.details
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Box(Modifier.fillMaxSize()) {
            MobileStateContent(
                isLoading = state.isLoading && details == null,
                error = state.error.takeIf { details == null },
                onRetry = { onEvent(DetailsState.Event.RetrySelected) },
                empty = details == null,
                emptyText = stringResource(R.string.details_mobile_empty),
            ) {
                if (details != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        item(key = "hero") {
                            DetailsMobileHero(
                                state = state,
                                details = details,
                                onBack = { onEvent(DetailsState.Event.BackSelected) },
                                onPosterClick = { onEvent(DetailsState.Event.PosterClicked) },
                                onWatchSelected = { onEvent(DetailsState.Event.WatchSelected) },
                                onLibraryToggle = { onEvent(DetailsState.Event.LibraryToggled) },
                                onFavoriteToggle = { onEvent(DetailsState.Event.FavoriteToggled) },
                            )
                        }
                        item(key = "actions") {
                            DetailsSecondaryActions(
                                state = state,
                                details = details,
                                onSubscriptionsSelected = { onEvent(DetailsState.Event.SubscriptionsSelected) },
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

            if (state.showPosterFullscreen && details != null) {
                PosterDialog(
                    details = details,
                    onDismiss = { onEvent(DetailsState.Event.PosterDismissed) },
                )
            }
        }
    }
}
