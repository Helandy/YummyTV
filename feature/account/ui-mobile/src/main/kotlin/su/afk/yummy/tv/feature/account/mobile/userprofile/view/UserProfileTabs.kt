package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileScrollableTabs
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabColorText
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.mobile.userprofile.utils.label
import su.afk.yummy.tv.feature.account.mobile.userprofile.utils.tabCountLabel
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.userprofile.utils.contentCount

@Composable
internal fun UserProfileTabs(
    tabs: List<UserProfileState.Tab>,
    selected: UserProfileState.Tab,
    profile: UserProfileSummary?,
    onSelected: (UserProfileState.Tab) -> Unit,
    transition: (() -> MobileTabTransition)? = null,
) {
    MobileScrollableTabs(
        tabCount = tabs.size,
        selectedIndex = tabs.indexOf(selected).coerceAtLeast(0),
        onSelected = { index -> tabs.getOrNull(index)?.let(onSelected) },
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        transition = transition,
        edgePadding = 8.dp,
        showDivider = true,
    ) { index, contentColor ->
        val tab = tabs[index]
        val count = tab.contentCount(profile)
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MobileTabColorText(
                text = tab.label(),
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null) {
                Badge(
                    modifier = Modifier.sizeIn(minWidth = 18.dp, minHeight = 18.dp),
                ) {
                    Text(
                        text = count.tabCountLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
