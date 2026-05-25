package su.afk.yummy.tv.feature.details

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.library.LibraryPoster
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.settings.PreferredPlayer
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.account.usecase.GetVideoSubscriptionsUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips

@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val playerNavigator: IPlayerNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val libraryStore: LibraryStore,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val getAnimeCollections: GetAnimeCollectionsUseCase,
    private val getAnimeListState: GetAnimeListStateUseCase,
    private val getVideoSubscriptions: GetVideoSubscriptionsUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val setVideoSubscription: SetVideoSubscriptionUseCase,
    private val stringProvider: StringProvider,
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
        load()
        libraryStore.observeIsInLibrary(animeId)
            .onEach { inLibrary -> setState { copy(isInLibrary = inLibrary || (isSignedIn && libraryList != null)) } }
            .launchIn(viewModelScope)
        libraryStore.observeIsFavorite(animeId)
            .onEach { favorite -> setState { copy(isFavorite = favorite || (isSignedIn && isFavorite)) } }
            .launchIn(viewModelScope)
        watchProgressStore.observeAll()
            .onEach { entries -> setState { copy(watchProgress = entries.associateBy { it.episodeUrl }) } }
            .launchIn(viewModelScope)
        settingsStore.yaniAccessToken
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
            DetailsState.Event.RetrySelected -> load()
            DetailsState.Event.WatchSelected -> onWatchSelected()
            is DetailsState.Event.AnimeSelected -> nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            is DetailsState.Event.BalancerConfirmed -> {
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
            }
            DetailsState.Event.BalancerPickerDismissed -> setState { copy(pendingBalancerSelection = null) }
            DetailsState.Event.FullDetailsSelected -> nav.navigate(detailsNavigator.getFullDetailsDest(animeId))
            DetailsState.Event.EpisodesSelected -> nav.navigate(detailsNavigator.getEpisodesDest(animeId))
            DetailsState.Event.TrailersSelected -> nav.navigate(detailsNavigator.getTrailersDest(animeId))
            DetailsState.Event.SimilarSelected -> nav.navigate(detailsNavigator.getSimilarDest(animeId))
            DetailsState.Event.ViewingOrderSelected -> nav.navigate(detailsNavigator.getViewingOrderDest(animeId))
            DetailsState.Event.ScreenshotsSelected -> nav.navigate(detailsNavigator.getScreenshotsDest(animeId))
            DetailsState.Event.RatingScreenSelected -> nav.navigate(detailsNavigator.getRatingDest(animeId))
            DetailsState.Event.CollectionsSelected -> nav.navigate(detailsNavigator.getCollectionsDest(animeId))
            DetailsState.Event.LibraryToggled -> viewModelScope.launch { toggleLibrary() }
            DetailsState.Event.FavoriteToggled -> viewModelScope.launch { toggleFavorite() }
            DetailsState.Event.LibraryListPickerDismissed -> setState { copy(showLibraryListPicker = false) }
            is DetailsState.Event.LibraryListSelected -> viewModelScope.launch { addToLibrary(event.list) }
            DetailsState.Event.PosterClicked -> setState { copy(showPosterFullscreen = true) }
            DetailsState.Event.PosterDismissed -> setState { copy(showPosterFullscreen = false) }
            is DetailsState.Event.CollectionSelected -> nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            DetailsState.Event.SubscriptionsSelected -> setState { copy(showSubscriptionsPicker = true) }
            DetailsState.Event.SubscriptionsDismissed -> setState { copy(showSubscriptionsPicker = false) }
            is DetailsState.Event.SubscriptionToggled -> toggleSubscription(event.key)
        }
    }

    private suspend fun toggleLibrary() {
        currentState.details ?: return
        if (currentState.isInLibrary) {
            libraryMutationVersion++
            libraryStore.remove(animeId)
            setState { copy(isInLibrary = false, libraryList = null) }
            runCatching { removeAnimeList(animeId) }
        } else {
            setState { copy(showLibraryListPicker = true) }
        }
    }

    private suspend fun addToLibrary(list: UserAnimeList) {
        val details = currentState.details ?: return
        libraryMutationVersion++
        setState { copy(showLibraryListPicker = false, isInLibrary = true, libraryList = list) }
        libraryStore.add(
            LibraryEntry(
                animeId = details.id,
                title = details.title,
                posterSmallUrl = details.poster?.small,
                posterMediumUrl = details.poster?.medium,
                posterBigUrl = details.poster?.big,
                posterFullsizeUrl = details.poster?.fullsize,
                posterMegaUrl = details.poster?.mega,
                listId = list.id,
                isFavorite = currentState.isFavorite,
            )
        )
        runCatching { setAnimeList(animeId, list) }
    }

    private suspend fun toggleFavorite() {
        val details = currentState.details ?: return
        val wasFavorite = currentState.isFavorite
        val nextFavorite = !wasFavorite
        favoriteMutationVersion++
        setState { copy(isFavorite = nextFavorite) }
        libraryStore.setFavorite(
            animeId = details.id,
            title = details.title,
            poster = details.poster?.toLibraryPoster(),
            favorite = nextFavorite,
        )
        if (!currentState.isSignedIn) return

        val result = runCatching { setAnimeFavorite(animeId, nextFavorite) }
        if (result.isFailure) {
            setState { copy(isFavorite = wasFavorite) }
            libraryStore.setFavorite(
                animeId = details.id,
                title = details.title,
                poster = details.poster?.toLibraryPoster(),
                favorite = wasFavorite,
            )
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
                loadVideos()
            },
            onFailure = { e ->
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
                        videosState = if (videos.isEmpty()) VideosUiState.Empty else VideosUiState.Content(videos),
                        subscriptions = videos.toSubscriptionOptions(subscribedKeys = subscriptions.subscribedKeys()),
                    )
                }
                refreshSubscriptions()
                if (currentState.isWatchLaunchPending) {
                    openInitialVideo(videos)
                }
            },
            onFailure = { setState { copy(videosState = VideosUiState.Empty, isWatchLaunchPending = false) } },
        )
    }

    private fun isSupportedPlayer(iframeUrl: String): Boolean =
        iframeUrl.contains("kodik", ignoreCase = true) ||
        iframeUrl.contains("aksor.tv", ignoreCase = true) ||
        iframeUrl.contains("iframeCVH", ignoreCase = true) ||
        iframeUrl.contains("alloha", ignoreCase = true)

    private fun matchesPreferredPlayer(iframeUrl: String, preferred: PreferredPlayer): Boolean =
        when (preferred) {
            PreferredPlayer.NONE -> false
            PreferredPlayer.KODIK -> iframeUrl.contains("kodik", ignoreCase = true)
            PreferredPlayer.AKSOR -> iframeUrl.contains("aksor.tv", ignoreCase = true)
            PreferredPlayer.ALLOHA -> iframeUrl.contains("alloha", ignoreCase = true)
            PreferredPlayer.CVH -> iframeUrl.contains("iframeCVH", ignoreCase = true)
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
        val video = selectInitialVideo(videos)
        setState { copy(isWatchLaunchPending = false) }
        if (video != null) {
            showBalancerPicker(video)
        }
    }

    private fun selectInitialVideo(videos: List<AnimeVideo>): AnimeVideo? {
        val resumeEntry = currentState.watchProgress.values
            .filter { it.animeId == animeId && it.positionMs > 0 }
            .maxByOrNull { it.updatedAt }
        val resumeVideo = resumeEntry?.let { entry ->
            videos.firstOrNull { it.iframeUrl == entry.episodeUrl }
        }
        if (resumeVideo != null) return resumeVideo

        val kodikVideos = videos.filter {
            it.player.contains("kodik", ignoreCase = true) || it.iframeUrl.contains("kodik", ignoreCase = true)
        }
        val supportedVideos = videos.filter { isSupportedPlayer(it.iframeUrl) }
        val source = kodikVideos.ifEmpty { supportedVideos.ifEmpty { videos } }
        return source.groupBy { it.dubbing }
            .maxByOrNull { (_, list) -> list.sumOf { it.views ?: 0 } }
            ?.value
            ?.minByOrNull { it.episode.toIntOrNull() ?: Int.MAX_VALUE }
            ?: source.firstOrNull()
    }

    private suspend fun loadSubscriptions(userId: Int) {
        val videos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        setState { copy(isSubscriptionsLoading = true) }
        runCatching { getVideoSubscriptions(userId) }.fold(
            onSuccess = { subscriptions ->
                if (!currentState.isSignedIn) {
                    setState { copy(isSubscriptionsLoading = false, subscriptions = emptyList()) }
                    return@fold
                }
                val subscribedKeys = subscriptions
                    .filter { it.animeId == animeId }
                    .flatMap { it.subscriptionMatchKeys() }
                    .toSet()
                setState {
                    copy(
                        isSubscriptionsLoading = false,
                        subscriptions = videos.toSubscriptionOptions(subscribedKeys),
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
        setSubscriptionState(key, !wasSubscribed)
        viewModelScope.launch {
            val result = runCatching { setVideoSubscription(option.representativeVideoId, !wasSubscribed) }
            if (result.isFailure) setSubscriptionState(key, wasSubscribed)
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
        val options = allVideos
            .filter { it.episode == video.episode }
            .groupBy { it.player }
            .entries
            .map { (playerName, playerVideos) ->
                val supported = isSupportedPlayer(playerVideos.first().iframeUrl)
                val rep = playerVideos.firstOrNull { it.dubbing == video.dubbing }
                    ?: playerVideos.maxByOrNull { it.views ?: 0 }
                    ?: playerVideos.first()
                BalancerOption(playerName = playerName, video = rep, isSupported = supported)
            }
        val supportedOptions = options.filter { it.isSupported }

        val preferredPlayer = preferredPlayerState.value
        if (preferredPlayer != PreferredPlayer.NONE) {
            val preferred = supportedOptions.firstOrNull { matchesPreferredPlayer(it.video.iframeUrl, preferredPlayer) }
            if (preferred != null) {
                navigateToPlayer(preferred.video)
                return
            }
        }

        when (supportedOptions.size) {
            0 -> navigateToPlayer(video)
            1 -> navigateToPlayer(supportedOptions.first().video)
            else -> setState { copy(pendingBalancerSelection = BalancerPickerState(video.episode, options)) }
        }
    }

    private fun navigateToPlayer(video: AnimeVideo) {
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        val details = currentState.details
        val playerName = video.player
        val selectedDubbing = video.dubbing

        val dubbingGroups = allVideos
            .filter { it.player == playerName }
            .groupBy { it.dubbing }
            .mapValues { (_, v) -> v.sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE } }
        val dubbingNames = dubbingGroups.keys.toList()
        val currentDubbingIdx = dubbingNames.indexOf(selectedDubbing).coerceAtLeast(0)
        val group = dubbingGroups[selectedDubbing] ?: emptyList()
        val idx = group.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        val episodeScreenshots = details?.screenshots.orEmpty()
        val screenshotUrls = group.map { ep ->
            episodeScreenshots.firstOrNull { it.episode == ep.episode }?.small.orEmpty()
        }

        val supportedBalancers = allVideos
            .map { it.player }.distinct()
            .filter { pName -> isSupportedPlayer(allVideos.first { it.player == pName }.iframeUrl) }
        val currentBalancerIdx = supportedBalancers.indexOf(playerName).coerceAtLeast(0)
        val allBalancerDubbingNames = supportedBalancers.map { bName ->
            allVideos.filter { it.player == bName }.map { it.dubbing }.distinct()
        }
        val allBalancerEpisodeUrls = supportedBalancers.mapIndexed { bIdx, bName ->
            allBalancerDubbingNames[bIdx].map { dName ->
                allVideos.filter { it.player == bName && it.dubbing == dName }
                    .sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE }
                    .map { it.iframeUrl }
            }
        }
        val allBalancerEpisodeNumbers = supportedBalancers.mapIndexed { bIdx, bName ->
            allBalancerDubbingNames[bIdx].map { dName ->
                allVideos.filter { it.player == bName && it.dubbing == dName }
                    .sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE }
                    .map { it.episode }
            }
        }
        val allBalancerEpisodeSkips = supportedBalancers.mapIndexed { bIdx, bName ->
            allBalancerDubbingNames[bIdx].map { dName ->
                allVideos.filter { it.player == bName && it.dubbing == dName }
                    .sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE }
                    .map { it.skips.toPlayerSkips() }
            }
        }

        nav.navigate(
            playerNavigator.getPlayerDest(
                iframeUrl = video.iframeUrl,
                animeTitle = details?.title ?: "",
                episode = video.episode,
                playerName = playerName,
                dubbing = selectedDubbing,
                episodeUrls = group.map { it.iframeUrl },
                episodeNumbers = group.map { it.episode },
                episodeVideoIds = group.map { it.id },
                currentEpisodeIndex = idx,
                screenshotUrls = screenshotUrls,
                animeId = animeId,
                posterUrl = details?.poster?.run { medium ?: big ?: fullsize ?: small } ?: "",
                allDubbingNames = dubbingNames,
                currentDubbingIndex = currentDubbingIdx,
                allDubbingEpisodeUrls = dubbingNames.map { n -> dubbingGroups[n]!!.map { it.iframeUrl } },
                allDubbingEpisodeNumbers = dubbingNames.map { n -> dubbingGroups[n]!!.map { it.episode } },
                allDubbingEpisodeVideoIds = dubbingNames.map { n -> dubbingGroups[n]!!.map { it.id } },
                allBalancerNames = supportedBalancers,
                currentBalancerIndex = currentBalancerIdx,
                allBalancerDubbingNames = allBalancerDubbingNames,
                allBalancerEpisodeUrls = allBalancerEpisodeUrls,
                allBalancerEpisodeNumbers = allBalancerEpisodeNumbers,
                allBalancerEpisodeVideoIds = supportedBalancers.mapIndexed { bIdx, bName ->
                    allBalancerDubbingNames[bIdx].map { dName ->
                        allVideos.filter { it.player == bName && it.dubbing == dName }
                            .sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE }
                            .map { it.id }
                    }
                },
                episodeSkips = group.map { it.skips.toPlayerSkips() },
                allDubbingEpisodeSkips = dubbingNames.map { n -> dubbingGroups[n]!!.map { it.skips.toPlayerSkips() } },
                allBalancerEpisodeSkips = allBalancerEpisodeSkips,
            )
        )
    }
}

private fun AnimeVideoSkips.toPlayerSkips(): PlayerSkips = PlayerSkips(
    opening = opening.toPlayerSkipSegment(),
    ending = ending.toPlayerSkipSegment(),
)

private fun AnimeVideoSkipSegment?.toPlayerSkipSegment(): PlayerSkipSegment? =
    this?.let { PlayerSkipSegment(startMs = it.startMs, endMs = it.endMs) }

private fun su.afk.yummy.tv.domain.anime.model.AnimePoster.toLibraryPoster(): LibraryPoster =
    LibraryPoster(
        small = small,
        medium = medium,
        big = big,
        fullsize = fullsize,
        mega = mega,
    )

private fun List<AnimeVideo>.toSubscriptionOptions(subscribedKeys: Set<String>): List<SubscriptionOption> =
    filter { it.id > 0 && it.dubbing.isNotBlank() }
        .groupBy { subscriptionGroupKey(it.playerId, it.player, it.dubbing) }
        .values
        .mapNotNull { videos ->
            val representative = videos.maxWithOrNull(
                compareBy<AnimeVideo> { it.episode.toIntOrNull() ?: 0 }
                    .thenBy { it.id }
            ) ?: return@mapNotNull null
            val matchKeys = subscriptionMatchKeys(
                playerId = representative.playerId,
                player = representative.player,
                dubbing = representative.dubbing,
            )
            SubscriptionOptionWithViews(
                option = SubscriptionOption(
                    key = subscriptionGroupKey(representative.playerId, representative.player, representative.dubbing),
                    playerId = representative.playerId,
                    player = representative.player,
                    dubbing = representative.dubbing,
                    episodesCount = videos.size,
                    representativeVideoId = representative.id,
                    isSubscribed = matchKeys.any { it in subscribedKeys },
                ),
                totalViews = videos.sumOf { it.views ?: 0 },
            )
        }
        .groupBy { it.option.dubbing.trim().lowercase() }
        .values
        .sortedByDescending { group -> group.sumOf { it.totalViews } }
        .flatMap { group ->
            group.sortedWith(
                compareByDescending<SubscriptionOptionWithViews> { it.totalViews }
                    .thenBy { it.option.player }
            )
        }
        .map { it.option }

private data class SubscriptionOptionWithViews(
    val option: SubscriptionOption,
    val totalViews: Int,
)

private fun List<SubscriptionOption>.subscribedKeys(): Set<String> =
    filter { it.isSubscribed }
        .flatMap { subscriptionMatchKeys(it.playerId, it.player, it.dubbing) }
        .toSet()

private fun VideoSubscription.subscriptionMatchKeys(): Set<String> =
    subscriptionMatchKeys(playerId = playerId, player = player, dubbing = dubbing)

private fun subscriptionMatchKeys(playerId: Int?, player: String, dubbing: String): Set<String> = buildSet {
    val normalizedDubbing = dubbing.normalizedSubscriptionPart()
    if (normalizedDubbing.isBlank()) return@buildSet
    if (playerId != null) add("playerId:$playerId|dubbing:$normalizedDubbing")
    val normalizedPlayer = player.normalizedSubscriptionPart()
    if (normalizedPlayer.isNotBlank()) add("player:$normalizedPlayer|dubbing:$normalizedDubbing")
}

private fun subscriptionGroupKey(playerId: Int?, player: String, dubbing: String): String {
    val normalizedDubbing = dubbing.normalizedSubscriptionPart()
    val normalizedPlayer = player.normalizedSubscriptionPart()
    return "playerId:${playerId ?: -1}|player:$normalizedPlayer|dubbing:$normalizedDubbing"
}

private fun String.normalizedSubscriptionPart(): String =
    trim().lowercase()
