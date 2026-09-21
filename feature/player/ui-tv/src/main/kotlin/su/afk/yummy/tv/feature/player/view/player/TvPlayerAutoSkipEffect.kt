package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.feature.player.common.PlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.PlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import kotlin.time.Duration.Companion.seconds

/**
 * Подсветка кнопки пропуска с фокусом: в ручном режиме на 10 секунд, в авто — на время
 * отсчёта [delaySeconds] (идёт только пока [isPlaying]), после которого сегмент пропускается.
 */
@Composable
internal fun TvPlayerAutoSkipEffect(
    activeSkip: PlayerActiveSkip?,
    autoSkipOpeningsEndings: Boolean,
    delaySeconds: Int,
    isPlaying: Boolean,
    skipUi: PlayerSkipUiState,
    focus: TvPlayerFocusRequesters,
    autoHide: PlayerAutoHideController,
    onControllerVisibleChange: (Boolean) -> Unit,
    onSkipActiveSegment: (reportSelection: Boolean) -> Unit,
) {
    val currentOnControllerVisibleChange by rememberUpdatedState(onControllerVisibleChange)
    val currentOnSkipActiveSegment by rememberUpdatedState(onSkipActiveSegment)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    LaunchedEffect(activeSkip?.key, autoSkipOpeningsEndings, delaySeconds) {
        val skip = activeSkip ?: return@LaunchedEffect
        skipUi.highlightedSkipKey = skip.key
        currentOnControllerVisibleChange(true)
        autoHide.cancel()
        requestFocusUntilTimeout(focus.skip)
        if (autoSkipOpeningsEndings) {
            skipUi.runAutoSkipCountdown(skip.key, delaySeconds) { currentIsPlaying }
            currentOnSkipActiveSegment(false)
        } else {
            delay(10.seconds)
        }
        if (skipUi.highlightedSkipKey == skip.key) skipUi.highlightedSkipKey = null
    }
}
