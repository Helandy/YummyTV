package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.library.model.FAVORITE_ONLY_LIBRARY_LIST_ID
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.RemoteLibrarySnapshot

internal fun remoteItem(
    animeId: Int,
    list: UserAnimeList? = null,
    favorite: Boolean = false,
    updatedAtSeconds: Long? = null,
) = UserAnimeListItem(
    animeId = animeId,
    title = "remote $animeId",
    posterUrl = null,
    rating = null,
    year = null,
    list = list,
    isFavorite = favorite,
    updatedAtSeconds = updatedAtSeconds,
)

internal fun localItem(
    animeId: Int,
    list: UserAnimeList? = null,
    favorite: Boolean = false,
    listUpdatedAt: Long = 0L,
    favoriteUpdatedAt: Long = 0L,
) = LibraryItem(
    animeId = animeId,
    title = "local $animeId",
    // Без списка — запись «только избранное»: id 0 занят списком WATCHING.
    listId = list?.id ?: FAVORITE_ONLY_LIBRARY_LIST_ID,
    isFavorite = favorite,
    listUpdatedAt = listUpdatedAt,
    favoriteUpdatedAt = favoriteUpdatedAt,
)

internal fun snapshot(
    lists: Map<UserAnimeList, List<UserAnimeListItem>> = emptyMap(),
    favorites: List<UserAnimeListItem> = emptyList(),
) = RemoteLibrarySnapshot(lists = lists, favorites = favorites)
