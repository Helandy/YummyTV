package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.PlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.PlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip
import su.afk.yummy.tv.feature.player.common.view.PlayerAutoSkipEffect
import kotlin.time.Duration.Companion.seconds

/**
 * Авто-скип с ТВ-подсветкой: заявляет фокус на кнопку пропуска (ставит его TvPlayerFocusEffects),
 * в ручном режиме подсвечена 5 секунд, в авто — на время отсчёта [delaySeconds] (идёт только пока [isPlaying]).
 * Фокус захватывается один раз за сегмент: повторные активации (перемотка внутри/через сегмент)
 * его не трогают, пока воспроизведение не выйдет из сегмента.
 */
@Composable
internal fun TvPlayerAutoSkipEffect(
    activeSkip: PlayerActiveSkip?,
    isMediaReady: Boolean,
    autoSkipOpeningsEndings: Boolean,
    delaySeconds: Int,
    isPlaying: Boolean,
    skipUi: PlayerSkipUiState,
    autoHide: PlayerAutoHideController,
    onRequestSkipFocus: () -> Unit,
    onControllerVisibleChange: (Boolean) -> Unit,
    onSkipActiveSegment: (reportSelection: Boolean) -> Unit,
) {
    // Ключ сегмента, для которого фокус уже захвачен; сбрасывается при выходе из сегмента.
    // Пока плеер буферизует (после каждого шага перемотки) activeSkip тоже null — это не выход.
    var focusCapturedKey by remember { mutableStateOf<String?>(null) }
    val leftSegment = activeSkip == null && isMediaReady
    LaunchedEffect(leftSegment) {
        if (leftSegment) focusCapturedKey = null
    }
    val currentOnRequestSkipFocus by rememberUpdatedState(onRequestSkipFocus)
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
            if (focusCapturedKey == skip.key) return@PlayerAutoSkipEffect
            focusCapturedKey = skip.key
            // Фокус ставит TvPlayerFocusEffects — здесь только заявка.
            currentOnRequestSkipFocus()
            currentOnControllerVisibleChange(true)
            autoHide.cancel()
        },
        onManualWindow = { delay(5.seconds) },
        onCompleted = { skip ->
            if (skipUi.highlightedSkipKey == skip.key) skipUi.highlightedSkipKey = null
        },
    )
}
