package su.afk.yummy.tv.feature.account.view

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
import su.afk.yummy.tv.domain.account.model.hasOpenAction
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.account.model.NotificationsTabState

/** One row in the notifications list: wires [NotificationRow]'s focus/click plumbing to [notificationsTabState]. */
@Composable
internal fun NotificationsTabRow(
    notificationsTabState: NotificationsTabState,
    notificationIds: List<Int>,
    index: Int,
    notification: ProfileNotification,
    expanded: Boolean,
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
    val linkRequesters = remember(notification.id, notification.links.size) {
        List(notification.links.size) { FocusRequester() }
    }
    val showLinks = expanded && linkRequesters.isNotEmpty()

    fun previous(): FocusRequester? = notificationsTabState.previousVerticalFocusRequester(
        index,
        notificationIds,
        selectedTabFocusRequester,
    )

    fun next(): FocusRequester? =
        notificationsTabState.nextVerticalFocusRequester(index, notificationIds)

    fun downFromRow(): Boolean = if (showLinks) {
        notificationsTabState.requestFocusSafely(linkRequesters.first())
    } else {
        notificationsTabState.requestNotificationFocus(index + 1, notificationIds)
    }

    NotificationRow(
        notification = notification,
        onClick = {
            notificationsTabState.handleNotificationFocus(index, notificationIds)
            if (notification.hasOpenAction) {
                notificationsTabState.beginOpeningNotification(notification.id)
                onEvent(AccountState.Event.NotificationSelected(notification.id))
            } else {
                onEvent(AccountState.Event.NotificationExpandToggled(notification.id))
            }
        },
        expanded = expanded,
        onLinkClick = { slug ->
            notificationsTabState.handleNotificationFocus(index, notificationIds)
            notificationsTabState.beginOpeningNotification(notification.id)
            onEvent(AccountState.Event.NotificationLinkSelected(notification.id, slug))
        },
        linkModifier = { linkIndex ->
            val lastLink = linkRequesters.lastIndex
            Modifier
                .focusRequester(linkRequesters[linkIndex])
                .focusProperties {
                    left = rowFocusRequester
                    up = if (linkIndex == 0) rowFocusRequester else linkRequesters[linkIndex - 1]
                    if (linkIndex < lastLink) {
                        down = linkRequesters[linkIndex + 1]
                    } else {
                        next()?.let { down = it }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> notificationsTabState.requestFocusSafely(rowFocusRequester)
                        Key.DirectionRight -> true
                        Key.DirectionUp ->
                            notificationsTabState.requestFocusSafely(
                                if (linkIndex == 0) rowFocusRequester else linkRequesters[linkIndex - 1],
                            )

                        Key.DirectionDown ->
                            if (linkIndex < lastLink) {
                                notificationsTabState.requestFocusSafely(linkRequesters[linkIndex + 1])
                            } else {
                                notificationsTabState.requestNotificationFocus(index + 1, notificationIds)
                            }

                        else -> false
                    }
                }
                .onFocusChanged {
                    if (it.isFocused) {
                        notificationsTabState.markContentFocused()
                        notificationsTabState.handleNotificationFocus(index, notificationIds)
                    }
                }
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
                if (showLinks) down = linkRequesters.first() else next()?.let { down = it }
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

                    // Превью-событие строки приходит раньше, чем у вложенных кнопок
                    // (ссылки, «Прочитать», «Удалить») — их стрелки обрабатывают они сами.
                    Key.DirectionDown -> {
                        if (!rowIsFocused) return@onPreviewKeyEvent false
                        downFromRow()
                    }

                    Key.DirectionUp -> {
                        if (!rowIsFocused) return@onPreviewKeyEvent false
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
                if (showLinks) down = linkRequesters.first() else next()?.let { down = it }
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

                    Key.DirectionDown -> downFromRow()

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
                if (showLinks) down = linkRequesters.first() else next()?.let { down = it }
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

                    Key.DirectionDown -> downFromRow()

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
