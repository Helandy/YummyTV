package su.afk.yummy.tv.core.designsystem.dialog

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow

/** Запрос выбора из списка из не-UI кода; ответ (id строки или `null`) приходит через [result]. */
class ChoiceDialogRequest(
    val title: String,
    val cancelText: String,
    val state: StateFlow<ChoiceDialogState>,
) {
    internal val result = CompletableDeferred<String?>()
}
