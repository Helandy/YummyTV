package su.afk.yummy.tv.feature.home.mobile

import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.components.InfoBanner
import su.afk.yummy.tv.core.designsystem.components.OfflineBanner
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.feature.home.HomeState
import su.afk.yummy.tv.feature.home.mapper.toHomeEventOrNull
import su.afk.yummy.tv.feature.home.mobile.view.ContinueWatchingSection
import su.afk.yummy.tv.feature.home.mobile.view.HomeAnnouncementDialog
import su.afk.yummy.tv.feature.home.mobile.view.HomeContinueWatchingActionsSheet
import su.afk.yummy.tv.feature.home.mobile.view.HomeFeedSectionRow
import su.afk.yummy.tv.feature.home.mobile.view.HomeHeroCarousel
import su.afk.yummy.tv.feature.home.mobile.view.HomeMobileSearchBar
import su.afk.yummy.tv.feature.home.mobile.view.HomeMobileSkeleton
import su.afk.yummy.tv.feature.home.mobile.view.HomeCommunityPromptDialog
import su.afk.yummy.tv.feature.home.mobile.view.HomeQuickActionsSection
import su.afk.yummy.tv.feature.home.mobile.view.HomeRecommendationActionsSheet
import su.afk.yummy.tv.feature.home.mobile.view.HomeSupportPromptDialog
import su.afk.yummy.tv.feature.home.mobile.view.MobileHomeBloggerVideosSection
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.home.utils.hasInitialContent
import su.afk.yummy.tv.feature.home.utils.isFirstScreenSettled
import su.afk.yummy.tv.core.designsystem.R as CoreR

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        HomeMobileScreen(
            HomeState.State(isLoading = false, isContinueWatchingLoaded = true),
            emptyFlow(),
        ) {}
    }

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun HomeMobileScreenLoadingPreview() = ScreenPreviewTheme {
    HomeMobileScreen(HomeState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun HomeMobileScreenErrorPreview() = ScreenPreviewTheme {
    HomeMobileScreen(
        HomeState.State(
            isLoading = false,
            isContinueWatchingLoaded = true,
            error = UiText.Res(R.string.home_load_error),
        ),
        emptyFlow(),
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeMobileScreen(
    state: HomeState.State,
    effect: Flow<HomeState.Effect>,
    onEvent: (HomeState.Event) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.home_mobile_recommendation_undo)
    var recommendationActionItem by remember { mutableStateOf<HomeFeedItem?>(null) }
    var continueWatchingActionEntry by remember { mutableStateOf<HomeContinueWatchingItem?>(null) }

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

                is HomeState.Effect.ShowRecommendationUndo -> {
                    snackbarScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = undoLabel,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onEvent(HomeState.Event.RecommendationRestoreRequested(event.animeId))
                        }
                    }
                }

                is HomeState.Effect.ShowContinueWatchingUndo -> {
                    snackbarScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = undoLabel,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onEvent(HomeState.Event.ContinueWatchingRemoveUndone(event.animeId))
                        }
                    }
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

    val onItemSelected: (HomeFeedItem) -> Unit = remember(onEvent) {
        { item -> item.action.toHomeEventOrNull()?.let(onEvent) }
    }

    ReportDrawnWhen { state.isFirstScreenSettled() }

    Box(modifier = Modifier.fillMaxSize()) {
        BaseScreen(
            isScroll = false,
            isLoading = !state.hasInitialContent(),
            loadingContent = { HomeMobileSkeleton() },
            error = state.error?.asString()?.let { ErrorItem(title = it, message = it) },
            onRetry = { onEvent(HomeState.Event.RetrySelected) },
            errorContent = state.error?.asString()?.let { message ->
                { _, retry ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MobileMessage(
                            title = message,
                            actionLabel = stringResource(CoreR.string.common_retry),
                            onAction = retry,
                            fillMaxSize = false,
                        )
                        val statusUrl = stringResource(R.string.home_error_status_url)
                        TextButton(onClick = { context.openExternalUri(statusUrl) }) {
                            Text(
                                text = stringResource(
                                    R.string.home_error_status_hint,
                                    statusUrl,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
        ) {
            val isOffline = LocalIsOffline.current
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(HomeState.Event.RefreshRequested) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    // Без общего предела ширины: лента почти целиком из горизонтальных рядов, им
                    // ширина окна только на пользу. Ограничены лишь баннеры и кнопки внизу.
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_feed"),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = MobileBottomBarDefaults.contentBottomPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    val feed = state.feed

                    // Item только пока есть что показать: пустой дал бы лишний зазор spacedBy сверху
                    item(key = "search") {
                        HomeMobileSearchBar(
                            onClick = { onEvent(HomeState.Event.SearchSelected) },
                            modifier = Modifier
                                .mobileContentMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }

                    if (isOffline || state.statusMessage != null) {
                        item(key = "banners") {
                            Column(modifier = Modifier.mobileContentMaxWidth()) {
                                OfflineBanner(isOffline = isOffline)
                                InfoBanner(message = state.statusMessage)
                            }
                        }
                    }

                    if (feed != null && feed.heroItems.isNotEmpty()) {
                        item(key = "hero") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MobileSectionHeader(
                                    title = stringResource(R.string.home_season_title),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                                HomeHeroCarousel(
                                    items = feed.heroItems,
                                    onItemSelected = onItemSelected,
                                )
                            }
                        }
                    }

                    if (state.continueWatching.isNotEmpty()) {
                        item(key = "continue_watching") {
                            ContinueWatchingSection(
                                entries = state.continueWatching,
                                launchingAnimeId = state.launchingContinueWatchingAnimeId,
                                onEntrySelected = {
                                    onEvent(HomeState.Event.ContinueWatchingSelected(it))
                                },
                                onEntryLongClick = { continueWatchingActionEntry = it },
                            )
                        }
                    }

                    feed?.sections
                        .orEmpty()
                        .filter { it.items.isNotEmpty() }
                        .forEach { section ->
                            item(key = "section_${section.type.name}") {
                                val isCollections = section.type == HomeFeedSectionType.COLLECTIONS
                                val isRecommendations =
                                    section.type == HomeFeedSectionType.RECOMMENDATIONS
                                val hasCatalog = isCollections || isRecommendations
                                HomeFeedSectionRow(
                                    section = section,
                                    onItemSelected = onItemSelected,
                                    actionLabel = if (hasCatalog) {
                                        stringResource(R.string.home_mobile_all)
                                    } else {
                                        null
                                    },
                                    onActionClick = when {
                                        isCollections -> {
                                            { onEvent(HomeState.Event.CollectionsCatalogSelected) }
                                        }

                                        isRecommendations -> {
                                            { onEvent(HomeState.Event.RecommendationsCatalogSelected) }
                                        }

                                        else -> null
                                    },
                                    // Управлять видимостью можно только рекомендациями.
                                    onItemLongClick = if (isRecommendations) {
                                        { item -> recommendationActionItem = item }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }

                    if (state.bloggerVideos.isNotEmpty()) {
                        item(key = "blogger_videos") {
                            MobileHomeBloggerVideosSection(
                                title = stringResource(R.string.home_mobile_blogger_videos),
                                allLabel = stringResource(R.string.home_mobile_all),
                                videos = state.bloggerVideos,
                                onVideoSelected = { onEvent(HomeState.Event.BloggerVideoSelected(it)) },
                                onAllSelected = { onEvent(HomeState.Event.BloggerVideosSelected) },
                            )
                        }
                    }

                    state.bloggerVideosError?.let { message ->
                        item(key = "blogger_videos_error") {
                            MobileMessage(
                                title = message.asString(),
                                actionLabel = stringResource(CoreR.string.common_retry),
                                onAction = { onEvent(HomeState.Event.BloggerVideosRetrySelected) },
                            )
                        }
                    }

                    item(key = "quick_actions") {
                        HomeQuickActionsSection(
                            title = stringResource(R.string.home_mobile_more),
                            scheduleTitle = stringResource(R.string.home_mobile_schedule),
                            reviewsTitle = stringResource(R.string.home_mobile_reviews),
                            onScheduleClick = { onEvent(HomeState.Event.ScheduleSelected) },
                            onReviewsClick = { onEvent(HomeState.Event.ReviewsSelected) },
                            modifier = Modifier.mobileContentMaxWidth(),
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MobileBottomBarDefaults.contentBottomPadding),
        )
    }

    recommendationActionItem?.let { item ->
        HomeRecommendationActionsSheet(
            title = item.title,
            onHide = {
                recommendationActionItem = null
                onEvent(HomeState.Event.RecommendationHideRequested(item.id))
            },
            onDismiss = { recommendationActionItem = null },
        )
    }

    continueWatchingActionEntry?.let { entry ->
        HomeContinueWatchingActionsSheet(
            title = entry.animeTitle,
            onDetails = {
                continueWatchingActionEntry = null
                onEvent(HomeState.Event.AnimeSelected(entry.animeId))
            },
            onRemove = {
                continueWatchingActionEntry = null
                onEvent(HomeState.Event.ContinueWatchingRemoveRequested(entry))
            },
            onDismiss = { continueWatchingActionEntry = null },
        )
    }

    if (state.supportPromptVisible) {
        HomeSupportPromptDialog(
            onDismiss = { onEvent(HomeState.Event.SupportPromptDismissed) },
        )
    }

    if (state.communityPromptVisible) {
        HomeCommunityPromptDialog(
            onLater = { onEvent(HomeState.Event.CommunityPromptDismissed) },
            onJoin = { onEvent(HomeState.Event.CommunityPromptJoinClicked) },
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
