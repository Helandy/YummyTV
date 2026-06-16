package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab

internal val libraryMobileTabs: List<LibraryTab>
    get() = LibraryTab.entries

internal fun LibraryTab.userAnimeList(): UserAnimeList? = when (this) {
    LibraryTab.WATCHING -> UserAnimeList.WATCHING
    LibraryTab.PLANNED -> UserAnimeList.PLANNED
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED
    LibraryTab.DROPPED -> UserAnimeList.DROPPED
    LibraryTab.CONTINUE_WATCHING,
    LibraryTab.FAVORITES -> null
}

internal fun LibraryTab.toLibraryMobilePage(): Int =
    libraryMobileTabs.indexOf(this).coerceAtLeast(0)

internal fun Int.toLibraryMobileTab(): LibraryTab =
    libraryMobileTabs.getOrElse(this) { LibraryTab.CONTINUE_WATCHING }

internal fun LibraryState.State.shouldShowRemoteLoader(tab: LibraryTab): Boolean {
    if (!isSignedIn || !isRemoteLoading || remoteError != null) return false
    return when (tab) {
        LibraryTab.CONTINUE_WATCHING -> false
        LibraryTab.FAVORITES -> mobileTabItemCount(tab) == 0
        LibraryTab.WATCHING,
        LibraryTab.PLANNED,
        LibraryTab.COMPLETED,
        LibraryTab.POSTPONED,
        LibraryTab.DROPPED -> mobileTabItemCount(tab) == 0
    }
}

internal fun LibraryState.State.mobileTabItemCount(tab: LibraryTab): Int = when (tab) {
    LibraryTab.CONTINUE_WATCHING -> continueWatching.size
    LibraryTab.FAVORITES -> items.count { it.isFavorite }
    LibraryTab.WATCHING,
    LibraryTab.PLANNED,
    LibraryTab.COMPLETED,
    LibraryTab.POSTPONED,
    LibraryTab.DROPPED -> {
        val localList = tab.userAnimeList()
        items.count { it.listId == localList?.id }
    }
}

internal fun LibraryEntry.posterUrl(): String? =
    posterMegaUrl ?: posterFullsizeUrl ?: posterBigUrl ?: posterMediumUrl ?: posterSmallUrl
