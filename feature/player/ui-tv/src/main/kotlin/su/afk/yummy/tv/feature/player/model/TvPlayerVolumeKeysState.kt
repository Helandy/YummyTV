package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.common.PlayerSystemVolumeController
import kotlin.math.roundToInt
import kotlin.time.Duration

/** Шаг громкости при перехвате кнопок пульта. */
private const val TV_VOLUME_KEY_STEP = 0.01f

/**
 * Перехват кнопок громкости в ТВ-плеере: меняет системную громкость шагом 1%
 * и показывает свой индикатор вместо системного.
 */
@Stable
internal class TvPlayerVolumeKeysState(
    private val scope: CoroutineScope,
    private val systemVolume: PlayerSystemVolumeController,
    private val indicatorDuration: Duration,
) {
    var indicatorText: String? by mutableStateOf(null)
        private set

    private var hideJob: Job? = null

    fun step(up: Boolean) {
        val fraction = systemVolume.stepBy(if (up) TV_VOLUME_KEY_STEP else -TV_VOLUME_KEY_STEP)
        indicatorText = "${(fraction * 100f).roundToInt()}%"
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(indicatorDuration)
            indicatorText = null
        }
    }
}

@Composable
internal fun rememberTvPlayerVolumeKeysState(
    systemVolume: PlayerSystemVolumeController,
    indicatorDuration: Duration,
): TvPlayerVolumeKeysState {
    val scope = rememberCoroutineScope()
    return remember(systemVolume) {
        TvPlayerVolumeKeysState(
            scope = scope,
            systemVolume = systemVolume,
            indicatorDuration = indicatorDuration,
        )
    }
}
