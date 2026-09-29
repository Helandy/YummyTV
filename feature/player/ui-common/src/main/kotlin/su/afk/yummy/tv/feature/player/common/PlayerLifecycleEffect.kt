package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.utils.positionSnapshot

/**
 * Общий для ТВ и мобилки жизненный цикл плеера: пауза и сохранение прогресса на ON_PAUSE,
 * возобновление на ON_RESUME и однократная выгрузка при уходе с плеера.
 *
 * Возобновление решается флагом, снятым на ON_PAUSE, а не текущим [wantsPlay]: пауза из ON_PAUSE
 * сама сбрасывает wantsPlay через onPlayWhenReadyChanged, и после кратковременного
 * ON_PAUSE → ON_RESUME (бывает сразу при появлении плеера) видео оставалось бы на паузе.
 *
 * Выгрузка идёт после остальных эффектов плеера, если этот эффект объявлен раньше них: Compose
 * освобождает эффекты в обратном порядке. На мобилке это важно — clearMediaItems() при ещё
 * подписанном слушателе выглядит для него как конец серии.
 *
 * @param keepPlayingOnPause плеер не ставится на паузу на ON_PAUSE (мобилка: PiP или каст;
 *   ТВ: никогда).
 * @param keepPlayingOnLeave плеер не выгружается, когда уходит из композиции (мобилка: PiP;
 *   ТВ: никогда). Прогресс сохраняется в любом случае.
 * @param releaseOnStop выгружать плеер уже на ON_STOP (ТВ: уход в фон = уход с плеера).
 * @param onPaused вызывается на ON_PAUSE до паузы, например чтобы понизить отсчёт следующей серии.
 * @param onRelease платформенная выгрузка: остановка сервиса на ТВ, clear/stop на мобилке.
 *   Вызывается и без подключённого плеера — ТВ-клиент тогда остановит сервис при подключении.
 */
@Composable
fun PlayerLifecycleEffect(
    player: Player?,
    reporter: PlayerProgressReporter,
    fallbackDurationMs: () -> Long,
    wantsPlay: () -> Boolean,
    keepPlayingOnPause: () -> Boolean,
    keepPlayingOnLeave: () -> Boolean,
    releaseOnStop: Boolean,
    onPaused: () -> Unit,
    onRelease: () -> Unit,
) {
    val currentPlayer by rememberUpdatedState(player)
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentWantsPlay by rememberUpdatedState(wantsPlay)
    val currentKeepPlayingOnPause by rememberUpdatedState(keepPlayingOnPause)
    val currentKeepPlayingOnLeave by rememberUpdatedState(keepPlayingOnLeave)
    val currentOnPaused by rememberUpdatedState(onPaused)
    val currentOnRelease by rememberUpdatedState(onRelease)

    val lifecycleOwner = LocalLifecycleOwner.current
    // Плеер не в ключах: на ТВ он появляется после подключения контроллера, и перезапуск эффекта
    // в этот момент выгрузил бы только что подключённый плеер.
    DisposableEffect(lifecycleOwner) {
        var released = false
        var resumeAfterPause = false

        fun saveProgress(activePlayer: Player) {
            if (activePlayer.mediaItemCount == 0) return
            val snapshot = activePlayer.positionSnapshot(currentFallbackDuration())
            reporter.notifyPositionChanged(snapshot.positionMs, snapshot.durationMs)
            reporter.saveProgress(snapshot.positionMs, snapshot.durationMs)
        }

        fun release() {
            if (released) return
            released = true
            currentPlayer?.let(::saveProgress)
            currentOnRelease()
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> if (!released) {
                    currentOnPaused()
                    val keepPlaying = currentKeepPlayingOnPause()
                    resumeAfterPause = currentWantsPlay() && !keepPlaying
                    currentPlayer?.let { activePlayer ->
                        saveProgress(activePlayer)
                        if (!keepPlaying) activePlayer.pause()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (!released && resumeAfterPause) currentPlayer?.play()
                    resumeAfterPause = false
                }

                Lifecycle.Event.ON_STOP -> if (releaseOnStop) release()

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (currentKeepPlayingOnLeave()) {
                currentPlayer?.let(::saveProgress)
            } else {
                release()
            }
        }
    }
}
