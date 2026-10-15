package su.afk.yummy.tv.core.designsystem.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Размещается внутри темы приложения один раз; показывает запросы [ConfirmDialogCenter]:
 * шторку на мобилке и оверлей с фокусом пультом на ТВ, в стиле пикеров деталки.
 */
@Composable
fun ConfirmDialogHost(isTelevision: Boolean) {
    DisposableEffect(Unit) {
        ConfirmDialogCenter.hostAttached = true
        onDispose { ConfirmDialogCenter.hostAttached = false }
    }
    val request by ConfirmDialogCenter.pending.collectAsState()
    val current = request ?: return
    val onAnswer: (Boolean) -> Unit = { ConfirmDialogCenter.answer(current, it) }
    if (isTelevision) {
        TvConfirmDialog(request = current, onAnswer = onAnswer)
    } else {
        MobileConfirmSheet(request = current, onAnswer = onAnswer)
    }
}
