package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import su.afk.yummy.tv.feature.player.common.PlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip

/** Авто-скип активного сегмента после отсчёта [delaySeconds], когда включена соответствующая настройка. */
@Composable
internal fun MobilePlayerAutoSkipEffect(
    activeSkip: PlayerActiveSkip?,
    autoSkipOpeningsEndings: Boolean,
    delaySeconds: Int,
    isPlaying: Boolean,
    skipUi: PlayerSkipUiState,
    onSkipActiveSegment: () -> Unit,
) {
    val currentOnSkipActiveSegment by rememberUpdatedState(onSkipActiveSegment)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    LaunchedEffect(activeSkip?.key, autoSkipOpeningsEndings, delaySeconds) {
        val skip = activeSkip ?: return@LaunchedEffect
        if (!autoSkipOpeningsEndings) return@LaunchedEffect
        skipUi.runAutoSkipCountdown(skip.key, delaySeconds) { currentIsPlaying }
        currentOnSkipActiveSegment()
    }
}
