package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList

internal data class LocalLibrarySyncResult(
    val syncedAny: Boolean,
    val error: String?,
)

internal fun LibraryEntry.userAnimeList(): UserAnimeList? =
    UserAnimeList.entries.firstOrNull { it.id == listId }
