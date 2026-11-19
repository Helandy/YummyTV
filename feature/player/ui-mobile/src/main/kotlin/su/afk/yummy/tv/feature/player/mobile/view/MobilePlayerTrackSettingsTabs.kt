package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileSegmentedTabs
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerTrackSettingsTab

@Composable
internal fun MobilePlayerTrackSettingsTabs(
    tabs: List<MobilePlayerTrackSettingsTab>,
    selectedTab: MobilePlayerTrackSettingsTab,
    labelFor: (MobilePlayerTrackSettingsTab) -> String,
    onTabSelected: (MobilePlayerTrackSettingsTab) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    transition: MobileTabTransition? = null,
) {
    MobileSegmentedTabs(
        tabCount = tabs.size,
        selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        onSelected = { index -> onTabSelected(tabs[index]) },
        modifier = modifier,
        transition = transition,
        equalWeight = !compact,
    ) { index, _ ->
        Text(
            text = labelFor(tabs[index]),
            color = LocalContentColor.current,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = (if (compact) Modifier else Modifier.fillMaxWidth())
                .padding(
                    horizontal = if (compact) 16.dp else 12.dp,
                    vertical = if (compact) 6.dp else 10.dp,
                ),
        )
    }
}
