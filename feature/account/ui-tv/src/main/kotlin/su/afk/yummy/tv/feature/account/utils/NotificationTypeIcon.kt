package su.afk.yummy.tv.feature.account.utils

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
import androidx.compose.ui.graphics.vector.ImageVector

/** Иконка для карточки уведомления — ветки те же, что у `notificationTypeTitle`. */
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
