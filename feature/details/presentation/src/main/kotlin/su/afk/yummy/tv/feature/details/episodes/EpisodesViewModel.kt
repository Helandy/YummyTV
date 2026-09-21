package su.afk.yummy.tv.feature.details.episodes

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentSet
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
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeEpisodeInfoUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeWatchProgressUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadStatusesUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.ObserveWatchLaterEpisodesUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.dubbings.selectEpisodeDubbingLaunchVideo
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadEnqueueResult
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadHandler
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeDownloadPrepareResult
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchLaterHandler
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchedHandler
import su.afk.yummy.tv.feature.details.episodes.utils.buildEpisodeGroups
import su.afk.yummy.tv.feature.details.episodes.utils.isActive
import su.afk.yummy.tv.feature.details.episodes.utils.resolveDownloadStatuses
import su.afk.yummy.tv.feature.details.episodes.utils.uiStatusKey
import su.afk.yummy.tv.feature.details.mapper.episodeDubbingItems
import su.afk.yummy.tv.feature.details.mapper.toUiState
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.player.isKodikPlayerUrl
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator

@HiltViewModel(assistedFactory = EpisodesViewModel.Factory::class)
class EpisodesViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    @Assisted private val pendingEpisode: String?,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val videoDownloadNavigator: IVideoDownloadNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val getAnimeEpisodeInfo: GetAnimeEpisodeInfoUseCase,
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase,
    private val observeAnimeWatchProgress: ObserveAnimeWatchProgressUseCase,
    private val settingsStore: PlayerSettingsStore,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val downloadHandler: EpisodeDownloadHandler,
    private val watchedHandler: EpisodeWatchedHandler,
    private val watchLaterHandler: EpisodeWatchLaterHandler,
    private val observeVideoDownloadStatuses: ObserveVideoDownloadStatusesUseCase,
    private val observeWatchLaterEpisodes: ObserveWatchLaterEpisodesUseCase,
    private val stringProvider: StringProvider,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<EpisodesState.State, EpisodesState.Event, EpisodesState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int, pendingEpisode: String?): EpisodesViewModel
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
    private var pendingEpisodeHandled = false

    init {
        analytics.eventEpisodesScreenOpened(animeId)
        viewModelScope.launch { loadMeta() }
        viewModelScope.launch { loadVideos() }
        viewModelScope.launch { loadEpisodeInfo() }
        observeWatchLaterEpisodes(animeId)
            .onEach { episodes ->
                setState { copy(watchLaterEpisodes = episodes.toPersistentSet()) }
            }
            .launchIn(viewModelScope)
        observeVideoDownloadStatuses(animeId)
            .onEach { statuses ->
                setState {
                    val uiStatuses =
                        statuses.values.associate { it.uiStatusKey to it.toUiState() }
                            .toImmutableMap()
                    copy(
                        downloadStatuses = uiStatuses,
                        resolvedDownloadStatuses = resolveDownloadStatuses(
                            episodeGroups,
                            uiStatuses
                        ),
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
            is EpisodesState.Event.EpisodeDescriptionToggled -> setState {
                copy(
                    expandedEpisodeDescriptions = if (event.episode in expandedEpisodeDescriptions) {
                        expandedEpisodeDescriptions - event.episode
                    } else {
                        expandedEpisodeDescriptions + event.episode
                    }
                )
            }

            is EpisodesState.Event.EpisodeSelected -> {
                analytics.eventEpisodesVideoSelected(animeId, event.video.id)
                showEpisodeDubbingPicker(event.video, restrictToBalancer = false)
            }

            is EpisodesState.Event.EpisodeDubbingSelected -> {
                setState { copy(pendingEpisodeDubbingSelection = null) }
                showBalancerPickerForDubbing(event.video)
            }

            EpisodesState.Event.EpisodeDubbingPickerDismissed ->
                setState { copy(pendingEpisodeDubbingSelection = null) }

            is EpisodesState.Event.EpisodeActionsRequested -> {
                val videos = event.videos.toImmutableList()
                val episode = videos.firstOrNull()?.episode.orEmpty()
                if (episode.isBlank()) return
                setState {
                    copy(
                        pendingEpisodeAction = EpisodesState.EpisodeAction(
                            episode = episode,
                            videos = videos,
                            isWatched = watchProgress.bestFor(videos)?.isWatchedProgress() == true,
                            isInWatchLater = episode.episodeGroupKey() in watchLaterEpisodes,
                        )
                    )
                }
            }

            EpisodesState.Event.EpisodeWatchedToggled -> {
                val action = currentState.pendingEpisodeAction ?: return
                setState { copy(pendingEpisodeAction = null) }
                viewModelScope.launch { toggleEpisodeWatched(action) }
            }

            EpisodesState.Event.EpisodeWatchLaterToggled -> {
                val action = currentState.pendingEpisodeAction ?: return
                setState { copy(pendingEpisodeAction = null) }
                viewModelScope.launch {
                    watchLaterHandler.toggle(
                        animeId = animeId,
                        episode = action.episode,
                        isInWatchLater = action.isInWatchLater,
                        meta = EpisodeWatchedHandler.EpisodeMeta(
                            animeTitle = animeTitle,
                            posterUrl = posterUrl,
                            screenshotUrl = screenshotsByEpisode[action.episode].orEmpty(),
                        ),
                    )
                    // Список отложенных на этом экране не виден, поэтому подтверждаем действие тостом.
                    setEffect(
                        EpisodesState.Effect.ShowToast(
                            stringProvider.get(
                                if (action.isInWatchLater) {
                                    R.string.details_episode_watch_later_removed
                                } else {
                                    R.string.details_episode_watch_later_added
                                }
                            )
                        )
                    )
                }
            }

            EpisodesState.Event.EpisodeActionsDismissed ->
                setState { copy(pendingEpisodeAction = null) }

            is EpisodesState.Event.BalancerConfirmed -> {
                analytics.eventEpisodesBalancerConfirmed(animeId, event.video)
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
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
                            videos = event.videos.toImmutableList(),
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

    /**
     * Ручная отметка серии просмотренной и её снятие.
     *
     * Список серий перечитывается с сервера: серверные отметки кешируются вместе с видео, и без
     * этого снятая отметка вернулась бы при следующем слиянии локального и серверного прогресса.
     * Локальное состояние подхватит подписка [observeAnimeWatchProgress] — вручную его не трогаем.
     */
    private suspend fun toggleEpisodeWatched(action: EpisodesState.EpisodeAction) {
        val succeeded = if (action.isWatched) {
            watchedHandler.unmarkWatched(
                animeId = animeId,
                episode = action.episode,
                videos = action.videos,
                isSignedIn = isSignedIn,
            )
        } else {
            watchedHandler.markWatched(
                animeId = animeId,
                episode = action.episode,
                videos = action.videos,
                bestDubbing = currentState.bestDubbing,
                existing = currentState.watchProgress.bestFor(action.videos),
                meta = EpisodeWatchedHandler.EpisodeMeta(
                    animeTitle = animeTitle,
                    posterUrl = posterUrl,
                    screenshotUrl = screenshotsByEpisode[action.episode].orEmpty(),
                ),
                isSignedIn = isSignedIn,
            )
        }

        if (!succeeded) {
            setEffect(
                EpisodesState.Effect.ShowToast(
                    stringProvider.get(R.string.details_episode_watched_sync_failed)
                )
            )
        }
        if (isSignedIn) {
            refreshVideosFromNetwork()
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

    private suspend fun loadEpisodeInfo() {
        val info = runCatching { getAnimeEpisodeInfo(animeId) }.getOrDefault(emptyMap())
        if (info.isNotEmpty()) {
            setState { copy(episodeInfo = info.toImmutableMap()) }
        }
    }

    private suspend fun loadVideos() {
        setState { copy(videosState = VideosUiState.Loading) }
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                setVideos(videos)
                consumePendingEpisode(videos)
                if (isSignedIn) {
                    refreshVideosFromNetwork()
                }
            },
            onFailure = {
                setState {
                    copy(
                        videosState = VideosUiState.Error(it.userMessage()),
                        watchProgress = DetailsWatchProgressIndex.Empty
                    )
                }
            },
        )
    }

    /**
     * Автозапуск/пикер для эпизода, с которым открыли экран (например, из истории просмотров).
     * `episodeGroupKey()`, а не точное сравнение строк — [pendingEpisode] приходит из другого API
     * (история), где нормализация номера серии может отличаться от `/anime/{id}/videos`.
     */
    private fun consumePendingEpisode(videos: List<AnimeVideo>) {
        if (pendingEpisodeHandled) return
        val target = pendingEpisode ?: return
        pendingEpisodeHandled = true
        val candidates = videos.filter { it.episode.episodeGroupKey() == target.episodeGroupKey() }
        val video = candidates.firstOrNull() ?: return
        if (candidates.size <= 1) {
            navigateToPlayer(video)
        } else {
            showEpisodeDubbingPicker(video, restrictToBalancer = false)
        }
    }

    private suspend fun refreshVideosFromNetwork() {
        runCatching { refreshAnimeVideos(animeId) }
            .onSuccess { videos -> setVideos(videos) }
    }

    private fun setVideos(videos: List<AnimeVideo>) {
        setState {
            val groups = buildEpisodeGroups(videos)
            copy(
                videosState = if (videos.isEmpty()) VideosUiState.Empty else VideosUiState.Content(
                    videos.toImmutableList()
                ),
                watchProgress = buildWatchProgressIndex(videos),
                episodeGroups = groups,
                bestDubbing = resolveBestDubbing(videos),
                resolvedDownloadStatuses = resolveDownloadStatuses(groups, downloadStatuses),
            )
        }
    }

    /** Озвучка с наибольшим числом просмотров среди kodik-источников. */
    private fun resolveBestDubbing(videos: List<AnimeVideo>): String {
        val source = videos.filter { it.iframeUrl.isKodikPlayerUrl() }.ifEmpty { videos }
        return source.groupBy { it.dubbing }
            .maxByOrNull { (_, list) -> list.sumOf { it.views ?: 0 } }
            ?.key ?: source.firstOrNull()?.dubbing ?: ""
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
            allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: videos,
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

    private fun showBalancerPickerForDubbing(video: AnimeVideo) {
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        val dubbingVideos = allVideos.filter {
            it.episode.episodeGroupKey() == video.episode.episodeGroupKey() &&
                    it.dubbing.trim() == video.dubbing.trim()
        }
        when (val selection = playerNavigationHandler.selectPlayer(
            video = video,
            allVideos = dubbingVideos,
            preferredPlayer = preferredPlayerState.value,
        )) {
            is DetailsPlayerSelection.Navigate -> navigateToPlayer(selection.video)
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
                candidateVideos.firstOrNull {
                    it.episode.episodeGroupKey() == episode.episodeGroupKey() &&
                            it.dubbing.trim() == item.name
                }
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
        val info = currentState.episodeInfo[episode]
        setState {
            copy(
                pendingEpisodeDubbingSelection = EpisodesState.EpisodeDubbingSelection(
                    episode = episode,
                    options = options.toImmutableList(),
                    episodeTitle = info?.title,
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
        nav.navigate(
            playerNavigationHandler.getPlayerDestination(
                video = video,
                animeTitle = animeTitle,
                animeId = animeId,
                posterUrl = posterUrl,
                screenshotByEpisode = screenshotsByEpisode,
                resumeFromMs = currentState.watchProgress.resumeFromMsFor(video),
            )
        )
    }

}
