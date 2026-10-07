package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.utils.downgradedCountdown
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.common.utils.playerEndPromptFor
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState

/**
 * Сценарий конца серии, общий для ТВ и мобилки: промпт следующей серии, промпт финального
 * действия тайтла, память об отказе пользователя и защёлка от повторных срабатываний.
 *
 * Живёт в пределах одной серии и потока (пересоздаётся по ключам в [rememberPlayerEndFlowState]).
 * Платформа отвечает только за визуальные побочные эффекты: показать контролы, закрыть панели.
 */
@Stable
class PlayerEndFlowState internal constructor() {
    var nextEpisodePrompt: PlayerEndPromptState by mutableStateOf(PlayerEndPromptState.Hidden)

    /** Пользователь отказался от перехода: не показываем промпт повторно по детекту конца. */
    var nextEpisodePromptDismissed: Boolean by mutableStateOf(false)
        private set

    var finalEpisodeActionPrompt: PlayerFinalEpisodeAction? by mutableStateOf(null)

    val anyVisible: Boolean
        get() = nextEpisodePrompt.isVisible || finalEpisodeActionPrompt != null

    // Конец серии приходит из нескольких источников (STATE_ENDED, перемотка в конец, поллинг по
    // позиции раз в секунду) — отрабатываем его один раз, пока пользователь не уйдёт от конца.
    private var endHandled = false

    /** Серия досмотрена до конца и пользователь ещё не ушёл от конца. */
    var ended: Boolean by mutableStateOf(false)
        private set

    /**
     * Единая точка конца эпизода: отчёт о завершении и решение, какой промпт показать.
     *
     * @param suppressPrompts отчитаться о завершении, но промпты не показывать (выход с экрана,
     *   PiP).
     * @return true, если показан промпт — платформе пора открыть под него UI.
     */
    fun onEpisodeEnd(
        positionMs: Long,
        durationMs: Long,
        completionTracker: PlayerCompletionTracker,
        playback: PlayerPlaybackUiState,
        autoPlayNextEpisode: Boolean,
        nextEpisodeDelaySeconds: Int,
        suppressPrompts: Boolean,
    ): Boolean {
        if (endHandled) return false
        endHandled = true
        ended = true
        completionTracker.onEpisodeEnd(positionMs = positionMs, durationMs = durationMs)
        if (suppressPrompts) return false
        if (playback.hasNextEpisode || playback.nextEpisodeDubbing != null) {
            if (nextEpisodePromptDismissed) return false
            // При переходе в другую озвучку авто-отсчёт не запускаем:
            // озвучку не меняем без явного подтверждения пользователя
            nextEpisodePrompt = playerEndPromptFor(
                autoPlayNextEpisode && playback.hasNextEpisode,
                nextEpisodeDelaySeconds,
            )
            return true
        }
        val action = playback.finalEpisodeAction
        if (action != PlayerFinalEpisodeAction.RateTitle &&
            action != PlayerFinalEpisodeAction.ManageSubscriptions
        ) {
            return false
        }
        finalEpisodeActionPrompt = action
        return true
    }

    /** Ушли от конца серии (перемотка назад) — конец эпизода должен отработать заново. */
    fun onLeftEnd() {
        nextEpisodePrompt = PlayerEndPromptState.Hidden
        nextEpisodePromptDismissed = false
        endHandled = false
        ended = false
    }

    /** Пользователь отказался от следующей серии («Остаться», Back). */
    fun dismissNextEpisode() {
        if (nextEpisodePrompt.isVisible) nextEpisodePromptDismissed = true
        nextEpisodePrompt = PlayerEndPromptState.Hidden
    }

    fun hideAll() {
        nextEpisodePrompt = PlayerEndPromptState.Hidden
        finalEpisodeActionPrompt = null
    }

    /** ON_PAUSE: активный отсчёт вырождается в промпт без отсчёта. */
    fun onPaused() {
        val downgraded = nextEpisodePrompt.downgradedCountdown()
        if (downgraded !== nextEpisodePrompt) nextEpisodePrompt = downgraded
    }
}

@Composable
fun rememberPlayerEndFlowState(
    episodeKey: String,
    streamUrl: String,
): PlayerEndFlowState = remember(episodeKey, streamUrl) { PlayerEndFlowState() }
