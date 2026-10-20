package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.model.PlayerEpisodeEndOutcome
import su.afk.yummy.tv.feature.player.common.utils.downgradedCountdown
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.common.utils.playerEndPromptFor
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeUnavailable
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

    /** Диалог по кнопке «следующая серия», когда в активной озвучке её нет. */
    var nextEpisodeUnavailable: PlayerNextEpisodeUnavailable? by mutableStateOf(null)

    val anyVisible: Boolean
        get() = nextEpisodePrompt.isVisible || finalEpisodeActionPrompt != null ||
                nextEpisodeUnavailable != null

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
     * @return что показать: промпт (платформе пора открыть под него UI) или диалог со списком
     *   источников, когда следующая серия есть только в другой озвучке или на другом балансере.
     */
    fun onEpisodeEnd(
        positionMs: Long,
        durationMs: Long,
        completionTracker: PlayerCompletionTracker,
        playback: PlayerPlaybackUiState,
        autoPlayNextEpisode: Boolean,
        nextEpisodeDelaySeconds: Int,
        suppressPrompts: Boolean,
    ): PlayerEpisodeEndOutcome {
        if (endHandled) return PlayerEpisodeEndOutcome.None
        endHandled = true
        ended = true
        completionTracker.onEpisodeEnd(positionMs = positionMs, durationMs = durationMs)
        if (suppressPrompts) return PlayerEpisodeEndOutcome.None
        if (playback.hasNextEpisode) {
            if (nextEpisodePromptDismissed) return PlayerEpisodeEndOutcome.None
            nextEpisodePrompt = playerEndPromptFor(autoPlayNextEpisode, nextEpisodeDelaySeconds)
            return PlayerEpisodeEndOutcome.PromptShown
        }
        // Серия есть только в другой озвучке или на другом балансере: тот же диалог со списком,
        // что и по кнопке «следующая», — источник выбирает пользователь.
        if (playback.nextEpisodeDubbing != null) return PlayerEpisodeEndOutcome.SourcesRequested
        val action = playback.finalEpisodeAction
        if (action != PlayerFinalEpisodeAction.RateTitle &&
            action != PlayerFinalEpisodeAction.ManageSubscriptions
        ) {
            return PlayerEpisodeEndOutcome.None
        }
        finalEpisodeActionPrompt = action
        return PlayerEpisodeEndOutcome.PromptShown
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
        nextEpisodeUnavailable = null
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
