package su.afk.yummy.tv.feature.details.details

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewNavigator
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.handler.DetailsLibraryHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsLibraryMutationResult
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsScreenDataHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsSubscriptionHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsVideoHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsVideosResult
import su.afk.yummy.tv.feature.details.details.handler.DetailsWatchTarget
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.DubbingOption
import su.afk.yummy.tv.feature.details.details.model.DubbingPickerState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.dubbings.selectEpisodeDubbingLaunchVideo
import su.afk.yummy.tv.feature.details.mapper.episodeDubbingItems
import su.afk.yummy.tv.feature.details.mapper.toLibraryPoster
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.fullscreenUrl
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator

@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val reviewsNavigator: IReviewsNavigator,
    private val bloggerVideosNavigator: IBloggerVideosNavigator,
    private val imageViewNavigator: IImageViewNavigator,
    private val stringProvider: StringProvider,
    private val screenDataHandler: DetailsScreenDataHandler,
    private val libraryHandler: DetailsLibraryHandler,
    private val videoHandler: DetailsVideoHandler,
    private val subscriptionHandler: DetailsSubscriptionHandler,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<DetailsState.State, DetailsState.Event, DetailsState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): DetailsViewModel
    }

    override fun createInitialState() = DetailsState.State()

    private val preferredPlayerState = screenDataHandler.preferredPlayer.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferredPlayer.NONE,
    )
    private val yaniUserIdState = screenDataHandler.yaniUserId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0,
    )
    private val askDubbingOnWatchState = screenDataHandler.askDubbingOnWatch.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )
    private var libraryMutationVersion = 0
    private var favoriteMutationVersion = 0
    private var localWatchProgress: List<AnimeWatchProgress> = emptyList()

    init {
        analytics.eventDetailsScreenOpened(animeId)
        load()
        screenDataHandler.observeLibraryState(animeId)
            .onEach { library ->
                setState {
                    copy(
                        isInLibrary = library.isInLibrary || (isSignedIn && libraryList != null),
                        isFavorite = library.isFavorite || (isSignedIn && isFavorite),
                    )
                }
            }
            .launchIn(viewModelScope)
        screenDataHandler.observeWatchProgress(animeId)
            .flowOn(Dispatchers.Default)
            .onEach { progress ->
                localWatchProgress = progress
                updateMergedWatchProgress()
            }
            .launchIn(viewModelScope)
        screenDataHandler.detailsButtonOrder
            .onEach { order -> setState { copy(detailsButtonOrder = order.toImmutableList()) } }
            .launchIn(viewModelScope)
        screenDataHandler.observeAccountSession()
            .onEach { session ->
                val wasSignedIn = currentState.isSignedIn
                val signedIn = session.isAuthorized
                setState {
                    copy(
                        isSignedIn = signedIn,
                        subscriptions = if (!signedIn) persistentListOf() else subscriptions,
                        showSubscriptionsPicker = if (!signedIn) false else showSubscriptionsPicker,
                        isSubscriptionsLoading = if (!signedIn) false else isSubscriptionsLoading,
                    )
                }
                if (signedIn && !wasSignedIn) {
                    viewModelScope.launch {
                        refreshLibraryState()
                        refreshVideosFromNetwork()
                    }
                } else if (!signedIn) {
                    setState { copy(libraryList = null) }
                    updateMergedWatchProgress(serverVideos = emptyList())
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

            is DetailsState.Event.DubbingSelected -> {
                setState { copy(pendingDubbingSelection = null) }
                val allVideos =
                    (currentState.videosState as? VideosUiState.Content)?.videos.orEmpty()
                showBalancerPicker(
                    video = event.video,
                    candidateVideos = allVideos.filter {
                        it.episode.episodeGroupKey() == event.video.episode.episodeGroupKey() &&
                                it.dubbing.trim() == event.video.dubbing.trim()
                    },
                )
            }

            DetailsState.Event.DubbingPickerDismissed -> setState { copy(pendingDubbingSelection = null) }

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

            DetailsState.Event.CommentsSelected -> {
                analytics.eventDetailsCommentsSelected(animeId)
                nav.navigate(commentsNavigator.getAnimeCommentsDest(animeId))
            }

            DetailsState.Event.ReviewsSelected -> nav.navigate(reviewsNavigator.list(animeId))
            DetailsState.Event.BloggerVideosSelected -> nav.navigate(
                bloggerVideosNavigator.anime(
                    animeId
                )
            )

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
                currentState.details?.poster?.fullscreenUrl()?.let { url ->
                    nav.navigate(imageViewNavigator(imageUrl = url))
                }
            }

            DetailsState.Event.SubscriptionsRouteSelected -> {
                analytics.eventDetailsSubscriptionsMobileSelected(animeId)
                nav.navigate(
                    detailsNavigator.getSubscriptionsDest(
                        animeId
                    )
                )
            }

            DetailsState.Event.SubscriptionsSelected -> {
                analytics.eventDetailsSubscriptionsTvSelected(animeId)
                setState { copy(showSubscriptionsPicker = true) }
                loadSubscriptionsForPicker()
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
        viewModelScope.launch { refreshLibraryState() }
        viewModelScope.launch { loadVideosIfCacheMissing() }
    }

    private suspend fun refreshLibraryState() {
        if (!currentState.isSignedIn) return
        val libraryVersion = libraryMutationVersion
        val favoriteVersion = favoriteMutationVersion
        libraryHandler.refreshAuthorizedState(animeId)
            .onSuccess { refreshed ->
                setState {
                    var next = this
                    if (libraryVersion == libraryMutationVersion) {
                        next = next.copy(
                            isInLibrary = isInLibrary || (refreshed?.isInLibrary == true),
                            libraryList = refreshed?.libraryList,
                        )
                    }
                    if (favoriteVersion == favoriteMutationVersion) {
                        next = next.copy(isFavorite = isFavorite || (refreshed?.isFavorite == true))
                    }
                    next
                }
            }
    }

    private suspend fun loadDetails() {
        setState { copy(isLoading = true, error = null) }
        screenDataHandler.loadDetails(animeId).fold(
            onSuccess = { details ->
                setState { copy(isLoading = false, details = details) }
                screenDataHandler.refreshLibraryMetadata(
                    animeId = details.id,
                    title = details.title,
                    poster = details.poster?.toLibraryPoster(),
                    year = details.year,
                )
            },
            onFailure = { e ->
                analytics.eventDetailsLoadError(e)
                setState {
                    copy(
                        isLoading = false,
                        error = e.userMessage(stringProvider.get(R.string.details_load_error)),
                    )
                }
            },
        )
    }

    private suspend fun loadVideos(loadSubscriptionsAfter: Boolean = false) {
        setState { copy(videosState = VideosUiState.Loading) }
        videoHandler.load(
            animeId = animeId,
            pendingSubscriptionStates = subscriptionHandler.pendingSubscriptionStates(animeId),
        ).fold(
            onSuccess = { result ->
                setVideos(result)
                if (loadSubscriptionsAfter || currentState.showSubscriptionsPicker && currentState.isSubscriptionsLoading) {
                    loadSubscriptions()
                }
                if (currentState.isWatchLaunchPending) {
                    openInitialVideo(result.videos)
                }
            },
            onFailure = {
                setState {
                    copy(
                        videosState = VideosUiState.Error(it.userMessage()),
                        watchProgress = DetailsWatchProgressIndex.Empty,
                        isWatchLaunchPending = false,
                        isSubscriptionsLoading = false,
                    )
                }
            },
        )
    }

    private suspend fun loadVideosIfCacheMissing() {
        val cached = videoHandler.loadCached(
            animeId = animeId,
            pendingSubscriptionStates = subscriptionHandler.pendingSubscriptionStates(animeId),
        )
        if (cached != null) {
            setVideos(cached)
            if (currentState.isSignedIn) {
                refreshVideosFromNetwork()
            }
            return
        }
        loadVideos()
    }

    private fun setVideos(result: DetailsVideosResult) {
        setState {
            copy(
                videosState = result.videosState,
                subscriptions = result.subscriptions.toImmutableList(),
                watchProgress = buildWatchProgressIndex(result.videos),
            )
        }
    }

    private suspend fun refreshVideosFromNetwork() {
        videoHandler.refresh(
            animeId = animeId,
            pendingSubscriptionStates = subscriptionHandler.pendingSubscriptionStates(animeId),
        ).onSuccess { result ->
            setVideos(result)
            if (currentState.isWatchLaunchPending) {
                openInitialVideo(result.videos)
            }
        }
    }

    private fun updateMergedWatchProgress(
        serverVideos: List<AnimeVideo> = (currentState.videosState as? VideosUiState.Content)?.videos.orEmpty(),
    ) {
        setState {
            copy(watchProgress = buildWatchProgressIndex(serverVideos))
        }
    }

    private fun buildWatchProgressIndex(videos: List<AnimeVideo>): DetailsWatchProgressIndex =
        DetailsWatchProgressIndex.merge(
            animeId = animeId,
            localEntries = localWatchProgress,
            videos = videos,
        )

    private fun onWatchSelected() {
        when (val videosState = currentState.videosState) {
            is VideosUiState.Content -> openInitialVideo(videosState.videos)
            VideosUiState.NotLoaded,
            VideosUiState.Empty,
            is VideosUiState.Error -> {
                setState { copy(isWatchLaunchPending = true) }
                viewModelScope.launch { loadVideos() }
            }

            VideosUiState.Loading -> setState { copy(isWatchLaunchPending = true) }
        }
    }

    private fun loadSubscriptionsForPicker() {
        if (!currentState.isSignedIn || currentState.isSubscriptionsLoading) {
            return
        }
        when (currentState.videosState) {
            is VideosUiState.Content -> viewModelScope.launch { loadSubscriptions() }
            VideosUiState.NotLoaded,
            VideosUiState.Empty,
            is VideosUiState.Error -> viewModelScope.launch { loadVideos(loadSubscriptionsAfter = true) }

            VideosUiState.Loading -> setState { copy(isSubscriptionsLoading = true) }
        }
    }

    private fun openInitialVideo(videos: List<AnimeVideo>) {
        when (val target = videoHandler.resolveWatchTarget(
            animeId = animeId,
            videos = videos,
            watchProgress = currentState.watchProgress,
        )) {
            is DetailsWatchTarget.Continue -> {
                setState { copy(isWatchLaunchPending = false) }
                navigateToPlayer(target.video)
            }

            is DetailsWatchTarget.Initial -> {
                setState { copy(isWatchLaunchPending = false) }
                if (askDubbingOnWatchState.value) {
                    showDubbingPicker(target.video)
                } else {
                    showBalancerPicker(target.video)
                }
            }

            null -> setState { copy(isWatchLaunchPending = false) }
        }
    }

    private suspend fun loadSubscriptions() {
        if (!currentState.isSignedIn) {
            setState { copy(isSubscriptionsLoading = false, subscriptions = persistentListOf()) }
            return
        }
        setState { copy(isSubscriptionsLoading = true) }
        subscriptionHandler.reloadSubscriptions(animeId).fold(
            onSuccess = { subscriptions ->
                setState {
                    copy(
                        isSubscriptionsLoading = false,
                        subscriptions = subscriptions.toImmutableList(),
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
        analytics.eventDetailsSubscriptionTvToggled(
            animeId = animeId,
            videoId = option.subscriptionVideoId,
            targetState = !wasSubscribed,
        )
        setSubscriptionState(key, !wasSubscribed)
        viewModelScope.launch {
            val changed = subscriptionHandler.commitSubscriptionChange(
                animeId = animeId,
                option = option,
                subscribed = !wasSubscribed,
            )
            if (!changed) {
                setSubscriptionState(key, wasSubscribed)
                return@launch
            }
            subscriptionHandler.reloadSubscriptions(animeId).onSuccess { subscriptions ->
                setState { copy(subscriptions = subscriptions.toImmutableList()) }
            }
        }
    }

    private fun setSubscriptionState(key: String, subscribed: Boolean) {
        setState {
            copy(
                subscriptions = subscriptions.map {
                    if (it.key == key) it.copy(isSubscribed = subscribed) else it
                }.toImmutableList()
            )
        }
    }

    private fun showDubbingPicker(video: AnimeVideo) {
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        val episode = video.episode
        val options = allVideos.episodeDubbingItems(episode).mapNotNull { item ->
            allVideos.selectEpisodeDubbingLaunchVideo(
                episode = episode,
                dubbingName = item.name,
                preferredPlayer = preferredPlayerState.value,
            )?.let { DubbingOption(video = it, item = item) }
        }
        if (options.isEmpty()) {
            showBalancerPicker(video)
            return
        }
        setState {
            copy(
                pendingDubbingSelection = DubbingPickerState(
                    episode = episode,
                    options = options.toImmutableList(),
                )
            )
        }
    }

    private fun showBalancerPicker(video: AnimeVideo, candidateVideos: List<AnimeVideo>? = null) {
        val allVideos = candidateVideos
            ?: (currentState.videosState as? VideosUiState.Content)?.videos
            ?: return
        when (val selection = playerNavigationHandler.selectPlayer(
            video = video,
            allVideos = allVideos,
            preferredPlayer = preferredPlayerState.value,
        )) {
            is DetailsPlayerSelection.Navigate -> navigateToPlayer(selection.video)
            is DetailsPlayerSelection.ShowPicker -> {
                reportUnsupportedPlayers(selection.picker)
                setState { copy(pendingBalancerSelection = selection.picker) }
            }
        }
    }

    private fun reportUnsupportedPlayers(picker: BalancerPickerState) {
        picker.options
            .filter { !it.isSupported }
            .forEach { option ->
                analytics.eventDetailsUnsupportedPlayerShown(
                    animeId = animeId,
                    episode = picker.episodeNumber,
                    playerName = option.playerName,
                )
            }
    }

    private fun navigateToPlayer(video: AnimeVideo) {
        val details = currentState.details
        nav.navigate(
            playerNavigationHandler.getPlayerDestination(
                video = video,
                animeTitle = details?.title ?: "",
                animeId = animeId,
                posterUrl = details?.poster?.run { medium ?: big ?: fullsize ?: small } ?: "",
                screenshotByEpisode = details.screenshotByEpisode(),
                resumeFromMs = currentState.watchProgress.resumeFromMsFor(video),
            )
        )
    }

    private fun navigateToPlayer(video: PlayerVideoSource) {
        val details = currentState.details
        nav.navigate(
            playerNavigationHandler.getPlayerDestination(
                video = video,
                animeTitle = details?.title ?: "",
                animeId = animeId,
                posterUrl = details?.poster?.run { medium ?: big ?: fullsize ?: small } ?: "",
                screenshotByEpisode = details.screenshotByEpisode(),
                resumeFromMs = currentState.watchProgress.resumeFromMsFor(video),
            )
        )
    }

    private fun AnimeDetails?.screenshotByEpisode(): Map<String, String> =
        this?.screenshots.orEmpty().mapNotNull { screenshot ->
            screenshot.episode?.let { episode -> episode to screenshot.small.orEmpty() }
        }.toMap()

}
