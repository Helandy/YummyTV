package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.userprofile.utils.contentCount
import su.afk.yummy.tv.feature.account.mobile.userprofile.utils.label
import su.afk.yummy.tv.feature.account.mobile.userprofile.utils.tabCountLabel
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.feature.account.common.utils.label

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UserProfileTabs(
    tabs: List<UserProfileState.Tab>,
    selected: UserProfileState.Tab,
    profile: UserProfileSummary?,
    onSelected: (UserProfileState.Tab) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        tabs.forEach { tab ->
            val count = tab.contentCount(profile)
            Tab(
                selected = tab == selected,
                onClick = { onSelected(tab) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = tab.label(),
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
                },
            )
        }
    }
}
