package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.core.common.player.isKodikPlayerUrl
import su.afk.yummy.tv.core.common.player.isSupportedPlayerUrl
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem

internal fun List<AnimeVideo>.selectInitialDetailsVideo(): AnimeVideo? {
    val kodikVideos = filter { it.iframeUrl.isKodikPlayerUrl() || it.player.isKodikPlayerUrl() }
    val supportedVideos = filter { it.iframeUrl.isSupportedPlayerUrl() }
    val source = kodikVideos.ifEmpty { supportedVideos.ifEmpty { this } }
    return source.groupBy { it.dubbing }
        .maxByOrNull { (_, list) -> list.sumOf { it.views ?: 0 } }
        ?.value
        ?.minByOrNull { it.episode.toIntOrNull() ?: Int.MAX_VALUE }
        ?: source.firstOrNull()
}

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
