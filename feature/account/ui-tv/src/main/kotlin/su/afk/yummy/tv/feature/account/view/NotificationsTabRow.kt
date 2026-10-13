package su.afk.yummy.tv.feature.account.view

import su.afk.yummy.tv.feature.account.account.model.NotificationsTabState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.feature.account.account.AccountState

/** One row in the notifications list: wires [NotificationRow]'s focus/click plumbing to [notificationsTabState]. */
@Composable
internal fun NotificationsTabRow(
    notificationsTabState: NotificationsTabState,
    notificationIds: List<Int>,
    index: Int,
    notification: ProfileNotification,
    mainMenuFocusRequester: FocusRequester?,
    selectedTabFocusRequester: FocusRequester?,
    onEvent: (AccountState.Event) -> Unit,
) {
    val rowFocusRequester = notificationsTabState.focusRequesters.rowFocusRequester(index, notification.id)
    val readFocusRequester = notificationsTabState.focusRequesters.readFocusRequester(notification.id)
    val deleteFocusRequester = notificationsTabState.focusRequesters.deleteFocusRequester(notification.id)
    val firstActionFocusRequester =
        readFocusRequester.takeIf { !notification.viewed } ?: deleteFocusRequester
    var rowIsFocused by remember(notification.id) { mutableStateOf(false) }

    fun previous(): FocusRequester? = notificationsTabState.previousVerticalFocusRequester(
        index,
        notificationIds,
        selectedTabFocusRequester,
    )

    fun next(): FocusRequester? =
        notificationsTabState.nextVerticalFocusRequester(index, notificationIds)

    NotificationRow(
        notification = notification,
        onClick = {
            notificationsTabState.handleNotificationFocus(index, notificationIds)
            if (notification.isNewEpisode && notification.animeSlug != null) {
                notificationsTabState.beginOpeningNotification(notification.id)
            }
            onEvent(AccountState.Event.NotificationSelected(notification.id))
        },
        onRead = {
            notificationsTabState.pendingFocus.beginPendingReadFocus(notification.id)
            notificationsTabState.requestDeleteFocus(index, notificationIds)
            onEvent(AccountState.Event.NotificationReadSelected(notification.id))
        },
        onDelete = {
            notificationsTabState.pendingFocus.beginPendingDeleteFocus(notification.id, index)
            val immediateTarget = if (index < notificationIds.lastIndex) index + 1 else index - 1
            if (immediateTarget >= 0) {
                notificationsTabState.requestNotificationFocus(immediateTarget, notificationIds)
            } else {
                notificationsTabState.requestEmptyOrTopFocus()
            }
            onEvent(AccountState.Event.NotificationDeleteSelected(notification.id))
        },
        onReadDirectionRight = {
            notificationsTabState.requestDeleteFocus(index, notificationIds)
            true
        },
        onDeleteDirectionLeft = {
            val target = readFocusRequester.takeIf { !notification.viewed } ?: rowFocusRequester
            notificationsTabState.handleNotificationFocus(index, notificationIds)
            notificationsTabState.requestFocusSafely(target)
            true
        },
        modifier = Modifier
            .focusRequester(rowFocusRequester)
            .focusProperties {
                mainMenuFocusRequester?.let { left = it }
                previous()?.let { up = it }
                next()?.let { down = it }
                right = firstActionFocusRequester
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (!rowIsFocused) return@onPreviewKeyEvent false
                        notificationsTabState.requestMainMenuFocus(mainMenuFocusRequester)
                    }

                    Key.DirectionRight -> {
                        if (!rowIsFocused) return@onPreviewKeyEvent false
                        if (notification.viewed) {
                            notificationsTabState.requestDeleteFocus(index, notificationIds)
                        } else {
                            notificationsTabState.requestFocusSafely(firstActionFocusRequester)
                        }
                        true
                    }

                    Key.DirectionDown -> {
                        notificationsTabState.requestNotificationFocus(index + 1, notificationIds)
                    }

                    Key.DirectionUp -> {
                        notificationsTabState.requestPreviousNotificationFocus(
                            index,
                            notificationIds,
                            selectedTabFocusRequester,
                        )
                    }

                    else -> false
                }
            }
            .onFocusChanged {
                rowIsFocused = it.isFocused
                if (it.isFocused) {
                    notificationsTabState.markContentFocused()
                    notificationsTabState.handleNotificationFocus(index, notificationIds)
                }
            },
        readModifier = Modifier
            .focusRequester(readFocusRequester)
            .focusProperties {
                left = rowFocusRequester
                right = deleteFocusRequester
                previous()?.let { up = it }
                next()?.let { down = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        notificationsTabState.requestFocusSafely(rowFocusRequester)
                        true
                    }

                    Key.DirectionRight -> {
                        notificationsTabState.requestDeleteFocus(index, notificationIds)
                        true
                    }

                    Key.DirectionDown -> {
                        notificationsTabState.requestNotificationFocus(index + 1, notificationIds)
                    }

                    Key.DirectionUp -> {
                        notificationsTabState.requestPreviousNotificationFocus(
                            index,
                            notificationIds,
                            selectedTabFocusRequester,
                        )
                    }

                    else -> false
                }
            }
            .onFocusChanged {
                if (it.isFocused) {
                    notificationsTabState.markContentFocused()
                    notificationsTabState.handleNotificationFocus(index, notificationIds)
                }
            },
        deleteModifier = Modifier
            .focusRequester(deleteFocusRequester)
            .focusProperties {
                left = readFocusRequester.takeIf { !notification.viewed } ?: rowFocusRequester
                previous()?.let { up = it }
                next()?.let { down = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionLeft -> {
                        val target =
                            readFocusRequester.takeIf { !notification.viewed } ?: rowFocusRequester
                        notificationsTabState.requestFocusSafely(target)
                        true
                    }

                    Key.DirectionDown -> {
                        notificationsTabState.requestNotificationFocus(index + 1, notificationIds)
                    }

                    Key.DirectionUp -> {
                        notificationsTabState.requestPreviousNotificationFocus(
                            index,
                            notificationIds,
                            selectedTabFocusRequester,
                        )
                    }

                    else -> false
                }
            }
            .onFocusChanged {
                if (it.isFocused) {
                    notificationsTabState.markContentFocused()
                    notificationsTabState.handleNotificationFocus(index, notificationIds)
                }
            },
    )
}
