package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.library.LibraryTab

internal data class LocalLibrarySyncResult(
    val syncedAny: Boolean,
    val error: Throwable?,
)

internal fun LibraryEntry.userAnimeList(): UserAnimeList? =
    UserAnimeList.entries.firstOrNull { it.id == listId }

internal fun LibraryTab.userAnimeList(): UserAnimeList? = when (this) {
    LibraryTab.WATCHING -> UserAnimeList.WATCHING
    LibraryTab.PLANNED -> UserAnimeList.PLANNED
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED
    LibraryTab.DROPPED -> UserAnimeList.DROPPED
    LibraryTab.CONTINUE_WATCHING,
    LibraryTab.FAVORITES -> null
}
