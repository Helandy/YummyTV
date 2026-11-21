package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.notifications.NotificationsState

@Composable
internal fun AccountHubPanel(
    state: AccountState.State,
    notificationsState: NotificationsState.State,
    onEvent: (AccountState.Event) -> Unit,
    onNotificationsEvent: (NotificationsState.Event) -> Unit,
    initialFocusRequester: FocusRequester? = null,
    isActiveDestination: Boolean = true,
    onStatsContentFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val contentModifier = modifier
        .fillMaxHeight()
        .fillMaxWidth(0.96f)
        .widthIn(max = 1680.dp)
    val unreadCount = notificationsState.unreadNotificationCount

    when (state.selectedTab) {
        AccountState.AccountTab.STATS -> StatsTab(
            state = state,
            onEvent = onEvent,
            selectedTabFocusRequester = initialFocusRequester,
            onStatsContentFocusChanged = onStatsContentFocusChanged,
            modifier = contentModifier,
        )

        AccountState.AccountTab.NOTIFICATIONS -> NotificationsTab(
            state = state,
            notificationsState = notificationsState,
            onEvent = onEvent,
            onNotificationsEvent = onNotificationsEvent,
            selectedTabFocusRequester = initialFocusRequester,
            isActiveDestination = isActiveDestination,
            onMarkAllRead = if (unreadCount > 0) {
                { onNotificationsEvent(NotificationsState.Event.AllNotificationsReadSelected) }
            } else {
                null
            },
            markAllReadEnabled = !notificationsState.isNotificationsLoading,
            modifier = contentModifier,
        )
    }
}
