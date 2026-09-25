package su.afk.yummy.tv.feature.player.common.utils

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.model.PlayerPositionSnapshot
import kotlin.time.Duration.Companion.seconds

fun Player.positionSnapshot(fallbackDurationMs: Long): PlayerPositionSnapshot =
    PlayerPositionSnapshot(
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = duration.takeIf { it > 0 } ?: fallbackDurationMs,
    )

fun PlaybackException.analyticsType(): String =
    this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

fun calculateBufferedProgress(
    bufferedPosition: Long,
    currentPosition: Long,
    duration: Long,
): Float {
    if (duration <= 0L) return 0f
    val playedProgress = currentPosition.toFloat() / duration
    val loadedProgress = bufferedPosition.coerceAtLeast(0L).toFloat() / duration
    return loadedProgress.coerceIn(playedProgress.coerceIn(0f, 1f), 1f)
}

/** Период polling-цикла позиции/буфера плеера — одинаковый для ТВ и мобилки. */
val PLAYER_PROGRESS_POLL_INTERVAL = 1.seconds

/** Обновляет долю загруженного буфера по плееру — общий шаг polling-циклов ТВ и мобилки. */
fun PlayerPlaybackProgressState.updateBufferedProgress(
    player: Player,
    currentPositionMs: Long = currentPosition,
    durationMs: Long = duration,
) {
    bufferedProgress = calculateBufferedProgress(
        bufferedPosition = player.bufferedPosition,
        currentPosition = currentPositionMs,
        duration = durationMs,
    )
}
