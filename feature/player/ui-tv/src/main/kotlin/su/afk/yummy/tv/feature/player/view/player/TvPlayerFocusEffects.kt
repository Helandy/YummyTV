package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.TvPlayerPanelsState
import su.afk.yummy.tv.feature.player.model.TvPlayerPromptsState
import su.afk.yummy.tv.feature.player.utils.toPlayerControlFocusTarget

/**
 * Управление фокусом TV-плеера. Приоритет: следующий эпизод -> финальное действие ->
 * контролы (внешняя цель -> возврат из панели -> play) -> скрытый оверлей.
 * Каждая панель получает фокус своим отдельным LaunchedEffect.
 */
@Composable
internal fun TvPlayerFocusEffects(
    focus: TvPlayerFocusRequesters,
    panels: TvPlayerPanelsState,
    prompts: TvPlayerPromptsState,
    controllerVisible: Boolean,
    recoveryHintVisible: Boolean,
    restoreControlFocusTarget: PlayerControlFocusTarget?,
    onControlFocusRestored: () -> Unit,
) {
    val currentOnControlFocusRestored by rememberUpdatedState(onControlFocusRestored)

    fun requestPanelReturnFocus(): Boolean {
        val target = panels.pendingReturnFocusTarget ?: return false
        val restored = focus.requestControl(target.toPlayerControlFocusTarget())
        if (restored) panels.pendingReturnFocusTarget = null
        return restored
    }

    LaunchedEffect(
        controllerVisible,
        panels.activePanel,
        prompts.nextEpisodePrompt.isVisible,
        prompts.finalEpisodeActionPrompt,
        recoveryHintVisible,
        restoreControlFocusTarget,
    ) {
        if (prompts.nextEpisodePrompt.isVisible) {
            withFrameNanos { }
            try {
                focus.nextEpisode.requestFocus()
            } catch (_: Exception) {
            }
        } else if (prompts.finalEpisodeActionPrompt != null) {
            withFrameNanos { }
            try {
                focus.finalEpisodeAction.requestFocus()
            } catch (_: Exception) {
            }
        } else if (recoveryHintVisible) {
            // Хинт восстановления сам запрашивает фокус на свои кнопки — не перехватываем
        } else if (controllerVisible && !panels.isAnyOpen) {
            withFrameNanos { }
            val restoredExternalTarget = restoreControlFocusTarget?.let { target ->
                focus.requestControl(target).also { restored ->
                    if (restored) currentOnControlFocusRestored()
                }
            } ?: false
            if (!restoredExternalTarget && !requestPanelReturnFocus()) {
                try {
                    focus.play.requestFocus()
                } catch (_: Exception) {
                }
            }
        } else if (!controllerVisible) {
            withFrameNanos { }
            try {
                focus.overlay.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(panels.isOpen(TvPlayerPanel.Quality)) {
        if (panels.isOpen(TvPlayerPanel.Quality)) {
            withFrameNanos { }
            try {
                focus.selectedQuality.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Dubbing)) {
        if (panels.isOpen(TvPlayerPanel.Dubbing)) {
            withFrameNanos { }
            try {
                focus.selectedDubbing.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Balancer)) {
        if (panels.isOpen(TvPlayerPanel.Balancer)) {
            withFrameNanos { }
            try {
                focus.selectedBalancer.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Speed)) {
        if (panels.isOpen(TvPlayerPanel.Speed)) {
            withFrameNanos { }
            try {
                focus.selectedSpeed.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Resize)) {
        if (panels.isOpen(TvPlayerPanel.Resize)) {
            withFrameNanos { }
            try {
                focus.selectedResize.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
}
