package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab

internal fun LibraryState.State.tvTabItemCount(tab: LibraryTab): Int = when (tab) {
    LibraryTab.CONTINUE_WATCHING -> continueWatching.size
    LibraryTab.FAVORITES -> items.count { it.isFavorite }

    LibraryTab.WATCHING,
    LibraryTab.PLANNED,
    LibraryTab.COMPLETED,
    LibraryTab.POSTPONED,
    LibraryTab.DROPPED -> {
        val localListId = tab.userAnimeListId()
        items.count { it.listId == localListId }
    }
}

internal fun LibraryTab.userAnimeListId(): Int? = when (this) {
    LibraryTab.CONTINUE_WATCHING -> null
    LibraryTab.FAVORITES -> null
    LibraryTab.WATCHING -> UserAnimeList.WATCHING.id
    LibraryTab.PLANNED -> UserAnimeList.PLANNED.id
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED.id
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED.id
    LibraryTab.DROPPED -> UserAnimeList.DROPPED.id
}
