package su.afk.yummy.tv.feature.player.common

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

data class PlayerPositionSnapshot(
    val positionMs: Long,
    val durationMs: Long,
)

fun Player.positionSnapshot(fallbackDurationMs: Long): PlayerPositionSnapshot =
    PlayerPositionSnapshot(
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = duration.takeIf { it > 0 } ?: fallbackDurationMs,
    )

fun PlaybackException.analyticsType(): String =
    this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

/** Full exception/cause type chain without messages that may contain signed media URLs. */
fun PlaybackException.diagnosticCauseChain(): String =
    generateSequence(this as Throwable?) { it.cause }
        .take(MAX_DIAGNOSTIC_CAUSES)
        .joinToString(separator = " <- ") { throwable ->
            throwable::class.java.name
        }

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

private const val MAX_DIAGNOSTIC_CAUSES = 8
