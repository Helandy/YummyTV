@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileCounterItem
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun AccountMobileProfileListCounters(
    counts: UserProfileCounts,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        AccountMobileProfileCounterItem(
            label = stringResource(R.string.account_profile_list_watching),
            count = counts.watching,
            color = YummySemanticColors.StatusWatching,
            icon = Icons.Filled.PlayArrow,
        ),
        AccountMobileProfileCounterItem(
            label = stringResource(R.string.account_profile_list_planned),
            count = counts.planned,
            color = YummySemanticColors.StatusPlanned,
            icon = Icons.Filled.Schedule,
        ),
        AccountMobileProfileCounterItem(
            label = stringResource(R.string.account_profile_list_completed),
            count = counts.completed,
            color = YummySemanticColors.StatusCompleted,
            icon = Icons.Filled.Check,
        ),
        AccountMobileProfileCounterItem(
            label = stringResource(R.string.account_profile_list_dropped),
            count = counts.dropped,
            color = YummySemanticColors.StatusDropped,
            icon = Icons.Filled.Close,
        ),
        AccountMobileProfileCounterItem(
            label = stringResource(R.string.account_profile_list_postponed),
            count = counts.postponed,
            color = YummySemanticColors.StatusPostponed,
            icon = Icons.Filled.Pause,
        ),
        AccountMobileProfileCounterItem(
            label = stringResource(R.string.account_profile_list_favorite),
            count = counts.favorite,
            color = YummySemanticColors.StatusFavorite,
            icon = Icons.Filled.Favorite,
        ),
    )
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            AccountMobileProfileCounterChip(item = item, modifier = Modifier.fillMaxWidth(0.48f))
        }
    }
}

@Composable
private fun AccountMobileProfileCounterChip(
    item: AccountMobileProfileCounterItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                text = item.count.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
