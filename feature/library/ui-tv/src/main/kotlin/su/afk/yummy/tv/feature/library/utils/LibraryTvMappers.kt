package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab

internal fun LibraryState.State.signedInFavoriteItems(): List<LibraryEntry> {
    val remoteFavorites = remoteItems[LibraryTab.FAVORITES].orEmpty()
    val remoteIds = remoteFavorites.map { it.animeId }.toSet()
    val localOnlyFavorites = items.filter { it.isFavorite && it.animeId !in remoteIds }
    val remoteEntries = remoteFavorites.map {
        LibraryEntry(
            animeId = it.animeId,
            title = it.title,
            posterSmallUrl = it.poster?.small,
            posterMediumUrl = it.poster?.medium,
            posterBigUrl = it.poster?.big,
            posterFullsizeUrl = it.poster?.fullsize,
            posterMegaUrl = it.poster?.mega,
            isFavorite = true,
        )
    }
    return localOnlyFavorites + remoteEntries
}

internal fun UserAnimeListItem.toLibraryEntry(): LibraryEntry =
    LibraryEntry(
        animeId = animeId,
        title = title,
        posterSmallUrl = poster?.small,
        posterMediumUrl = poster?.medium,
        posterBigUrl = poster?.big,
        posterFullsizeUrl = poster?.fullsize,
        posterMegaUrl = poster?.mega,
        listId = list?.id ?: 0,
        isFavorite = isFavorite,
    )

internal fun LibraryTab.userAnimeListId(): Int? = when (this) {
    LibraryTab.CONTINUE_WATCHING -> null
    LibraryTab.FAVORITES -> null
    LibraryTab.WATCHING -> UserAnimeList.WATCHING.id
    LibraryTab.PLANNED -> UserAnimeList.PLANNED.id
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED.id
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED.id
    LibraryTab.DROPPED -> UserAnimeList.DROPPED.id
}
