@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun AccountTabs(
    selected: AccountState.AccountTab,
    onSelected: (AccountState.AccountTab) -> Unit,
    onMarkAllRead: (() -> Unit)? = null,
    markAllReadEnabled: Boolean = true,
) {
    val statsFocusRequester = remember { FocusRequester() }
    val notificationsFocusRequester = remember { FocusRequester() }
    val markAllReadFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selected, onMarkAllRead != null) {
        if (selected == AccountState.AccountTab.NOTIFICATIONS && onMarkAllRead != null) {
            runCatching { notificationsFocusRequester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountTabButton(
                label = stringResource(R.string.account_tab_stats),
                selected = selected == AccountState.AccountTab.STATS,
                onClick = { onSelected(AccountState.AccountTab.STATS) },
                modifier = Modifier
                    .focusRequester(statsFocusRequester)
                    .focusProperties { right = notificationsFocusRequester },
            )
            AccountTabButton(
                label = stringResource(R.string.account_tab_notifications),
                selected = selected == AccountState.AccountTab.NOTIFICATIONS,
                onClick = { onSelected(AccountState.AccountTab.NOTIFICATIONS) },
                modifier = Modifier
                    .focusRequester(notificationsFocusRequester)
                    .focusProperties {
                        left = statsFocusRequester
                        if (onMarkAllRead != null) {
                            right = markAllReadFocusRequester
                        }
                    },
            )
        }
        if (onMarkAllRead != null) {
            AccountAction(
                label = stringResource(R.string.account_mark_all_read),
                onClick = onMarkAllRead,
                modifier = Modifier
                    .width(260.dp)
                    .focusRequester(markAllReadFocusRequester)
                    .focusProperties { left = notificationsFocusRequester },
                enabled = markAllReadEnabled,
            )
        }
    }
}
