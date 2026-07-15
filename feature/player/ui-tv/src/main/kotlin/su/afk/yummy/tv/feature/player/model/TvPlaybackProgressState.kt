package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Позиция/длительность/seek-состояние TV-плеера, обновляемые polling-циклом и перемоткой. */
@Stable
internal class TvPlaybackProgressState {
    var currentPosition: Long by mutableLongStateOf(0L)
    var duration: Long by mutableLongStateOf(0L)
    var isSeeking: Boolean by mutableStateOf(false)
    var seekProgress: Float by mutableFloatStateOf(0f)
    var lastSeekTimeMs: Long by mutableLongStateOf(0L)
    var lastPositionNotifyTimeMs: Long = 0L

    val displayTimeMs: Long
        get() = if (isSeeking) (seekProgress * duration).toLong() else currentPosition
}

@Composable
internal fun rememberTvPlaybackProgressState(): TvPlaybackProgressState =
    remember { TvPlaybackProgressState() }
