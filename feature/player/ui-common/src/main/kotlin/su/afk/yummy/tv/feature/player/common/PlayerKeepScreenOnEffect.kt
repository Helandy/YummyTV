package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

private const val SCREEN_OFF_AFTER_END_DELAY_MS = 60_000L

/**
 * Держит экран включённым, пока играет плеер.
 *
 * После конца видео через минуту отпускает экран, чтобы он погас по системному таймауту
 * (юзеры засыпают под видео).
 *
 * @param ended серия досмотрена до конца.
 */
@Composable
fun PlayerKeepScreenOnEffect(
    ended: Boolean = false,
) {
    val hostView = LocalView.current
    DisposableEffect(hostView) {
        val wasKeepingScreenOn = hostView.keepScreenOn
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = wasKeepingScreenOn }
    }
    LaunchedEffect(hostView, ended) {
        if (!ended) return@LaunchedEffect
        delay(SCREEN_OFF_AFTER_END_DELAY_MS)
        hostView.keepScreenOn = false
        try {
            awaitCancellation()
        } finally {
            hostView.keepScreenOn = true
        }
    }
}
