package su.afk.yummy.tv.core.designsystem.dialog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Точка входа для диалога выбора из списка из data-слоя: рисует его [ChoiceDialogHost] в теме приложения.
 * Если хоста нет на экране, ответ `null` приходит сразу, чтобы вызывающий не завис.
 */
object ChoiceDialogCenter {
    private val pendingState = MutableStateFlow<ChoiceDialogRequest?>(null)
    internal val pending: StateFlow<ChoiceDialogRequest?> = pendingState.asStateFlow()

    @Volatile
    internal var hostAttached = false

    /** Возвращает [ChoiceDialogItem.id] выбранной строки или `null`, если диалог закрыли без выбора. */
    suspend fun pick(title: String, cancelText: String, state: StateFlow<ChoiceDialogState>): String? {
        if (!hostAttached) return null
        val request = ChoiceDialogRequest(title, cancelText, state)
        pendingState.value?.result?.complete(null)
        pendingState.value = request
        return try {
            request.result.await()
        } catch (e: CancellationException) {
            throw e
        } finally {
            if (pendingState.value === request) pendingState.value = null
        }
    }

    internal fun answer(request: ChoiceDialogRequest, id: String?) {
        request.result.complete(id)
        if (pendingState.value === request) pendingState.value = null
    }
}
