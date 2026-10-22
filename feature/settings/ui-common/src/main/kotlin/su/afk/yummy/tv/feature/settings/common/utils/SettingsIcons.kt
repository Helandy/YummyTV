package su.afk.yummy.tv.feature.settings.common.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.components.color
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.TitleCardLayout

fun TitleCardLayout.icon(): ImageVector = when (this) {
    TitleCardLayout.TILE -> Icons.Filled.GridView
    TitleCardLayout.ROW -> Icons.Filled.ViewList
}

fun PosterCardSize.icon(): ImageVector = when (this) {
    PosterCardSize.COMPACT -> Icons.Filled.PhotoSizeSelectSmall
    PosterCardSize.STANDARD -> Icons.Filled.PhotoSizeSelectLarge
    PosterCardSize.LARGE -> Icons.Filled.PhotoSizeSelectActual
}

fun LibraryContinueWatchingCardSize.icon(): ImageVector = when (this) {
    LibraryContinueWatchingCardSize.COMPACT -> Icons.Filled.PhotoSizeSelectSmall
    LibraryContinueWatchingCardSize.STANDARD -> Icons.Filled.PhotoSizeSelectLarge
    LibraryContinueWatchingCardSize.LARGE -> Icons.Filled.PhotoSizeSelectActual
}

/** Списки блока новых серий — те же вкладки библиотеки, поэтому значок и цвет общие с ними. */
private fun NewEpisodesSource.tabKind(): LibraryTabKind = when (this) {
    NewEpisodesSource.WATCHING -> LibraryTabKind.WATCHING
    NewEpisodesSource.PLANNED -> LibraryTabKind.PLANNED
    NewEpisodesSource.COMPLETED -> LibraryTabKind.COMPLETED
    NewEpisodesSource.POSTPONED -> LibraryTabKind.POSTPONED
    NewEpisodesSource.DROPPED -> LibraryTabKind.DROPPED
    NewEpisodesSource.FAVORITES -> LibraryTabKind.FAVORITES
}

fun NewEpisodesSource.icon(): ImageVector = tabKind().icon()

@Composable
fun NewEpisodesSource.color(): Color = tabKind().color()
