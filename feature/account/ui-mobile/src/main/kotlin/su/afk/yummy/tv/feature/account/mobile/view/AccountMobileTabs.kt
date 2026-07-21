package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileTabs(
    selected: AccountState.AccountTab,
    unreadCount: Int,
    onSelected: (AccountState.AccountTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountMobileTab(
                label = stringResource(R.string.account_tab_stats),
                selected = selected == AccountState.AccountTab.STATS,
                onClick = { onSelected(AccountState.AccountTab.STATS) },
                modifier = Modifier.weight(1f),
            )
            AccountMobileTab(
                label = stringResource(R.string.account_tab_notifications),
                selected = selected == AccountState.AccountTab.NOTIFICATIONS,
                badgeCount = unreadCount,
                onClick = { onSelected(AccountState.AccountTab.NOTIFICATIONS) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AccountMobileTab(
    label: String,
    selected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0f
        ),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier.sizeIn(minWidth = 18.dp, minHeight = 18.dp),
                    containerColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            }
        }
    }
}
