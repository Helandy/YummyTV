package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun notificationTypeLabel(type: String): String = when (type) {
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

/** Иконка для карточки уведомления — ветки те же, что у [notificationTypeLabel]. */
internal fun notificationTypeIcon(type: String): ImageVector = when (type) {
    "news", "post" -> Icons.Filled.Campaign
    "edit", "animeupdate", "viewingorderupdate", "viewing_order_update" -> Icons.Filled.Edit
    "message" -> Icons.AutoMirrored.Filled.Message
    "comment" -> Icons.AutoMirrored.Filled.Comment
    "review" -> Icons.Filled.RateReview
    "anime_episode" -> Icons.Filled.PlayCircle
    "friend" -> Icons.Filled.PersonAdd
    "collection" -> Icons.Filled.Bookmarks
    "blogvideo" -> Icons.Filled.Videocam
    else -> Icons.Filled.Notifications
}
