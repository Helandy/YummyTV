package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalView
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.utils.awaitNextEvent
import su.afk.yummy.tv.feature.player.common.utils.isPlaybackActive

/** Через сколько отпускать экран без воспроизведения: после конца серии, на паузе и при ошибке. */
private const val SCREEN_OFF_AFTER_IDLE_DELAY_MS = 60_000L

/**
 * Держит экран включённым, пока играет плеер.
 *
 * После конца серии, на паузе, при ошибке или пока плеера ещё нет экран держится ещё минуту, затем
 * отпускается до системного таймаута: юзеры засыпают под видео, а забытая пауза не должна часами
 * жечь экран. Возврат к воспроизведению снова держит экран.
 *
 * @param player плеер, `null` — ещё не подключён.
 * @param ended серия досмотрена до конца.
 */
@Composable
fun PlayerKeepScreenOnEffect(
    player: Player?,
    ended: Boolean = false,
) {
    val hostView = LocalView.current
    val active by produceState(initialValue = player?.isPlaybackActive() == true, player) {
        val activePlayer = player ?: return@produceState
        while (true) {
            value = activePlayer.isPlaybackActive()
            activePlayer.awaitNextEvent()
        }
    }
    DisposableEffect(hostView) {
        val wasKeepingScreenOn = hostView.keepScreenOn
        onDispose { hostView.keepScreenOn = wasKeepingScreenOn }
    }
    LaunchedEffect(hostView, ended, active) {
        hostView.keepScreenOn = true
        if (!ended && active) return@LaunchedEffect
        delay(SCREEN_OFF_AFTER_IDLE_DELAY_MS)
        hostView.keepScreenOn = false
    }
}
