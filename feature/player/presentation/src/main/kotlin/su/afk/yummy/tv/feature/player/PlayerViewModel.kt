package su.afk.yummy.tv.feature.player

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.player.PlayerViewModel.Companion.CHANGE_PLAYER_HINT_DELAY_MS
import su.afk.yummy.tv.feature.player.behavior.AllohaSourceBehavior
import su.afk.yummy.tv.feature.player.behavior.DefaultSourceBehavior
import su.afk.yummy.tv.feature.player.behavior.PlayerSourceBehavior
import su.afk.yummy.tv.feature.player.delegate.PlayerNavigationDelegate
import su.afk.yummy.tv.feature.player.delegate.PlayerOfflineSourceLoader
import su.afk.yummy.tv.feature.player.delegate.PlayerPreferencesBinder
import su.afk.yummy.tv.feature.player.handler.PlayerDisplaySettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerFinalEpisodeActionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackProgressHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceGraphLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerSourceSelectionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceStreamHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerStreamResumeMode
import su.afk.yummy.tv.feature.player.host.PlayerChangePlayerHint
import su.afk.yummy.tv.feature.player.host.PlayerSourceHost
import su.afk.yummy.tv.feature.player.host.PlayerStreamLoadRequest
import su.afk.yummy.tv.feature.player.mapper.PlayerDestinationStateMapper
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.navigator.PLAYER_CONTENT_KEY
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.activePlayerId
import su.afk.yummy.tv.feature.player.utils.activeScreenshotUrl
import su.afk.yummy.tv.feature.player.utils.activeVideoId

/**
 * Оркестратор экрана плеера: источники, поток, прогресс.
 *
 * Поведение, зависящее от балансера, живёт в [PlayerSourceBehavior] ([AllohaSourceBehavior],
 * [DefaultSourceBehavior]); настройки, навигация и офлайн-источники — в делегатах.
 */
@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject internal constructor(
    @Assisted private val dest: PlayerDestination,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val sourceStreamHandler: PlayerSourceStreamHandler,
    private val playbackProgressHandler: PlayerPlaybackProgressHandler,
    private val preferences: PlayerPreferencesBinder,
    private val displaySettings: PlayerDisplaySettingsHandler,
    private val finalEpisodeActionHandler: PlayerFinalEpisodeActionHandler,
    private val destinationStateMapper: PlayerDestinationStateMapper,
    private val sourceSelectionHandler: PlayerSourceSelectionHandler,
    private val offlineSources: PlayerOfflineSourceLoader,
    private val navigation: PlayerNavigationDelegate,
    private val strings: StringProvider,
    private val analytics: PlayerAnalytics,
    private val allohaSource: AllohaSourceBehavior,
    private val defaultSource: DefaultSourceBehavior,
) : BaseViewModel<PlayerState.State, PlayerState.Event, PlayerState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(dest: PlayerDestination): PlayerViewModel
    }

    private var activeDest: PlayerDestination = dest

    private var pendingDestinationResumeMs: Long? = dest.resumeFromMs.takeIf { it > 0L }
    private var sourceGraphJob: Job? = null
    private var extractionJob: Job? = null
    private var finalEpisodeActionJob: Job? = null

    private val changePlayerHint = PlayerChangePlayerHint(viewModelScope, ::setState)

    /** Порядок важен: решения принимает первое поведение, чей `handles` подошёл. */
    private val sourceBehaviors: List<PlayerSourceBehavior> = listOf(allohaSource, defaultSource)

    private val host = object : PlayerSourceHost {
        override val state: PlayerState.State get() = currentState
        override val scope: CoroutineScope get() = viewModelScope
        override val changePlayerHint: PlayerChangePlayerHint get() = this@PlayerViewModel.changePlayerHint

        override fun update(reducer: PlayerState.State.() -> PlayerState.State) = setState(reducer)

        override fun loadStream(request: PlayerStreamLoadRequest) = this@PlayerViewModel.loadStream(
            resumeMode = request.resumeMode,
            refreshSourcesOnFailure = request.refreshSourcesOnFailure,
            forceFreshAllohaSession = request.forceFreshAllohaSession,
            selectedQualityOverride = request.selectedQualityOverride,
            forceRefresh = request.forceRefresh,
        )

        override fun cancelStreamLoad() {
            extractionJob?.cancel()
        }

        override fun closeSourceSessions() = this@PlayerViewModel.closeSourceSessions()

        override fun streamErrorMessage(): String =
            sourceStreamHandler.playbackErrorMessage(strings.get(R.string.player_stream_error))
    }

    override fun createInitialState() = destinationStateMapper.toState(dest)

    init {
        analytics.eventScreenOpened(dest.animeId)
        sourceBehaviors.forEach { it.attach(host) }
        preferences.bind(host)
        if (dest.downloadId > 0L) {
            loadDownloadedDestination(dest.downloadId)
        } else if (dest.localFileUri.isNotBlank()) {
            displaySettings.observeActive(host)
            loadLocalFileDestination(dest.localFileUri, dest.animeTitle)
        } else {
            loadFinalEpisodeAction(dest.animeId)
            displaySettings.observeActive(host)
            loadSourceGraph()
            loadStream()
        }
    }

    private fun loadDestination(newDest: PlayerDestination) {
        if (newDest == activeDest) return
        closeSourceSessions()
        resetSourceBehaviors()
        // Результаты старой серии отменяем, а не сверяем с activeDest: его переписывает и
        // syncBackStackDestination, и такая сверка выбрасывала граф, пришедший позже потока.
        sourceGraphJob?.cancel()
        changePlayerHint.cancel()
        activeDest = newDest
        pendingDestinationResumeMs = newDest.resumeFromMs.takeIf { it > 0L }
        // Настройки и обучения переносим из текущего состояния: их подписки живут весь экран.
        setState {
            destinationStateMapper.toState(
                newDest,
                autoSkipOpeningsEndings = autoSkipOpeningsEndings,
                autoSkipDelaySeconds = autoSkipDelaySeconds,
                autoPlayNextEpisode = autoPlayNextEpisode,
                nextEpisodeSwitchDelaySeconds = nextEpisodeSwitchDelaySeconds,
                pictureInPictureEnabled = pictureInPictureEnabled,
            ).copy(
                playerOrientationMode = playerOrientationMode,
                mobileGestureTutorialReady = mobileGestureTutorialReady,
                showMobileGestureTutorial = showMobileGestureTutorial,
                tvControlsTutorialReady = tvControlsTutorialReady,
                showTvControlsTutorial = showTvControlsTutorial,
                tvPlayerVolumeKeysEnabled = tvPlayerVolumeKeysEnabled,
                advancedPlayerVolumeEnabled = advancedPlayerVolumeEnabled,
                showOpeningOnTimeline = showOpeningOnTimeline,
            )
        }
        loadFinalEpisodeAction(newDest.animeId)
        displaySettings.observeActive(host, force = true)
        if (newDest.downloadId > 0L) {
            loadDownloadedDestination(newDest.downloadId)
        } else if (newDest.localFileUri.isNotBlank()) {
            loadLocalFileDestination(newDest.localFileUri, newDest.animeTitle)
        } else {
            loadSourceGraph()
            loadStream()
        }
    }

    override fun onEvent(event: PlayerState.Event) {
        when (event) {
            is PlayerState.Event.NavigateToDestination -> loadDestination(event.destination)

            PlayerState.Event.Back -> navigation.back(host, beforeLeave = ::closeSourceSessions)

            PlayerState.Event.MobileGestureTutorialDismissed ->
                preferences.dismissMobileGestureTutorial(host)

            PlayerState.Event.TvControlsTutorialDismissed ->
                preferences.dismissTvControlsTutorial(host)

            PlayerState.Event.OpenDetails -> {
                analytics.eventOpenDetails(currentState.animeId)
                navigation.openDetails(host)
            }

            PlayerState.Event.RetryStream -> {
                analytics.eventRetryStream(currentState.animeId)
                // Ручной повтор - бюджет тихих повторов заново.
                defaultSource.reset()
                if (currentState.isLocalFile) {
                    setState { copy(retryKey = retryKey + 1) }
                    activeDest.localFileUri.takeIf(String::isNotBlank)?.let {
                        loadLocalFileDestination(it, activeDest.animeTitle)
                    }
                } else if (currentState.isOfflinePlayback) {
                    setState { copy(retryKey = retryKey + 1) }
                    loadDownloadedDestination(activeDest.downloadId)
                } else if (activeSourceBehavior()?.onRetryRequested() != true) {
                    setState { copy(retryKey = retryKey + 1) }
                    closeSourceSessions()
                    loadStream(refreshSourcesOnFailure = true, forceRefresh = true)
                }
            }

            PlayerState.Event.TvAppBackgrounded ->
                navigation.returnToDetailsAfterTvBackground(host, beforeLeave = ::closeSourceSessions)

            PlayerState.Event.RateTitle -> {
                analytics.eventRateTitle(currentState.animeId)
                navigation.openRating(host)
            }

            PlayerState.Event.ManageSubscriptions -> {
                analytics.eventManageSubscriptions(currentState.animeId)
                navigation.openSubscriptions(host)
            }

            is PlayerState.Event.PlaybackError -> {
                if (activeSourceBehavior()?.onPlaybackError(event) == true) return
                // Тихие ретраи исчерпаны (или offline) — сейчас юзеру показывается окно
                // «повторить/сменить». Логируем именно здесь, а не на каждую ошибку плеера,
                // чтобы не репортить транзиентные сбои, которые сами починились ретраем.
                analytics.eventPlaybackError(
                    state = currentState,
                    message = event.message,
                    errorCode = event.errorCode,
                    errorType = event.errorType,
                    retryAttempts = defaultSource.retryAttempts,
                )
                defaultSource.reset()
                changePlayerHint.cancel()
                setState {
                    copy(
                        streamUrl = null,
                        isPlaybackRecovering = false,
                        playerError = sourceStreamHandler.playbackErrorMessage(
                            message = event.message,
                            errorCode = event.errorCode,
                        ),
                        showChangePlayerHint = false,
                    )
                }
            }

            PlayerState.Event.PlaybackReady -> {
                sourceBehaviors.forEach { it.onPlaybackReady() }
                if (currentState.isPlaybackRecovering && sourceBehaviors.none { it.isRecovering }) {
                    (activeSourceBehavior() ?: defaultSource).onPlaybackRecovered()
                    setState { copy(isPlaybackRecovering = false) }
                }
            }

            PlayerState.Event.PrevEpisode -> {
                closeSourceSessions()
                analytics.eventPrevEpisode(currentState.animeId)
                applySourceSelection(
                    sourceSelectionHandler.previousEpisode(currentState),
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                    refreshSourcesBeforeStream = true,
                )
            }

            is PlayerState.Event.NextEpisode -> {
                closeSourceSessions()
                analytics.eventNextEpisode(currentState, event.source)
                val nextState = sourceSelectionHandler.nextEpisode(currentState)
                    ?: sourceSelectionHandler.nextEpisodeInOtherDubbing(currentState)
                applySourceSelection(
                    nextState,
                    sourceScopeChanged = true,
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                    refreshSourcesBeforeStream = true,
                )
                nextState?.let(::saveContinueTarget)
            }

            is PlayerState.Event.EpisodeCompleted -> {
                if (!isActivePlaybackSource(event.episodeUrl)) return
                playbackProgressHandler.reportEpisodeFullyCompleted(
                    state = currentState,
                    positionMs = event.positionMs,
                    durationMs = event.durationMs,
                )
                saveWatchedProgressIfNeeded(event.positionMs, event.durationMs)
            }

            is PlayerState.Event.DubbingSelected -> {
                closeSourceSessions()
                analytics.eventDubbingSelected(
                    state = currentState,
                    index = event.index,
                    positionMs = event.currentPosMs,
                )
                applySourceSelection(
                    sourceSelectionHandler.selectDubbing(
                        state = currentState,
                        index = event.index,
                        currentPosMs = event.currentPosMs,
                    ),
                    sourceScopeChanged = true,
                )
            }

            is PlayerState.Event.BalancerSelected -> {
                closeSourceSessions()
                analytics.eventBalancerSelected(
                    state = currentState,
                    index = event.index,
                    positionMs = event.currentPosMs,
                )
                applySourceSelection(
                    sourceSelectionHandler.selectBalancer(
                        state = currentState,
                        index = event.index,
                        currentPosMs = event.currentPosMs,
                    ),
                    sourceScopeChanged = true,
                )
            }

            is PlayerState.Event.QualitySelected -> {
                analytics.eventQualitySelected(currentState.animeId, event.quality)
                val position = event.currentPosMs.coerceAtLeast(0L)
                allohaSource.onQualitySelected(event.quality)
                setState {
                    copy(
                        selectedQuality = event.quality,
                        resumeFromMs = position,
                        playbackPositionMs = position,
                    )
                }
            }

            is PlayerState.Event.AllohaAudioTrackSelected ->
                allohaSource.onAudioTrackSelected(event.audioId, event.currentPosMs)

            is PlayerState.Event.AllohaSubtitleSelected ->
                allohaSource.onSubtitleSelected(event.index)

            is PlayerState.Event.SpeedSelected -> {
                analytics.eventSpeedSelected(currentState.animeId, event.speed)
                setState { copy(selectedSpeed = event.speed.coerceAtLeast(0.1f)) }
            }

            is PlayerState.Event.ResizeModeSelected -> {
                analytics.eventResizeModeSelected(currentState.animeId, event.mode)
                displaySettings.selectResizeMode(host, event.mode)
            }

            is PlayerState.Event.ZoomLevelSelected -> {
                analytics.eventZoomLevelSelected(currentState.animeId, event.level)
                displaySettings.selectZoomLevel(host, event.level)
            }

            is PlayerState.Event.MobileVideoTransformChanged ->
                displaySettings.changeMobileTransform(
                    host,
                    PlayerMobileVideoTransformSettings(
                        scale = event.scale,
                        offsetX = event.offsetX,
                        offsetY = event.offsetY,
                    ),
                )

            is PlayerState.Event.PlaybackPositionChanged -> {
                if (!isActivePlaybackSource(event.episodeUrl)) return
                val position = event.positionMs.coerceAtLeast(0L)
                val duration = event.durationMs.coerceAtLeast(0L)
                sourceBehaviors.forEach { it.onPlaybackPositionChanged(position) }
                setState {
                    copy(
                        resumeFromMs = position,
                        playbackPositionMs = position,
                        playbackDurationMs = duration,
                    )
                }
                playbackProgressHandler.recordWatchedTick(currentState, position, duration)
                saveWatchedProgressIfNeeded(position, duration)
            }

            is PlayerState.Event.SkipSegmentSelected -> {
                analytics.eventSkipSegmentSelected(
                    state = currentState,
                    type = event.type,
                    fromMs = event.fromMs,
                    toMs = event.toMs,
                )
            }

            is PlayerState.Event.SaveProgress -> {
                val s = currentState
                val snapshot = event.snapshot
                viewModelScope.launch {
                    playbackProgressHandler.saveProgress(
                        playbackProgressHandler.progressSaveRequest(s, snapshot),
                    )
                }
            }
        }
    }

    /** Поведение, которое принимает решения по ошибкам и ретраям текущего источника. */
    private fun activeSourceBehavior(): PlayerSourceBehavior? =
        sourceBehaviors.firstOrNull { it.handles(currentState) }

    private fun closeSourceSessions() {
        sourceBehaviors.forEach { it.close() }
    }

    private fun resetSourceBehaviors() {
        sourceBehaviors.forEach { it.reset() }
    }

    private fun loadDownloadedDestination(downloadId: Long) {
        closeSourceSessions()
        changePlayerHint.cancel()
        viewModelScope.launch {
            val item = offlineSources.findDownloaded(downloadId)
            if (item == null) {
                setState { offlineSources.missingDownload(this) }
                return@launch
            }
            setState { offlineSources.downloaded(this, item) }
            loadFinalEpisodeAction(item.animeId)
        }
    }

    /**
     * Воспроизведение локального файла, открытого извне (ACTION_VIEW, content://).
     * Идёт по офлайн-маршруту, чтобы не запускать сетевые ретраи/резолв source-graph,
     * но с отдельным локальным data-source (флаг [PlayerState.State.isLocalFile]).
     */
    private fun loadLocalFileDestination(uri: String, title: String) {
        closeSourceSessions()
        changePlayerHint.cancel()
        setState { offlineSources.localFile(this, uri, title) }
    }

    private fun loadFinalEpisodeAction(animeId: Int) {
        finalEpisodeActionJob?.cancel()
        setState { copy(finalEpisodeAction = PlayerFinalEpisodeAction.Loading) }
        finalEpisodeActionJob = viewModelScope.launch {
            val action = finalEpisodeActionHandler.resolve(animeId)
            setState { copy(finalEpisodeAction = action) }
        }
    }

    /**
     * Приводит ключ плеера в back stack к серии, которая реально играет.
     *
     * NavKey переживает смерть процесса (`rememberNavBackStack`), а смена серии внутри плеера
     * меняла только state ViewModel — после перезапуска приложение возвращалось на серию, с
     * которой плеер когда-то открыли. Запись делит [PLAYER_CONTENT_KEY] с текущей
     * (см. `PlayerNavRegistrar`), поэтому подмена ключа не пересоздаёт NavEntry и не рвёт
     * воспроизведение.
     */
    private fun syncBackStackDestination() {
        if (activeDest.downloadId > 0L || activeDest.localFileUri.isNotBlank()) return
        val state = currentState
        val iframeUrl = activeIframeUrl(state)
        val episode = activeEpisode(state)
        if (iframeUrl.isBlank() || episode.isBlank()) return

        val destination = activeDest.copy(
            iframeUrl = iframeUrl,
            episode = episode,
            playerName = activeBalancerName(state),
            dubbing = activeDubbingName(state),
            selectedVideoId = activeVideoId(state),
            selectedPlayerId = activePlayerId(state),
            selectedScreenshotUrl = activeScreenshotUrl(state),
            // Позицию не дублируем в ключ: её вернёт локальный прогресс
            // (PlayerStreamHandler.loadResumePosition), иначе ключ пришлось бы переписывать
            // на каждом тике воспроизведения.
            resumeFromMs = 0L,
        )
        if (destination == activeDest) return
        // Ключ и activeDest двигаются только вместе: разъехавшись, они дадут повторный
        // NavigateToDestination со старой серией, если запись пересоздадут.
        if (nav.backStack.lastOrNull() !is PlayerDestination) return
        // activeDest обновляем до replace: экран пришлёт NavigateToDestination с новым ключом,
        // и loadDestination должен схлопнуться на guard, а не перезапустить загрузку.
        activeDest = destination
        nav.replace(destination)
    }

    private fun saveContinueTarget(state: PlayerState.State) {
        val request = playbackProgressHandler.continueTargetRequest(state) ?: return
        viewModelScope.launch {
            playbackProgressHandler.saveContinueTarget(request)
        }
    }

    private fun saveWatchedProgressIfNeeded(positionMs: Long, durationMs: Long) {
        val completionState = currentState
        val request = playbackProgressHandler.watchedProgressRequest(
            state = completionState,
            positionMs = positionMs,
            durationMs = durationMs,
        ) ?: return
        val nextState = sourceSelectionHandler.nextEpisode(completionState)
        viewModelScope.launch {
            playbackProgressHandler.saveProgress(request)
            val nextTargetRequest =
                if (playbackProgressHandler.shouldSuggestNextEpisodeOnWatched()) {
                    nextState?.let(playbackProgressHandler::continueTargetRequest)
                } else {
                    null
                }
            if (nextTargetRequest != null) {
                playbackProgressHandler.saveContinueTarget(nextTargetRequest)
            } else {
                playbackProgressHandler.suppressContinueWatchingDisplay(completionState)
            }
        }
    }

    /**
     * Применяет выбранный пользователем источник и запускает загрузку потока.
     *
     * Переключение серии сначала обновляет `/videos`, а смена балансера или озвучки может
     * использовать уже загруженный граф источников.
     */
    private fun applySourceSelection(
        state: PlayerState.State?,
        sourceScopeChanged: Boolean = false,
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
        refreshSourcesBeforeStream: Boolean = false,
    ) {
        if (state == null) return
        resetSourceBehaviors()
        setState { sourceStreamHandler.preparingStreamLoad(state, resumeMode) }
        if (sourceScopeChanged) {
            displaySettings.observeActive(host)
        }
        if (refreshSourcesBeforeStream) {
            refreshSourceGraphThenLoadStream(resumeMode)
        } else {
            loadStream(resumeMode)
        }
    }

    private fun isActivePlaybackSource(episodeUrl: String): Boolean =
        episodeUrl.isBlank() || episodeUrl == activeIframeUrl(currentState)

    /** Обновляет граф источников из сети и один раз запускает получение потока. */
    private fun refreshSourceGraphThenLoadStream(
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
    ) {
        loadSourceGraph(
            forceRefreshVideos = true,
            loadStreamOnFailure = true,
            loadStreamAfterRefresh = true,
            resumeMode = resumeMode,
            refreshStreamOnFailure = false,
        )
    }

    /**
     * Запускает загрузку графа источников и применяет результат handler-а к состоянию экрана.
     *
     * Устаревший результат от прошлой серии не применится: [loadDestination] отменяет [sourceGraphJob].
     */
    private fun loadSourceGraph(
        forceRefreshVideos: Boolean = false,
        loadStreamOnFailure: Boolean = false,
        loadStreamAfterRefresh: Boolean = false,
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
        refreshStreamOnFailure: Boolean = !forceRefreshVideos,
    ) {
        sourceGraphJob?.cancel()
        sourceGraphJob = viewModelScope.launch {
            when (
                val result = sourceStreamHandler.loadSourceGraph(
                    state = currentState,
                    forceRefreshVideos = forceRefreshVideos,
                    loadStreamOnFailure = loadStreamOnFailure,
                    loadStreamAfterRefresh = loadStreamAfterRefresh,
                    resumeMode = resumeMode,
                    refreshStreamOnFailure = refreshStreamOnFailure,
                )
            ) {
                PlayerSourceGraphLoadResult.Ignore -> Unit

                is PlayerSourceGraphLoadResult.LoadStream -> {
                    loadStream(
                        resumeMode = result.resumeMode,
                        refreshSourcesOnFailure = result.refreshSourcesOnFailure,
                    )
                }

                is PlayerSourceGraphLoadResult.SourceGraph -> {
                    val previousIframeUrl = activeIframeUrl(currentState)
                    setState {
                        sourceStreamHandler.applySourceGraph(
                            state = this,
                            sourceGraph = result.sourceGraph,
                        )
                    }
                    displaySettings.observeActive(host)
                    if (
                        result.loadStreamAfterRefresh ||
                        activeIframeUrl(currentState) != previousIframeUrl
                    ) {
                        loadStream(
                            resumeMode = result.resumeMode,
                            refreshSourcesOnFailure = result.refreshStreamOnFailure,
                        )
                    }
                }
            }
        }
    }

    /**
     * Запускает получение потока для активного источника.
     *
     * Первая ошибка resolve может запросить принудительное обновление источников; повтор отключает
     * этот флаг, чтобы последующие ошибки показались пользователю без бесконечного цикла.
     */
    private fun loadStream(
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
        refreshSourcesOnFailure: Boolean = true,
        forceFreshAllohaSession: Boolean = false,
        selectedQualityOverride: String? = null,
        forceRefresh: Boolean = false,
    ) {
        if (resumeMode == PlayerStreamResumeMode.SelectedSourceOnly) {
            pendingDestinationResumeMs = null
        }
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            val preserveStreamDuringRecovery = sourceBehaviors.any { it.keepsStreamWhileResolving() }
            val wasAlreadyResolving = currentState.streamUrl == null &&
                currentState.playerError == null &&
                currentState.kodikBlockedError == null
            val hintTimerAlreadyRunning = wasAlreadyResolving && changePlayerHint.isRunning
            if (!hintTimerAlreadyRunning && !preserveStreamDuringRecovery) {
                startChangePlayerHintTimer()
            }
            val canPreserveCurrent = resumeMode == PlayerStreamResumeMode.PreserveCurrent
            val stateResumeMs = sourceBehaviors
                .firstNotNullOfOrNull { it.recoveryResumePositionMs() }
                ?.takeIf { canPreserveCurrent }
                ?: currentState.playbackPositionMs.takeIf { canPreserveCurrent && it > 0L }
                ?: currentState.resumeFromMs.takeIf { canPreserveCurrent && it > 0L }
            val destinationResumeMs = pendingDestinationResumeMs.takeIf { canPreserveCurrent }
            setState {
                sourceStreamHandler.preparingStreamResolve(
                    state = this,
                    preserveCurrentStream = preserveStreamDuringRecovery,
                )
            }
            val s = currentState
            val pendingResume = s.dubbingResumeMs.takeIf { canPreserveCurrent && it > 0L }
                ?: destinationResumeMs
                ?: stateResumeMs
            when (
                val result = sourceStreamHandler.resolveStream(
                    state = s,
                    pendingResume = pendingResume,
                    destinationResumeMs = destinationResumeMs,
                    resumeMode = resumeMode,
                    refreshSourcesOnFailure = refreshSourcesOnFailure,
                    reuseAllohaPlaybackSession = !forceFreshAllohaSession,
                    selectedQualityOverride = selectedQualityOverride,
                    forceRefresh = forceRefresh,
                )
            ) {
                is PlayerStreamLoadResult.RefreshSources -> {
                    refreshSourceGraphThenLoadStream(result.resumeMode)
                }

                is PlayerStreamLoadResult.State -> applyResolvedStream(result)
            }
        }
    }

    private suspend fun applyResolvedStream(result: PlayerStreamLoadResult.State) {
        val resolveFailed = result.state.playerError != null ||
            result.state.kodikBlockedError != null
        if (!resolveFailed) {
            sourceBehaviors.forEach { it.onStreamActivated(result) }
        }
        if (resolveFailed && sourceBehaviors.any { it.retriesFailedResolve() }) return
        // Все поведения должны закрыть своё восстановление, поэтому не any { }.
        val completedRecovery = sourceBehaviors.fold(false) { completed, behavior ->
            behavior.onStreamResolved(result, resolveFailed) || completed
        }
        if (result.consumedDestinationResume) {
            pendingDestinationResumeMs = null
        }
        changePlayerHint.cancel()
        setState {
            copy(
                streamUrl = result.state.streamUrl,
                streamHeaders = result.state.streamHeaders,
                streamQualityMap = result.state.streamQualityMap,
                selectedQuality = result.state.selectedQuality,
                allohaAudioTracks = result.state.allohaAudioTracks,
                selectedAllohaAudioId = result.state.selectedAllohaAudioId,
                allohaSubtitles = result.state.allohaSubtitles,
                selectedAllohaSubtitleIndex = result.state.selectedAllohaSubtitleIndex,
                playerError = result.state.playerError,
                kodikBlockedError = result.state.kodikBlockedError,
                resumeFromMs = result.state.resumeFromMs,
                dubbingResumeMs = result.state.dubbingResumeMs,
                retryKey = if (completedRecovery && !resolveFailed) {
                    retryKey + 1
                } else {
                    retryKey
                },
                isPlaybackRecovering = completedRecovery && !resolveFailed,
                showChangePlayerHint = false,
            )
        }
        if (resolveFailed) return
        syncBackStackDestination()
        sourceBehaviors.forEach { it.afterStreamApplied(result) }
    }

    /** Показывает подсказку "сменить плеер" в UI, если поток не резолвится дольше [CHANGE_PLAYER_HINT_DELAY_MS]. */
    private fun startChangePlayerHintTimer() {
        changePlayerHint.cancel()
        setState { copy(showChangePlayerHint = false) }
        changePlayerHint.start(CHANGE_PLAYER_HINT_DELAY_MS)
    }

    override fun onCleared() {
        resetSourceBehaviors()
        sourceBehaviors.forEach { it.close(immediately = false) }
        changePlayerHint.cancel()
        super.onCleared()
    }

    private companion object {
        private const val CHANGE_PLAYER_HINT_DELAY_MS = 10_000L
    }
}
