package su.afk.yummy.tv.feature.player.mobile.view

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import su.afk.yummy.tv.core.designsystem.locals.LocalResolveKodikThumbnailUrl
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.utils.cast.CastSupport
import su.afk.yummy.tv.core.utils.kodik.resolveContinueWatchingImage
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
import su.afk.yummy.tv.feature.player.common.rememberPlayerBufferingState
import su.afk.yummy.tv.feature.player.common.rememberPlayerCompletionTracker
import su.afk.yummy.tv.feature.player.common.rememberPlayerMediaReadyState
import su.afk.yummy.tv.feature.player.common.rememberPlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.common.rememberPlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.rememberPlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.rememberPlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.rememberPlayerSystemVolumeController
import su.afk.yummy.tv.feature.player.common.rememberPlayerTrackSelection
import su.afk.yummy.tv.feature.player.common.rememberPlayerVolumeController
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemUpdater
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerMediaController
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.common.toastIcon
import su.afk.yummy.tv.feature.player.common.utils.currentSkip
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.common.utils.playerContentScale
import su.afk.yummy.tv.feature.player.common.utils.playerEndPromptFor
import su.afk.yummy.tv.feature.player.common.utils.skippedMessageRes
import su.afk.yummy.tv.feature.player.common.view.PlayerEndPromptCountdownEffect
import su.afk.yummy.tv.feature.player.mobile.cast.MobileCastingIndicator
import su.afk.yummy.tv.feature.player.mobile.cast.rememberMobileCastConnectionState
import su.afk.yummy.tv.feature.player.mobile.cast.stopCasting
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerKeyAction
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerTrackSettingsTab
import su.afk.yummy.tv.feature.player.mobile.model.MobileVerticalGestureZone
import su.afk.yummy.tv.feature.player.mobile.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.mobile.model.rememberMobilePlayerGestureController
import su.afk.yummy.tv.feature.player.mobile.model.rememberMobilePlayerOverlayController
import su.afk.yummy.tv.feature.player.mobile.model.rememberMobilePlayerSeekController
import su.afk.yummy.tv.feature.player.common.model.rememberPlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.mobile.utils.buildMobileMediaItemKey
import su.afk.yummy.tv.feature.player.mobile.utils.buildMobilePlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.mobile.utils.buildMobilePlayerPlaybackKey
import su.afk.yummy.tv.feature.player.mobile.utils.formatMobilePlayerTime
import su.afk.yummy.tv.feature.player.mobile.utils.gestureIcon
import su.afk.yummy.tv.feature.player.mobile.utils.mobilePlayerNotificationMeta
import su.afk.yummy.tv.feature.player.mobile.utils.toGesturePercentText
import su.afk.yummy.tv.feature.player.mobile.utils.toMobilePlayerKeyAction
import su.afk.yummy.tv.feature.player.mobile.view.tutorial.MobilePlayerGestureTutorial
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.view.deriveQualityUrls
import kotlin.math.roundToInt
import su.afk.yummy.tv.feature.player.mobile.R as UiR

@OptIn(UnstableApi::class)
@Composable
internal fun MobileNativePlayer(
    state: PlayerState.State,
    streamUrl: String,
    videoTransform: MobileVideoTransform,
    onVideoTransformChanged: (MobileVideoTransform) -> Unit,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }
    val supportsPictureInPicture = remember(context) { MobilePlayerPipController.canEnter(context) }
    val isInPictureInPictureMode = MobilePlayerPipController.isInPictureInPictureMode
    val tutorialBlocksPlayback =
        !state.mobileGestureTutorialReady || state.showMobileGestureTutorial
    val tutorialVisible =
        state.mobileGestureTutorialReady &&
            state.showMobileGestureTutorial &&
            !isInPictureInPictureMode
    val pipSession = remember { MobilePlayerPipController.createSession() }
    val playerNamePrefix = stringResource(R.string.player_name_prefix)
    val ui = rememberPlayerPlaybackUiState(state, playerNamePrefix)
    val qualities = remember(streamUrl, state.streamQualityMap) {
        state.streamQualityMap ?: deriveQualityUrls(streamUrl)
    }
    val selectedQuality = state.selectedQuality?.takeIf { it in qualities }
        ?: qualities.keys.lastOrNull()
    val selectedSpeed = state.selectedSpeed
    // Новая серия/стрим начинают со значений из state (позиция возобновления).
    val progress = rememberPlayerPlaybackProgressState(
        ui.activeIframeUrl,
        streamUrl,
        initialPositionMs = state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs,
        initialDurationMs = state.playbackDurationMs,
    )
    var settingsMode by remember { mutableStateOf<MobilePlayerSettingsMode?>(null) }
    // Шторка настроек дорожек открывается на том табе, который юзер выбрал в прошлый раз,
    // пока открыт этот экран плеера (и переживает поворот). Недоступный таб шторка сама
    // откатит на первый.
    var settingsTrackTab by rememberSaveable { mutableStateOf(MobilePlayerTrackSettingsTab.Dubbing) }
    var volumePanelOpen by remember { mutableStateOf(false) }
    var wantsPlay by remember { mutableStateOf(true) }
    val resumeAfterLifecyclePause = remember { mutableStateOf(false) }
    var nextEpisodePromptState by remember(ui.activeIframeUrl, streamUrl) {
        mutableStateOf<PlayerEndPromptState>(PlayerEndPromptState.Hidden)
    }
    val skipUi = rememberPlayerSkipUiState(ui.activeIframeUrl)
    val currentUrl = selectedQuality?.let(qualities::get) ?: streamUrl
    val playbackConfigKey = remember(
        currentUrl,
        state.streamHeaders,
        state.retryKey,
        // Side-loaded subtitles are part of the MediaItem, so a new pick has to rebuild the key.
        state.selectedAllohaSubtitleIndex,
    ) {
        buildMobilePlayerPlaybackKey(state = state, url = currentUrl)
    }
    val mediaController = rememberPlayerMediaController()
    val castSupported = remember(context) { CastSupport.isSupported(context) }
    val castConnection = rememberMobileCastConnectionState()
    // Пока идёт Cast-сессия, локальный экран остаётся как есть (таймлайн/контролы видны) - сворачивать
    // в PiP незачем: видео и так уходит на приёмник, а не рендерится в локальном окне.
    SideEffect {
        pipSession.setEnabled(
            state.pictureInPictureEnabled && !tutorialBlocksPlayback && !castConnection.isCasting
        )
    }
    val systemVolume = rememberPlayerSystemVolumeController()
    val volumeController = rememberPlayerVolumeController()
    val advancedVolumeEnabled = state.advancedPlayerVolumeEnabled
    val playerVolumeLevel by volumeController.volume.collectAsStateWithLifecycle()
    val playerVolumePercent = (playerVolumeLevel * 100f).roundToInt()
    val playbackConfig = rememberPlayerPlaybackConfig()

    // При выключении режима прячем регулятор; звук вернётся к системному через эффект
    // применения player.volume (100%). Сохранённый уровень при этом не сбрасываем.
    LaunchedEffect(advancedVolumeEnabled) {
        if (!advancedVolumeEnabled) volumePanelOpen = false
    }
    val stepSeekToast = rememberPlayerStepSeekToastState(
        streamUrl = streamUrl,
        toastDuration = MOBILE_PLAYER_SEEK_TOAST_DURATION,
    )
    val canChangePlayer = ui.balancerNames.size > 1
    val canChangeDubbing = ui.dubbingNames.size > 1
    // Пока виден хинт восстановления, оверлей не автоскрываем:
    // кнопки «Сменить плеер/озвучку» и контролы должны оставаться на экране
    val recoveryHintVisible = state.isPlaybackRecovering && state.showChangePlayerHint &&
        (canChangePlayer || canChangeDubbing) && !isInPictureInPictureMode
    val overlay = rememberMobilePlayerOverlayController(
        canHide = {
            wantsPlay &&
                settingsMode == null &&
                !progress.isSeeking &&
                !recoveryHintVisible &&
                !tutorialBlocksPlayback &&
                !castConnection.isCasting
        },
        wantsPlay = { wantsPlay },
        isPromptVisible = { nextEpisodePromptState.isVisible },
    )

    LaunchedEffect(recoveryHintVisible) {
        if (recoveryHintVisible) {
            overlay.cancelHide()
            overlay.visible = true
        }
    }

    // Видео-области при касте всё равно нет (кадры уходят на приёмник) - таймлайн и контролы
    // не должны прятаться сами по себе, иначе экран останется пустым без единой подсказки.
    LaunchedEffect(castConnection.isCasting) {
        if (castConnection.isCasting) {
            overlay.cancelHide()
            overlay.visible = true
        }
    }
    val gestures = rememberMobilePlayerGestureController(
        activity = activity,
        initialTransform = videoTransform,
        volumeLevelProvider = {
            if (advancedVolumeEnabled) {
                volumeController.volume.value
            } else {
                systemVolume.currentFraction()
            }
        },
        onVolumeChanged = { level ->
            if (advancedVolumeEnabled) {
                volumeController.setPercent((level * 100f).roundToInt())
            } else {
                systemVolume.setFraction(level)
            }
        },
        onGestureStart = { overlay.cancelHide() },
        onVideoTransformChanged = onVideoTransformChanged,
    )
    val effectiveSpeed = if (gestures.isSpeedBoosted) MOBILE_PLAYER_SPEED_BOOST else selectedSpeed
    val playbackShouldPlay = wantsPlay && !tutorialBlocksPlayback
    val mediaItemUpdater = remember { PlayerMediaItemUpdater() }
    val transformScopeKey = remember(state.animeId, state.animeTitle, ui.activeBalancerName) {
        "${state.animeId}|${state.animeTitle}|${ui.activeBalancerName}"
    }
    val notificationMeta = mobilePlayerNotificationMeta(ui)
    val resolveKodikThumbnail = LocalResolveKodikThumbnailUrl.current
    var notificationArtworkUrl by remember { mutableStateOf<String?>(null) }
    val mediaItemKey = remember(
        playbackConfigKey,
        state.animeTitle,
        notificationMeta,
        notificationArtworkUrl,
    ) {
        buildMobileMediaItemKey(
            playbackKey = playbackConfigKey,
            animeTitle = state.animeTitle,
            meta = notificationMeta,
            artworkUrl = notificationArtworkUrl,
        )
    }

    LaunchedEffect(
        ui.activeScreenshotUrl,
        ui.activeIframeUrl,
        state.posterUrl,
        resolveKodikThumbnail,
    ) {
        notificationArtworkUrl = state.posterUrl.takeIf { it.isNotBlank() }
        notificationArtworkUrl = resolveContinueWatchingImage(
            screenshotUrl = ui.activeScreenshotUrl,
            episodeUrl = ui.activeIframeUrl,
            posterUrl = state.posterUrl,
            resolveKodikThumbnail = resolveKodikThumbnail,
        )
    }

    DisposableEffect(pipSession) {
        MobilePlayerPipController.registerSession(pipSession)
        onDispose {
            MobilePlayerPipController.unregisterSession(pipSession)
        }
    }

    PlayerKeepScreenOnEffect()

    val player = mediaController
    val isBuffering = rememberPlayerBufferingState(player)
    val isMediaReady = rememberPlayerMediaReadyState(player, playbackConfigKey)

    LaunchedEffect(player, playbackConfigKey, mediaItemKey, ui.activeIframeUrl) {
        val activePlayer = player ?: return@LaunchedEffect
        mediaItemUpdater.update(
            player = activePlayer,
            playbackConfig = playbackConfig,
            config = buildMobilePlayerMediaItemConfig(
                playbackKey = playbackConfigKey,
                mediaItemKey = mediaItemKey,
                url = currentUrl,
                episodeUrl = ui.activeIframeUrl,
                state = state,
                meta = notificationMeta,
                artworkUrl = notificationArtworkUrl,
            ),
        )
        activePlayer.playWhenReady = playbackShouldPlay
    }

    if (player == null) {
        PlayerBlackBackdrop()
        return
    }

    val subtitlesOffLabel = stringResource(UiR.string.player_mobile_subtitles_off)
    val trackFallbackTemplate = stringResource(UiR.string.player_mobile_track_fallback)
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

    LaunchedEffect(tutorialBlocksPlayback, isInPictureInPictureMode, player) {
        if (tutorialBlocksPlayback) {
            player.pause()
            overlay.visible = false
            overlay.cancelHide()
            settingsMode = null
            nextEpisodePromptState = PlayerEndPromptState.Hidden
            gestures.resetForPictureInPicture()
        }
    }

    val progressSource = remember(player, ui) {
        PlayerProgressSource(
            episodeUrl = ui.activeIframeUrl,
            episode = ui.activeEpisode,
            videoId = ui.activeVideoId,
            playerName = ui.activeBalancerName,
            dubbing = ui.activeDubbing,
            screenshotUrl = ui.activeScreenshotUrl,
        )
    }
    val reporter = rememberPlayerProgressReporter(
        source = { progressSource },
        onEvent = { event ->
            // Все источники позиции (polling, перемотка, listener, lifecycle) идут через репортер.
            if (event is PlayerState.Event.PlaybackPositionChanged) {
                progress.currentPosition = event.positionMs.coerceAtLeast(0L)
                progress.duration = event.durationMs.coerceAtLeast(0L)
            }
            onEvent(event)
        },
    )
    val completionTracker = rememberPlayerCompletionTracker(
        contentKey = ui.activeIframeUrl,
        streamUrl = streamUrl,
        reporter = reporter,
        onEvent = onEvent,
    )

    fun handleEpisodeEnd(positionMs: Long, durationMs: Long) {
        completionTracker.onEpisodeEnd(positionMs, durationMs)
        if (
            (ui.hasNextEpisode || ui.nextEpisodeDubbing != null) &&
            !isInPictureInPictureMode &&
            !nextEpisodePromptState.isVisible
        ) {
            // Авто-отсчёт только внутри текущей озвучки: смену озвучки
            // пользователь должен подтвердить явно
            nextEpisodePromptState = playerEndPromptFor(
                state.autoPlayNextEpisode && ui.hasNextEpisode,
                state.nextEpisodeSwitchDelaySeconds,
            )
            overlay.visible = false
            settingsMode = null
            overlay.cancelHide()
        }
    }

    val seekController = rememberMobilePlayerSeekController(
        player = player,
        fallbackDurationMs = { progress.duration },
        reporter = reporter,
        stepSeekToast = stepSeekToast,
        onEpisodeEnd = ::handleEpisodeEnd,
        onBackwardStep = { nextEpisodePromptState = PlayerEndPromptState.Hidden },
    )

    MobilePlayerListenerEffect(
        player = player,
        activity = activity,
        pipSession = pipSession,
        reporter = reporter,
        overlay = overlay,
        skipUi = skipUi,
        stepSeekToast = stepSeekToast,
        seekController = seekController,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { playbackShouldPlay },
        onWantsPlayChanged = {
            if (!tutorialBlocksPlayback) wantsPlay = it
        },
        onEpisodeEnd = ::handleEpisodeEnd,
        onEvent = onEvent,
    )

    MobilePlayerLifecycleEffect(
        player = player,
        pipSession = pipSession,
        reporter = reporter,
        resumeAfterPause = resumeAfterLifecyclePause,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { playbackShouldPlay },
        isCasting = { castConnection.isCasting },
        promptState = { nextEpisodePromptState },
        onPromptStateChange = { nextEpisodePromptState = it },
    )

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            overlay.visible = false
            nextEpisodePromptState = PlayerEndPromptState.Hidden
            gestures.resetForPictureInPicture()
            settingsMode = null
            overlay.cancelHide()
            stepSeekToast.clear()
        }
    }

    LaunchedEffect(transformScopeKey) {
        gestures.endTransformGesture()
        gestures.liveVideoTransform = videoTransform
    }

    LaunchedEffect(ui.activeIframeUrl) {
        nextEpisodePromptState = PlayerEndPromptState.Hidden
        settingsMode = null
    }

    PlayerEndPromptCountdownEffect(
        promptState = nextEpisodePromptState,
        contentKey = ui.activeIframeUrl,
        onPromptStateChange = { nextEpisodePromptState = it },
        onFinished = {
            nextEpisodePromptState = PlayerEndPromptState.Hidden
            onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
        },
    )

    BackHandler(enabled = nextEpisodePromptState.isVisible && !isInPictureInPictureMode) {
        nextEpisodePromptState = PlayerEndPromptState.Hidden
        overlay.show()
    }

    LaunchedEffect(videoTransform) {
        if (!gestures.transformGestureActive) {
            gestures.liveVideoTransform = videoTransform
        }
    }

    LaunchedEffect(player, effectiveSpeed, playbackShouldPlay) {
        player.setPlaybackSpeed(effectiveSpeed)
        player.playWhenReady = playbackShouldPlay
        pipSession.setPlaying(playbackShouldPlay, activity)
    }

    // «Продвинутая» громкость: внутренний уровень плеера (0–100%), независимо от системы.
    // При выключенном режиме держим 100%, чтобы работал системный звук как раньше.
    LaunchedEffect(player, advancedVolumeEnabled, playerVolumeLevel) {
        player.volume = if (advancedVolumeEnabled) playerVolumeLevel else 1f
    }

    MobilePlayerProgressPollingEffect(
        player = player,
        episodeKey = ui.activeIframeUrl,
        isMediaReady = isMediaReady,
        reporter = reporter,
        progress = progress,
    )

    // Позиция тикает раз в секунду; через derivedStateOf экран перекомпоновывается только
    // когда активная заставка реально меняется.
    val activeSkip by remember(isMediaReady, ui.activeSkips, skipUi.dismissedSkipKeys) {
        derivedStateOf {
            if (isMediaReady) {
                currentSkip(ui.activeSkips, progress.currentPosition, skipUi.dismissedSkipKeys)
            } else {
                null
            }
        }
    }

    fun skipActiveSegment(reportSelection: Boolean) {
        val skip = activeSkip ?: return
        if (skip.key !in skipUi.dismissedSkipKeys) skipUi.dismissedSkipKeys += skip.key
        skipUi.showSnackbar(
            context.getString(
                skip.type.skippedMessageRes(),
                formatMobilePlayerTime(skip.segment.startMs),
                formatMobilePlayerTime(skip.segment.endMs),
            )
        )
        if (reportSelection) {
            onEvent(
                PlayerState.Event.SkipSegmentSelected(
                    type = skip.type,
                    fromMs = player.currentPosition.coerceAtLeast(0L),
                    toMs = skip.segment.endMs,
                )
            )
        }
        seekController.seekTo(skip.segment.endMs)
    }

    MobilePlayerAutoSkipEffect(
        activeSkip = activeSkip,
        autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
        delaySeconds = state.autoSkipDelaySeconds,
        isPlaying = playbackShouldPlay,
        skipUi = skipUi,
        onSkipActiveSegment = { skipActiveSegment(reportSelection = false) },
    )


    // Корень держит фокус, чтобы клавиатура управляла плеером без предварительного клика.
    val keyboardFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { keyboardFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { gestures.playerSize = it }
            .focusRequester(keyboardFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (isInPictureInPictureMode || tutorialBlocksPlayback) return@onKeyEvent false
                when (event.toMobilePlayerKeyAction()) {
                    MobilePlayerKeyAction.PlayPause -> {
                        if (wantsPlay) player.pause() else player.play()
                    }

                    MobilePlayerKeyAction.SeekBackward ->
                        seekController.stepSeek(StepSeekDirection.Backward)

                    MobilePlayerKeyAction.SeekForward ->
                        seekController.stepSeek(StepSeekDirection.Forward)

                    null -> return@onKeyEvent false
                }
                overlay.show()
                true
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            ContentFrame(
                player = player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = playerContentScale(state.resizeMode, state.zoomLevel),
                keepContentOnReset = state.isPlaybackRecovering,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = gestures.liveVideoTransform.scale
                        scaleY = gestures.liveVideoTransform.scale
                        translationX = gestures.liveVideoTransform.offset.x
                        translationY = gestures.liveVideoTransform.offset.y
                    },
                shutter = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                },
            )
        }

        PlayerSubtitleOverlay(
            player = player,
            style = state.subtitleStyle,
            modifier = Modifier.fillMaxSize(),
        )

        PlayerBufferingIndicator(
            visible = isBuffering || state.isPlaybackRecovering,
            modifier = Modifier.align(Alignment.Center),
        )

        MobilePlayerGestureLayer(
            enabled = !isInPictureInPictureMode && !tutorialBlocksPlayback,
            onTap = { if (!castConnection.isCasting) overlay.toggle() },
            onDoubleTap = seekController::stepSeek,
            onTransformStart = gestures::startTransformGesture,
            onTransform = gestures::applyVideoTransform,
            onTransformEnd = gestures::endTransformGesture,
            onVerticalDragStart = gestures::startVerticalGesture,
            onVerticalDrag = gestures::applyVerticalGesture,
            onVerticalDragEnd = gestures::endVerticalGesture,
            onLongPressStart = gestures::startSpeedBoost,
            onLongPressEnd = gestures::endSpeedBoost,
        )

        MobilePlayerTopBar(
            title = state.animeTitle,
            episode = ui.activeEpisode,
            dubbing = ui.activeDubbing,
            playerName = ui.activeBalancerName,
            onBack = { onEvent(PlayerState.Event.Back) },
            onDetails = { onEvent(PlayerState.Event.OpenDetails) },
            onPictureInPicture = { activity?.let(pipSession::enter) },
            showDetails = state.animeId > 0,
            showPictureInPicture = state.pictureInPictureEnabled &&
                supportsPictureInPicture &&
                !isInPictureInPictureMode &&
                !castConnection.isCasting,
            showCast = castSupported && !isInPictureInPictureMode,
            visible = overlay.visible && !isInPictureInPictureMode && !tutorialBlocksPlayback,
        )

        MobilePlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = overlay.visible && !isInPictureInPictureMode && !tutorialBlocksPlayback,
            wantsPlay = wantsPlay,
            progress = progress,
            openingStartMs = ui.activeSkips.opening?.startMs?.takeIf { state.showOpeningOnTimeline },
            openingEndMs = ui.activeSkips.opening?.endMs?.takeIf { state.showOpeningOnTimeline },
            hasPrevEpisode = ui.hasPrevEpisode,
            hasNextEpisode = ui.hasNextEpisode,
            onPlayPause = {
                if (wantsPlay) player.pause() else player.play()
                overlay.show()
            },
            onSeekChange = { value ->
                progress.isSeeking = true
                progress.seekProgress = value
                overlay.visible = true
                overlay.cancelHide()
            },
            onSeekFinished = {
                val duration = progress.duration
                if (duration > 0) {
                    val newPosition = (progress.seekProgress * duration).toLong().coerceIn(0L, duration)
                    seekController.seekTo(newPosition)
                }
                progress.isSeeking = false
                overlay.show()
            },
            onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.Controls))
            },
            onTrackSettings = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                settingsMode = MobilePlayerSettingsMode.Track
                overlay.visible = true
                overlay.cancelHide()
            },
            onPlaybackSettings = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                settingsMode = MobilePlayerSettingsMode.Playback
                overlay.visible = true
                overlay.cancelHide()
            },
            showVolumeButton = advancedVolumeEnabled,
            onVolumeSettings = {
                volumePanelOpen = !volumePanelOpen
                overlay.visible = true
                overlay.cancelHide()
            },
        )

        val skipButtonBottomPadding by animateDpAsState(
            targetValue = if (overlay.visible && !isInPictureInPictureMode) 132.dp else 36.dp,
            label = "skipButtonBottomPadding",
        )

        MobilePlayerSkipButton(
            skip = activeSkip.takeUnless {
                isInPictureInPictureMode || tutorialBlocksPlayback
            },
            countdownSeconds = skipUi.autoSkipRemainingSeconds(activeSkip?.key),
            countdownProgress = skipUi.autoSkipProgress(activeSkip?.key),
            onClick = {
                skipActiveSegment(reportSelection = true)
                // Панель не открываем принудительно, но не даём ей скрыться по таймеру.
                if (overlay.visible) overlay.show()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 18.dp, bottom = skipButtonBottomPadding),
        )

        if (volumePanelOpen &&
            advancedVolumeEnabled &&
            overlay.visible &&
            !isInPictureInPictureMode &&
            !tutorialBlocksPlayback
        ) {
            MobilePlayerVolumePanel(
                percent = playerVolumePercent,
                onPercentChange = { volumeController.setPercent(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 132.dp)
                    .padding(horizontal = 18.dp),
            )
        }

        MobilePlayerSeekToast(
            text = stepSeekToast.text.takeUnless {
                isInPictureInPictureMode || tutorialBlocksPlayback
            },
            icon = stepSeekToast.direction.toastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (overlay.visible && !isInPictureInPictureMode) 128.dp else 36.dp),
        )

        MobilePlayerSkipToast(
            // Тосты делят одну позицию, поэтому перемотка имеет приоритет.
            text = skipUi.snackbarText?.takeIf {
                stepSeekToast.text == null &&
                    !isInPictureInPictureMode &&
                    !tutorialBlocksPlayback
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (overlay.visible && !isInPictureInPictureMode) 128.dp else 36.dp),
        )

        MobilePlayerZoomIndicator(
            visible = gestures.transformGestureActive &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            scale = gestures.liveVideoTransform.scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )

        MobilePlayerGestureIndicator(
            visible = gestures.brightnessGestureActive &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            icon = MobileVerticalGestureZone.Brightness.gestureIcon,
            percentText = gestures.brightnessLevel.toGesturePercentText(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp),
        )

        MobilePlayerGestureIndicator(
            visible = gestures.volumeGestureActive &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            icon = MobileVerticalGestureZone.Volume.gestureIcon,
            percentText = gestures.volumeLevel.toGesturePercentText(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp),
        )

        MobilePlayerSpeedBoostIndicator(
            visible = gestures.isSpeedBoosted &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )

        if (recoveryHintVisible && !tutorialBlocksPlayback) {
            MobilePlayerRecoveryHint(
                onChangePlayer = if (canChangePlayer) {
                    {
                        settingsTrackTab = MobilePlayerTrackSettingsTab.Player
                        settingsMode = MobilePlayerSettingsMode.Track
                        overlay.cancelHide()
                    }
                } else {
                    null
                },
                onChangeDubbing = if (canChangeDubbing) {
                    {
                        settingsTrackTab = MobilePlayerTrackSettingsTab.Dubbing
                        settingsMode = MobilePlayerSettingsMode.Track
                        overlay.cancelHide()
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp),
            )
        }

        if (nextEpisodePromptState.isVisible &&
            (ui.hasNextEpisode || ui.nextEpisodeDubbing != null) &&
            !isInPictureInPictureMode &&
            !tutorialBlocksPlayback
        ) {
            MobilePlayerEndPrompt(
                title = when (val prompt = nextEpisodePromptState) {
                    is PlayerEndPromptState.WithCountdown -> stringResource(
                        R.string.player_next_episode_prompt_countdown,
                        prompt.seconds,
                    )

                    else -> {
                        val nextEpisodeDubbing = ui.nextEpisodeDubbing
                        if (!ui.hasNextEpisode && nextEpisodeDubbing != null) {
                            stringResource(
                                R.string.player_next_episode_prompt_other_dubbing,
                                nextEpisodeDubbing,
                            )
                        } else {
                            stringResource(R.string.player_next_episode_prompt)
                        }
                    }
                },
                primaryLabel = stringResource(R.string.player_watch_next),
                stayLabel = stringResource(R.string.player_stay),
                onPrimary = {
                    nextEpisodePromptState = PlayerEndPromptState.Hidden
                    onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
                },
                onStay = {
                    nextEpisodePromptState = PlayerEndPromptState.Hidden
                    overlay.show()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val activeSettingsMode = settingsMode
        if (
            activeSettingsMode != null &&
            !isInPictureInPictureMode &&
            !tutorialBlocksPlayback
        ) {
            MobilePlayerSettingsSheet(
                mode = activeSettingsMode,
                qualities = qualities.keys.toList(),
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    val position = player.currentPosition.coerceAtLeast(0)
                    reporter.saveProgress(position, progress.duration)
                    onEvent(PlayerState.Event.QualitySelected(quality, position))
                },
                selectedSpeed = selectedSpeed,
                onSpeedSelected = { onEvent(PlayerState.Event.SpeedSelected(it)) },
                resizeModes = PlayerResizeMode.entries,
                selectedResizeMode = state.resizeMode,
                onResizeModeSelected = { onEvent(PlayerState.Event.ResizeModeSelected(it)) },
                dubbingNames = ui.dubbingNames,
                dubbingEpisodeCounts = ui.dubbingEpisodeCounts,
                dubbingViews = ui.dubbingViews,
                dubbingSourceNames = ui.dubbingSourceNames,
                dubbingAvailability = ui.dubbingAvailability,
                selectedDubbingIndex = ui.currentDubbingIndex,
                onDubbingSelected = {
                    onEvent(
                        PlayerState.Event.DubbingSelected(
                            it,
                            player.currentPosition
                        )
                    )
                },
                balancerNames = ui.balancerNames,
                balancerAvailability = ui.balancerAvailability,
                selectedBalancerIndex = ui.currentBalancerIndex,
                onBalancerSelected = { index ->
                    val balancerIndex =
                        ui.availableBalancerIndices.getOrElse(index) { state.sourceSelection.balancerIndex }
                    onEvent(
                        PlayerState.Event.BalancerSelected(
                            balancerIndex,
                            player.currentPosition
                        )
                    )
                },
                audioTrackNames = (if (usesAlloha) alloha.audioOptions else trackSelection.audioOptions)
                    .map(PlayerTrackOption::label),
                selectedAudioTrackIndex = if (usesAlloha) {
                    alloha.selectedAudioIndex
                } else {
                    trackSelection.selectedAudioIndex
                },
                onAudioTrackSelected = { index ->
                    if (usesAlloha) {
                        alloha.audioIdAt(index)?.let { id ->
                            onEvent(
                                PlayerState.Event.AllohaAudioTrackSelected(
                                    id,
                                    player.currentPosition.coerceAtLeast(0L),
                                )
                            )
                        }
                    } else {
                        trackSelection.selectAudio(index)
                    }
                },
                // The tab shows Alloha's own lists and is labelled "Alloha", so it is gated on the
                // source actually being Alloha - not merely on some track being selectable. The
                // in-stream fallback offered that tab for Kodik sources too.
                showAudioSection = usesAlloha && alloha.hasAudioChoice,
                subtitleTrackNames = (if (usesAlloha) alloha.subtitleOptions else trackSelection.textOptions)
                    .map(PlayerTrackOption::label),
                selectedSubtitleTrackIndex = if (usesAlloha) {
                    alloha.selectedSubtitleOptionIndex
                } else {
                    trackSelection.selectedTextIndex
                },
                onSubtitleTrackSelected = { index ->
                    if (usesAlloha) {
                        onEvent(
                            PlayerState.Event.AllohaSubtitleSelected(alloha.subtitleIndexAt(index))
                        )
                    } else {
                        trackSelection.selectText(index)
                    }
                },
                showSubtitleSection = usesAlloha && alloha.hasSubtitleChoice,
                onDismiss = { settingsMode = null },
                initialTrackTab = settingsTrackTab,
                onTrackTabChanged = { settingsTrackTab = it },
            )
        }

        if (tutorialVisible) {
            MobilePlayerGestureTutorial(
                onDismiss = {
                    onEvent(PlayerState.Event.MobileGestureTutorialDismissed)
                },
            )
        }

        // Последний ребёнок Box - только так кнопка отключения гарантированно получает тапы
        // (иначе их перехватывает MobilePlayerGestureLayer выше по дереву). Сам индикатор не
        // перехватывает тапы вне кнопки, поэтому жесты и таймлайн/контролы под ним не глушатся.
        if (castConnection.isCasting) {
            MobileCastingIndicator(
                deviceName = castConnection.deviceName,
                onStopCasting = { stopCasting(context) },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private const val MOBILE_PLAYER_SPEED_BOOST = 2f
