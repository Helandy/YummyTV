package su.afk.yummy.tv.feature.library.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Удерживает экран включённым на время долгого импорта или экспорта списка аниме
 * (особенно актуально на Android TV, где система может усыпить устройство во время пакетной синхронизации).
 */
@Composable
fun KeepTransferScreenOnEffect(keepScreenOn: Boolean) {
    if (!keepScreenOn) return
    val hostView = LocalView.current
    DisposableEffect(hostView) {
        val wasKeepingScreenOn = hostView.keepScreenOn
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = wasKeepingScreenOn }
    }
}
