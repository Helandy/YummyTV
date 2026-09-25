package su.afk.yummy.tv.feature.home

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlags
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.model.settings.SupportPromptSnapshot
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.anime.usecase.SetAnimeRecommendationIgnoredUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.model.HomeAnnouncement
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.home.utils.hasPlayableTarget
import su.afk.yummy.tv.feature.home.utils.toToastTimeString
import su.afk.yummy.tv.feature.home.utils.withoutHiddenRecommendations
import su.afk.yummy.tv.feature.home.utils.withoutScheduleSection
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.getPlayerDest
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import su.afk.yummy.tv.feature.search.ISearchNavigator
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HomeViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val reviewsNavigator: IReviewsNavigator,
    private val bloggerVideosNavigator: IBloggerVideosNavigator,
    private val scheduleNavigator: IScheduleNavigator,
    private val searchNavigator: ISearchNavigator,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val getBloggerVideos: GetBloggerVideosUseCase,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val refreshHomeFeed: RefreshHomeFeedUseCase,
    private val setAnimeRecommendationIgnored: SetAnimeRecommendationIgnoredUseCase,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
    private val stringProvider: StringProvider,
    private val resolveContinueWatchingLaunch: ResolveContinueWatchingLaunchUseCase,
    private val playerNavigator: IPlayerNavigator,
    private val settingsStore: SettingsStore,
    private val featureToggleProvider: FeatureToggleProvider,
    private val featureToggleUpdateObserver: FeatureToggleUpdateObserver,
    private val analytics: HomeAnalytics,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<HomeState.State, HomeState.Event, HomeState.Effect>() {

    override fun createInitialState() = HomeState.State()

    private var supportPromptTimerJob: Job? = null
    private var supportPromptDisplayedThisSession = false

    /** Лента без скрытых рекомендаций — из неё тайтл возвращается при откате. */
    private var rawFeed: HomeFeed? = null

    init {
        analytics.eventScreenOpened()
        observeContinueWatching()
            .onEach { items ->
                setState {
                    copy(
                        continueWatching = items.toImmutableList(),
                        isContinueWatchingLoaded = true,
                    )
                }
            }
            .launchIn(viewModelScope)
        observeHiddenRecommendations()
        observeSupportPrompt()
        observeAnnouncement()
        loadBloggerVideos()
        load()
    }

    override fun onEvent(event: HomeState.Event) {
        when (event) {
            is HomeState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(event.seriesId)
                nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            }

            is HomeState.Event.CollectionSelected -> {
                analytics.eventCollectionSelected(event.collectionId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }

            is HomeState.Event.ContinueWatchingSelected -> {
                analytics.eventContinueWatchingSelected(event.entry)
                launchContinueWatching(event.entry)
            }

            HomeState.Event.RetrySelected -> {
                analytics.eventRetry()
                load()
            }

            HomeState.Event.ScreenResumed -> syncCachedContinueWatching()

            HomeState.Event.RefreshRequested -> refresh()

            HomeState.Event.CollectionsCatalogSelected -> {
                nav.navigate(collectionNavigator.getCollectionsCatalogDest())
            }

            HomeState.Event.ScheduleSelected -> {
                nav.navigate(scheduleNavigator.getScheduleDest())
            }

            HomeState.Event.SearchSelected -> nav.navigate(searchNavigator.getSearchDest())

            HomeState.Event.ReviewsSelected -> {
                nav.navigate(reviewsNavigator.feed())
            }

            HomeState.Event.BloggerVideosSelected -> nav.navigate(bloggerVideosNavigator.feed())

            HomeState.Event.BloggerVideosRetrySelected -> loadBloggerVideos()

            is HomeState.Event.BloggerVideoSelected ->
                nav.navigate(bloggerVideosNavigator.video(event.video.id))

            HomeState.Event.SupportPromptDismissed -> dismissSupportPrompt()

            HomeState.Event.AnnouncementDismissed -> dismissAnnouncement()

            is HomeState.Event.RecommendationHideRequested ->
                setRecommendationHidden(event.animeId, hidden = true)

            is HomeState.Event.RecommendationRestoreRequested ->
                setRecommendationHidden(event.animeId, hidden = false)
        }
    }

    // Скрытия приходят и с других экранов («Похожее» в деталях), поэтому набор берём из стора.
    private fun observeHiddenRecommendations() {
        settingsStore.hiddenRecommendationIds
            .onEach { hiddenIds ->
                setState { copy(hiddenRecommendationIds = hiddenIds.toPersistentSet()) }
                applyHiddenRecommendations()
            }
            .launchIn(viewModelScope)
    }

    private fun setRecommendationHidden(animeId: Int, hidden: Boolean) {
        if (animeId in currentState.pendingRecommendationIds) return
        viewModelScope.launch {
            if (settingsStore.yaniUserId.first() <= 0) {
                setEffect(
                    HomeState.Effect.ShowToast(
                        stringProvider.get(R.string.home_recommendation_auth_required)
                    )
                )
                return@launch
            }
            setState {
                copy(
                    hiddenRecommendationIds = if (hidden) {
                        hiddenRecommendationIds + animeId
                    } else {
                        hiddenRecommendationIds - animeId
                    },
                    pendingRecommendationIds = pendingRecommendationIds + animeId,
                )
            }
            applyHiddenRecommendations()
            runSuspendCatching { setAnimeRecommendationIgnored(animeId, hidden) }.fold(
                onSuccess = { success ->
                    if (success) {
                        if (hidden) {
                            analytics.eventRecommendationHidden(animeId)
                            setEffect(
                                HomeState.Effect.ShowRecommendationUndo(
                                    message = stringProvider.get(
                                        R.string.home_recommendation_hidden
                                    ),
                                    animeId = animeId,
                                )
                            )
                        } else {
                            analytics.eventRecommendationRestored(animeId)
                        }
                        setState {
                            copy(pendingRecommendationIds = pendingRecommendationIds - animeId)
                        }
                    } else {
                        rollbackRecommendation(animeId, hidden)
                    }
                },
                onFailure = { rollbackRecommendation(animeId, hidden) },
            )
        }
    }

    /** Возвращает тайтл в исходное состояние, если сервер не принял изменение. */
    private fun rollbackRecommendation(animeId: Int, hidden: Boolean) {
        setState {
            copy(
                hiddenRecommendationIds = if (hidden) {
                    hiddenRecommendationIds - animeId
                } else {
                    hiddenRecommendationIds + animeId
                },
                pendingRecommendationIds = pendingRecommendationIds - animeId,
            )
        }
        applyHiddenRecommendations()
        setEffect(
            HomeState.Effect.ShowToast(
                stringProvider.get(R.string.home_recommendation_error)
            )
        )
    }

    /** Пересобирает видимую ленту из [rawFeed] с учётом скрытых рекомендаций. */
    private fun applyHiddenRecommendations() {
        val feed = rawFeed ?: return
        val hiddenIds = currentState.hiddenRecommendationIds
        setState {
            copy(
                feed = feed.withoutHiddenRecommendations(hiddenIds).withoutScheduleSection(),
                hasSchedule = feed.sections.any { it.type == HomeFeedSectionType.SCHEDULE },
            )
        }
    }

    private fun observeSupportPrompt() {
        viewModelScope.launch {
            settingsStore.ensureSupportPromptInstallTimeInitialized()
        }
        settingsStore.supportPromptSnapshot
            .onEach(::applySupportPromptSnapshot)
            .launchIn(viewModelScope)
    }

    private fun applySupportPromptSnapshot(snapshot: SupportPromptSnapshot) {
        supportPromptTimerJob?.cancel()
        if (snapshot.dismissed) {
            if (!supportPromptDisplayedThisSession) {
                setState { copy(supportPromptVisible = false) }
            }
            return
        }

        val remainingMs =
            SUPPORT_PROMPT_DELAY_MS - (System.currentTimeMillis() - snapshot.firstEligibleTimeMs)
        if (remainingMs <= 0L) {
            showSupportPromptOnce()
        } else {
            setState { copy(supportPromptVisible = false) }
            supportPromptTimerJob = viewModelScope.launch {
                delay(remainingMs.milliseconds)
                showSupportPromptOnce()
            }
        }
    }

    private fun showSupportPromptOnce() {
        supportPromptDisplayedThisSession = true
        setState { copy(supportPromptVisible = true) }
        viewModelScope.launch {
            settingsStore.dismissSupportPrompt()
        }
    }

    private fun dismissSupportPrompt() {
        setState { copy(supportPromptVisible = false) }
        viewModelScope.launch {
            settingsStore.dismissSupportPrompt()
        }
    }

    private fun observeAnnouncement() {
        checkAnnouncement()
        val initialActivationId = featureToggleUpdateObserver.currentActivationId
        featureToggleUpdateObserver.updates
            .filter { activationId -> activationId > initialActivationId }
            .onEach { checkAnnouncement() }
            .launchIn(viewModelScope)
    }

    private fun checkAnnouncement() {
        viewModelScope.launch {
            val id = featureToggleProvider.getString(FeatureFlags.announcementId).trim()
            val message = featureToggleProvider.getString(FeatureFlags.announcementMessage).trim()
            // Пустой id/сообщение или id == "0" означают, что объявление выключено.
            if (id.isBlank() || id == "0" || message.isBlank()) {
                analyticsTracker.log(TAG_ANNOUNCEMENT) {
                    "Skipped: id='$id' message.isBlank=${message.isBlank()}"
                }
                setState { copy(announcement = null) }
                return@launch
            }
            val lastSeenId = settingsStore.lastSeenAnnouncementId.first()
            if (id == lastSeenId) {
                analyticsTracker.log(TAG_ANNOUNCEMENT) { "Skipped: id='$id' already seen" }
                return@launch
            }
            val title = featureToggleProvider.getString(FeatureFlags.announcementTitle).trim()
            val button = featureToggleProvider.getString(FeatureFlags.announcementButton).trim()
            analyticsTracker.log(TAG_ANNOUNCEMENT) {
                "Showing: id='$id' lastSeenId='$lastSeenId' title.isBlank=${title.isBlank()} button.isBlank=${button.isBlank()}"
            }
            setState {
                copy(
                    announcement = HomeAnnouncement(
                        id = id,
                        title = title.ifBlank { null },
                        message = message,
                        buttonText = button.ifBlank { null },
                    )
                )
            }
        }
    }

    private fun dismissAnnouncement() {
        val id = currentState.announcement?.id ?: return
        setState { copy(announcement = null) }
        viewModelScope.launch {
            settingsStore.markAnnouncementSeen(id)
        }
    }

    private fun launchContinueWatching(entry: HomeContinueWatchingItem) {
        if (!entry.hasPlayableTarget()) {
            nav.navigate(detailsNavigator.getDetailsDest(entry.animeId))
            return
        }
        viewModelScope.launch {
            val result = resolveContinueWatchingLaunch(
                entry = entry,
                refreshProgressOnLaunch = settingsStore
                    .refreshContinueWatchingProgressOnLaunch
                    .first(),
            )
            result.remoteProgressSwitch?.let { progress ->
                setEffect(
                    HomeState.Effect.ShowToast(
                        stringProvider.get(
                            R.string.home_remote_continue_progress_toast,
                            progress.episode,
                            progress.positionMs.toToastTimeString(),
                        )
                    )
                )
            }
            nav.navigate(playerNavigator.getPlayerDest(result))
        }
    }

    private fun load() {
        viewModelScope.launch {
            val cachedFeed = if (currentState.feed == null) {
                runSuspendCatching { getCachedHomeFeed() }.getOrNull()
            } else {
                null
            }
            if (cachedFeed != null) {
                setState { copy(error = null) }
                applyFeed(cachedFeed, isLoading = false)
            } else if (currentState.feed == null) {
                setState { copy(isLoading = true, error = null) }
            } else {
                setState { copy(error = null) }
            }
            runSuspendCatching { getHomeFeed() }.fold(
                onSuccess = { feed -> applyFeed(feed, isLoading = false) },
                onFailure = { e ->
                    analytics.eventLoadError(e)
                    setState {
                        copy(
                            isLoading = false,
                            error = if (feed == null) {
                                e.userMessage(stringProvider.get(R.string.home_load_error))
                            } else {
                                error
                            },
                        )
                    }
                },
            )
        }
    }

    private fun loadBloggerVideos() {
        viewModelScope.launch {
            setState { copy(isBloggerVideosLoading = true, bloggerVideosError = null) }
            runSuspendCatching { getBloggerVideos(limit = 10) }.fold(
                onSuccess = { videos ->
                    setState {
                        copy(
                            bloggerVideos = videos.toImmutableList(),
                            isBloggerVideosLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    setState {
                        copy(
                            isBloggerVideosLoading = false,
                            bloggerVideosError = error.userMessage(stringProvider.get(R.string.home_blogger_videos_load_error))
                        )
                    }
                },
            )
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (currentState.isLoading && currentState.feed == null) return@launch
            val showInitialLoading = currentState.feed == null
            if (showInitialLoading) {
                setState { copy(isLoading = true, error = null) }
            }
            runSuspendCatching { refreshHomeFeed() }.fold(
                onSuccess = { feed -> applyFeed(feed, isLoading = false) },
                onFailure = { e ->
                    analytics.eventLoadError(e)
                    if (currentState.feed == null) {
                        setState {
                            copy(
                                isLoading = false,
                                error = e.userMessage(stringProvider.get(R.string.home_load_error))
                            )
                        }
                    } else if (showInitialLoading) {
                        setState { copy(isLoading = false) }
                    }
                },
            )
        }
    }

    private fun syncCachedContinueWatching() {
        if (currentState.feed == null) return
        viewModelScope.launch {
            val cachedFeed = runSuspendCatching { getCachedHomeFeed() }.getOrNull() ?: return@launch
            val currentFeed = rawFeed
            applyFeed(
                feed = currentFeed?.copy(continueWatchingItems = cachedFeed.continueWatchingItems)
                    ?: cachedFeed,
                isLoading = false,
            )
        }
    }

    private fun applyFeed(feed: HomeFeed, isLoading: Boolean) {
        rawFeed = feed
        setState {
            copy(
                isLoading = isLoading,
                feed = feed.withoutHiddenRecommendations(hiddenRecommendationIds)
                    .withoutScheduleSection(),
                hasSchedule = feed.sections.any { it.type == HomeFeedSectionType.SCHEDULE },
            )
        }
    }

    private companion object {
        val SUPPORT_PROMPT_DELAY_MS: Long = TimeUnit.DAYS.toMillis(7)
        const val TAG_ANNOUNCEMENT = "HomeAnnouncement"
    }
}
