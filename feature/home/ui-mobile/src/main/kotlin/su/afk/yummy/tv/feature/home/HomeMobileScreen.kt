package su.afk.yummy.tv.feature.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.feature.home.mobile.R
import su.afk.yummy.tv.feature.home.view.ContinueWatchingSection
import su.afk.yummy.tv.feature.home.view.HomeFeedSectionRow
import su.afk.yummy.tv.feature.home.view.HomeHeroCarousel
import su.afk.yummy.tv.feature.home.view.HomeQuickActionsSection
import su.afk.yummy.tv.feature.home.view.HomeSearchEntry
import su.afk.yummy.tv.feature.home.view.HomeSupportPromptDialog
import su.afk.yummy.tv.feature.home.view.MobileHomeBloggerVideosSection

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
    LaunchedEffect(Unit) {
        onEvent(HomeState.Event.ScreenResumed)
    }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is HomeState.Effect.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is HomeState.Effect.OpenUri -> context.openExternalUri(event.uri)
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

    val mainActions = LocalMobileMainActions.current
    val onItemSelected: (HomeFeedItem) -> Unit = remember(onEvent) {
        { item ->
            when (val action = item.action) {
                is HomeFeedItemAction.OpenSeries -> onEvent(HomeState.Event.AnimeSelected(action.seriesId))
                is HomeFeedItemAction.OpenVideo -> Unit
                is HomeFeedItemAction.OpenCollection -> onEvent(
                    HomeState.Event.CollectionSelected(
                        action.collectionId
                    )
                )
            }
        }
    }

    BaseScreen(
        isScroll = false,
        isLoading = state.isLoading || state.feed == null || !state.isContinueWatchingLoaded,
        error = state.error?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(HomeState.Event.RetrySelected) },
        errorContent = state.error?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.home_mobile_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = MobileBottomBarDefaults.ContentBottomPadding +
                        MobileBottomBarDefaults.ExtraContentBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val feed = state.feed

            if (mainActions != null) {
                item(key = "search") {
                    HomeSearchEntry(
                        text = stringResource(R.string.home_mobile_search_hint),
                        onClick = mainActions.onSearchClick,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (feed != null && feed.heroItems.isNotEmpty()) {
                item(key = "hero") {
                    HomeHeroCarousel(
                        items = feed.heroItems,
                        onItemSelected = onItemSelected,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (state.continueWatching.isNotEmpty()) {
                item(key = "continue_watching") {
                    ContinueWatchingSection(
                        entries = state.continueWatching,
                        onEntrySelected = { onEvent(HomeState.Event.ContinueWatchingSelected(it)) },
                    )
                }
            }

            feed?.sections
                .orEmpty()
                .filter {
                    it.items.isNotEmpty() &&
                            it.type != HomeFeedSectionType.RECOMMENDATIONS &&
                            it.type != HomeFeedSectionType.SCHEDULE
                }
                .forEach { section ->
                    item(key = "section_${section.type.name}") {
                        val isCollections = section.type == HomeFeedSectionType.COLLECTIONS
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
                    showSchedule = feed?.sections?.any {
                        it.type == HomeFeedSectionType.SCHEDULE
                    } == true,
                    onScheduleClick = { onEvent(HomeState.Event.ScheduleSelected) },
                    onReviewsClick = { onEvent(HomeState.Event.ReviewsSelected) },
                )
            }
        }
    }

    if (state.supportPromptVisible) {
        HomeSupportPromptDialog(
            onDismiss = { onEvent(HomeState.Event.SupportPromptDismissed) },
        )
    }
}
