package su.afk.yummy.tv.core.designsystem.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Размещается внутри темы приложения один раз; показывает запросы [ChoiceDialogCenter]:
 * шторку на мобилке и оверлей с фокусом пультом на ТВ.
 */
@Composable
fun ChoiceDialogHost(isTelevision: Boolean) {
    DisposableEffect(Unit) {
        ChoiceDialogCenter.attachHost()
        onDispose { ChoiceDialogCenter.detachHost() }
    }
    val request by ChoiceDialogCenter.pending.collectAsState()
    val current = request ?: return
    val onAnswer: (String?) -> Unit = { ChoiceDialogCenter.answer(current, it) }
    if (isTelevision) {
        TvChoiceDialog(request = current, onAnswer = onAnswer)
    } else {
        MobileChoiceSheet(request = current, onAnswer = onAnswer)
    }
}
