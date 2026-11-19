package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileSegmentedTabs
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabColorText
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun AccountMobileTabs(
    selected: AccountState.AccountTab,
    unreadCount: Int,
    onSelected: (AccountState.AccountTab) -> Unit,
    modifier: Modifier = Modifier,
    transition: (() -> MobileTabTransition)? = null,
) {
    val tabs = AccountState.AccountTab.entries
    MobileSegmentedTabs(
        tabCount = tabs.size,
        selectedIndex = tabs.indexOf(selected).coerceAtLeast(0),
        onSelected = { index -> onSelected(tabs[index]) },
        modifier = modifier,
        transition = transition,
    ) { index, selectionFraction, contentColor ->
        when (tabs[index]) {
            AccountState.AccountTab.STATS -> AccountMobileTabContent(
                label = stringResource(R.string.account_tab_stats),
                icon = Icons.Filled.QueryStats,
                selectionFraction = selectionFraction,
                contentColor = contentColor,
            )

            AccountState.AccountTab.NOTIFICATIONS -> AccountMobileTabContent(
                label = stringResource(R.string.account_tab_notifications),
                icon = Icons.Filled.Notifications,
                selectionFraction = selectionFraction,
                contentColor = contentColor,
                badgeCount = unreadCount,
            )
        }
    }
}

@Composable
private fun AccountMobileTabContent(
    label: String,
    icon: ImageVector,
    selectionFraction: () -> Float,
    contentColor: ColorProducer,
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
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(color = contentColor(), blendMode = BlendMode.SrcIn)
                },
        )
        MobileTabColorText(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (badgeCount > 0) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
            val badgeTextColor = remember(selectionFraction, primaryColor, onPrimaryColor) {
                ColorProducer { lerp(onPrimaryColor, primaryColor, selectionFraction()) }
            }
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = lerp(primaryColor, onPrimaryColor, selectionFraction()),
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    }
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                MobileTabColorText(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = badgeTextColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
