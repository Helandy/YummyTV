package su.afk.yummy.tv.feature.home

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.SupportPromptSnapshot
import su.afk.yummy.tv.core.utils.runSuspendCatching
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.watching.handler.ContinueWatchingLaunchHandler
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val refreshHomeFeed: RefreshHomeFeedUseCase,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
    private val stringProvider: StringProvider,
    private val continueWatchingLaunchHandler: ContinueWatchingLaunchHandler,
    private val settingsStore: SettingsStore,
    private val analytics: HomeAnalytics,
) : BaseViewModelNew<HomeState.State, HomeState.Event, HomeState.Effect>(savedStateHandle) {

    override fun createInitialState() = HomeState.State()

    private var supportPromptTimerJob: Job? = null
    private var supportPromptDisplayedThisSession = false

    init {
        analytics.eventScreenOpened()
        observeContinueWatching()
            .onEach { items ->
                setState {
                    copy(
                        continueWatching = items,
                        isContinueWatchingLoaded = true,
                    )
                }
            }
            .launchIn(viewModelScope)
        observeSupportPrompt()
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

            HomeState.Event.SupportPromptDismissed -> dismissSupportPrompt()
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
                delay(remainingMs)
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

    private fun launchContinueWatching(entry: HomeContinueWatchingItem) {
        if (!entry.hasPlayableTarget()) {
            nav.navigate(detailsNavigator.getDetailsDest(entry.animeId))
            return
        }
        viewModelScope.launch {
            val result = continueWatchingLaunchHandler.getPlayerLaunchResult(entry)
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
            nav.navigate(result.destination)
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runSuspendCatching { getHomeFeed() }.fold(
                onSuccess = { feed -> applyFeed(feed, isLoading = false) },
                onFailure = { e ->
                    analytics.eventLoadError(e)
                    setState {
                        copy(
                            isLoading = false,
                            error = e.message ?: stringProvider.get(R.string.home_load_error)
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
                                error = e.message ?: stringProvider.get(R.string.home_load_error)
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
            val currentFeed = currentState.feed
            applyFeed(
                feed = currentFeed?.copy(continueWatchingItems = cachedFeed.continueWatchingItems)
                    ?: cachedFeed,
                isLoading = false,
            )
        }
    }

    private fun applyFeed(feed: HomeFeed, isLoading: Boolean) {
        setState {
            copy(
                isLoading = isLoading,
                feed = feed,
            )
        }
    }

    private fun HomeContinueWatchingItem.hasPlayableTarget(): Boolean =
        videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()

    private fun Long.toToastTimeString(): String {
        val totalSeconds = coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    private companion object {
        val SUPPORT_PROMPT_DELAY_MS: Long = TimeUnit.DAYS.toMillis(7)
    }
}
