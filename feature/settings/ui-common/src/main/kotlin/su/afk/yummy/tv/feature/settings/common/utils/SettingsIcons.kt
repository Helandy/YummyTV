package su.afk.yummy.tv.feature.settings.common.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.components.color
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleEdge
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
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

fun PlayerSubtitleBackground.icon(): ImageVector = when (this) {
    PlayerSubtitleBackground.NONE -> Icons.Filled.FormatColorReset
    PlayerSubtitleBackground.TRANSLUCENT -> Icons.Filled.Opacity
    PlayerSubtitleBackground.SOLID -> Icons.Filled.Square
}

/** Один значок на все цвета: различаются они тоном, который накладывается через `color`. */
@Suppress("UnusedReceiverParameter")
fun PlayerSubtitleTextColor.icon(): ImageVector = Icons.Filled.FormatColorText

fun PlayerSubtitleEdge.icon(): ImageVector = when (this) {
    PlayerSubtitleEdge.NONE -> Icons.Filled.Block
    PlayerSubtitleEdge.OUTLINE -> Icons.Filled.Title
    PlayerSubtitleEdge.SHADOW -> Icons.Filled.Layers
}
