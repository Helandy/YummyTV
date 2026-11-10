package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.settings.HomeBlockKind

/** Иконка блока главной в настройках порядка и видимости. */
fun HomeBlockKind.icon(): ImageVector = when (this) {
    HomeBlockKind.HERO -> Icons.Filled.Star
    HomeBlockKind.CONTINUE_WATCHING -> Icons.Filled.PlayCircle
    HomeBlockKind.MY_NEW_EPISODES -> Icons.Filled.NotificationsActive
    HomeBlockKind.NEW_RELEASES -> Icons.Filled.NewReleases
    HomeBlockKind.NEW_TITLES -> Icons.Filled.Slideshow
    HomeBlockKind.RECOMMENDATIONS -> Icons.Filled.Recommend
    HomeBlockKind.ANNOUNCEMENTS -> Icons.Filled.Campaign
    HomeBlockKind.COLLECTIONS -> Icons.Filled.VideoLibrary
    HomeBlockKind.BLOGGER_VIDEOS -> Icons.Filled.Videocam
    HomeBlockKind.QUICK_ACTIONS -> Icons.Filled.Bolt
}

/** Цвет акцента блока главной: часть — семантические цвета статусов, остальные — из темы. */
@Composable
fun HomeBlockKind.color(): Color = when (this) {
    HomeBlockKind.HERO -> YummySemanticColors.StatusFavorite
    HomeBlockKind.CONTINUE_WATCHING -> MaterialTheme.colorScheme.primary
    HomeBlockKind.MY_NEW_EPISODES -> YummySemanticColors.StatusWatching
    HomeBlockKind.NEW_RELEASES -> YummySemanticColors.StatusPlanned
    HomeBlockKind.NEW_TITLES -> YummySemanticColors.StatusCompleted
    HomeBlockKind.RECOMMENDATIONS -> YummySemanticColors.StatusPostponed
    HomeBlockKind.ANNOUNCEMENTS -> YummySemanticColors.StatusDropped
    HomeBlockKind.COLLECTIONS -> MaterialTheme.colorScheme.secondary
    HomeBlockKind.BLOGGER_VIDEOS -> MaterialTheme.colorScheme.onSurfaceVariant
    HomeBlockKind.QUICK_ACTIONS -> MaterialTheme.colorScheme.primary
}
