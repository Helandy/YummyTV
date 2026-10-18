package su.afk.yummy.tv.data.library.storage.mapper

import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus
import su.afk.yummy.tv.core.model.anime.AnimeSeason
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.core.storage.library.LibraryPoster as StorageLibraryPoster

internal fun LibraryEntry.toLibraryItem(): LibraryItem =
    LibraryItem(
        animeId = animeId,
        title = title,
        poster = LibraryPoster(
            small = posterSmallUrl,
            medium = posterMediumUrl,
            big = posterBigUrl,
            fullsize = posterFullsizeUrl,
            mega = posterMegaUrl,
        ),
        addedAt = addedAt,
        listId = listId,
        isFavorite = isFavorite,
        listUpdatedAt = listUpdatedAt,
        favoriteUpdatedAt = favoriteUpdatedAt,
        userRating = userRating,
        year = year,
        rating = rating,
        nextEpisodeAtSeconds = nextEpisodeAtSeconds,
        season = AnimeSeason.fromSlug(season),
        kind = kind,
        releaseStatus = AnimeReleaseStatus.fromSlug(releaseStatus),
    )

internal fun LibraryItem.toLibraryEntry(): LibraryEntry =
    LibraryEntry(
        animeId = animeId,
        title = title,
        posterSmallUrl = poster?.small,
        posterMediumUrl = poster?.medium,
        posterBigUrl = poster?.big,
        posterFullsizeUrl = poster?.fullsize,
        posterMegaUrl = poster?.mega,
        addedAt = addedAt,
        listId = listId,
        isFavorite = isFavorite,
        listUpdatedAt = listUpdatedAt,
        favoriteUpdatedAt = favoriteUpdatedAt,
        userRating = userRating,
        year = year,
        rating = rating,
        nextEpisodeAtSeconds = nextEpisodeAtSeconds,
        season = season?.slug,
        kind = kind,
        releaseStatus = releaseStatus?.slug,
    )

internal fun LibraryPoster?.toStoragePoster(): StorageLibraryPoster? =
    this?.let {
        StorageLibraryPoster(
            small = small,
            medium = medium,
            big = big,
            fullsize = fullsize,
            mega = mega,
        )
    }
