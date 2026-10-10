package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.feature.player.common.PlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.PlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip
import su.afk.yummy.tv.feature.player.common.view.PlayerAutoSkipEffect
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import kotlin.time.Duration.Companion.seconds

/**
 * Авто-скип с ТВ-подсветкой: кнопка пропуска получает фокус, в ручном режиме подсвечена 10 секунд,
 * в авто — на время отсчёта [delaySeconds] (идёт только пока [isPlaying]).
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

    PlayerAutoSkipEffect(
        activeSkip = activeSkip,
        autoSkipOpeningsEndings = autoSkipOpeningsEndings,
        delaySeconds = delaySeconds,
        isPlaying = isPlaying,
        skipUi = skipUi,
        onSkipActiveSegment = { onSkipActiveSegment(false) },
        onActivated = { skip ->
            skipUi.highlightedSkipKey = skip.key
            currentOnControllerVisibleChange(true)
            autoHide.cancel()
            requestFocusUntilTimeout(focus.skip)
        },
        onManualWindow = { delay(10.seconds) },
        onCompleted = { skip ->
            if (skipUi.highlightedSkipKey == skip.key) skipUi.highlightedSkipKey = null
        },
    )
}
