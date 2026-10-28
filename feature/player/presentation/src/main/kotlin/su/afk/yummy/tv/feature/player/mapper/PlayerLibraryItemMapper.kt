package su.afk.yummy.tv.feature.player.mapper

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem

internal fun AnimeDetails.toLibraryItem(
    list: UserAnimeList,
    isFavorite: Boolean,
) = LibraryItem(
    animeId = id,
    title = title,
    poster = poster,
    listId = list.id,
    isFavorite = isFavorite,
    year = year,
    rating = rating.average,
)
