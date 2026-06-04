@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun NotificationsPreview(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfacePanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.account_notifications_preview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            AccountAction(
                label = stringResource(R.string.account_open),
                onClick = { onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.NOTIFICATIONS)) },
                modifier = Modifier.width(140.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (state.isNotificationsLoading && state.notifications.isEmpty()) {
            Text(text = stringResource(R.string.account_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (state.notifications.isEmpty()) {
            EmptyText(stringResource(R.string.account_notifications_empty))
        } else {
            state.notifications.take(3).forEach { notification ->
                NotificationMiniRow(notification)
            }
        }
    }
}
