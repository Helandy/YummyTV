package su.afk.yummy.tv.core.designsystem.dialog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Точка входа для диалога подтверждения из data-слоя: рисует его [ConfirmDialogHost] в теме приложения.
 * Если хоста нет на экране, ответ `false` приходит сразу, чтобы вызывающий не завис.
 */
object ConfirmDialogCenter {
    private val pendingState = MutableStateFlow<ConfirmDialogRequest?>(null)
    internal val pending: StateFlow<ConfirmDialogRequest?> = pendingState.asStateFlow()

    @Volatile
    internal var hostAttached = false

    /** Есть ли на экране [ConfirmDialogHost]; без него [ask] сразу отвечает `false`. */
    val isHostAttached: Boolean get() = hostAttached

    suspend fun ask(title: String, message: String, confirmText: String, dismissText: String): Boolean {
        if (!hostAttached) return false
        val request = ConfirmDialogRequest(title, message, confirmText, dismissText)
        pendingState.value?.result?.complete(false)
        pendingState.value = request
        return try {
            request.result.await()
        } catch (e: CancellationException) {
            throw e
        } finally {
            if (pendingState.value === request) pendingState.value = null
        }
    }

    internal fun answer(request: ConfirmDialogRequest, value: Boolean) {
        request.result.complete(value)
        if (pendingState.value === request) pendingState.value = null
    }
}
