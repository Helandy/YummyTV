package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.utils.PLAYER_PROGRESS_POLL_INTERVAL
import su.afk.yummy.tv.feature.player.common.utils.isAtPlayerEnd
import su.afk.yummy.tv.feature.player.common.utils.updateBufferedProgress

/**
 * Секундный цикл плеера для ТВ и мобилки: позиция (с защитой после seek), длительность,
 * буферизация, notify раз в секунду и сохранение каждые 10 секунд.
 *
 * Здесь же страховка конца эпизода: часть потоков не доигрывает до duration и не даёт
 * STATE_ENDED, поэтому конец ловим ещё и по позиции.
 *
 * Цикл перезапускается на каждой серии ([episodeKey]), иначе флаг «позиция уже была вне конца»
 * переживал бы смену серии. Держатель [progress] может пересоздаваться чаще (новый стрим той же
 * серии) — он читается через rememberUpdatedState.
 */
@Composable
fun PlayerProgressPollingEffect(
    player: Player,
    progress: PlayerPlaybackProgressState,
    reporter: PlayerProgressReporter,
    episodeKey: String,
    onPositionAtEnd: (positionMs: Long, durationMs: Long) -> Unit,
) {
    val currentProgress by rememberUpdatedState(progress)
    val currentOnPositionAtEnd by rememberUpdatedState(onPositionAtEnd)

    LaunchedEffect(player, episodeKey) {
        // Серию, открытую сразу с конечной позиции, концом не считаем: сначала должна быть
        // позиция вне зоны конца, иначе промпт выскочит на старте
        var sawPositionBeforeEnd = false
        while (true) {
            val state = currentProgress
            val sinceSeek = System.currentTimeMillis() - state.lastSeekTimeMs
            if (!state.isSeeking && sinceSeek > SEEK_SETTLE_MS) {
                state.currentPosition = player.currentPosition.coerceAtLeast(0)
            }
            val dur = player.duration
            if (dur > 0) state.duration = dur
            state.updateBufferedProgress(player)
            val now = System.currentTimeMillis()
            if (!state.isSeeking && state.duration > 0 &&
                now - state.lastPositionNotifyTimeMs >= NOTIFY_INTERVAL_MS
            ) {
                reporter.notifyPositionChanged(state.currentPosition, state.duration)
                state.lastPositionNotifyTimeMs = now
            }
            if (episodeKey.isNotBlank() && state.duration > 0 &&
                now - reporter.lastSaveTimeMs > SAVE_INTERVAL_MS
            ) {
                reporter.saveProgress(state.currentPosition, state.duration)
            }
            if (!state.isSeeking && state.duration > 0) {
                if (isAtPlayerEnd(state.currentPosition, state.duration)) {
                    if (sawPositionBeforeEnd) {
                        currentOnPositionAtEnd(state.currentPosition, state.duration)
                    }
                } else {
                    sawPositionBeforeEnd = true
                }
            }
            delay(PLAYER_PROGRESS_POLL_INTERVAL)
        }
    }
}

/** Сколько после seek не доверять позиции плеера: он ещё отдаёт старую. */
private const val SEEK_SETTLE_MS = 1_000L
private const val NOTIFY_INTERVAL_MS = 1_000L
private const val SAVE_INTERVAL_MS = 10_000L
