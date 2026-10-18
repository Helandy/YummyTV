package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction

/** Иконка кнопки деталки — как на самом экране деталки; для настроек порядка кнопок. */
fun DetailsButtonAction.icon(): ImageVector = when (this) {
    DetailsButtonAction.WATCH -> Icons.Filled.PlayArrow
    DetailsButtonAction.LIBRARY -> Icons.AutoMirrored.Filled.PlaylistAdd
    DetailsButtonAction.FAVORITE -> Icons.Filled.Favorite
    DetailsButtonAction.EPISODES -> Icons.Filled.VideoLibrary
    DetailsButtonAction.SUBSCRIPTIONS -> Icons.Filled.Notifications
    DetailsButtonAction.FULL_DETAILS -> Icons.Filled.Info
    DetailsButtonAction.TRAILERS -> Icons.Filled.Movie
    DetailsButtonAction.SIMILAR -> Icons.Filled.AutoAwesome
    DetailsButtonAction.VIEWING_ORDER -> Icons.Filled.FormatListNumbered
    DetailsButtonAction.RATING -> Icons.Filled.Star
    DetailsButtonAction.COLLECTIONS -> Icons.Filled.CollectionsBookmark
    DetailsButtonAction.COMMENTS -> Icons.AutoMirrored.Filled.Comment
    DetailsButtonAction.REVIEWS -> Icons.Filled.RateReview
    DetailsButtonAction.BLOGGER_VIDEOS -> Icons.Filled.VideoCameraFront
    DetailsButtonAction.SCREENSHOTS -> Icons.Filled.PhotoLibrary
}
