package su.afk.yummy.tv.feature.player.common

import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState

/**
 * Действия конца серии, общие для ТВ и мобилки: единая точка конца эпизода, переход к следующей
 * серии и финальные действия тайтла. Платформа подключает только визуальные побочные эффекты.
 *
 * Создаётся на каждую композицию (как локальные функции экрана), поэтому всегда видит свежие
 * [playback] и настройки.
 *
 * @param isPromptSuppressed не показывать промпты (выход с экрана, PiP): о завершении всё равно отчитываемся.
 * @param onPromptShown платформа открывает UI под показанный промпт (контролы, закрытие панелей).
 * @param blocksActions запретить переход и финальные действия (экран закрывается).
 * @param onActionStarted платформенный сброс перед отправкой события (закрыть панели).
 */
class PlayerEpisodeEndController(
    private val endFlow: PlayerEndFlowState,
    private val completionTracker: PlayerCompletionTracker,
    private val reporter: PlayerProgressReporter,
    private val progress: PlayerPlaybackProgressState,
    private val playback: PlayerPlaybackUiState,
    private val autoPlayNextEpisode: Boolean,
    private val nextEpisodeDelaySeconds: Int,
    private val onEvent: (PlayerState.Event) -> Unit,
    private val isPromptSuppressed: () -> Boolean = { false },
    private val onPromptShown: () -> Unit = {},
    private val blocksActions: () -> Boolean = { false },
    private val onActionStarted: () -> Unit = {},
) {
    /** Единая точка конца эпизода: STATE_ENDED, перемотка в конец и детект по позиции. */
    fun onEpisodeEnd(positionMs: Long, durationMs: Long) {
        val promptShown = endFlow.onEpisodeEnd(
            positionMs = positionMs,
            durationMs = durationMs,
            completionTracker = completionTracker,
            playback = playback,
            autoPlayNextEpisode = autoPlayNextEpisode,
            nextEpisodeDelaySeconds = nextEpisodeDelaySeconds,
            suppressPrompts = isPromptSuppressed(),
        )
        if (promptShown) onPromptShown()
    }

    /**
     * Прогресс сохраняем до события: после него VM меняет источник, а репортёр читает источник
     * лениво, и позиция ушла бы в новую серию.
     */
    fun playNextEpisode() {
        if (blocksActions()) return
        reporter.saveProgress(progress.currentPosition, progress.duration)
        endFlow.hideAll()
        onActionStarted()
        onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
    }

    fun rateTitle() {
        if (blocksActions()) return
        endFlow.finalEpisodeActionPrompt = null
        onActionStarted()
        onEvent(PlayerState.Event.RateTitle)
    }

    fun manageSubscriptions() {
        if (blocksActions()) return
        endFlow.finalEpisodeActionPrompt = null
        onActionStarted()
        onEvent(PlayerState.Event.ManageSubscriptions)
    }
}
