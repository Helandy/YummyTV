package su.afk.yummy.tv.domain.library.utils

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster

internal fun UserAnimeListItem.updatedAtMillis(fallback: Long): Long =
    updatedAtSeconds?.takeIf { it > 0L }?.let { it * 1_000L } ?: fallback

/** Списки аккаунта, участвующие в синхронизации библиотеки. */
internal val SYNCED_LISTS = listOf(
    UserAnimeList.WATCHING,
    UserAnimeList.PLANNED,
    UserAnimeList.COMPLETED,
    UserAnimeList.POSTPONED,
    UserAnimeList.DROPPED,
)

internal fun UserAnimeListItem.toLibraryItem(
    current: LibraryItem?,
    now: Long,
    listId: Int,
    isFavorite: Boolean,
    listUpdatedAt: Long,
    favoriteUpdatedAt: Long,
): LibraryItem = LibraryItem(
    animeId = animeId,
    title = title.ifBlank { current?.title.orEmpty() },
    poster = LibraryPoster(
        small = poster?.small ?: current?.poster?.small,
        medium = poster?.medium ?: posterUrl ?: current?.poster?.medium,
        big = poster?.big ?: current?.poster?.big,
        fullsize = poster?.fullsize ?: current?.poster?.fullsize,
        mega = poster?.mega ?: current?.poster?.mega,
    ),
    addedAt = current?.addedAt ?: now,
    listId = listId,
    isFavorite = isFavorite,
    listUpdatedAt = listUpdatedAt,
    favoriteUpdatedAt = if (isFavorite) {
        favoriteUpdatedAt
    } else {
        current?.favoriteUpdatedAt ?: 0L
    },
    userRating = userRating ?: current?.userRating,
    year = year ?: current?.year,
    rating = rating ?: current?.rating,
    nextEpisodeAtSeconds = nextEpisodeAtSeconds ?: current?.nextEpisodeAtSeconds,
    season = season ?: current?.season,
)
