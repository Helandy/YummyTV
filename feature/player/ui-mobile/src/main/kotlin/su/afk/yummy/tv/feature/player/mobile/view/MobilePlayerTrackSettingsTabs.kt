package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerTrackSettingsTab

@Composable
internal fun MobilePlayerTrackSettingsTabs(
    tabs: List<MobilePlayerTrackSettingsTab>,
    selectedTab: MobilePlayerTrackSettingsTab,
    labelFor: (MobilePlayerTrackSettingsTab) -> String,
    onTabSelected: (MobilePlayerTrackSettingsTab) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = if (compact) modifier else modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                MobilePlayerTrackSettingsTabItem(
                    label = labelFor(tab),
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    compact = compact,
                    modifier = if (compact) Modifier else Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MobilePlayerTrackSettingsTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = (if (compact) Modifier else Modifier.fillMaxWidth())
                .padding(horizontal = if (compact) 16.dp else 12.dp, vertical = if (compact) 6.dp else 10.dp),
        )
    }
}
