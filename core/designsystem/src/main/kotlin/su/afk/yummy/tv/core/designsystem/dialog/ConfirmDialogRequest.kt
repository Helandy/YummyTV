package su.afk.yummy.tv.core.designsystem.dialog

import kotlinx.coroutines.CompletableDeferred

/** Запрос подтверждения из не-UI кода; ответ приходит через [result]. */
class ConfirmDialogRequest(
    val title: String,
    val message: String,
    val confirmText: String,
    val dismissText: String,
) {
    internal val result = CompletableDeferred<Boolean>()
}
