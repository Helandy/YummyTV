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
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeWatchProgressUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadStatusesUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.episodes.dubbings.episodeDubbingItems
import su.afk.yummy.tv.feature.details.episodes.dubbings.selectEpisodeDubbingLaunchVideo
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadEnqueueResult
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadHandler
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadPrepareResult
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator

@HiltViewModel(assistedFactory = EpisodesViewModel.Factory::class)
class EpisodesViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val videoDownloadNavigator: IVideoDownloadNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase,
    private val observeAnimeWatchProgress: ObserveAnimeWatchProgressUseCase,
    private val settingsStore: SettingsStore,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val downloadHandler: EpisodeDownloadHandler,
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
            EpisodesState.Event.RetryVideosSelected -> viewModelScope.launch { loadVideos() }
            is EpisodesState.Event.EpisodeDubbingsSelected -> {
                analytics.eventEpisodesEpisodeDubbingsSelected(animeId)
                nav.navigate(detailsNavigator.getEpisodeDubbingsDest(animeId, event.episode))
            }

            is EpisodesState.Event.TvEpisodeSelected -> {
                analytics.eventEpisodesVideoSelected(animeId, event.video.id)
                showTvBalancerPicker(event.video)
            }

            is EpisodesState.Event.EpisodeDubbingSelected -> {
                setState { copy(pendingEpisodeDubbingSelection = null) }
                navigateToPlayer(event.video)
            }

            EpisodesState.Event.EpisodeDubbingPickerDismissed ->
                setState { copy(pendingEpisodeDubbingSelection = null) }

            is EpisodesState.Event.VideoSelected -> {
                analytics.eventEpisodesVideoSelected(animeId, event.video.id)
                showBalancerPicker(event.video)
            }

            is EpisodesState.Event.BalancerConfirmed -> {
                analytics.eventEpisodesBalancerConfirmed(animeId, event.video)
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
            }

            is EpisodesState.Event.TvBalancerConfirmed -> {
                analytics.eventEpisodesBalancerConfirmed(animeId, event.video)
                setState { copy(pendingBalancerSelection = null) }
                showEpisodeDubbingPicker(event.video)
            }

            is EpisodesState.Event.EpisodeDownloadSelected -> {
                downloadHandler.beginNewDownload()
                showDownloadDubbingPicker(event.videos)
            }

            is EpisodesState.Event.DownloadedEpisodeSelected -> {
                val downloadedDubbing = event.download.dubbing.ifBlank { return }
                setState {
                    copy(
                        pendingDownloadedEpisodeAction = EpisodesState.DownloadedEpisodeAction(
                            downloadId = event.download.downloadId,
                            episode = event.videos.firstOrNull()?.episode.orEmpty(),
                            downloadedDubbing = downloadedDubbing,
                            playerName = event.download.playerName,
                            qualityLabel = event.download.qualityLabel,
                            bytesDownloaded = event.download.bytesDownloaded,
                            videos = event.videos,
                            hasAlternativeDubbings = event.videos.any {
                                downloadHandler.downloadDubbingName(it) != downloadedDubbing
                            },
                        )
                    )
                }
            }

            EpisodesState.Event.PlayDownloadedEpisodeSelected -> {
                val action = currentState.pendingDownloadedEpisodeAction ?: return
                setState { copy(pendingDownloadedEpisodeAction = null) }
                nav.navigate(playerNavigationHandler.getDownloadedPlayerDestination(action.downloadId))
            }

            EpisodesState.Event.RedownloadDubbingSelected -> {
                val action = currentState.pendingDownloadedEpisodeAction ?: return
                downloadHandler.beginReplacement(action.downloadId)
                setState { copy(pendingDownloadedEpisodeAction = null) }
                showDownloadDubbingPicker(
                    videos = action.videos,
                    excludedDubbing = action.downloadedDubbing,
                )
            }

            EpisodesState.Event.DeleteDownloadedEpisodeSelected -> {
                val action = currentState.pendingDownloadedEpisodeAction ?: return
                setState { copy(pendingDownloadedEpisodeAction = null) }
                viewModelScope.launch {
                    downloadHandler.delete(action.downloadId)
                }
            }

            EpisodesState.Event.DownloadedEpisodeActionDismissed -> {
                setState { copy(pendingDownloadedEpisodeAction = null) }
            }

            is EpisodesState.Event.DownloadDubbingSelected -> {
                setState { copy(pendingDownloadDubbingSelection = null) }
                showDownloadBalancerPicker(event.videos)
            }

            is EpisodesState.Event.DownloadBalancerSelected -> {
                setState { copy(pendingDownloadBalancerSelection = null) }
                prepareEpisodeDownload(event.video)
            }

            EpisodesState.Event.DownloadDubbingPickerDismissed -> {
                downloadHandler.dismissSourcePicker()
                setState { copy(pendingDownloadDubbingSelection = null) }
            }

            EpisodesState.Event.DownloadBalancerPickerDismissed -> {
                downloadHandler.dismissSourcePicker()
                setState { copy(pendingDownloadBalancerSelection = null) }
            }

            is EpisodesState.Event.DownloadQualitySelected -> enqueueSelectedDownload(event.option)

            EpisodesState.Event.DownloadQualityPickerDismissed -> {
                downloadHandler.clearPending()
                setState { copy(pendingDownloadQualitySelection = null) }
            }

            EpisodesState.Event.BalancerPickerDismissed ->
                setState { copy(pendingBalancerSelection = null) }

            EpisodesState.Event.OpenDownloadsScreenSelected ->
                nav.navigate(videoDownloadNavigator.getVideoDownloadDest())
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
                        videosState = VideosUiState.Error(it.message),
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
                watchProgress = buildWatchProgressIndex(videos),
            )
        }
    }

    private fun updateMergedWatchProgress(
        serverVideos: List<AnimeVideo> = (currentState.videosState as? VideosUiState.Content)?.videos.orEmpty(),
    ) {
        setState { copy(watchProgress = buildWatchProgressIndex(serverVideos)) }
    }

    private fun buildWatchProgressIndex(videos: List<AnimeVideo>): DetailsWatchProgressIndex =
        DetailsWatchProgressIndex.merge(
            animeId = animeId,
            localEntries = localWatchProgress,
            videos = videos,
        )

    private fun showDownloadDubbingPicker(
        videos: List<AnimeVideo>,
        excludedDubbing: String? = null,
    ) {
        val selection = downloadHandler.dubbingSelection(
            videos = videos,
            statuses = currentState.downloadStatuses,
            resolvingKeys = currentState.resolvingDownloadKeys,
            excludedDubbing = excludedDubbing,
        )
        setState {
            copy(pendingDownloadDubbingSelection = selection)
        }
    }

    private fun showDownloadBalancerPicker(videos: List<AnimeVideo>) {
        val selection = downloadHandler.balancerSelection(
            videos = videos,
            statuses = currentState.downloadStatuses,
            resolvingKeys = currentState.resolvingDownloadKeys,
        )
        setState {
            copy(pendingDownloadBalancerSelection = selection)
        }
    }

    private fun prepareEpisodeDownload(video: AnimeVideo) {
        val key = downloadHandler.downloadStatusKey(video)
        if (currentState.downloadStatuses[key]?.status?.isActive == true || key in currentState.resolvingDownloadKeys) {
            return
        }
        viewModelScope.launch {
            setState { copy(resolvingDownloadKeys = resolvingDownloadKeys + key) }
            setEffect(EpisodesState.Effect.ShowToast(stringProvider.get(R.string.details_download_resolving_quality)))
            when (val result = downloadHandler.prepare(video)) {
                is EpisodeDownloadPrepareResult.Ready -> setState {
                    copy(
                        resolvingDownloadKeys = resolvingDownloadKeys - result.key,
                        pendingDownloadQualitySelection = result.selection,
                    )
                }

                is EpisodeDownloadPrepareResult.Failure -> {
                    setState {
                        copy(
                            resolvingDownloadKeys = resolvingDownloadKeys - result.key,
                            pendingDownloadQualitySelection = null,
                        )
                    }
                    setEffect(EpisodesState.Effect.ShowToast(result.message))
                }
            }
        }
    }

    private fun enqueueSelectedDownload(option: EpisodesState.EpisodeDownloadQualityOption) {
        val episodeVideos = (currentState.videosState as? VideosUiState.Content)
            ?.videos
            .orEmpty()
            .filter { it.episode == currentState.pendingDownloadQualitySelection?.episode }
        viewModelScope.launch {
            when (downloadHandler.enqueue(
                option = option,
                animeId = animeId,
                animeTitle = animeTitle,
                posterUrl = posterUrl,
                episodeVideos = episodeVideos,
            )) {
                EpisodeDownloadEnqueueResult.ReplacementDeleteFailed -> setEffect(
                    EpisodesState.Effect.ShowToast(
                        stringProvider.get(R.string.details_download_replace_delete_error)
                    )
                )

                EpisodeDownloadEnqueueResult.EnqueueFailed -> {
                    setEffect(
                        EpisodesState.Effect.ShowToast(
                            stringProvider.get(R.string.details_download_enqueue_error)
                        )
                    )
                    setState { copy(pendingDownloadQualitySelection = null) }
                }

                EpisodeDownloadEnqueueResult.Success ->
                    setState { copy(pendingDownloadQualitySelection = null) }

                EpisodeDownloadEnqueueResult.MissingCandidate -> Unit
            }
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
            is DetailsPlayerSelection.ShowPicker -> {
                reportUnsupportedPlayers(selection.picker)
                setState { copy(pendingBalancerSelection = selection.picker) }
            }
        }
    }

    private fun showTvBalancerPicker(video: AnimeVideo) {
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        when (val selection = playerNavigationHandler.selectPlayer(
            video = video,
            allVideos = allVideos,
            preferredPlayer = preferredPlayerState.value,
        )) {
            is DetailsPlayerSelection.Navigate ->
                showEpisodeDubbingPicker(selection.video, restrictToBalancer = false)

            is DetailsPlayerSelection.ShowPicker -> {
                reportUnsupportedPlayers(selection.picker)
                setState { copy(pendingBalancerSelection = selection.picker) }
            }
        }
    }

    private fun showEpisodeDubbingPicker(
        balancerVideo: AnimeVideo,
        restrictToBalancer: Boolean = true
    ) {
        val episode = balancerVideo.episode
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        // Пользователь явно выбрал балансер в пикере — сужаем озвучки до него.
        // Если балансер подставился тихо (по предпочтению) — показываем все озвучки со всех балансеров.
        val candidateVideos = if (restrictToBalancer) {
            allVideos.filter { it.player == balancerVideo.player }
        } else {
            allVideos
        }
        val options = candidateVideos.episodeDubbingItems(episode).mapNotNull { item ->
            val video = if (restrictToBalancer) {
                candidateVideos.firstOrNull { it.episode == episode && it.dubbing.trim() == item.name }
            } else {
                allVideos.selectEpisodeDubbingLaunchVideo(
                    episode = episode,
                    dubbingName = item.name,
                    preferredPlayer = preferredPlayerState.value,
                )
            }
            video?.let { EpisodesState.EpisodeDubbingOption(video = it, item = item) }
        }
        if (options.isEmpty()) {
            navigateToPlayer(balancerVideo)
            return
        }
        setState {
            copy(
                pendingEpisodeDubbingSelection = EpisodesState.EpisodeDubbingSelection(
                    episode = episode,
                    options = options,
                )
            )
        }
    }

    private fun reportUnsupportedPlayers(picker: BalancerPickerState) {
        picker.options
            .filter { !it.isSupported }
            .forEach { option ->
                analytics.eventEpisodesUnsupportedPlayerShown(
                    animeId = animeId,
                    episode = picker.episodeNumber,
                    playerName = option.playerName,
                )
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

    private companion object {
        val VideoDownloadItem.uiStatusKey: String
            get() = listOf(videoId.toString(), iframeUrl).joinToString("|")

        fun VideoDownloadItem.toUiState(): EpisodesState.EpisodeDownloadUiState =
            EpisodesState.EpisodeDownloadUiState(
                downloadId = id,
                dubbing = dubbing.ifBlank { playerName },
                playerName = playerName,
                qualityLabel = qualityLabel,
                bytesDownloaded = bytesDownloaded,
                status = when (status) {
                    VideoDownloadStatus.Queued,
                    VideoDownloadStatus.Resolving -> EpisodesState.EpisodeDownloadUiStatus.Queued

                    VideoDownloadStatus.Downloading,
                    VideoDownloadStatus.Deleting -> EpisodesState.EpisodeDownloadUiStatus.Downloading

                    VideoDownloadStatus.Paused -> EpisodesState.EpisodeDownloadUiStatus.Paused
                    VideoDownloadStatus.Downloaded -> EpisodesState.EpisodeDownloadUiStatus.Downloaded
                    VideoDownloadStatus.Failed -> EpisodesState.EpisodeDownloadUiStatus.Failed
                    VideoDownloadStatus.Idle,
                    VideoDownloadStatus.Deleted -> EpisodesState.EpisodeDownloadUiStatus.Failed
                },
                progress = progress.coerceIn(0f, 1f),
                errorMessage = errorMessage?.takeIf { it.isNotBlank() },
            )

        val EpisodesState.EpisodeDownloadUiStatus.isActive: Boolean
            get() = this != EpisodesState.EpisodeDownloadUiStatus.Failed
    }

}
