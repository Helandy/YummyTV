package su.afk.yummy.tv.core.designsystem.dialog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Точка входа для диалога выбора из списка из data-слоя: рисует его [ChoiceDialogHost] в теме приложения.
 * Если хоста нет на экране, ответ `null` приходит сразу, чтобы вызывающий не завис.
 */
object ChoiceDialogCenter {
    /** Ответ [pick] при нажатии «Повторить» (см. [ChoiceDialogState.Message.retryText]); не совпадает ни с одним id строки. */
    const val RETRY_ID: String = "\u0000retry"

    private val pendingState = MutableStateFlow<ChoiceDialogRequest?>(null)
    internal val pending: StateFlow<ChoiceDialogRequest?> = pendingState.asStateFlow()

    private val hostCount = AtomicInteger(0)

    /** Хостов может быть несколько (вкладки и отдельная Activity плеера), поэтому считаем, а не храним флаг. */
    internal val hostAttached: Boolean get() = hostCount.get() > 0

    internal fun attachHost() {
        hostCount.incrementAndGet()
    }

    internal fun detachHost() {
        hostCount.updateAndGet { (it - 1).coerceAtLeast(0) }
    }

    /** Возвращает [ChoiceDialogItem.id] выбранной строки, [RETRY_ID] или `null`, если диалог закрыли без выбора. */
    suspend fun pick(title: String, cancelText: String, state: StateFlow<ChoiceDialogState>): String? {
        if (!hostAttached) return null
        val request = ChoiceDialogRequest(title, cancelText, state)
        pendingState.value?.result?.complete(null)
        pendingState.value = request
        return try {
            coroutineScope {
                // Вызывающий может выбрать сам (AutoSelect): закрываем диалог без нажатия.
                val watcher = launch {
                    val auto = state.first { it is ChoiceDialogState.AutoSelect } as ChoiceDialogState.AutoSelect
                    request.result.complete(auto.id)
                }
                try {
                    request.result.await()
                } finally {
                    watcher.cancel()
                }
            }
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
