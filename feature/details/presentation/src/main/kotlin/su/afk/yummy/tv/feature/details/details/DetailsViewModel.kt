package su.afk.yummy.tv.feature.details.details

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.handler.DetailsLibraryHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsLibraryMutationResult
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsSubscriptionHandler
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.selectInitialDetailsVideo
import su.afk.yummy.tv.feature.details.utils.subscribedKeys
import su.afk.yummy.tv.feature.details.utils.toLibraryPoster
import su.afk.yummy.tv.feature.details.utils.toSubscriptionOptions
import su.afk.yummy.tv.feature.player.PlayerVideoSource

@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val libraryStore: LibraryStore,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val getAnimeCollections: GetAnimeCollectionsUseCase,
    private val getAnimeListState: GetAnimeListStateUseCase,
    private val stringProvider: StringProvider,
    private val libraryHandler: DetailsLibraryHandler,
    private val subscriptionHandler: DetailsSubscriptionHandler,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<DetailsState.State, DetailsState.Event, DetailsState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): DetailsViewModel
    }

    override fun createInitialState() = DetailsState.State()

    private val preferredPlayerState = settingsStore.preferredPlayer.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferredPlayer.NONE,
    )
    private val yaniUserIdState = settingsStore.yaniUserId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0,
    )
    private var libraryMutationVersion = 0
    private var favoriteMutationVersion = 0

    init {
        analytics.eventDetailsScreenOpened(animeId)
        load()
        libraryStore.observeIsInLibrary(animeId)
            .onEach { inLibrary -> setState { copy(isInLibrary = inLibrary || (isSignedIn && libraryList != null)) } }
            .launchIn(viewModelScope)
        libraryStore.observeIsFavorite(animeId)
            .onEach { favorite -> setState { copy(isFavorite = favorite || (isSignedIn && isFavorite)) } }
            .launchIn(viewModelScope)
        watchProgressStore.observeByAnimeId(animeId)
            .map { entries -> entries.associateBy { it.episodeUrl } }
            .flowOn(Dispatchers.Default)
            .onEach { progress -> setState { copy(watchProgress = progress) } }
            .launchIn(viewModelScope)
        settingsStore.detailsButtonOrder
            .onEach { order -> setState { copy(detailsButtonOrder = order) } }
            .launchIn(viewModelScope)
        yaniAuthPreferences.refreshToken
            .onEach { token ->
                setState {
                    copy(
                        isSignedIn = token.isNotBlank(),
                        subscriptions = if (token.isBlank()) emptyList() else subscriptions,
                        showSubscriptionsPicker = if (token.isBlank()) false else showSubscriptionsPicker,
                    )
                }
                if (token.isNotBlank()) {
                    refreshSubscriptions()
                }
            }
            .launchIn(viewModelScope)
        settingsStore.yaniUserId
            .onEach { userId ->
                if (userId > 0 && currentState.isSignedIn) {
                    viewModelScope.launch { loadSubscriptions(userId) }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: DetailsState.Event) {
        when (event) {
            DetailsState.Event.BackSelected -> nav.back()
            DetailsState.Event.RetrySelected -> {
                analytics.eventDetailsRetry(animeId)
                load()
            }

            DetailsState.Event.WatchSelected -> {
                analytics.eventDetailsWatchSelected(animeId)
                onWatchSelected()
            }

            is DetailsState.Event.AnimeSelected -> {
                analytics.eventDetailsAnimeSelected(animeId, event.seriesId)
                nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            }

            is DetailsState.Event.BalancerConfirmed -> {
                analytics.eventDetailsBalancerConfirmed(animeId, event.video)
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
            }

            DetailsState.Event.BalancerPickerDismissed -> setState { copy(pendingBalancerSelection = null) }
            DetailsState.Event.FullDetailsSelected ->
                nav.navigate(detailsNavigator.getFullDetailsDest(animeId))

            DetailsState.Event.EpisodesSelected ->
                nav.navigate(detailsNavigator.getEpisodesDest(animeId))

            DetailsState.Event.TrailersSelected ->
                nav.navigate(detailsNavigator.getTrailersDest(animeId))

            DetailsState.Event.SimilarSelected ->
                nav.navigate(detailsNavigator.getSimilarDest(animeId))

            DetailsState.Event.ViewingOrderSelected ->
                nav.navigate(detailsNavigator.getViewingOrderDest(animeId))

            DetailsState.Event.ScreenshotsSelected ->
                nav.navigate(detailsNavigator.getScreenshotsDest(animeId))

            DetailsState.Event.RatingScreenSelected ->
                nav.navigate(detailsNavigator.getRatingDest(animeId))

            DetailsState.Event.CollectionsSelected ->
                nav.navigate(detailsNavigator.getCollectionsDest(animeId))

            DetailsState.Event.LibraryToggled ->
                viewModelScope.launch { toggleLibrary() }

            DetailsState.Event.FavoriteToggled ->
                viewModelScope.launch { toggleFavorite() }

            DetailsState.Event.LibraryListPickerDismissed -> setState { copy(showLibraryListPicker = false) }
            is DetailsState.Event.LibraryListSelected -> {
                analytics.eventDetailsLibraryListSelected(animeId, event.list)
                viewModelScope.launch { addToLibrary(event.list) }
            }

            DetailsState.Event.PosterClicked -> {
                analytics.eventDetailsPosterClicked(animeId)
                setState { copy(showPosterFullscreen = true) }
            }

            DetailsState.Event.PosterDismissed -> setState { copy(showPosterFullscreen = false) }
            is DetailsState.Event.CollectionSelected -> {
                analytics.eventDetailsCollectionSelected(animeId, event.collectionId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }

            DetailsState.Event.SubscriptionsRouteSelected -> {
                analytics.eventDetailsSubscriptionsRouteSelected(animeId)
                nav.navigate(
                    detailsNavigator.getSubscriptionsDest(
                        animeId
                    )
                )
            }

            DetailsState.Event.SubscriptionsSelected -> {
                analytics.eventDetailsSubscriptionsSelected(animeId)
                setState { copy(showSubscriptionsPicker = true) }
            }

            DetailsState.Event.SubscriptionsDismissed -> setState { copy(showSubscriptionsPicker = false) }
            is DetailsState.Event.SubscriptionToggled -> toggleSubscription(event.key)
        }
    }

    private suspend fun toggleLibrary() {
        val details = currentState.details ?: return
        if (currentState.isInLibrary) {
            val previousList = currentState.libraryList
            val wasInLibrary = currentState.isInLibrary
            val wasFavorite = currentState.isFavorite
            val wasSignedIn = currentState.isSignedIn
            libraryMutationVersion++
            setState { copy(isInLibrary = false, libraryList = null) }
            when (val result = libraryHandler.removeFromLibrary(
                animeId = animeId,
                details = details,
                previousList = previousList,
                wasInLibrary = wasInLibrary,
                isFavorite = wasFavorite,
                isSignedIn = wasSignedIn,
            )) {
                DetailsLibraryMutationResult.Success -> Unit
                is DetailsLibraryMutationResult.RollbackFavorite -> Unit
                is DetailsLibraryMutationResult.RollbackLibrary -> setState {
                    copy(isInLibrary = result.isInLibrary, libraryList = result.libraryList)
                }
            }
        } else {
            setState { copy(showLibraryListPicker = true) }
        }
    }

    private suspend fun addToLibrary(list: UserAnimeList) {
        val details = currentState.details ?: return
        val wasInLibrary = currentState.isInLibrary
        val previousList = currentState.libraryList
        val wasFavorite = currentState.isFavorite
        val wasSignedIn = currentState.isSignedIn
        libraryMutationVersion++
        setState { copy(showLibraryListPicker = false, isInLibrary = true, libraryList = list) }
        when (val result = libraryHandler.addToLibrary(
            animeId = animeId,
            details = details,
            list = list,
            wasInLibrary = wasInLibrary,
            previousList = previousList,
            isFavorite = wasFavorite,
            isSignedIn = wasSignedIn,
        )) {
            DetailsLibraryMutationResult.Success -> Unit
            is DetailsLibraryMutationResult.RollbackFavorite -> Unit
            is DetailsLibraryMutationResult.RollbackLibrary -> {
                setState {
                    copy(
                        isInLibrary = result.isInLibrary,
                        libraryList = result.libraryList
                    )
                }
            }
        }
    }

    private suspend fun toggleFavorite() {
        val details = currentState.details ?: return
        val wasFavorite = currentState.isFavorite
        val nextFavorite = !wasFavorite
        val wasSignedIn = currentState.isSignedIn
        favoriteMutationVersion++
        setState { copy(isFavorite = nextFavorite) }
        when (val result = libraryHandler.setFavorite(
            animeId = animeId,
            details = details,
            favorite = nextFavorite,
            previousFavorite = wasFavorite,
            isSignedIn = wasSignedIn,
        )) {
            DetailsLibraryMutationResult.Success -> Unit
            is DetailsLibraryMutationResult.RollbackLibrary -> Unit
            is DetailsLibraryMutationResult.RollbackFavorite -> {
                setState { copy(isFavorite = result.isFavorite) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch { loadDetails() }
        viewModelScope.launch { loadExtras() }
    }

    private suspend fun loadExtras() {
        val libraryVersion = libraryMutationVersion
        val favoriteVersion = favoriteMutationVersion
        runCatching { getAnimeCollections(animeId) }
            .onSuccess { setState { copy(collections = it) } }
        runCatching { getAnimeListState(animeId) }
            .onSuccess {
                setState {
                    var next = this
                    if (libraryVersion == libraryMutationVersion) {
                        next = next.copy(
                            isInLibrary = isInLibrary || it?.list != null,
                            libraryList = it?.list,
                        )
                    }
                    if (favoriteVersion == favoriteMutationVersion) {
                        next = next.copy(isFavorite = isFavorite || it?.isFavorite == true)
                    }
                    next
                }
            }
    }

    private fun refreshSubscriptions() {
        val userId = yaniUserIdState.value
        if (userId <= 0) return
        viewModelScope.launch { loadSubscriptions(userId) }
    }

    private suspend fun loadDetails() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeDetails(animeId) }.fold(
            onSuccess = { details ->
                setState { copy(isLoading = false, details = details) }
                libraryStore.refreshMetadata(
                    animeId = details.id,
                    title = details.title,
                    poster = details.poster?.toLibraryPoster(),
                )
                loadVideos()
            },
            onFailure = { e ->
                analytics.eventDetailsLoadError(e)
                setState {
                    copy(
                        isLoading = false,
                        error = e.message ?: stringProvider.get(R.string.details_load_error),
                    )
                }
            },
        )
    }

    private suspend fun loadVideos() {
        setState { copy(videosState = VideosUiState.Loading) }
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                setState {
                    copy(
                        videosState = if (videos.isEmpty()) VideosUiState.Empty else VideosUiState.Content(
                            videos
                        ),
                        subscriptions = videos.toSubscriptionOptions(optimisticKeys = subscriptions.subscribedKeys()),
                    )
                }
                refreshSubscriptions()
                if (currentState.isWatchLaunchPending) {
                    openInitialVideo(videos)
                }
            },
            onFailure = {
                setState {
                    copy(
                        videosState = VideosUiState.Empty,
                        isWatchLaunchPending = false
                    )
                }
            },
        )
    }

    private fun onWatchSelected() {
        when (val videosState = currentState.videosState) {
            is VideosUiState.Content -> openInitialVideo(videosState.videos)
            VideosUiState.Empty -> {
                setState { copy(isWatchLaunchPending = true) }
                viewModelScope.launch { loadVideos() }
            }

            VideosUiState.Loading -> setState { copy(isWatchLaunchPending = true) }
        }
    }

    private fun openInitialVideo(videos: List<AnimeVideo>) {
        val continueTarget = resolveDetailsContinueTarget(
            animeId = animeId,
            videos = videos,
            watchProgress = currentState.watchProgress,
        )
        if (continueTarget != null) {
            setState { copy(isWatchLaunchPending = false) }
            navigateToPlayer(continueTarget.video)
            return
        }

        val video = videos.selectInitialDetailsVideo()
        setState { copy(isWatchLaunchPending = false) }
        if (video != null) {
            showBalancerPicker(video)
        }
    }

    private suspend fun loadSubscriptions(userId: Int) {
        val videos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        if (!currentState.isSignedIn) {
            setState { copy(isSubscriptionsLoading = false, subscriptions = emptyList()) }
            return
        }
        setState { copy(isSubscriptionsLoading = true) }
        subscriptionHandler.loadDetailsSubscriptions(
            animeId = animeId,
            details = currentState.details,
            videos = videos,
            userId = userId,
            optimisticKeys = currentState.subscriptions.subscribedKeys(),
        ).fold(
            onSuccess = { subscriptions ->
                setState {
                    copy(
                        isSubscriptionsLoading = false,
                        subscriptions = subscriptions,
                    )
                }
            },
            onFailure = { setState { copy(isSubscriptionsLoading = false) } },
        )
    }

    private fun toggleSubscription(key: String) {
        if (!currentState.isSignedIn) return
        val option = currentState.subscriptions.firstOrNull { it.key == key } ?: return
        val wasSubscribed = option.isSubscribed
        analytics.eventDetailsSubscriptionToggled(
            animeId = animeId,
            videoId = option.representativeVideoId,
            targetState = !wasSubscribed,
        )
        setSubscriptionState(key, !wasSubscribed)
        viewModelScope.launch {
            val changed = subscriptionHandler.commitSubscriptionChange(
                videoId = option.representativeVideoId,
                subscribed = !wasSubscribed,
            )
            if (!changed) {
                setSubscriptionState(key, wasSubscribed)
            } else {
                refreshSubscriptions()
            }
        }
    }

    private fun setSubscriptionState(key: String, subscribed: Boolean) {
        setState {
            copy(
                subscriptions = subscriptions.map {
                    if (it.key == key) it.copy(isSubscribed = subscribed) else it
                }
            )
        }
    }

    private fun showBalancerPicker(video: AnimeVideo) {
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        when (val selection = playerNavigationHandler.selectPlayer(
            video = video,
            allVideos = allVideos,
            preferredPlayer = preferredPlayerState.value,
        )) {
            is DetailsPlayerSelection.Navigate -> navigateToPlayer(selection.video)
            is DetailsPlayerSelection.ShowPicker -> setState {
                copy(pendingBalancerSelection = selection.picker)
            }
        }
    }

    private fun navigateToPlayer(video: AnimeVideo) {
        val details = currentState.details
        viewModelScope.launch(Dispatchers.Default) {
            val destination = playerNavigationHandler.getPlayerDestination(
                video = video,
                animeTitle = details?.title ?: "",
                animeId = animeId,
                posterUrl = details?.poster?.run { medium ?: big ?: fullsize ?: small } ?: "",
                screenshotByEpisode = details?.screenshots.orEmpty().mapNotNull { screenshot ->
                    screenshot.episode?.let { episode -> episode to screenshot.small.orEmpty() }
                }.toMap(),
            )
            withContext(Dispatchers.Main) { nav.navigate(destination) }
        }
    }

    private fun navigateToPlayer(video: PlayerVideoSource) {
        val details = currentState.details
        viewModelScope.launch(Dispatchers.Default) {
            val destination = playerNavigationHandler.getPlayerDestination(
                video = video,
                animeTitle = details?.title ?: "",
                animeId = animeId,
                posterUrl = details?.poster?.run { medium ?: big ?: fullsize ?: small } ?: "",
                screenshotByEpisode = details?.screenshots.orEmpty().mapNotNull { screenshot ->
                    screenshot.episode?.let { episode -> episode to screenshot.small.orEmpty() }
                }.toMap(),
            )
            withContext(Dispatchers.Main) { nav.navigate(destination) }
        }
    }

}
