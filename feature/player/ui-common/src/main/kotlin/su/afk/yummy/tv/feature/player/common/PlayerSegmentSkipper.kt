package su.afk.yummy.tv.feature.player.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.utils.currentSkip
import su.afk.yummy.tv.feature.player.common.utils.skipPlayerSegment

/** Активный сегмент пропуска (опенинг/эндинг) и действие «пропустить», общие для ТВ и мобилки. */
class PlayerSegmentSkipper internal constructor(
    private val activeSkipState: State<PlayerActiveSkip?>,
    private val context: Context,
    private val player: Player,
    private val skipUi: PlayerSkipUiState,
    private val seekController: PlayerSeekController,
    private val onEvent: (PlayerState.Event) -> Unit,
) {
    val activeSkip: PlayerActiveSkip? get() = activeSkipState.value

    /** @param reportSelection false для автопропуска: он не считается выбором пользователя. */
    fun skip(reportSelection: Boolean = true) {
        val skip = activeSkip ?: return
        skipPlayerSegment(
            skip = skip,
            context = context,
            player = player,
            skipUi = skipUi,
            seekController = seekController,
            reportSelection = reportSelection,
            onEvent = onEvent,
        )
    }
}

@Composable
fun rememberPlayerSegmentSkipper(
    player: Player,
    skipUi: PlayerSkipUiState,
    seekController: PlayerSeekController,
    progress: PlayerPlaybackProgressState,
    isMediaReady: Boolean,
    skips: PlayerSkips,
    onEvent: (PlayerState.Event) -> Unit,
): PlayerSegmentSkipper {
    val context = LocalContext.current
    // Позиция тикает постоянно; через derivedStateOf экран перекомпоновывается только когда
    // активная заставка реально меняется, а не на каждом тике.
    val activeSkip = remember(isMediaReady, skips, skipUi.dismissedSkipKeys) {
        derivedStateOf {
            if (isMediaReady) {
                currentSkip(skips, progress.currentPosition, skipUi.dismissedSkipKeys)
            } else {
                null
            }
        }
    }
    return PlayerSegmentSkipper(
        activeSkipState = activeSkip,
        context = context,
        player = player,
        skipUi = skipUi,
        seekController = seekController,
        onEvent = onEvent,
    )
}
