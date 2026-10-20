package su.afk.yummy.tv.feature.details.mobile.details

import androidx.compose.foundation.layout.Arrangement
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

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.mobile.NotificationPermissionGateHost
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.model.anime.bestUrl
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.utils.system.shareText
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsInfoBlock
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsInfoSheet
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsMobileHero
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsMobileSkeleton
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsPickerSheets
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsRatingOverviewCard
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsRatingOverviewSkeleton
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsScreenshotsRow
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsSecondaryActions
import su.afk.yummy.tv.feature.details.mobile.details.view.DetailsVideoSection
import su.afk.yummy.tv.feature.details.mobile.details.view.SubscriptionsSignInSheet
import su.afk.yummy.tv.feature.details.presentation.R

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DetailsMobileScreenDefaultPreview() = ScreenPreviewTheme {
    DetailsMobileScreen(DetailsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun DetailsMobileScreenLoadingPreview() = ScreenPreviewTheme {
    DetailsMobileScreen(DetailsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun DetailsMobileScreenErrorPreview() = ScreenPreviewTheme {
    DetailsMobileScreen(
        DetailsState.State(
            isLoading = false,
            error = UiText.Res(R.string.details_load_error)
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DetailsMobileScreen(
    state: DetailsState.State,
    effect: Flow<DetailsState.Effect>,
    onEvent: (DetailsState.Event) -> Unit,
) {
    val details = state.details
    val emptyText = stringResource(R.string.details_mobile_empty)
    val error = state.error?.asString().takeIf { details == null }
    val listState = rememberLazyListState()
    val notificationPermissionGate = rememberNotificationPermissionGate()
    val context = LocalContext.current
    LaunchedEffect(effect) {
        effect.collect { detailsEffect ->
            when (detailsEffect) {
                is DetailsState.Effect.ShareAnime ->
                    context.shareText(text = detailsEffect.url, subject = detailsEffect.title)
            }
        }
    }
    BaseScreen(
        isScroll = false,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        customTopBar = {
            if (details == null) {
                MobileTopBar(
                    title = "",
                    onBack = { onEvent(DetailsState.Event.BackSelected) },
                )
            }
        },
        isLoading = state.isLoading && details == null,
        loadingContent = { DetailsMobileSkeleton() },
        error = error?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(DetailsState.Event.RetrySelected) },
        isEmpty = details == null,
        emptyContent = { MobileMessage(title = emptyText) },
        errorContent = error?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(CoreR.string.common_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        val details = requireNotNull(state.details)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .mobileContentMaxWidth()
                .fillMaxSize()
                .testTag("details_root")
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(key = "hero") {
                DetailsMobileHero(
                    state = state,
                    details = details,
                    onBack = { onEvent(DetailsState.Event.BackSelected) },
                    onShare = { onEvent(DetailsState.Event.ShareSelected) },
                    onPosterClick = { onEvent(DetailsState.Event.PosterClicked) },
                    onInfoClick = { onEvent(DetailsState.Event.InfoSelected) },
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
                    onSubscriptionsLockedSelected = {
                        onEvent(DetailsState.Event.SubscriptionsSignInPromptSelected)
                    },
                    onEpisodesSelected = { onEvent(DetailsState.Event.EpisodesSelected) },
                    onSimilarSelected = { onEvent(DetailsState.Event.SimilarSelected) },
                    onViewingOrderSelected = { onEvent(DetailsState.Event.ViewingOrderSelected) },
                    onCollectionsSelected = { onEvent(DetailsState.Event.CollectionsSelected) },
                    onReviewsSelected = { onEvent(DetailsState.Event.ReviewsSelected) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(key = "description") {
                DetailsInfoBlock(
                    details = details,
                    onGenreSelected = { onEvent(DetailsState.Event.GenreSelected(it)) },
                    onStudioSelected = { id, url -> onEvent(DetailsState.Event.StudioSelected(id, url)) },
                    onDirectorSelected = { onEvent(DetailsState.Event.DirectorSelected(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (details.screenshots.isNotEmpty()) {
                item(key = "screenshots") {
                    DetailsScreenshotsRow(
                        screenshots = details.screenshots,
                        onScreenshotClick = { onEvent(DetailsState.Event.ScreenshotSelected(it)) },
                    )
                }
            }
            item(key = "video") {
                DetailsVideoSection(
                    trailers = state.trailers,
                    bloggerVideosImageModel = details.poster.bestUrl(),
                    isTrailersLoading = state.isExtrasLoading,
                    hasBloggerVideos = state.hasBloggerVideos,
                    onTrailersClick = { onEvent(DetailsState.Event.TrailersSelected) },
                    onBloggerVideosClick = { onEvent(DetailsState.Event.BloggerVideosSelected) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            val ratingOverview = state.ratingOverview
            if (ratingOverview != null) {
                item(key = "rating") {
                    DetailsRatingOverviewCard(
                        overview = ratingOverview,
                        userRating = state.userRating,
                        onClick = { onEvent(DetailsState.Event.RatingScreenSelected) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else if (state.isExtrasLoading) {
                item(key = "rating") {
                    DetailsRatingOverviewSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            item(key = "comments") {
                FilledTonalButton(
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
                        title = error.asString(),
                        actionLabel = stringResource(CoreR.string.common_retry),
                        onAction = { onEvent(DetailsState.Event.RetrySelected) },
                    )
                }
            }
        }
        DetailsPickerSheets(
            state = state,
            onLibraryListSelected = { list ->
                val select = DetailsState.Event.LibraryListSelected(list)
                // О новых сериях «Смотрю» сообщают пуши — разрешение спрашиваем при добавлении.
                if (state.isSignedIn && list == UserAnimeList.WATCHING) {
                    notificationPermissionGate { onEvent(select) }
                } else {
                    onEvent(select)
                }
            },
            onLibraryDismiss = { onEvent(DetailsState.Event.LibraryListPickerDismissed) },
            onSubscriptionToggle = { onEvent(DetailsState.Event.SubscriptionToggled(it)) },
            onSubscriptionsDismiss = { onEvent(DetailsState.Event.SubscriptionsDismissed) },
            onBalancerConfirmed = { onEvent(DetailsState.Event.BalancerConfirmed(it)) },
            onBalancerDismiss = { onEvent(DetailsState.Event.BalancerPickerDismissed) },
            onDubbingSelected = { onEvent(DetailsState.Event.DubbingSelected(it.video)) },
            onDubbingDismiss = { onEvent(DetailsState.Event.DubbingPickerDismissed) },
        )
        NotificationPermissionGateHost(
            state = notificationPermissionGate,
            permissionWasRequested = state.notificationPermissionRequested,
            onPermissionRequested = { onEvent(DetailsState.Event.NotificationPermissionRequested) },
            explanationRes = CoreR.string.notification_permission_explanation_episode_push,
        )
        if (state.showInfoSheet) {
            state.details?.let { details ->
                DetailsInfoSheet(
                    titles = details.otherTitles.filter { it.isNotBlank() },
                    onDismiss = { onEvent(DetailsState.Event.InfoDismissed) },
                )
            }
        }
        if (state.showSubscriptionsSignInPrompt) {
            SubscriptionsSignInSheet(
                onSignIn = { onEvent(DetailsState.Event.SignInSelected) },
                onDismiss = { onEvent(DetailsState.Event.SubscriptionsSignInPromptDismissed) },
            )
        }

    }
}
