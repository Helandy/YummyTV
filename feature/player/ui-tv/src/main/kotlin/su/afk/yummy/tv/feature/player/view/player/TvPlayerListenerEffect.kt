package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.PlayerCompletionTracker
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.PlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.logPlaybackError
import su.afk.yummy.tv.feature.player.common.playerEndPromptFor
import su.afk.yummy.tv.feature.player.common.positionSnapshot
import su.afk.yummy.tv.feature.player.common.toPlaybackErrorEvent
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.TvPlayerExitState
import su.afk.yummy.tv.feature.player.model.TvPlayerPanelsState
import su.afk.yummy.tv.feature.player.model.TvPlayerPromptsState
import su.afk.yummy.tv.feature.player.model.TvPlayerSkipUiState

/**
 * Player.Listener TV-плеера: play/pause, завершение эпизода с промптами, ошибки.
 * Порядок dispose важен: cancel jobs -> removeListener -> notify/save -> pause/clear/stopService.
 */
@Composable
internal fun TvPlayerListenerEffect(
    player: Player,
    reporter: PlayerProgressReporter,
    completionTracker: PlayerCompletionTracker,
    autoHide: PlayerAutoHideController,
    skipUi: TvPlayerSkipUiState,
    stepSeekToast: PlayerStepSeekToastState,
    panels: TvPlayerPanelsState,
    prompts: TvPlayerPromptsState,
    exitState: TvPlayerExitState,
    fallbackDurationMs: () -> Long,
    hasNextEpisode: () -> Boolean,
    nextEpisodeSwitchesDubbing: () -> Boolean,
    finalEpisodeAction: () -> PlayerFinalEpisodeAction,
    autoPlayNextEpisode: () -> Boolean,
    wantsPlay: () -> Boolean,
    onWantsPlayChanged: (Boolean) -> Unit,
    onControllerVisibleChange: (Boolean) -> Unit,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentHasNextEpisode by rememberUpdatedState(hasNextEpisode)
    val currentNextEpisodeSwitchesDubbing by rememberUpdatedState(nextEpisodeSwitchesDubbing)
    val currentFinalEpisodeAction by rememberUpdatedState(finalEpisodeAction)
    val currentAutoPlayNextEpisode by rememberUpdatedState(autoPlayNextEpisode)
    val currentWantsPlay by rememberUpdatedState(wantsPlay)
    val currentOnWantsPlayChanged by rememberUpdatedState(onWantsPlayChanged)
    val currentOnControllerVisibleChange by rememberUpdatedState(onControllerVisibleChange)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentCompletionTracker by rememberUpdatedState(completionTracker)
    val currentStepSeekToast by rememberUpdatedState(stepSeekToast)

    DisposableEffect(player) {
        player.playWhenReady = currentWantsPlay()
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                currentOnWantsPlayChanged(pwr)
                if (pwr) autoHide.schedule() else autoHide.cancel()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    currentOnEvent(PlayerState.Event.PlaybackReady)
                }
                if (playbackState == Player.STATE_ENDED) {
                    val snapshot = player.positionSnapshot(currentFallbackDuration())
                    currentCompletionTracker.onEpisodeEnd(
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                    )
                    if (exitState.requested) return
                    if (currentHasNextEpisode()) {
                        // При переходе в другую озвучку авто-отсчёт не запускаем:
                        // озвучку не меняем без явного подтверждения пользователя
                        prompts.nextEpisodePrompt = playerEndPromptFor(
                            currentAutoPlayNextEpisode() && !currentNextEpisodeSwitchesDubbing()
                        )
                        currentOnControllerVisibleChange(true)
                        panels.close()
                        autoHide.cancel()
                    } else {
                        val action = currentFinalEpisodeAction()
                        if (action == PlayerFinalEpisodeAction.RateTitle ||
                            action == PlayerFinalEpisodeAction.ManageSubscriptions
                        ) {
                            prompts.finalEpisodeActionPrompt = action
                            currentOnControllerVisibleChange(true)
                            panels.close()
                            autoHide.cancel()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val position = player.currentPosition.coerceAtLeast(0L)
                logPlaybackError("TV", position, error)
                currentOnEvent(error.toPlaybackErrorEvent(position))
            }
        }
        player.addListener(listener)
        if (currentWantsPlay()) autoHide.schedule() else autoHide.cancel()
        onDispose {
            autoHide.cancel()
            skipUi.cancel()
            currentStepSeekToast.cancel()
            player.removeListener(listener)
            if (player.mediaItemCount > 0) {
                val snapshot = player.positionSnapshot(currentFallbackDuration())
                reporter.notifyPositionChanged(snapshot.positionMs, snapshot.durationMs)
                reporter.saveProgress(snapshot.positionMs, snapshot.durationMs)
            }
            releaseTvPlaybackResources(context, player)
        }
    }
}
