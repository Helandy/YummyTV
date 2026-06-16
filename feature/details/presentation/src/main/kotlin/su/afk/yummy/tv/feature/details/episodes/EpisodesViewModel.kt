package su.afk.yummy.tv.feature.details.episodes

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler

@HiltViewModel(assistedFactory = EpisodesViewModel.Factory::class)
class EpisodesViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<EpisodesState.State, EpisodesState.Event, EpisodesState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): EpisodesViewModel
    }

    override fun createInitialState() = EpisodesState.State()

    private val preferredPlayerState = settingsStore.preferredPlayer.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferredPlayer.NONE,
    )

    private var animeTitle = ""
    private var posterUrl = ""
    private var screenshotsByEpisode: Map<String, String> = emptyMap()
    private var localWatchProgress: List<WatchProgressEntry> = emptyList()
    private var isSignedIn = false

    init {
        analytics.eventEpisodesScreenOpened(animeId)
        viewModelScope.launch { loadMeta() }
        viewModelScope.launch { loadVideos() }
        watchProgressStore.observeByAnimeId(animeId)
            .flowOn(Dispatchers.Default)
            .onEach { progress ->
                localWatchProgress = progress
                updateMergedWatchProgress()
            }
            .launchIn(viewModelScope)
        yaniAuthPreferences.refreshToken
            .onEach { token ->
                val wasSignedIn = isSignedIn
                isSignedIn = token.isNotBlank()
                if (isSignedIn && !wasSignedIn) {
                    viewModelScope.launch { refreshVideosFromNetwork() }
                } else if (!isSignedIn) {
                    updateMergedWatchProgress(serverVideos = emptyList())
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: EpisodesState.Event) {
        when (event) {
            EpisodesState.Event.BackSelected -> nav.back()
            is EpisodesState.Event.EpisodeDubbingsSelected -> {
                analytics.eventEpisodesEpisodeDubbingsSelected(animeId)
                nav.navigate(detailsNavigator.getEpisodeDubbingsDest(animeId, event.episode))
            }

            is EpisodesState.Event.VideoSelected -> {
                analytics.eventEpisodesVideoSelected(animeId, event.video.id)
                showBalancerPicker(event.video)
            }

            is EpisodesState.Event.BalancerConfirmed -> {
                analytics.eventEpisodesBalancerConfirmed(animeId, event.video)
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
            }

            EpisodesState.Event.BalancerPickerDismissed ->
                setState { copy(pendingBalancerSelection = null) }
        }
    }

    private suspend fun loadMeta() {
        runCatching { getAnimeDetails(animeId) }.onSuccess { details ->
            animeTitle = details.title
            posterUrl = details.poster?.run { medium ?: big ?: fullsize ?: small } ?: ""
            screenshotsByEpisode = details.screenshots
                .mapNotNull { s -> s.episode?.let { ep -> ep to (s.small ?: "") } }
                .toMap()
        }
    }

    private suspend fun loadVideos() {
        setState { copy(videosState = VideosUiState.Loading) }
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                setVideos(videos)
                if (isSignedIn) {
                    refreshVideosFromNetwork()
                }
            },
            onFailure = {
                setState {
                    copy(
                        videosState = VideosUiState.Empty,
                        watchProgress = DetailsWatchProgressIndex.Empty
                    )
                }
            },
        )
    }

    private suspend fun refreshVideosFromNetwork() {
        runCatching { refreshAnimeVideos(animeId) }
            .onSuccess { videos -> setVideos(videos) }
    }

    private fun setVideos(videos: List<AnimeVideo>) {
        setState {
            copy(
                videosState = if (videos.isEmpty()) VideosUiState.Empty else VideosUiState.Content(
                    videos
                ),
                watchProgress = videos.toWatchProgressIndex(),
            )
        }
    }

    private fun updateMergedWatchProgress(
        serverVideos: List<AnimeVideo> = (currentState.videosState as? VideosUiState.Content)?.videos.orEmpty(),
    ) {
        setState { copy(watchProgress = serverVideos.toWatchProgressIndex()) }
    }

    private fun List<AnimeVideo>.toWatchProgressIndex(): DetailsWatchProgressIndex =
        DetailsWatchProgressIndex.merge(
            animeId = animeId,
            localEntries = localWatchProgress,
            videos = this,
        )

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
        val title = animeTitle
        val poster = posterUrl
        val screenshots = screenshotsByEpisode
        viewModelScope.launch(Dispatchers.Default) {
            val destination = playerNavigationHandler.getPlayerDestination(
                video = video,
                animeTitle = title,
                animeId = animeId,
                posterUrl = poster,
                screenshotByEpisode = screenshots,
                resumeFromMs = currentState.watchProgress.resumeFromMsFor(video),
            )
            withContext(Dispatchers.Main) { nav.navigate(destination) }
        }
    }

}
