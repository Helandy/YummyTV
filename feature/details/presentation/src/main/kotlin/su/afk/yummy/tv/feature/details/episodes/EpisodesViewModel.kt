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
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeWatchProgress
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeWatchProgressUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadQualityOption
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.usecase.EnqueueVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadStatusesUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.PrepareVideoDownloadQualityOptionsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.presentation.R

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
    private val observeAnimeWatchProgress: ObserveAnimeWatchProgressUseCase,
    private val settingsStore: SettingsStore,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val resolvePlayerStream: ResolvePlayerStreamUseCase,
    private val prepareDownloadQualities: PrepareVideoDownloadQualityOptionsUseCase,
    private val enqueueVideoDownload: EnqueueVideoDownloadUseCase,
    private val observeVideoDownloadStatuses: ObserveVideoDownloadStatusesUseCase,
    private val stringProvider: StringProvider,
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
    private var localWatchProgress: List<AnimeWatchProgress> = emptyList()
    private var isSignedIn = false
    private var pendingDownloadCandidate: DownloadCandidate? = null

    init {
        analytics.eventEpisodesScreenOpened(animeId)
        viewModelScope.launch { loadMeta() }
        viewModelScope.launch { loadVideos() }
        observeVideoDownloadStatuses(animeId)
            .onEach { statuses ->
                setState {
                    copy(
                        downloadStatuses = statuses.values.associate {
                            it.uiStatusKey to it.toUiState()
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
        observeAnimeWatchProgress(animeId)
            .flowOn(Dispatchers.Default)
            .onEach { progress ->
                localWatchProgress = progress
                updateMergedWatchProgress()
            }
            .launchIn(viewModelScope)
        observeAccountSession()
            .onEach { session ->
                val wasSignedIn = isSignedIn
                isSignedIn = session.isAuthorized
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

            is EpisodesState.Event.EpisodeDownloadSelected -> showDownloadDubbingPicker(event.videos)

            is EpisodesState.Event.DownloadDubbingSelected -> {
                setState { copy(pendingDownloadDubbingSelection = null) }
                prepareEpisodeDownload(event.video)
            }

            EpisodesState.Event.DownloadDubbingPickerDismissed -> {
                setState { copy(pendingDownloadDubbingSelection = null) }
            }

            is EpisodesState.Event.DownloadQualitySelected -> enqueueSelectedDownload(event.option)

            EpisodesState.Event.DownloadQualityPickerDismissed -> {
                pendingDownloadCandidate = null
                setState { copy(pendingDownloadQualitySelection = null) }
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

    private fun showDownloadDubbingPicker(videos: List<AnimeVideo>) {
        val options = videos
            .sortedWith(compareBy({ it.dubbing }, { it.player }, { it.playerId ?: 0 }, { it.id }))
            .map { video ->
                val key = video.downloadStatusKey()
                EpisodesState.EpisodeDownloadDubbingOption(
                    video = video,
                    title = video.dubbing.ifBlank { video.player },
                    subtitle = video.player.takeIf { it.isNotBlank() && it != video.dubbing },
                    status = currentState.downloadStatuses[key],
                    resolving = key in currentState.resolvingDownloadKeys,
                )
            }
        val episode = videos.firstOrNull()?.episode.orEmpty()
        setState {
            copy(
                pendingDownloadDubbingSelection = EpisodesState.EpisodeDownloadDubbingSelection(
                    episode = episode,
                    options = options,
                )
            )
        }
    }

    private fun prepareEpisodeDownload(video: AnimeVideo) {
        val key = video.downloadStatusKey()
        if (currentState.downloadStatuses[key]?.status?.isActive == true || key in currentState.resolvingDownloadKeys) {
            return
        }
        viewModelScope.launch {
            setState { copy(resolvingDownloadKeys = resolvingDownloadKeys + key) }
            setEffect(EpisodesState.Effect.ShowToast(stringProvider.get(R.string.details_download_resolving_quality)))
            try {
                when (val result = resolvePlayerStream(
                    PlayerStreamRequest(
                        iframeUrl = video.iframeUrl,
                        autoQualityLabel = stringProvider.get(R.string.details_quality_auto),
                    )
                )) {
                    is PlayerStreamResolveResult.Stream -> showDownloadQualityPicker(video, result)

                    is PlayerStreamResolveResult.KodikBlocked -> {
                        setState { copy(resolvingDownloadKeys = resolvingDownloadKeys - key) }
                        setEffect(
                            EpisodesState.Effect.ShowToast(
                                result.message
                                    ?: stringProvider.get(R.string.details_download_resolve_error)
                            )
                        )
                    }

                    PlayerStreamResolveResult.Failed,
                    PlayerStreamResolveResult.Unsupported -> {
                        setState { copy(resolvingDownloadKeys = resolvingDownloadKeys - key) }
                        setEffect(EpisodesState.Effect.ShowToast(stringProvider.get(R.string.details_download_resolve_error)))
                    }
                }
            } catch (throwable: Throwable) {
                setState { copy(resolvingDownloadKeys = resolvingDownloadKeys - key) }
                setEffect(
                    EpisodesState.Effect.ShowToast(
                        throwable.localizedMessage
                            ?: stringProvider.get(R.string.details_download_resolve_error)
                    )
                )
            }
        }
    }

    private fun showDownloadQualityPicker(
        video: AnimeVideo,
        result: PlayerStreamResolveResult.Stream,
    ) {
        val key = video.downloadStatusKey()
        val options = prepareDownloadQualities(result.url, result.qualities)
        pendingDownloadCandidate = DownloadCandidate(
            video = video,
            options = options,
            headers = result.headers,
        )
        setState {
            copy(
                resolvingDownloadKeys = resolvingDownloadKeys - key,
                pendingDownloadQualitySelection = EpisodesState.EpisodeDownloadQualitySelection(
                    videoId = video.id,
                    episode = video.episode,
                    options = options.map { it.toUiOption() },
                )
            )
        }
    }

    private fun enqueueSelectedDownload(option: EpisodesState.EpisodeDownloadQualityOption) {
        val candidate = pendingDownloadCandidate ?: return
        val quality =
            candidate.options.firstOrNull { it.label == option.label && it.url == option.url }
                ?: return
        val video = candidate.video
        val request = VideoDownloadRequest(
            animeId = animeId,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            episode = video.episode,
            videoId = video.id,
            playerName = video.player,
            playerId = video.playerId,
            dubbing = video.dubbing,
            iframeUrl = video.iframeUrl,
            screenshotUrl = screenshotsByEpisode[video.episode].orEmpty(),
            quality = quality,
            headers = candidate.headers,
        )
        viewModelScope.launch {
            runCatching { enqueueVideoDownload(request) }
                .onFailure {
                    setEffect(EpisodesState.Effect.ShowToast(stringProvider.get(R.string.details_download_enqueue_error)))
                }
            pendingDownloadCandidate = null
            setState { copy(pendingDownloadQualitySelection = null) }
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

    private fun AnimeVideo.downloadStatusKey(): String =
        listOf(id.toString(), iframeUrl).joinToString("|")

    private val VideoDownloadItem.uiStatusKey: String
        get() = listOf(videoId.toString(), iframeUrl).joinToString("|")

    private fun VideoDownloadItem.toUiState(): EpisodesState.EpisodeDownloadUiState =
        EpisodesState.EpisodeDownloadUiState(
            status = when (status) {
                VideoDownloadStatus.Queued,
                VideoDownloadStatus.Resolving -> EpisodesState.EpisodeDownloadUiStatus.Queued

                VideoDownloadStatus.Downloading,
                VideoDownloadStatus.Deleting -> EpisodesState.EpisodeDownloadUiStatus.Downloading

                VideoDownloadStatus.Downloaded -> EpisodesState.EpisodeDownloadUiStatus.Downloaded
                VideoDownloadStatus.Failed -> EpisodesState.EpisodeDownloadUiStatus.Failed
                VideoDownloadStatus.Idle,
                VideoDownloadStatus.Deleted -> EpisodesState.EpisodeDownloadUiStatus.Failed
            },
            progress = progress.coerceIn(0f, 1f),
        )

    private val EpisodesState.EpisodeDownloadUiStatus.isActive: Boolean
        get() = this != EpisodesState.EpisodeDownloadUiStatus.Failed

    private fun VideoDownloadQualityOption.toUiOption(): EpisodesState.EpisodeDownloadQualityOption =
        EpisodesState.EpisodeDownloadQualityOption(label = label, url = url)

    private data class DownloadCandidate(
        val video: AnimeVideo,
        val options: List<VideoDownloadQualityOption>,
        val headers: Map<String, String>,
    )
}
