package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileSegmentedTabs
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun AccountMobileTabs(
    selected: AccountState.AccountTab,
    unreadCount: Int,
    onSelected: (AccountState.AccountTab) -> Unit,
    modifier: Modifier = Modifier,
    transition: MobileTabTransition? = null,
) {
    val tabs = AccountState.AccountTab.entries
    MobileSegmentedTabs(
        tabCount = tabs.size,
        selectedIndex = tabs.indexOf(selected).coerceAtLeast(0),
        onSelected = { index -> onSelected(tabs[index]) },
        modifier = modifier,
        transition = transition,
    ) { index, selectionFraction ->
        when (tabs[index]) {
            AccountState.AccountTab.STATS -> AccountMobileTabContent(
                label = stringResource(R.string.account_tab_stats),
                icon = Icons.Filled.QueryStats,
                selectionFraction = selectionFraction,
            )

            AccountState.AccountTab.NOTIFICATIONS -> AccountMobileTabContent(
                label = stringResource(R.string.account_tab_notifications),
                icon = Icons.Filled.Notifications,
                selectionFraction = selectionFraction,
                badgeCount = unreadCount,
            )
        }
    }
}

@Composable
private fun AccountMobileTabContent(
    label: String,
    icon: ImageVector,
    selectionFraction: Float,
    badgeCount: Int = 0,
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (badgeCount > 0) {
            val containerColor = lerp(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
                selectionFraction,
            )
            val contentColor = lerp(
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.primary,
                selectionFraction,
            )
            Badge(
                modifier = Modifier.sizeIn(minWidth = 18.dp, minHeight = 18.dp),
                containerColor = containerColor,
                contentColor = contentColor,
            ) {
                Text(if (badgeCount > 99) "99+" else badgeCount.toString())
            }
        }
    }
}
