package su.afk.yummy.tv.feature.home.mobile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import su.afk.yummy.tv.core.designsystem.components.OfflineBanner
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.feature.home.HomeState
import su.afk.yummy.tv.feature.home.mobile.view.ContinueWatchingSection
import su.afk.yummy.tv.feature.home.mobile.view.HomeAnnouncementDialog
import su.afk.yummy.tv.feature.home.mobile.view.HomeFeedSectionRow
import su.afk.yummy.tv.feature.home.mobile.view.HomeHeroCarousel
import su.afk.yummy.tv.feature.home.mobile.view.HomeQuickActionsSection
import su.afk.yummy.tv.feature.home.mobile.view.HomeRecommendationActionsSheet
import su.afk.yummy.tv.feature.home.mobile.view.HomeSearchEntry
import su.afk.yummy.tv.feature.home.mobile.view.HomeSupportPromptDialog
import su.afk.yummy.tv.feature.home.mobile.view.MobileHomeBloggerVideosSection
import su.afk.yummy.tv.feature.home.toHomeEventOrNull
import su.afk.yummy.tv.feature.home.presentation.R as PresentationR

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        HomeMobileScreen(
            HomeState.State(isLoading = false, isContinueWatchingLoaded = true),
            emptyFlow()
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
            error = "Не удалось загрузить главную"
        ), emptyFlow()
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

    val mainActions = LocalMobileMainActions.current
    val onItemSelected: (HomeFeedItem) -> Unit = remember(onEvent) {
        { item -> item.action.toHomeEventOrNull()?.let(onEvent) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BaseScreen(
            isScroll = false,
            isLoading = state.isLoading || state.feed == null || !state.isContinueWatchingLoaded,
            error = state.error?.let { ErrorItem(title = it, message = it) },
            onRetry = { onEvent(HomeState.Event.RetrySelected) },
            errorContent = state.error?.let { message ->
                { _, retry ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MobileMessage(
                            title = message,
                            actionLabel = stringResource(R.string.home_mobile_retry),
                            onAction = retry,
                            fillMaxSize = false,
                        )
                        val statusUrl = stringResource(PresentationR.string.home_error_status_url)
                        TextButton(onClick = { context.openExternalUri(statusUrl) }) {
                            Text(
                                text = stringResource(
                                    PresentationR.string.home_error_status_hint,
                                    statusUrl,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = MobileBottomBarDefaults.contentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val feed = state.feed

                if (mainActions != null) {
                    item(key = "search") {
                        // Плашка внутри item'а поиска: отдельный item дал бы лишний
                        // зазор spacedBy даже со скрытой плашкой
                        Column {
                            HomeSearchEntry(
                                text = stringResource(R.string.home_mobile_search_hint),
                                onClick = mainActions.onSearchClick,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            OfflineBanner(isOffline = LocalIsOffline.current)
                        }
                    }
                }

                if (feed != null && feed.heroItems.isNotEmpty()) {
                    item(key = "hero") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MobileSectionHeader(
                                title = stringResource(R.string.home_mobile_season_title),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            HomeHeroCarousel(
                                items = feed.heroItems,
                                onItemSelected = onItemSelected,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }

                if (state.continueWatching.isNotEmpty()) {
                    item(key = "continue_watching") {
                        ContinueWatchingSection(
                            entries = state.continueWatching,
                            onEntrySelected = {
                                onEvent(HomeState.Event.ContinueWatchingSelected(it))
                            },
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
                            HomeFeedSectionRow(
                                section = section,
                                onItemSelected = onItemSelected,
                                actionLabel = if (isCollections) {
                                    stringResource(R.string.home_mobile_all)
                                } else {
                                    null
                                },
                                onActionClick = if (isCollections) {
                                    { onEvent(HomeState.Event.CollectionsCatalogSelected) }
                                } else {
                                    null
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
                            title = message,
                            actionLabel = stringResource(R.string.home_mobile_retry),
                            onAction = { onEvent(HomeState.Event.BloggerVideosRetrySelected) },
                        )
                    }
                }

                item(key = "quick_actions") {
                    HomeQuickActionsSection(
                        title = stringResource(R.string.home_mobile_more),
                        scheduleTitle = stringResource(R.string.home_mobile_schedule),
                        reviewsTitle = stringResource(R.string.home_mobile_reviews),
                        showSchedule = state.hasSchedule,
                        onScheduleClick = { onEvent(HomeState.Event.ScheduleSelected) },
                        onReviewsClick = { onEvent(HomeState.Event.ReviewsSelected) },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = MobileBottomBarDefaults.BarHeight),
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
