package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.feature.player.common.PlayerEndFlowState
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.TvPlayerPanelsState
import su.afk.yummy.tv.feature.player.utils.toPlayerControlFocusTarget

/**
 * Управление фокусом TV-плеера. Приоритет: следующий эпизод -> финальное действие ->
 * контролы (внешняя цель -> возврат из панели -> play) -> скрытый оверлей. Заявка [skipFocusRequested]
 * от авто-скипа отдаёт фокус «Пропустить» вместо play.
 * Каждая панель получает фокус своим отдельным LaunchedEffect.
 */
@Composable
internal fun TvPlayerFocusEffects(
    focus: TvPlayerFocusRequesters,
    panels: TvPlayerPanelsState,
    prompts: PlayerEndFlowState,
    controllerVisible: Boolean,
    recoveryHintVisible: Boolean,
    tutorialActive: Boolean = false,
    restoreControlFocusTarget: PlayerControlFocusTarget?,
    skipFocusRequested: Boolean = false,
    onSkipFocusHandled: () -> Unit = {},
    onControlFocusRestored: () -> Unit,
) {
    val currentOnControlFocusRestored by rememberUpdatedState(onControlFocusRestored)
    val currentSkipFocusRequested by rememberUpdatedState(skipFocusRequested)
    val currentOnSkipFocusHandled by rememberUpdatedState(onSkipFocusHandled)

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
        tutorialActive,
        restoreControlFocusTarget,
    ) {
        // Пока открыто обучение, фокус целиком принадлежит его собственной кнопке —
        // не перехватываем его обратно на плеер, иначе DPAD в оверлее обучения
        // случайно уводит фокус на панель управления под ним.
        if (tutorialActive) {
            // no-op
        } else if (prompts.nextEpisodePrompt.isVisible) {
            requestFocusUntilTimeout(focus.nextEpisode)
        } else if (prompts.finalEpisodeActionPrompt != null) {
            requestFocusUntilTimeout(focus.finalEpisodeAction)
        } else if (recoveryHintVisible) {
            // Хинт восстановления сам запрашивает фокус на свои кнопки — не перехватываем
        } else if (controllerVisible && !panels.isAnyOpen) {
            // Заявку на «Пропустить» исполняет отдельный эффект ниже; play не запрашиваем, чтобы его не перебить.
            val skipFocusOwnsFocus = currentSkipFocusRequested
            // Контролы могли только что появиться — вся цепочка повторяется по кадрам, пока
            // какая-то цель не возьмёт фокус.
            requestFocusUntilTimeout {
                val restoredExternalTarget = restoreControlFocusTarget?.let { target ->
                    focus.requestControl(target).also { restored ->
                        if (restored) currentOnControlFocusRestored()
                    }
                } ?: false
                restoredExternalTarget ||
                    requestPanelReturnFocus() ||
                    skipFocusOwnsFocus ||
                    focus.play.requestFocus()
            }
        } else if (!controllerVisible) {
            requestFocusUntilTimeout(focus.overlay)
        }
    }

    LaunchedEffect(skipFocusRequested, controllerVisible) {
        if (!skipFocusRequested || !controllerVisible) return@LaunchedEffect
        if (!panels.isAnyOpen && !prompts.anyVisible && !tutorialActive) {
            requestFocusUntilTimeout(focus.skip)
        }
        currentOnSkipFocusHandled()
    }

    LaunchedEffect(panels.isOpen(TvPlayerPanel.Quality)) {
        if (panels.isOpen(TvPlayerPanel.Quality)) {
            requestFocusUntilTimeout(focus.selectedQuality)
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Dubbing)) {
        if (panels.isOpen(TvPlayerPanel.Dubbing)) {
            requestFocusUntilTimeout(focus.selectedDubbing)
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Balancer)) {
        if (panels.isOpen(TvPlayerPanel.Balancer)) {
            requestFocusUntilTimeout(focus.selectedBalancer)
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Speed)) {
        if (panels.isOpen(TvPlayerPanel.Speed)) {
            requestFocusUntilTimeout(focus.selectedSpeed)
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Resize)) {
        if (panels.isOpen(TvPlayerPanel.Resize)) {
            requestFocusUntilTimeout(focus.selectedResize)
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Volume)) {
        if (panels.isOpen(TvPlayerPanel.Volume)) {
            requestFocusUntilTimeout(focus.selectedVolume)
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Alloha)) {
        if (panels.isOpen(TvPlayerPanel.Alloha)) {
            requestFocusUntilTimeout(focus.selectedAlloha)
        }
    }
}
