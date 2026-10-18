package su.afk.yummy.tv.feature.library.mapper

import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.feature.library.model.LibraryTab

fun LibraryTabKind.toLibraryTab(): LibraryTab = when (this) {
    LibraryTabKind.HISTORY -> LibraryTab.HISTORY
    LibraryTabKind.CONTINUE_WATCHING -> LibraryTab.CONTINUE_WATCHING
    LibraryTabKind.FAVORITES -> LibraryTab.FAVORITES
    LibraryTabKind.WATCHING -> LibraryTab.WATCHING
    LibraryTabKind.PLANNED -> LibraryTab.PLANNED
    LibraryTabKind.COMPLETED -> LibraryTab.COMPLETED
    LibraryTabKind.POSTPONED -> LibraryTab.POSTPONED
    LibraryTabKind.DROPPED -> LibraryTab.DROPPED
}

fun LibraryTab.toLibraryTabKind(): LibraryTabKind = when (this) {
    LibraryTab.HISTORY -> LibraryTabKind.HISTORY
    LibraryTab.CONTINUE_WATCHING -> LibraryTabKind.CONTINUE_WATCHING
    LibraryTab.FAVORITES -> LibraryTabKind.FAVORITES
    LibraryTab.WATCHING -> LibraryTabKind.WATCHING
    LibraryTab.PLANNED -> LibraryTabKind.PLANNED
    LibraryTab.COMPLETED -> LibraryTabKind.COMPLETED
    LibraryTab.POSTPONED -> LibraryTabKind.POSTPONED
    LibraryTab.DROPPED -> LibraryTabKind.DROPPED
}
