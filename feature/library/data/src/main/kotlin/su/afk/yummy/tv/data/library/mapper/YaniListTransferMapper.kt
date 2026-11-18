package su.afk.yummy.tv.data.library.mapper

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus
import su.afk.yummy.tv.core.model.anime.AnimeSeason
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.utils.formatting.toShortAnimeTypeName
import su.afk.yummy.tv.core.utils.poster.animePosterOrNull
import su.afk.yummy.tv.data.library.dto.YaniTransferLookupAnimeDto
import su.afk.yummy.tv.data.library.dto.YaniTransferUserAnimeDto
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExportableLibraryAnime
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ResolvedExternalAnime
import su.afk.yummy.tv.domain.library.utils.normalizedExternalScore

internal fun YaniTransferUserAnimeDto.toExportableLibraryAnime(): ExportableLibraryAnime? {
    val id = animeId?.takeIf { it > 0 } ?: return null
    val listId = user?.list?.list?.id
    return ExportableLibraryAnime(
        animeId = id,
        title = title,
        list = listId.toUserAnimeListOrNull(),
        isFavorite = user?.list?.isFav == true,
        userRating = user?.rating.normalizedExternalScore(),
        typeAlias = type?.alias,
        shikimoriId = remoteIds?.shikimoriId?.takeIf { it > 0 },
        myAnimeListId = remoteIds?.myAnimeListId?.takeIf { it > 0 },
    )
}

internal fun LibraryEntry.toExportableLibraryAnime(
    lookup: YaniTransferLookupAnimeDto?,
): ExportableLibraryAnime =
    ExportableLibraryAnime(
        animeId = animeId,
        title = title.ifBlank { lookup?.title.orEmpty() },
        list = listId.toUserAnimeListOrNull(),
        isFavorite = isFavorite,
        userRating = userRating.normalizedExternalScore(),
        typeAlias = lookup?.type?.alias,
        shikimoriId = lookup?.remoteIds?.shikimoriId?.takeIf { it > 0 },
        myAnimeListId = lookup?.remoteIds?.myAnimeListId?.takeIf { it > 0 },
    )

internal fun YaniTransferLookupAnimeDto.externalIdFor(format: ExternalListFormat): Int? {
    val shiki = remoteIds?.shikimoriId?.takeIf { it > 0 }
    val mal = remoteIds?.myAnimeListId?.takeIf { it > 0 }
    return when (format) {
        ExternalListFormat.SHIKIMORI_JSON -> shiki ?: mal
        ExternalListFormat.MAL_XML -> mal ?: shiki
    }
}

internal fun YaniTransferLookupAnimeDto.toResolvedExternalAnime(
    externalId: Int,
): ResolvedExternalAnime? {
    val id = animeId?.takeIf { it > 0 } ?: return null
    return ResolvedExternalAnime(
        externalId = externalId,
        animeId = id,
        title = title,
        poster = poster?.let {
            animePosterOrNull(
                small = it.small,
                medium = it.medium,
                big = it.big,
                fullsize = it.fullsize,
                mega = it.mega,
            )
        },
        rating = rating?.average?.takeIf { it > 0.0 },
        year = year?.takeIf { it > 0 },
        season = season.toTransferAnimeSeason(),
        kind = (type?.shortName?.takeIf { it.isNotBlank() } ?: type?.name).toShortAnimeTypeName(),
        releaseStatus = AnimeReleaseStatus.fromSlug(animeStatus?.alias),
    )
}

internal fun JsonElement?.toTransferAnimeSeason(): AnimeSeason? {
    val primitive = this as? JsonPrimitive ?: return null
    return if (primitive.isString) {
        AnimeSeason.fromSlug(primitive.content)
    } else {
        AnimeSeason.fromNumber(primitive.intOrNull)
    }
}

private fun Int?.toUserAnimeListOrNull(): UserAnimeList? {
    val value = this ?: return null
    return UserAnimeList.entries.firstOrNull { it.id == value }
}
