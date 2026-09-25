package su.afk.yummy.tv.feature.player.view.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerAllohaTracks
import su.afk.yummy.tv.feature.player.common.PlayerBlackBackdrop
import su.afk.yummy.tv.feature.player.common.PlayerBufferingIndicator
import su.afk.yummy.tv.feature.player.common.PlayerKeepScreenOnEffect
import su.afk.yummy.tv.feature.player.common.PlayerSubtitleOverlay
import su.afk.yummy.tv.feature.player.common.PlayerTrackOption
import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.model.PlayerProgressSource
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.rememberPlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.rememberPlayerBufferingState
import su.afk.yummy.tv.feature.player.common.rememberPlayerCompletionTracker
import su.afk.yummy.tv.feature.player.common.rememberPlayerMediaReadyState
import su.afk.yummy.tv.feature.player.common.rememberPlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.rememberPlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.rememberPlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.rememberPlayerSystemVolumeController
import su.afk.yummy.tv.feature.player.common.rememberPlayerTrackSelection
import su.afk.yummy.tv.feature.player.common.rememberPlayerVolumeController
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemUpdater
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackSessionClient
import su.afk.yummy.tv.feature.player.common.toastIcon
import su.afk.yummy.tv.feature.player.common.utils.currentSkip
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.common.utils.playerEndPromptFor
import su.afk.yummy.tv.feature.player.common.utils.skippedMessageRes
import su.afk.yummy.tv.feature.player.common.view.PlayerEndPromptCountdownEffect
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.TvPlayerExitState
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerPanelsState
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerPromptsState
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerSeekController
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerVolumeKeysState
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.buildTvMediaItemKey
import su.afk.yummy.tv.feature.player.utils.buildTvPlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.utils.buildTvPlayerPlaybackKey
import su.afk.yummy.tv.feature.player.utils.formatTime
import su.afk.yummy.tv.feature.player.utils.speedLabel
import su.afk.yummy.tv.feature.player.utils.tvPlayerContentScale
import su.afk.yummy.tv.feature.player.view.TvPlayerRecoveryHint
import kotlin.math.roundToInt
import su.afk.yummy.tv.feature.player.common.model.rememberPlayerPlaybackProgressState

@OptIn(UnstableApi::class)
@Composable
internal fun TvExoPlayerView(
    state: PlayerState.State,
    playback: PlayerPlaybackUiState,
    streamUrl: String,
    restoreControlFocusTarget: PlayerControlFocusTarget?,
    exitState: TvPlayerExitState,
    pausedForTutorial: Boolean = false,
    onControlFocusRestored: () -> Unit,
    onDubbingSelected: (dubbingIndex: Int, currentPositionMs: Long) -> Unit,
    onBalancerSelected: (balancerIndex: Int, currentPositionMs: Long) -> Unit,
    onPlayerEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val episodeKey = playback.activeIframeUrl
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f) }
    val activeQuality = playback.activeQuality
    val activeSpeed = state.selectedSpeed.coerceAtLeast(0.1f)
    var seekOnSwitch by remember(streamUrl, state.retryKey) {
        mutableLongStateOf(state.resumeFromMs)
    }
    var wantsPlay by remember { mutableStateOf(true) }
    val progress = rememberPlayerPlaybackProgressState()
    // Буфер нового стрима/серии считается с нуля (позиция приходит из polling-цикла).
    LaunchedEffect(streamUrl, episodeKey) { progress.bufferedProgress = 0f }
    var controllerVisible by remember { mutableStateOf(true) }
    val panels = rememberTvPlayerPanelsState()
    val prompts = rememberTvPlayerPromptsState(episodeKey, streamUrl)
    val skipUi = rememberPlayerSkipUiState(streamUrl)
    val stepSeekToast = rememberPlayerStepSeekToastState(
        streamUrl = streamUrl,
        toastDuration = TV_PLAYER_INLINE_TOAST_DURATION,
    )
    val focus = rememberTvPlayerFocusRequesters()
    val systemVolume = rememberPlayerSystemVolumeController()
    val volumeController = rememberPlayerVolumeController()
    val advancedVolumeEnabled = state.advancedPlayerVolumeEnabled
    val playerVolumeLevel by volumeController.volume.collectAsStateWithLifecycle()
    val playerVolumePercent = (playerVolumeLevel * 100f).roundToInt()
    val volumeKeys = rememberTvPlayerVolumeKeysState(
        indicatorDuration = TV_PLAYER_INLINE_TOAST_DURATION,
    )
    val canChangePlayer = playback.balancerNames.size > 1
    val canChangeDubbing = playback.dubbingNames.size > 1
    // Пока виден хинт восстановления, оверлей нельзя автоскрывать:
    // иначе фокус уйдёт на скрытый key-оверлей и кнопки хинта станут недостижимы
    val recoveryHintVisible = state.isPlaybackRecovering && state.showChangePlayerHint &&
        (canChangePlayer || canChangeDubbing)
    val autoHide = rememberPlayerAutoHideController(
        canHide = { !panels.isAnyOpen && !prompts.anyVisible && !recoveryHintVisible },
        onHide = { controllerVisible = false },
    )

    fun onInteraction() {
        controllerVisible = true
        when {
            panels.isAnyOpen || prompts.anyVisible || recoveryHintVisible -> autoHide.cancel()
            wantsPlay -> autoHide.schedule()
            else -> autoHide.cancel()
        }
    }

    LaunchedEffect(recoveryHintVisible) {
        if (recoveryHintVisible) autoHide.cancel()
    }

    val currentUrl = playback.playbackUrl

    val playbackSession = rememberPlayerPlaybackSessionClient()
    val player = playbackSession.player
    val isBuffering = rememberPlayerBufferingState(player)
    val playbackConfig = rememberPlayerPlaybackConfig()
    val mediaItemUpdater = remember { PlayerMediaItemUpdater() }
    val playbackKey =
        remember(
            currentUrl,
            state.streamHeaders,
            state.offlineCacheKey,
            state.retryKey,
            // Side-loaded subtitles are part of the MediaItem, so a new pick has to rebuild the key.
            state.selectedAllohaSubtitleIndex,
        ) {
            buildTvPlayerPlaybackKey(state = state, url = currentUrl)
        }
    val isMediaReady = rememberPlayerMediaReadyState(player, playbackKey)
    val mediaItemKey = remember(
        playbackKey,
        state.animeTitle,
        playback.activeEpisode,
        playback.activeDubbing,
        playback.activeBalancerName,
        playback.activeScreenshotUrl,
    ) {
        buildTvMediaItemKey(
            playbackKey = playbackKey,
            animeTitle = state.animeTitle,
            playback = playback,
        )
    }

    val progressSource = remember(
        episodeKey,
        playback.activeEpisode,
        playback.activeVideoId,
        playback.activeBalancerName,
        playback.activeDubbing,
        playback.activeScreenshotUrl,
    ) {
        PlayerProgressSource(
            episodeUrl = episodeKey,
            episode = playback.activeEpisode,
            videoId = playback.activeVideoId,
            playerName = playback.activeBalancerName,
            dubbing = playback.activeDubbing,
            screenshotUrl = playback.activeScreenshotUrl,
        )
    }
    val reporter = rememberPlayerProgressReporter(
        source = { progressSource },
        onEvent = onPlayerEvent,
    )

    TvPlayerLifecycleEffect(
        player = player,
        playbackSession = playbackSession,
        reporter = reporter,
        prompts = prompts,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { wantsPlay },
    )

    LaunchedEffect(player, playbackKey, mediaItemKey, episodeKey) {
        val activePlayer = player ?: return@LaunchedEffect
        mediaItemUpdater.update(
            player = activePlayer,
            playbackConfig = playbackConfig,
            config = buildTvPlayerMediaItemConfig(
                playbackKey = playbackKey,
                mediaItemKey = mediaItemKey,
                url = currentUrl,
                state = state,
                playback = playback,
                durationMs = progress.duration,
                playbackPositionMs = seekOnSwitch,
            ),
        )
        activePlayer.playWhenReady = wantsPlay
    }

    if (player == null) {
        PlayerBlackBackdrop()
        return
    }

    PlayerKeepScreenOnEffect()

    val subtitlesOffLabel = stringResource(R.string.player_subtitles_off)
    val trackFallbackTemplate = stringResource(R.string.player_track_fallback)
    val trackSelection = rememberPlayerTrackSelection(
        player = player,
        offLabel = subtitlesOffLabel,
        fallbackLabel = { index -> trackFallbackTemplate.format(index + 1) },
    )
    val alloha = remember(
        state.allohaAudioTracks,
        state.selectedAllohaAudioId,
        state.allohaSubtitles,
        state.selectedAllohaSubtitleIndex,
        subtitlesOffLabel,
    ) {
        PlayerAllohaTracks(
            audioTracks = state.allohaAudioTracks,
            selectedAudioId = state.selectedAllohaAudioId,
            subtitles = state.allohaSubtitles,
            selectedSubtitleIndex = state.selectedAllohaSubtitleIndex,
            subtitlesOffLabel = subtitlesOffLabel,
        )
    }
    // Alloha reports its own dubbing/subtitle lists; everything else falls back to in-stream tracks.
    val usesAlloha = alloha.isAvailable
    // The panel shows Alloha's own lists and is labelled "Alloha", so it is gated on the source
    // actually being Alloha - not merely on some track being selectable. Falling back to in-stream
    // tracks here offered that panel for Kodik sources whose stream happens to carry extra tracks.
    val hasSelectableAudio = usesAlloha && alloha.hasAudioChoice
    val hasSelectableSubtitles = usesAlloha && alloha.hasSubtitleChoice
    val audioTrackNames = if (usesAlloha) alloha.audioOptions else trackSelection.audioOptions
    val subtitleTrackNames = if (usesAlloha) alloha.subtitleOptions else trackSelection.textOptions
    val selectedAudioIndex =
        if (usesAlloha) alloha.selectedAudioIndex else trackSelection.selectedAudioIndex
    val selectedSubtitleIndex =
        if (usesAlloha) alloha.selectedSubtitleOptionIndex else trackSelection.selectedTextIndex

    LaunchedEffect(pausedForTutorial) {
        if (pausedForTutorial) player.pause()
    }

    LaunchedEffect(exitState.requested) {
        if (exitState.requested) {
            prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
            prompts.finalEpisodeActionPrompt = null
            autoHide.cancel()
            player.pause()
        }
    }
    val completionTracker = rememberPlayerCompletionTracker(
        contentKey = episodeKey,
        streamUrl = streamUrl,
        reporter = reporter,
        onEvent = onPlayerEvent,
    )
    var endHandled by remember(episodeKey, streamUrl) { mutableStateOf(false) }

    /**
     * Единая точка конца эпизода: STATE_ENDED, перемотка в конец и детект по позиции.
     * Повторные вызовы гасит endHandled — поллинг тикает каждые 500 мс.
     */
    fun handleEpisodeEnd(positionMs: Long, durationMs: Long) {
        if (endHandled) return
        endHandled = true
        completionTracker.onEpisodeEnd(positionMs = positionMs, durationMs = durationMs)
        if (exitState.requested) return
        if (playback.hasNextEpisode || playback.nextEpisodeDubbing != null) {
            if (prompts.nextEpisodePromptDismissed) return
            // При переходе в другую озвучку авто-отсчёт не запускаем:
            // озвучку не меняем без явного подтверждения пользователя
            prompts.nextEpisodePrompt = playerEndPromptFor(
                state.autoPlayNextEpisode && playback.hasNextEpisode,
                state.nextEpisodeSwitchDelaySeconds,
            )
        } else {
            val action = playback.finalEpisodeAction
            if (action != PlayerFinalEpisodeAction.RateTitle &&
                action != PlayerFinalEpisodeAction.ManageSubscriptions
            ) {
                return
            }
            prompts.finalEpisodeActionPrompt = action
        }
        controllerVisible = true
        panels.close()
        autoHide.cancel()
    }

    val seekController = rememberTvPlayerSeekController(
        player = player,
        progress = progress,
        reporter = reporter,
        stepSeekToast = stepSeekToast,
        onEpisodeEnd = { positionMs, durationMs ->
            handleEpisodeEnd(positionMs, durationMs)
        },
        onBackwardStep = {
            // Ушли от конца серии — конец эпизода должен отработать заново
            prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
            prompts.nextEpisodePromptDismissed = false
            endHandled = false
        },
    )

    fun togglePanel(panel: TvPlayerPanel, returnFocusTarget: PanelReturnFocusTarget) {
        val opened = panels.toggle(panel)
        if (!opened) panels.pendingReturnFocusTarget = returnFocusTarget
        if (opened) autoHide.cancel() else onInteraction()
    }

    fun exitPanelDown(returnFocusTarget: PanelReturnFocusTarget) {
        panels.close(returnFocusTarget)
        onInteraction()
    }

    fun playNextEpisode() {
        if (exitState.requested) return
        reporter.saveProgress(progress.currentPosition, progress.duration)
        prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
        prompts.finalEpisodeActionPrompt = null
        panels.close()
        onPlayerEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
    }

    fun rateTitle() {
        if (exitState.requested) return
        prompts.finalEpisodeActionPrompt = null
        panels.close()
        onPlayerEvent(PlayerState.Event.RateTitle)
    }

    fun manageSubscriptions() {
        if (exitState.requested) return
        prompts.finalEpisodeActionPrompt = null
        panels.close()
        onPlayerEvent(PlayerState.Event.ManageSubscriptions)
    }

    // Позиция тикает каждые 500 мс; через derivedStateOf экран перекомпоновывается только
    // когда активная заставка реально меняется, а не на каждом тике.
    val activeSkip by remember(isMediaReady, playback.activeSkips, skipUi.dismissedSkipKeys) {
        derivedStateOf {
            if (isMediaReady) {
                currentSkip(playback.activeSkips, progress.currentPosition, skipUi.dismissedSkipKeys)
            } else {
                null
            }
        }
    }

    fun skipActiveSegment(reportSelection: Boolean = true) {
        val skip = activeSkip ?: return
        if (skip.key !in skipUi.dismissedSkipKeys) skipUi.dismissedSkipKeys += skip.key
        skipUi.highlightedSkipKey = null
        val message = context.getString(
            skip.type.skippedMessageRes(),
            formatTime(skip.segment.startMs),
            formatTime(skip.segment.endMs),
        )
        skipUi.showSnackbar(message)
        val fromPosition = player.currentPosition.coerceAtLeast(0L)
        if (reportSelection) {
            onPlayerEvent(
                PlayerState.Event.SkipSegmentSelected(
                    type = skip.type,
                    fromMs = fromPosition,
                    toMs = skip.segment.endMs,
                ),
            )
        }
        seekController.seekTo(skip.segment.endMs)
        onInteraction()
    }

    TvPlayerListenerEffect(
        player = player,
        autoHide = autoHide,
        skipUi = skipUi,
        stepSeekToast = stepSeekToast,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { wantsPlay },
        onWantsPlayChanged = { wantsPlay = it },
        onEpisodeEnd = { positionMs, durationMs ->
            handleEpisodeEnd(positionMs, durationMs)
        },
        onEvent = onPlayerEvent,
    )

    LaunchedEffect(player, activeSpeed) {
        player.setPlaybackSpeed(activeSpeed)
    }

    // «Продвинутая» громкость: внутренний уровень плеера (0–100%), независимо от системы.
    // При выключенном режиме держим 100%, чтобы работал системный звук как раньше.
    LaunchedEffect(player, advancedVolumeEnabled, playerVolumeLevel) {
        player.volume = if (advancedVolumeEnabled) playerVolumeLevel else 1f
    }

    TvPlayerProgressPollingEffect(
        player = player,
        progress = progress,
        reporter = reporter,
        episodeKey = { episodeKey },
        onPositionAtEnd = { positionMs, durationMs ->
            handleEpisodeEnd(positionMs, durationMs)
        },
    )

    TvPlayerFocusEffects(
        focus = focus,
        panels = panels,
        prompts = prompts,
        controllerVisible = controllerVisible,
        recoveryHintVisible = recoveryHintVisible,
        tutorialActive = pausedForTutorial,
        restoreControlFocusTarget = restoreControlFocusTarget,
        onControlFocusRestored = onControlFocusRestored,
    )

    TvPlayerAutoSkipEffect(
        activeSkip = activeSkip,
        autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
        delaySeconds = state.autoSkipDelaySeconds,
        isPlaying = wantsPlay,
        skipUi = skipUi,
        focus = focus,
        autoHide = autoHide,
        onControllerVisibleChange = { controllerVisible = it },
        onSkipActiveSegment = { reportSelection -> skipActiveSegment(reportSelection) },
    )

    PlayerEndPromptCountdownEffect(
        promptState = prompts.nextEpisodePrompt,
        contentKey = episodeKey,
        onPromptStateChange = { prompts.nextEpisodePrompt = it },
        onFinished = {
            if (!exitState.requested) playNextEpisode()
        },
    )

    BackHandler(enabled = panels.isAnyOpen || prompts.anyVisible || controllerVisible) {
        if (panels.isAnyOpen || prompts.anyVisible) {
            if (prompts.nextEpisodePrompt.isVisible) prompts.nextEpisodePromptDismissed = true
            prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
            prompts.finalEpisodeActionPrompt = null
            panels.close(
                returnFocusTarget = when (panels.activePanel) {
                    TvPlayerPanel.Quality -> PanelReturnFocusTarget.Quality
                    TvPlayerPanel.Dubbing -> PanelReturnFocusTarget.Dubbing
                    TvPlayerPanel.Balancer -> PanelReturnFocusTarget.Balancer
                    TvPlayerPanel.Speed -> PanelReturnFocusTarget.Speed
                    TvPlayerPanel.Resize -> PanelReturnFocusTarget.Resize
                    TvPlayerPanel.Volume -> PanelReturnFocusTarget.Volume
                    TvPlayerPanel.Alloha -> PanelReturnFocusTarget.Alloha
                    null -> null
                },
            )
        } else {
            autoHide.cancel()
            controllerVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                // «Продвинутая» громкость меняет внутренний уровень плеера (±1%, 0–200%),
                // иначе — системную (если включён перехват). Без обеих настроек кнопки
                // уходят системе — поведение по умолчанию не меняем.
                if (!advancedVolumeEnabled && !state.tvPlayerVolumeKeysEnabled) {
                    return@onPreviewKeyEvent false
                }
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent event.key == Key.VolumeUp ||
                        event.key == Key.VolumeDown
                }
                val up = when (event.key) {
                    Key.VolumeUp -> true
                    Key.VolumeDown -> false
                    else -> return@onPreviewKeyEvent false
                }
                if (advancedVolumeEnabled) {
                    volumeKeys.show(volumeController.stepBy(if (up) 1 else -1))
                } else {
                    val fraction = systemVolume.stepBy(if (up) 0.01f else -0.01f)
                    volumeKeys.show((fraction * 100f).roundToInt())
                }
                true
            },
    ) {
        ContentFrame(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            contentScale = tvPlayerContentScale(state.resizeMode, state.zoomLevel),
            keepContentOnReset = state.isPlaybackRecovering,
            shutter = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
        )

        PlayerSubtitleOverlay(
            player = player,
            style = state.subtitleStyle,
            modifier = Modifier.fillMaxSize(),
        )

        TvPlayerPointerOverlay(
            enabled = !panels.isAnyOpen && !prompts.anyVisible && !recoveryHintVisible,
            onClick = {
                if (controllerVisible) {
                    autoHide.cancel()
                    controllerVisible = false
                } else {
                    onInteraction()
                }
            },
        )

        PlayerBufferingIndicator(
            visible = isBuffering || state.isPlaybackRecovering,
            modifier = Modifier.align(Alignment.Center),
        )

        if (recoveryHintVisible) {
            TvPlayerRecoveryHint(
                onChangePlayer = if (canChangePlayer) {
                    { togglePanel(TvPlayerPanel.Balancer, PanelReturnFocusTarget.Balancer) }
                } else {
                    null
                },
                onChangeDubbing = if (canChangeDubbing) {
                    { togglePanel(TvPlayerPanel.Dubbing, PanelReturnFocusTarget.Dubbing) }
                } else {
                    null
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 100.dp),
            )
        }

        if (!controllerVisible) {
            TvPlayerHiddenKeyOverlay(
                focusRequester = focus.overlay,
                onSeekBackward = { seekController.stepSeek(StepSeekDirection.Backward) },
                onSeekForward = { seekController.stepSeek(StepSeekDirection.Forward) },
                onInteraction = ::onInteraction,
            )
        }

        TvPlayerInfoBar(
            visible = controllerVisible,
            animeTitle = state.animeTitle,
            episode = playback.activeEpisode,
            dubbing = playback.activeDubbing,
            modifier = Modifier.align(Alignment.TopStart),
        )

        TvPlayerNameBadge(
            visible = controllerVisible,
            playerName = playback.activeBalancerName,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        TvPlayerControlsOverlay(
            visible = controllerVisible,
            focus = focus,
            progress = progress,
            wantsPlay = wantsPlay,
            playback = playback,
            animeTitle = state.animeTitle,
            activeSkip = activeSkip,
            autoSkipRemainingSeconds = skipUi.autoSkipRemainingSeconds(activeSkip?.key),
            autoSkipProgress = skipUi.autoSkipProgress(activeSkip?.key),
            showOpeningOnTimeline = state.showOpeningOnTimeline,
            highlightedSkipKey = skipUi.highlightedSkipKey,
            qualityCount = playback.qualityLabels.size,
            currentQualityLabel = activeQuality.orEmpty(),
            currentSpeedLabel = activeSpeed.speedLabel(),
            showVolumeButton = advancedVolumeEnabled,
            showAllohaButton = hasSelectableAudio || hasSelectableSubtitles,
            onPlayPause = { if (wantsPlay) player.pause() else player.play() },
            onSeekTo = seekController::seekTo,
            onInteraction = ::onInteraction,
            onSkipActiveSegment = { skipActiveSegment() },
            onPrevEpisode = { onPlayerEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = {
                onPlayerEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.Controls))
            },
            onRateTitle = ::rateTitle,
            onManageSubscriptions = ::manageSubscriptions,
            onToggleQuality = {
                togglePanel(TvPlayerPanel.Quality, PanelReturnFocusTarget.Quality)
            },
            onToggleDubbing = {
                togglePanel(TvPlayerPanel.Dubbing, PanelReturnFocusTarget.Dubbing)
            },
            onToggleBalancer = {
                togglePanel(TvPlayerPanel.Balancer, PanelReturnFocusTarget.Balancer)
            },
            onToggleResize = {
                togglePanel(TvPlayerPanel.Resize, PanelReturnFocusTarget.Resize)
            },
            onToggleSpeed = {
                togglePanel(TvPlayerPanel.Speed, PanelReturnFocusTarget.Speed)
            },
            onToggleVolume = {
                togglePanel(TvPlayerPanel.Volume, PanelReturnFocusTarget.Volume)
            },
            onToggleAlloha = {
                togglePanel(TvPlayerPanel.Alloha, PanelReturnFocusTarget.Alloha)
            },
        )

        TvPlayerPanelsHost(
            panels = panels,
            focus = focus,
            playback = playback,
            qualities = playback.qualityLabels,
            activeQuality = activeQuality,
            speeds = speeds,
            activeSpeed = activeSpeed,
            resizeMode = state.resizeMode,
            zoomLevel = state.zoomLevel,
            volumePercent = playerVolumePercent,
            audioTrackNames = audioTrackNames.map(PlayerTrackOption::label),
            selectedAudioTrackIndex = selectedAudioIndex,
            subtitleTrackNames = subtitleTrackNames.map(PlayerTrackOption::label),
            selectedSubtitleTrackIndex = selectedSubtitleIndex,
            onQualitySelected = { idx ->
                val quality = playback.qualityLabels[idx]
                if (quality != activeQuality) {
                    val position = player.currentPosition.coerceAtLeast(0L)
                    seekOnSwitch = position
                    reporter.saveProgress(position, progress.duration)
                    onPlayerEvent(PlayerState.Event.QualitySelected(quality, position))
                }
                panels.close(PanelReturnFocusTarget.Quality)
                onInteraction()
            },
            onDubbingSelected = { idx ->
                onDubbingSelected(idx, player.currentPosition)
                panels.close(PanelReturnFocusTarget.Dubbing)
                onInteraction()
            },
            onBalancerSelected = { idx ->
                onBalancerSelected(idx, player.currentPosition)
                panels.close(PanelReturnFocusTarget.Balancer)
                onInteraction()
            },
            onSpeedSelected = { idx ->
                val speed = speeds[idx]
                if (speed != activeSpeed) onPlayerEvent(PlayerState.Event.SpeedSelected(speed))
                panels.close(PanelReturnFocusTarget.Speed)
                onInteraction()
            },
            onResizeModeSelected = { mode ->
                if (mode != state.resizeMode) {
                    onPlayerEvent(PlayerState.Event.ResizeModeSelected(mode))
                }
                onInteraction()
            },
            onZoomLevelSelected = { level ->
                if (level != state.zoomLevel || state.resizeMode != PlayerResizeMode.ZOOM) {
                    onPlayerEvent(PlayerState.Event.ZoomLevelSelected(level))
                }
                onInteraction()
            },
            onVolumeChange = { volumeController.setPercent(it) },
            onAudioTrackSelected = { idx ->
                if (usesAlloha) {
                    alloha.audioIdAt(idx)?.let { id ->
                        val position = player.currentPosition.coerceAtLeast(0L)
                        seekOnSwitch = position
                        onPlayerEvent(
                            PlayerState.Event.AllohaAudioTrackSelected(id, position),
                        )
                    }
                } else {
                    trackSelection.selectAudio(idx)
                }
                panels.close(PanelReturnFocusTarget.Alloha)
                onInteraction()
            },
            onSubtitleTrackSelected = { idx ->
                if (usesAlloha) {
                    seekOnSwitch = player.currentPosition.coerceAtLeast(0L)
                    onPlayerEvent(
                        PlayerState.Event.AllohaSubtitleSelected(alloha.subtitleIndexAt(idx)),
                    )
                } else {
                    trackSelection.selectText(idx)
                }
                panels.close(PanelReturnFocusTarget.Alloha)
                onInteraction()
            },
            onExitPanelDown = ::exitPanelDown,
        )

        TvPlayerSkipSnackbar(
            text = skipUi.snackbarText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
        )

        TvPlayerEndPrompts(
            prompts = prompts,
            focus = focus,
            hasNextEpisode = playback.hasNextEpisode,
            nextEpisodeDubbing = playback.nextEpisodeDubbing,
            onPlayNextEpisode = ::playNextEpisode,
            onRateTitle = ::rateTitle,
            onManageSubscriptions = ::manageSubscriptions,
            onInteraction = ::onInteraction,
        )

        TvPlayerInlineToast(
            text = stepSeekToast.text,
            icon = stepSeekToast.direction.toastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
        )

        TvPlayerInlineToast(
            text = volumeKeys.indicatorText,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp),
        )
    }
}
