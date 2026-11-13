package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind

/** Иконка блока экрана деталей в настройках порядка и видимости. */
fun DetailsSectionKind.icon(): ImageVector = when (this) {
    DetailsSectionKind.INFO -> Icons.Filled.Category
    DetailsSectionKind.DESCRIPTION -> Icons.AutoMirrored.Filled.Notes
    DetailsSectionKind.SCREENSHOTS -> Icons.Filled.PhotoLibrary
    DetailsSectionKind.VIDEO -> Icons.Filled.Movie
    DetailsSectionKind.RATING -> Icons.Filled.Star
    DetailsSectionKind.UNIVERSE -> Icons.Filled.FormatListNumbered
    DetailsSectionKind.SIMILAR -> Icons.Filled.AutoAwesome
    DetailsSectionKind.COLLECTIONS -> Icons.Filled.CollectionsBookmark
    DetailsSectionKind.REVIEWS -> Icons.Filled.RateReview
    DetailsSectionKind.COMMENTS -> Icons.AutoMirrored.Filled.Comment
}

/** Цвет акцента блока экрана деталей: часть — семантические цвета статусов, остальные — из темы. */
@Composable
fun DetailsSectionKind.color(): Color = when (this) {
    DetailsSectionKind.INFO -> MaterialTheme.colorScheme.secondary
    DetailsSectionKind.DESCRIPTION -> MaterialTheme.colorScheme.onSurfaceVariant
    DetailsSectionKind.SCREENSHOTS -> YummySemanticColors.StatusCompleted
    DetailsSectionKind.VIDEO -> YummySemanticColors.StatusDropped
    DetailsSectionKind.RATING -> YummySemanticColors.StatusFavorite
    DetailsSectionKind.UNIVERSE -> YummySemanticColors.StatusPlanned
    DetailsSectionKind.SIMILAR -> YummySemanticColors.StatusPostponed
    DetailsSectionKind.COLLECTIONS -> MaterialTheme.colorScheme.secondary
    DetailsSectionKind.REVIEWS -> YummySemanticColors.StatusWatching
    DetailsSectionKind.COMMENTS -> MaterialTheme.colorScheme.primary
}
