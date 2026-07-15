package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.PlayerAutoHideController
import su.afk.yummy.tv.feature.player.model.ActiveSkip
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.TvPlayerSkipUiState
import kotlin.time.Duration.Companion.seconds

/** Авто-скип активного сегмента либо подсветка кнопки пропуска с фокусом на 10 секунд. */
@Composable
internal fun TvPlayerAutoSkipEffect(
    activeSkip: ActiveSkip?,
    autoSkipOpeningsEndings: Boolean,
    skipUi: TvPlayerSkipUiState,
    focus: TvPlayerFocusRequesters,
    autoHide: PlayerAutoHideController,
    onControllerVisibleChange: (Boolean) -> Unit,
    onSkipActiveSegment: (reportSelection: Boolean) -> Unit,
) {
    val currentOnControllerVisibleChange by rememberUpdatedState(onControllerVisibleChange)
    val currentOnSkipActiveSegment by rememberUpdatedState(onSkipActiveSegment)

    LaunchedEffect(activeSkip?.key, autoSkipOpeningsEndings) {
        val skip = activeSkip ?: return@LaunchedEffect
        if (autoSkipOpeningsEndings) {
            currentOnSkipActiveSegment(false)
        } else {
            skipUi.highlightedSkipKey = skip.key
            currentOnControllerVisibleChange(true)
            autoHide.cancel()
            withFrameNanos { }
            try {
                focus.skip.requestFocus()
            } catch (_: Exception) {
            }
            delay(10.seconds)
            if (skipUi.highlightedSkipKey == skip.key) skipUi.highlightedSkipKey = null
        }
    }
}
