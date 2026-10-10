package su.afk.yummy.tv.feature.player.common.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import su.afk.yummy.tv.feature.player.common.PlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip

/**
 * Авто-скип активного сегмента после отсчёта [delaySeconds], когда включена настройка
 * [autoSkipOpeningsEndings]; отсчёт идёт только пока [isPlaying].
 *
 * Платформа может дополнить сценарий: [onActivated] — сразу при появлении сегмента (подсветка,
 * фокус), [onManualWindow] — вместо авто-скипа, когда настройка выключена, [onCompleted] — после.
 */
@Composable
fun PlayerAutoSkipEffect(
    activeSkip: PlayerActiveSkip?,
    autoSkipOpeningsEndings: Boolean,
    delaySeconds: Int,
    isPlaying: Boolean,
    skipUi: PlayerSkipUiState,
    onSkipActiveSegment: () -> Unit,
    onActivated: suspend (PlayerActiveSkip) -> Unit = {},
    onManualWindow: suspend (PlayerActiveSkip) -> Unit = {},
    onCompleted: (PlayerActiveSkip) -> Unit = {},
) {
    val currentOnSkipActiveSegment by rememberUpdatedState(onSkipActiveSegment)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentOnActivated by rememberUpdatedState(onActivated)
    val currentOnManualWindow by rememberUpdatedState(onManualWindow)
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    LaunchedEffect(activeSkip?.key, autoSkipOpeningsEndings, delaySeconds) {
        val skip = activeSkip ?: return@LaunchedEffect
        currentOnActivated(skip)
        if (autoSkipOpeningsEndings) {
            skipUi.runAutoSkipCountdown(skip.key, delaySeconds) { currentIsPlaying }
            currentOnSkipActiveSegment()
        } else {
            currentOnManualWindow(skip)
        }
        currentOnCompleted(skip)
    }
}
