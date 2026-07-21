package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.utils.calculateBufferedProgress
import su.afk.yummy.tv.feature.player.mobile.utils.segments
import kotlin.time.Duration.Companion.seconds

/**
 * Секундный цикл: notify позиции, буферизация, сохранение каждые 10 секунд
 * и авто-скип опенингов/эндингов.
 */
@Composable
internal fun MobilePlayerProgressPollingEffect(
    player: Player,
    episodeKey: String,
    isMediaReady: Boolean,
    autoSkipOpeningsEndings: Boolean,
    reporter: PlayerProgressReporter,
    skippedSegments: MutableList<String>,
    isSeeking: () -> Boolean,
    currentPositionMs: () -> Long,
    fallbackDurationMs: () -> Long,
    activeSkips: () -> PlayerSkips,
    onBufferedProgressChange: (Float) -> Unit,
) {
    val currentIsSeeking by rememberUpdatedState(isSeeking)
    val currentPosition by rememberUpdatedState(currentPositionMs)
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentActiveSkips by rememberUpdatedState(activeSkips)
    val currentOnBufferedProgressChange by rememberUpdatedState(onBufferedProgressChange)

    LaunchedEffect(player, episodeKey, isMediaReady, autoSkipOpeningsEndings) {
        while (true) {
            var position = currentPosition()
            var dur = currentFallbackDuration()
            if (!currentIsSeeking()) {
                position = player.currentPosition.coerceAtLeast(0)
                dur = player.duration.takeIf { it > 0 } ?: 0L
                reporter.notifyPositionChanged(position, dur)
            }
            currentOnBufferedProgressChange(
                calculateBufferedProgress(
                    bufferedPosition = player.bufferedPosition,
                    currentPosition = position,
                    duration = dur,
                )
            )
            val now = System.currentTimeMillis()
            if (dur > 0 && now - reporter.lastSaveTimeMs >= 10_000L) {
                reporter.saveProgress(position, dur)
            }
            if (isMediaReady && autoSkipOpeningsEndings) {
                currentActiveSkips().segments().forEach { (key, segment) ->
                    val segmentKey = "$episodeKey-$key"
                    if (segmentKey !in skippedSegments &&
                        position in segment.startMs..segment.endMs
                    ) {
                        skippedSegments += segmentKey
                        player.seekTo(segment.endMs)
                        reporter.notifyPositionChanged(segment.endMs, dur)
                    }
                }
            }
            delay(1.seconds)
        }
    }
}
