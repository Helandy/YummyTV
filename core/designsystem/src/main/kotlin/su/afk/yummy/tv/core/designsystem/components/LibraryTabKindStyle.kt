package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.core.model.settings.LibraryTabKind

/** Иконка вкладки библиотеки — одна для вкладок и настроек порядка; у списков как на карточках тайтлов. */
fun LibraryTabKind.icon(): ImageVector = when (this) {
    LibraryTabKind.HISTORY -> Icons.Filled.History
    LibraryTabKind.CONTINUE_WATCHING -> Icons.Filled.PlayCircle
    LibraryTabKind.FAVORITES -> Icons.Filled.Favorite
    LibraryTabKind.WATCHING -> CardListStatus.WATCHING.icon()
    LibraryTabKind.PLANNED -> CardListStatus.PLANNED.icon()
    LibraryTabKind.COMPLETED -> CardListStatus.COMPLETED.icon()
    LibraryTabKind.POSTPONED -> CardListStatus.POSTPONED.icon()
    LibraryTabKind.DROPPED -> CardListStatus.DROPPED.icon()
}

/** Цвет акцента вкладки библиотеки: статусы списков — семантические, служебные вкладки — из темы. */
@Composable
fun LibraryTabKind.color(): Color = when (this) {
    // tertiary в теме — тёмный primaryContainer, на тёмном фоне иконка «Истории» терялась.
    LibraryTabKind.HISTORY -> MaterialTheme.colorScheme.secondary
    LibraryTabKind.CONTINUE_WATCHING -> MaterialTheme.colorScheme.primary
    LibraryTabKind.FAVORITES -> YummySemanticColors.StatusFavorite
    LibraryTabKind.WATCHING -> YummySemanticColors.StatusWatching
    LibraryTabKind.PLANNED -> YummySemanticColors.StatusPlanned
    LibraryTabKind.COMPLETED -> YummySemanticColors.StatusCompleted
    LibraryTabKind.POSTPONED -> YummySemanticColors.StatusPostponed
    LibraryTabKind.DROPPED -> YummySemanticColors.StatusDropped
}
