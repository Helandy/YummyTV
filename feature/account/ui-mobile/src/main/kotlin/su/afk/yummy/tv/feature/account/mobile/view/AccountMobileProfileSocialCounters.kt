@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserSocialCounts
import su.afk.yummy.tv.feature.account.presentation.R

private data class AccountMobileSocialItem(
    val icon: ImageVector,
    val label: String,
    val count: Int,
)

@Composable
internal fun AccountMobileProfileSocialCounters(
    counts: UserSocialCounts,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        AccountMobileSocialItem(
            icon = Icons.Filled.Groups,
            label = stringResource(R.string.account_profile_social_friends),
            count = counts.friends,
        ),
        AccountMobileSocialItem(
            icon = Icons.Filled.RateReview,
            label = stringResource(R.string.account_profile_social_reviews),
            count = counts.reviews,
        ),
        AccountMobileSocialItem(
            icon = Icons.Filled.ChatBubbleOutline,
            label = stringResource(R.string.account_profile_social_comments),
            count = counts.comments,
        ),
        AccountMobileSocialItem(
            icon = Icons.Filled.Article,
            label = stringResource(R.string.account_profile_social_posts),
            count = counts.posts,
        ),
        AccountMobileSocialItem(
            icon = Icons.Filled.CollectionsBookmark,
            label = stringResource(R.string.account_profile_social_collections),
            count = counts.collections,
        ),
    )
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
