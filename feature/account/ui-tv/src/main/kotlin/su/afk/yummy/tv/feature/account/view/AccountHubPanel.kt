package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.AccountState

@Composable
internal fun AccountHubPanel(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    initialFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val contentModifier = modifier
        .fillMaxHeight()
        .fillMaxWidth(0.92f)
        .widthIn(max = 1440.dp)
    val unreadCount = state.notificationCounts.sumOf { it.count }

    when (state.selectedTab) {
        AccountState.AccountTab.STATS -> StatsTab(
            state = state,
            onEvent = onEvent,
            selectedTabFocusRequester = initialFocusRequester,
            modifier = contentModifier,
        )
        AccountState.AccountTab.NOTIFICATIONS -> NotificationsTab(
            state = state,
            onEvent = onEvent,
            selectedTabFocusRequester = initialFocusRequester,
            onMarkAllRead = if (unreadCount > 0) {
                { onEvent(AccountState.Event.AllNotificationsReadSelected) }
            } else {
                null
            },
            markAllReadEnabled = !state.isNotificationsLoading,
            modifier = contentModifier,
        )
    }
}
