@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import su.afk.yummy.tv.domain.account.model.UserSocialCounts
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileProfileListCounters(
    counts: UserProfileCounts,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        AccountMobileProfileCounterItem(
            stringResource(R.string.account_profile_list_watching),
            counts.watching,
            Color(0xFFFF6B6B)
        ),
        AccountMobileProfileCounterItem(
            stringResource(R.string.account_profile_list_planned),
            counts.planned,
            Color(0xFFA678E8)
        ),
        AccountMobileProfileCounterItem(
            stringResource(R.string.account_profile_list_completed),
            counts.completed,
            Color(0xFF69D38B)
        ),
        AccountMobileProfileCounterItem(
            stringResource(R.string.account_profile_list_dropped),
            counts.dropped,
            Color(0xFF9CA3AF)
        ),
        AccountMobileProfileCounterItem(
            stringResource(R.string.account_profile_list_postponed),
            counts.postponed,
            Color(0xFFFFC857)
        ),
        AccountMobileProfileCounterItem(
            stringResource(R.string.account_profile_list_favorite),
            counts.favorite,
            Color(0xFFD86BFF)
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
internal fun AccountMobileProfileSocialCounters(
    counts: UserSocialCounts,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        stringResource(R.string.account_profile_social_friends) to counts.friends,
        stringResource(R.string.account_profile_social_reviews) to counts.reviews,
        stringResource(R.string.account_profile_social_comments) to counts.comments,
        stringResource(R.string.account_profile_social_posts) to counts.posts,
        stringResource(R.string.account_profile_social_collections) to counts.collections,
    )
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (label, count) ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
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
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
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
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(item.color),
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

private data class AccountMobileProfileCounterItem(
    val label: String,
    val count: Int,
    val color: Color,
)
