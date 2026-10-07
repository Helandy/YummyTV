package su.afk.yummy.tv.feature.library.mapper

import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.feature.library.model.LibraryTab

internal fun LibraryTabKind.toLibraryTab(): LibraryTab = when (this) {
    LibraryTabKind.HISTORY -> LibraryTab.HISTORY
    LibraryTabKind.CONTINUE_WATCHING -> LibraryTab.CONTINUE_WATCHING
    LibraryTabKind.FAVORITES -> LibraryTab.FAVORITES
    LibraryTabKind.WATCHING -> LibraryTab.WATCHING
    LibraryTabKind.PLANNED -> LibraryTab.PLANNED
    LibraryTabKind.COMPLETED -> LibraryTab.COMPLETED
    LibraryTabKind.POSTPONED -> LibraryTab.POSTPONED
    LibraryTabKind.DROPPED -> LibraryTab.DROPPED
}
