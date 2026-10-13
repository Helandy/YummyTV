package su.afk.yummy.tv.feature.account.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.account.model.NotificationsTabState

/** All reactive side effects for [NotificationsTabState], kept out of the screen composable. */
@Composable
internal fun NotificationsTabEffects(
    notificationsTabState: NotificationsTabState,
    notificationIds: List<Int>,
    notificationReadStates: List<Boolean>,
    isActiveDestination: Boolean,
    isNotificationsLoading: Boolean,
    isNotificationOpening: Boolean,
    showOpeningOverlay: Boolean,
    hubError: AccountUiError?,
) {
    // 1. Resume the open-notification restore flow once we're active again after navigating away.
    LaunchedEffect(
        isActiveDestination,
        notificationsTabState.openNotificationRequest,
        notificationIds,
    ) {
        val request = notificationsTabState.openNotificationRequest ?: return@LaunchedEffect
        if (!isActiveDestination) {
            notificationsTabState.pendingFocus.markOpenRequestAwaitingReturn()
            return@LaunchedEffect
        }
        if (!request.awaitingReturn) return@LaunchedEffect
        notificationsTabState.beginRestoreAfterNotificationOpen(notificationIds)
    }

    // 2. Remember whether a loading flash was seen while a read/delete focus move is pending -
    // effects 5 and 6 use this to know when to give up waiting on a mutation that never lands.
    LaunchedEffect(
        isNotificationsLoading,
        notificationsTabState.pendingFocus.pendingReadFocus,
        notificationsTabState.pendingFocus.pendingDeleteFocus,
    ) {
        if (isNotificationsLoading) {
            if (notificationsTabState.pendingFocus.pendingReadFocus != null) notificationsTabState.pendingFocus.markPendingReadSawLoading()
            if (notificationsTabState.pendingFocus.pendingDeleteFocus != null) notificationsTabState.pendingFocus.markPendingDeleteSawLoading()
        }
    }

    // 3. Keep the full-screen opening overlay from ever losing focus while it's shown.
    LaunchedEffect(showOpeningOverlay) {
        if (showOpeningOverlay) {
            requestFocusUntilTimeout(notificationsTabState.focusRequesters.openingOverlayFocusRequester)
        }
    }

    // 4. Clear the overlay/open-request if opening failed or we left the screen entirely.
    LaunchedEffect(isNotificationOpening, hubError, isActiveDestination) {
        if (
            notificationsTabState.showOpeningOverlayImmediately &&
            (!isActiveDestination || (!isNotificationOpening && hubError != null))
        ) {
            notificationsTabState.clearShowOpeningOverlayImmediately()
        }
        if (
            notificationsTabState.openNotificationRequest != null &&
            !isNotificationOpening &&
            hubError != null
        ) {
            notificationsTabState.clearOpenNotificationRequest()
        }
    }

    // 5. Once a marked-read notification's `viewed` flag flips, move focus onto its Delete button.
    LaunchedEffect(
        notificationIds,
        notificationReadStates,
        notificationsTabState.pendingFocus.pendingReadFocus,
        isNotificationsLoading,
    ) {
        val pending = notificationsTabState.pendingFocus.pendingReadFocus ?: return@LaunchedEffect
        val targetIndex = notificationIds.indexOf(pending.notificationId)
        when {
            targetIndex < 0 -> {
                if (pending.sawLoading && !isNotificationsLoading) {
                    notificationsTabState.pendingFocus.clearPendingReadFocus()
                }
            }

            notificationReadStates[targetIndex] -> {
                val deleteRequester =
                    notificationsTabState.focusRequesters.deleteFocusRequester(pending.notificationId)
                notificationsTabState.handleNotificationFocus(targetIndex, notificationIds)
                notificationsTabState.requestFocusAfterScroll(
                    targetIndex,
                    notificationIds,
                    deleteRequester
                )
                notificationsTabState.pendingFocus.clearPendingReadFocus()
            }

            pending.sawLoading && !isNotificationsLoading -> {
                notificationsTabState.pendingFocus.clearPendingReadFocus()
            }
        }
    }

    // 6. Once a deleted notification actually leaves the list, restore focus near where it was.
    LaunchedEffect(
        notificationIds,
        notificationsTabState.pendingFocus.pendingDeleteFocus,
        isNotificationsLoading,
    ) {
        val pending = notificationsTabState.pendingFocus.pendingDeleteFocus ?: return@LaunchedEffect
        if (notificationIds.contains(pending.notificationId)) {
            if (pending.sawLoading && !isNotificationsLoading) {
                notificationsTabState.pendingFocus.clearPendingDeleteFocus()
            }
            return@LaunchedEffect
        }

        if (notificationIds.isEmpty()) {
            notificationsTabState.requestEmptyOrTopFocus()
        } else {
            val restoredIndex = pending.fallbackIndex.coerceIn(0, notificationIds.lastIndex)
            notificationsTabState.handleNotificationFocus(restoredIndex, notificationIds)
            notificationsTabState.requestFocusAfterScroll(
                index = restoredIndex,
                notificationIds = notificationIds,
                focusRequester = notificationsTabState.focusRequesters.rowFocusRequester(
                    restoredIndex,
                    notificationIds[restoredIndex]
                ),
            )
        }
        notificationsTabState.pendingFocus.clearPendingDeleteFocus()
    }

    // 7. Cache hygiene: drop FocusRequesters for notifications that are no longer in the list.
    LaunchedEffect(notificationIds) {
        notificationsTabState.focusRequesters.pruneStaleFocusRequesters(notificationIds)
    }
}
