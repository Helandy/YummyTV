@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun ProfileListCountersRow(
    counts: UserProfileCounts,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_watching),
            counts.watching,
            Color(0xFFFF6B6B)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_planned),
            counts.planned,
            Color(0xFFA678E8)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_completed),
            counts.completed,
            Color(0xFF69D38B)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_dropped),
            counts.dropped,
            Color(0xFF9CA3AF)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_postponed),
            counts.postponed,
            Color(0xFFFFC857)
        ),
        ProfileCounterItem(
            stringResource(R.string.account_profile_list_favorite),
            counts.favorite,
            Color(0xFFD86BFF)
        ),
    )

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { item -> ProfileCounterChip(item) }
    }
}

@Composable
private fun ProfileCounterChip(item: ProfileCounterItem) {
    Row(
        modifier = Modifier
            .widthIn(min = 132.dp, max = 180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(item.color),
            )
        }
        Column {
            Text(
                text = item.count.toString(),
                style = MaterialTheme.typography.titleMedium,
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

private data class ProfileCounterItem(
    val label: String,
    val count: Int,
    val color: Color,
)
