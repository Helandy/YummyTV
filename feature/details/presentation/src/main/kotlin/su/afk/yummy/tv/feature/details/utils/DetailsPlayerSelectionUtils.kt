package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.player.isAksorPlayerUrl
import su.afk.yummy.tv.feature.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.isCvhPlayerUrl
import su.afk.yummy.tv.feature.player.isKodikPlayerUrl
import su.afk.yummy.tv.feature.player.isRutubePlayerUrl
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.isVkPlayerUrl

internal fun String.matchesPreferredPlayer(preferred: PreferredPlayer): Boolean =
    when (preferred) {
        PreferredPlayer.NONE -> false
        PreferredPlayer.KODIK -> isKodikPlayerUrl()
        PreferredPlayer.AKSOR -> isAksorPlayerUrl()
        PreferredPlayer.ALLOHA -> isAllohaPlayerUrl()
        PreferredPlayer.CVH -> isCvhPlayerUrl()
        PreferredPlayer.VK -> isVkPlayerUrl()
        PreferredPlayer.RUTUBE -> isRutubePlayerUrl()
    }

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
    poster = poster?.toLibraryPoster(),
    listId = list.id,
    isFavorite = isFavorite,
)
