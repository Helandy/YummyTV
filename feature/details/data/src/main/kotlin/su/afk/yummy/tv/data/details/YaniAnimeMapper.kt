package su.afk.yummy.tv.data.details

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import su.afk.yummy.tv.domain.anime.AnimeDetails
import su.afk.yummy.tv.domain.anime.AnimeEpisodes
import su.afk.yummy.tv.domain.anime.AnimeGenre
import su.afk.yummy.tv.domain.anime.AnimePerson
import su.afk.yummy.tv.domain.anime.AnimePoster
import su.afk.yummy.tv.domain.anime.AnimeRating
import su.afk.yummy.tv.domain.anime.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.AnimeScreenshot
import su.afk.yummy.tv.domain.anime.AnimeStudio
import su.afk.yummy.tv.domain.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.AnimeViewingOrderItem

internal fun YaniAnimeDetailsDto.toAnimeDetails(): AnimeDetails {
    val source = response
    return AnimeDetails(
        id = source.animeId ?: 0,
        title = source.title,
        description = source.description,
        poster = source.poster?.toAnimePoster(),
        rating = source.rating.toAnimeRating(),
        genres = source.genres.mapNotNull { it.toGenre() },
        year = source.year?.takeIf { it > 0 },
        ageRating = source.minAge?.titleLong.knownText() ?: source.minAge?.title.knownText(),
        views = source.views,
        status = source.animeStatus?.title.knownText(),
        type = source.type?.name.knownText() ?: source.type?.shortname.knownText(),
        episodes = source.episodes?.toAnimeEpisodes(),
        otherTitles = source.otherTitles.filter { it.isNotBlank() },
        creators = source.creators.mapNotNull { it.toPerson() },
        studios = source.studios.mapNotNull { it.toStudio() },
        viewingOrder = source.viewingOrder.mapNotNull { it.toViewingOrderItem() },
        screenshots = source.randomScreenshots.map { it.toAnimeScreenshot() },
        blockedIn = source.blockedIn.filter { it.isNotBlank() },
    )
}

private fun YaniAnimePosterDto.toAnimePoster(): AnimePoster = AnimePoster(
    small = small?.toHttpsUrl(),
    medium = medium?.toHttpsUrl(),
    big = big?.toHttpsUrl(),
    fullsize = fullsize?.toHttpsUrl(),
    mega = mega?.toHttpsUrl(),
)

private fun YaniAnimeRatingDto.toAnimeRating(): AnimeRating = AnimeRating(
    average = average?.takeIf { it > 0.0 },
    counters = counters,
    kinopoisk = kinopoisk?.takeIf { it > 0.0 },
    shikimori = shikimori?.takeIf { it > 0.0 },
    myAnimeList = myAnimeList?.takeIf { it > 0.0 },
)

private fun YaniNamedDto.toGenre(): AnimeGenre? =
    title.knownText()?.let { AnimeGenre(id = id, title = it) }

private fun YaniNamedDto.toPerson(): AnimePerson? =
    title.knownText()?.let { AnimePerson(id = id, title = it) }

private fun YaniNamedDto.toStudio(): AnimeStudio? =
    title.knownText()?.let { AnimeStudio(id = id, title = it) }

private fun YaniEpisodesDto.toAnimeEpisodes(): AnimeEpisodes = AnimeEpisodes(
    count = count?.takeIf { it > 0 },
    aired = aired?.takeIf { it > 0 },
    nextDateEpochSeconds = nextDate?.takeIf { it > 0 },
    prevDateEpochSeconds = prevDate?.takeIf { it > 0 },
)

private fun YaniViewingOrderItemDto.toViewingOrderItem(): AnimeViewingOrderItem? {
    val id = animeId ?: return null
    val safeTitle = title.takeIf { it.isNotBlank() } ?: return null

    return AnimeViewingOrderItem(
        animeId = id,
        title = safeTitle,
        relation = data?.text.knownText(),
        type = type?.name.knownText() ?: type?.shortname.knownText(),
        episodesCount = type?.value?.takeIf { it > 0 },
        poster = poster?.toAnimePoster(),
        year = year?.takeIf { it > 0 },
        rating = rating?.takeIf { it > 0.0 },
    )
}

private fun YaniScreenshotDto.toAnimeScreenshot(): AnimeScreenshot = AnimeScreenshot(
    id = id,
    episode = episode,
    small = sizes.small?.toHttpsUrl(),
    full = sizes.full?.toHttpsUrl(),
)

internal fun YaniAnimeVideoDto.toAnimeVideo(): AnimeVideo = AnimeVideo(
    id = videoId,
    episode = number,
    dubbing = data.dubbing,
    player = data.player,
    iframeUrl = iframeUrl.toHttpsUrl(),
    durationSeconds = duration,
    views = views,
    skips = skips.toAnimeVideoSkips(),
)

private fun YaniVideoSkipsDto?.toAnimeVideoSkips(): AnimeVideoSkips = AnimeVideoSkips(
    opening = this?.opening.toAnimeVideoSkipSegment(),
    ending = this?.ending.toAnimeVideoSkipSegment(),
)

private fun JsonElement?.toAnimeVideoSkipSegment(): AnimeVideoSkipSegment? {
    val (start, end) = when (this) {
        is JsonObject -> {
            val start = this["time"]?.jsonPrimitive?.intOrNull?.takeIf { it >= 0 } ?: return null
            val length = this["length"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 } ?: return null
            start to start + length
        }
        is JsonArray -> {
            val start = getOrNull(0)?.jsonPrimitive?.intOrNull?.takeIf { it >= 0 } ?: return null
            val end = getOrNull(1)?.jsonPrimitive?.intOrNull?.takeIf { it > start } ?: return null
            start to end
        }
        else -> return null
    }
    return AnimeVideoSkipSegment(
        startMs = start * 1_000L,
        endMs = end * 1_000L,
    )
}

internal fun YaniRecommendationItemDto.toAnimeRecommendation(): AnimeRecommendation? {
    val id = animeId ?: return null
    val safeTitle = title.takeIf { it.isNotBlank() } ?: return null
    return AnimeRecommendation(
        animeId = id,
        title = safeTitle,
        poster = poster?.toAnimePoster(),
        rating = rating.average?.takeIf { it > 0.0 },
        type = type?.name.knownText() ?: type?.shortname.knownText(),
        year = year?.takeIf { it > 0 },
    )
}

internal fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}

private fun String?.knownText(): String? {
    val value = this?.trim().orEmpty()
    return value.takeIf {
        it.isNotBlank() &&
                !it.equals("unknown", ignoreCase = true) &&
                !it.equals("unknow", ignoreCase = true)
    }
}
