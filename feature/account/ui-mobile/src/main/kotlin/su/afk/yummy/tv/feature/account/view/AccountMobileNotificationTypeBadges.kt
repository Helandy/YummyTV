@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileNotificationTypeBadges(counts: List<NotificationCount>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        counts.filter { it.count > 0 }.forEach { item ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = androidx.compose.ui.Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                ) {
                    Text(
                        notificationTypeLabel(item.type),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Badge { Text(if (item.count > 99) "99+" else item.count.toString()) }
                }
            }
        }
    }
}

@Composable
private fun notificationTypeLabel(type: String): String = when (type) {
    "news" -> stringResource(R.string.account_notification_type_news)
    "edit" -> stringResource(R.string.account_notification_type_edit)
    "message" -> stringResource(R.string.account_notification_type_message)
    "comment" -> stringResource(R.string.account_notification_type_comment)
    "animeupdate" -> stringResource(R.string.account_notification_type_animeupdate)
    "review" -> stringResource(R.string.account_notification_type_review)
    "viewingorderupdate", "viewing_order_update" -> stringResource(R.string.account_notification_type_viewing_order_update)
    "anime_episode" -> stringResource(R.string.account_notification_type_anime_episode)
    "friend" -> stringResource(R.string.account_notification_type_friend)
    "collection" -> stringResource(R.string.account_notification_type_collection)
    "post" -> stringResource(R.string.account_notification_type_post)
    "blogvideo" -> stringResource(R.string.account_notification_type_blogvideo)
    else -> type
}
