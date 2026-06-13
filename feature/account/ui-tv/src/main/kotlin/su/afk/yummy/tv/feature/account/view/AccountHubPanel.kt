@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage

@Composable
internal fun AccountHubPanel(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    initialFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val notificationsContentFocusRequester = remember { FocusRequester() }
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
        AccountState.AccountTab.NOTIFICATIONS -> Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AccountHeader(state = state, onEvent = onEvent)
            AccountTabs(
                selected = state.selectedTab,
                onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                selectedTabFocusRequester = initialFocusRequester,
                contentFocusRequester = notificationsContentFocusRequester,
                onMarkAllRead = if (unreadCount > 0) {
                    { onEvent(AccountState.Event.AllNotificationsReadSelected) }
                } else {
                    null
                },
                markAllReadEnabled = !state.isNotificationsLoading,
            )
            ErrorText((state.error ?: state.hubError).accountErrorMessage())
            NotificationsTab(
                state = state,
                onEvent = onEvent,
                contentFocusRequester = notificationsContentFocusRequester,
                upFocusRequester = initialFocusRequester,
            )
        }
    }
}
