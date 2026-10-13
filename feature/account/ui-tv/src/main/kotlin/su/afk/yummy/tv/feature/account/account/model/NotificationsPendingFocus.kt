package su.afk.yummy.tv.feature.account.account.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Незавершённые операции над уведомлением, которые ждут отражения в списке: открытие (переход и
 * возврат), отметка прочитанным и удаление. Переживают пересоздание (rememberSaveable), чтобы фокус
 * вернулся на место и после смены конфигурации. Решения о фокусе принимает NotificationsTabState.
 */
@Stable
internal class NotificationsPendingFocus internal constructor(
    private val pendingOpenNotificationIdState: MutableState<Int?>,
    private val awaitingOpenReturnState: MutableState<Boolean>,
    private val pendingReadFocusIdState: MutableState<Int?>,
    private val pendingReadSawLoadingState: MutableState<Boolean>,
    private val pendingDeletedNotificationIdState: MutableState<Int?>,
    private val pendingDeleteFallbackIndexState: MutableState<Int?>,
    private val pendingDeleteSawLoadingState: MutableState<Boolean>,
) {
    val openNotificationRequest: PendingNotificationOpenRequest?
        get() = pendingOpenNotificationIdState.value?.let {
            PendingNotificationOpenRequest(it, awaitingOpenReturnState.value)
        }
    val pendingReadFocus: PendingNotificationReadFocus?
        get() = pendingReadFocusIdState.value?.let {
            PendingNotificationReadFocus(it, pendingReadSawLoadingState.value)
        }
    val pendingDeleteFocus: PendingNotificationDeleteFocus?
        get() {
            val id = pendingDeletedNotificationIdState.value ?: return null
            val fallbackIndex = pendingDeleteFallbackIndexState.value ?: return null
            return PendingNotificationDeleteFocus(
                id,
                fallbackIndex,
                pendingDeleteSawLoadingState.value
            )
        }

    fun beginOpenRequest(notificationId: Int) {
        pendingOpenNotificationIdState.value = notificationId
        awaitingOpenReturnState.value = false
    }

    fun markOpenRequestAwaitingReturn() {
        awaitingOpenReturnState.value = true
    }

    fun clearOpenRequest() {
        pendingOpenNotificationIdState.value = null
        awaitingOpenReturnState.value = false
    }

    fun beginPendingReadFocus(notificationId: Int) {
        pendingReadFocusIdState.value = notificationId
        pendingReadSawLoadingState.value = false
    }

    fun markPendingReadSawLoading() {
        pendingReadSawLoadingState.value = true
    }

    fun clearPendingReadFocus() {
        pendingReadFocusIdState.value = null
        pendingReadSawLoadingState.value = false
    }

    fun beginPendingDeleteFocus(notificationId: Int, fallbackIndex: Int) {
        pendingDeletedNotificationIdState.value = notificationId
        pendingDeleteFallbackIndexState.value = fallbackIndex
        pendingDeleteSawLoadingState.value = false
    }

    fun markPendingDeleteSawLoading() {
        pendingDeleteSawLoadingState.value = true
    }

    fun clearPendingDeleteFocus() {
        pendingDeletedNotificationIdState.value = null
        pendingDeleteFallbackIndexState.value = null
        pendingDeleteSawLoadingState.value = false
    }
}

@Composable
internal fun rememberNotificationsPendingFocus(): NotificationsPendingFocus {
    val pendingOpenNotificationIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    val awaitingOpenReturnState = rememberSaveable { mutableStateOf(false) }
    val pendingReadFocusIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingReadSawLoadingState = rememberSaveable { mutableStateOf(false) }
    val pendingDeletedNotificationIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingDeleteFallbackIndexState = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingDeleteSawLoadingState = rememberSaveable { mutableStateOf(false) }
    return remember {
        NotificationsPendingFocus(
            pendingOpenNotificationIdState = pendingOpenNotificationIdState,
            awaitingOpenReturnState = awaitingOpenReturnState,
            pendingReadFocusIdState = pendingReadFocusIdState,
            pendingReadSawLoadingState = pendingReadSawLoadingState,
            pendingDeletedNotificationIdState = pendingDeletedNotificationIdState,
            pendingDeleteFallbackIndexState = pendingDeleteFallbackIndexState,
            pendingDeleteSawLoadingState = pendingDeleteSawLoadingState,
        )
    }
}
