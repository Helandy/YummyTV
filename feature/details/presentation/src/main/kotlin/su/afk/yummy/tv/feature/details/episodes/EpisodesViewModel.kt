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
import su.afk.yummy.tv.core.model.anime.kodikThumbnailIframeUrl
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
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
import su.afk.yummy.tv.domain.videodownload.usecase.CancelOrDeleteVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.EnqueueVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadStatusesUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.PrepareVideoDownloadQualityOptionsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.episodes.dubbings.episodeDubbingItems
import su.afk.yummy.tv.feature.details.episodes.dubbings.selectEpisodeDubbingLaunchVideo
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority
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
    private val resolvePlayerStream: ResolvePlayerStreamUseCase,
    private val prepareDownloadQualities: PrepareVideoDownloadQualityOptionsUseCase,
    private val enqueueVideoDownload: EnqueueVideoDownloadUseCase,
    private val cancelOrDeleteVideoDownload: CancelOrDeleteVideoDownloadUseCase,
    private val observeVideoDownloadStatuses: ObserveVideoDownloadStatusesUseCase,
    private val stringProvider: StringProvider,
    private val analytics: DetailsAnalytics,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
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
    private var pendingReplacementDownloadId: Long? = null

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
                pendingReplacementDownloadId = null
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
                                it.downloadDubbingName() != downloadedDubbing
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
                pendingReplacementDownloadId = action.downloadId
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
                    cancelOrDeleteVideoDownload(action.downloadId)
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
                pendingReplacementDownloadId = null
                setState { copy(pendingDownloadDubbingSelection = null) }
            }

            EpisodesState.Event.DownloadBalancerPickerDismissed -> {
                pendingReplacementDownloadId = null
                setState { copy(pendingDownloadBalancerSelection = null) }
            }

            is EpisodesState.Event.DownloadQualitySelected -> enqueueSelectedDownload(event.option)

            EpisodesState.Event.DownloadQualityPickerDismissed -> {
                pendingDownloadCandidate = null
                pendingReplacementDownloadId = null
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

    private fun showDownloadDubbingPicker(
        videos: List<AnimeVideo>,
        excludedDubbing: String? = null,
    ) {
        val availableVideos = videos.filter { video ->
            excludedDubbing == null || video.downloadDubbingName() != excludedDubbing
        }
        val options = availableVideos
            .groupBy { it.downloadDubbingName() }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<AnimeVideo>>> { (_, group) ->
                    group.sumOf { it.views ?: 0 }
                }
                    .thenBy { (dubbing, _) -> dubbing }
            )
            .map { (dubbing, group) ->
                EpisodesState.EpisodeDownloadDubbingOption(
                    videos = group,
                    title = dubbing,
                    subtitle = group
                        .map { it.player }
                        .distinct()
                        .joinToString(" / ")
                        .takeIf { it.isNotBlank() && it != dubbing },
                    status = group.aggregateDubbingDownloadStatus(),
                    resolving = group.all { it.downloadStatusKey() in currentState.resolvingDownloadKeys },
                )
            }
        val episode = videos.firstOrNull()?.episode.orEmpty()
        setState {
            copy(
                pendingDownloadDubbingSelection = EpisodesState.EpisodeDownloadDubbingSelection(
                    episode = episode,
                    options = options,
                    hasAlternativeDubbings = excludedDubbing != null,
                )
            )
        }
    }

    private fun showDownloadBalancerPicker(videos: List<AnimeVideo>) {
        val options = videos
            .sortedWith(
                compareByDescending<AnimeVideo> { it.views ?: 0 }
                    .thenBy {
                        minOf(
                            it.player.playerDisplayOrderPriority(),
                            it.iframeUrl.playerDisplayOrderPriority(),
                        )
                    }
                    .thenBy { it.player }
                    .thenBy { it.playerId ?: Int.MAX_VALUE }
                    .thenBy { it.id }
            )
            .map { video ->
                val key = video.downloadStatusKey()
                EpisodesState.EpisodeDownloadBalancerOption(
                    video = video,
                    title = video.player.ifBlank { video.dubbing },
                    subtitle = null,
                    status = currentState.downloadStatuses[key],
                    resolving = key in currentState.resolvingDownloadKeys,
                )
            }
        val firstVideo = videos.firstOrNull()
        val dubbing = firstVideo?.dubbing?.ifBlank { firstVideo.player }.orEmpty()
        setState {
            copy(
                pendingDownloadBalancerSelection = EpisodesState.EpisodeDownloadBalancerSelection(
                    episode = firstVideo?.episode.orEmpty(),
                    dubbing = dubbing,
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

                    is PlayerStreamResolveResult.Unavailable -> {
                        setState { copy(resolvingDownloadKeys = resolvingDownloadKeys - key) }
                        setEffect(
                            EpisodesState.Effect.ShowToast(
                                result.message
                                    ?: stringProvider.get(R.string.details_download_dubbing_unavailable)
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
        val isAlloha = video.player.isAllohaPlayerUrl() || video.iframeUrl.isAllohaPlayerUrl()
        val options = prepareDownloadQualities(
            streamUrl = result.url,
            qualityMap = result.qualities,
            qualityHeaders = result.qualityHeaders,
            numericQualitiesOnly = isAlloha,
        )
        if (options.isEmpty()) {
            pendingDownloadCandidate = null
            setState {
                copy(
                    resolvingDownloadKeys = resolvingDownloadKeys - key,
                    pendingDownloadQualitySelection = null,
                )
            }
            setEffect(
                EpisodesState.Effect.ShowToast(
                    stringProvider.get(R.string.details_download_resolve_error)
                )
            )
            return
        }
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
        val episodeVideos = (currentState.videosState as? VideosUiState.Content)
            ?.videos
            .orEmpty()
            .filter { it.episode == video.episode }
        val replacementDownloadId = pendingReplacementDownloadId
        viewModelScope.launch {
            val screenshotUrl = episodeVideos.kodikThumbnailIframeUrl()
                ?.let { resolveKodikThumbnailUrl(it) }
                .orEmpty()
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
                screenshotUrl = screenshotUrl,
                quality = quality,
                headers = quality.headers.ifEmpty { candidate.headers },
            )
            replacementDownloadId?.let { downloadId ->
                runCatching { cancelOrDeleteVideoDownload(downloadId) }
                    .onFailure {
                        setEffect(
                            EpisodesState.Effect.ShowToast(
                                stringProvider.get(R.string.details_download_replace_delete_error)
                            )
                        )
                        return@launch
                    }
            }
            runCatching { enqueueVideoDownload(request) }
                .onFailure {
                    setEffect(EpisodesState.Effect.ShowToast(stringProvider.get(R.string.details_download_enqueue_error)))
                }
            pendingDownloadCandidate = null
            pendingReplacementDownloadId = null
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

    private fun AnimeVideo.downloadStatusKey(): String =
        listOf(id.toString(), iframeUrl).joinToString("|")

    private fun AnimeVideo.downloadDubbingName(): String = dubbing.ifBlank { player }

    private val VideoDownloadItem.uiStatusKey: String
        get() = listOf(videoId.toString(), iframeUrl).joinToString("|")

    private fun VideoDownloadItem.toUiState(): EpisodesState.EpisodeDownloadUiState =
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

    private val EpisodesState.EpisodeDownloadUiStatus.isActive: Boolean
        get() = this != EpisodesState.EpisodeDownloadUiStatus.Failed

    private fun List<AnimeVideo>.aggregateDubbingDownloadStatus(): EpisodesState.EpisodeDownloadUiState? {
        val statuses = map { currentState.downloadStatuses[it.downloadStatusKey()] }
        if (statuses.isEmpty() || statuses.any { it == null }) return null
        val present = statuses.filterNotNull()
        return when {
            present.all {
                it.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
                        it.status == EpisodesState.EpisodeDownloadUiStatus.Downloading
            } -> present.first()

            present.all { it.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded } -> present.first()
            present.all { it.status == EpisodesState.EpisodeDownloadUiStatus.Paused } -> present.first()
            present.all { it.status == EpisodesState.EpisodeDownloadUiStatus.Failed } -> present.first()
            else -> null
        }
    }

    private fun VideoDownloadQualityOption.toUiOption(): EpisodesState.EpisodeDownloadQualityOption =
        EpisodesState.EpisodeDownloadQualityOption(
            label = label,
            url = url,
        )

    private data class DownloadCandidate(
        val video: AnimeVideo,
        val options: List<VideoDownloadQualityOption>,
        val headers: Map<String, String>,
    )
}
