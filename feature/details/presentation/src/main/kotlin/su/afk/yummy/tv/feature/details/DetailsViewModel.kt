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
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.settings.PreferredPlayer
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.player.IPlayerNavigator

@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val playerNavigator: IPlayerNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val libraryStore: LibraryStore,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
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

    init {
        load()
        libraryStore.observeIsInLibrary(animeId)
            .onEach { inLibrary -> setState { copy(isInLibrary = inLibrary) } }
            .launchIn(viewModelScope)
        watchProgressStore.observeAll()
            .onEach { entries -> setState { copy(watchProgress = entries.associateBy { it.episodeUrl }) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: DetailsState.Event) {
        when (event) {
            DetailsState.Event.BackSelected -> nav.back()
            DetailsState.Event.RetrySelected -> load()
            is DetailsState.Event.AnimeSelected -> nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            is DetailsState.Event.VideoSelected -> showBalancerPicker(event.video)
            is DetailsState.Event.BalancerConfirmed -> {
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
            }
            DetailsState.Event.BalancerPickerDismissed -> setState { copy(pendingBalancerSelection = null) }
            DetailsState.Event.EpisodesSelected -> nav.navigate(detailsNavigator.getEpisodesDest(animeId))
            DetailsState.Event.TrailersSelected -> nav.navigate(detailsNavigator.getTrailersDest(animeId))
            DetailsState.Event.SimilarSelected -> nav.navigate(detailsNavigator.getSimilarDest(animeId))
            DetailsState.Event.ViewingOrderSelected -> nav.navigate(detailsNavigator.getViewingOrderDest(animeId))
            DetailsState.Event.ScreenshotsSelected -> nav.navigate(detailsNavigator.getScreenshotsDest(animeId))
            DetailsState.Event.LibraryToggled -> viewModelScope.launch { toggleLibrary() }
            DetailsState.Event.PosterClicked -> setState { copy(showPosterFullscreen = true) }
            DetailsState.Event.PosterDismissed -> setState { copy(showPosterFullscreen = false) }
        }
    }

    private suspend fun toggleLibrary() {
        val details = currentState.details ?: return
        if (currentState.isInLibrary) {
            libraryStore.remove(animeId)
        } else {
            libraryStore.add(
                LibraryEntry(
                    animeId = details.id,
                    title = details.title,
                    posterUrl = details.poster?.run { medium ?: big ?: fullsize ?: small },
                )
            )
        }
    }

    private fun load() {
        viewModelScope.launch { loadDetails() }
        viewModelScope.launch { loadVideos() }
    }

    private suspend fun loadDetails() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeDetails(animeId) }.fold(
            onSuccess = { details -> setState { copy(isLoading = false, details = details) } },
            onFailure = { e ->
                setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.details_load_error)) }
            },
        )
    }

    private suspend fun loadVideos() {
        setState { copy(videosState = VideosUiState.Loading) }
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                setState { copy(videosState = if (videos.isEmpty()) VideosUiState.Empty else VideosUiState.Content(videos)) }
            },
            onFailure = { setState { copy(videosState = VideosUiState.Empty) } },
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

        if (supportedOptions.size <= 1) {
            navigateToPlayer(video)
        } else {
            setState { copy(pendingBalancerSelection = BalancerPickerState(video.episode, options)) }
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

        nav.navigate(
            playerNavigator.getPlayerDest(
                iframeUrl = video.iframeUrl,
                animeTitle = details?.title ?: "",
                episode = video.episode,
                playerName = playerName,
                dubbing = selectedDubbing,
                episodeUrls = group.map { it.iframeUrl },
                episodeNumbers = group.map { it.episode },
                currentEpisodeIndex = idx,
                screenshotUrls = screenshotUrls,
                animeId = animeId,
                posterUrl = details?.poster?.run { medium ?: big ?: fullsize ?: small } ?: "",
                allDubbingNames = dubbingNames,
                currentDubbingIndex = currentDubbingIdx,
                allDubbingEpisodeUrls = dubbingNames.map { n -> dubbingGroups[n]!!.map { it.iframeUrl } },
                allDubbingEpisodeNumbers = dubbingNames.map { n -> dubbingGroups[n]!!.map { it.episode } },
                allBalancerNames = supportedBalancers,
                currentBalancerIndex = currentBalancerIdx,
                allBalancerDubbingNames = allBalancerDubbingNames,
                allBalancerEpisodeUrls = allBalancerEpisodeUrls,
                allBalancerEpisodeNumbers = allBalancerEpisodeNumbers,
            )
        )
    }
}
