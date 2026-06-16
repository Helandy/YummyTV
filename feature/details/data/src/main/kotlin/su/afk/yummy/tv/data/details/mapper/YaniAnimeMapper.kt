package su.afk.yummy.tv.data.details.mapper

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import su.afk.yummy.tv.data.details.dto.YaniAgeRatingDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniAnimePosterDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeRatingDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeVideoDto
import su.afk.yummy.tv.data.details.dto.YaniEpisodesDto
import su.afk.yummy.tv.data.details.dto.YaniNamedDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationItemDto
import su.afk.yummy.tv.data.details.dto.YaniScreenshotDto
import su.afk.yummy.tv.data.details.dto.YaniVideoSkipsDto
import su.afk.yummy.tv.data.details.dto.YaniViewingOrderItemDto
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeEpisodes
import su.afk.yummy.tv.domain.anime.model.AnimeGenre
import su.afk.yummy.tv.domain.anime.model.AnimePerson
import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.domain.anime.model.AnimeRating
import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot
import su.afk.yummy.tv.domain.anime.model.AnimeStudio
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.model.AnimeViewingOrderItem

internal fun YaniAnimeDetailsDto.toAnimeDetails(): AnimeDetails {
    val source = response
    return AnimeDetails(
        id = source.animeId ?: 0,
        animeUrl = source.animeUrl,
        title = source.title,
        description = source.description,
        poster = source.poster?.toAnimePoster(),
        rating = source.rating.toAnimeRating(),
        genres = source.genres.mapNotNull { it.toGenre() },
        year = source.year?.takeIf { it > 0 },
        ageRating = source.minAge.toShortAgeRating(),
        views = source.views,
        status = source.animeStatus?.title.knownText(),
        type = source.type?.name.knownText() ?: source.type?.shortname.knownText(),
        episodes = source.episodes?.toAnimeEpisodes(),
        otherTitles = source.otherTitles.filter { it.isNotBlank() },
        creators = source.creators.mapNotNull { it.toPerson() },
        studios = source.studios.mapNotNull { it.toStudio() },
        viewingOrder = source.viewingOrder.mapNotNull { it.toViewingOrderItem() },
        screenshots = source.randomScreenshots.map { it.toAnimeScreenshot() },
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

private fun YaniAgeRatingDto?.toShortAgeRating(): String? {
    this ?: return null
    value.toShortAgeRatingLabel()?.let { return it }
    return title.knownText()?.toShortAgeRatingTitle()
        ?: titleLong.knownText()?.toShortAgeRatingTitle()
}

private fun Int?.toShortAgeRatingLabel(): String? = when (this) {
    1 -> "G"
    2 -> "PG"
    3 -> "PG-13"
    4 -> "R-17"
    5 -> "R+"
    else -> null
}

private fun String.toShortAgeRatingTitle(): String {
    return when (val code = substringBefore("(").trim()) {
        "G", "PG", "PG-13", "R+" -> code
        "R-17", "R-17+" -> "R-17"
        else -> this
    }
}

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
    playerId = data.playerId,
    iframeUrl = iframeUrl.toHttpsUrl(),
    durationSeconds = duration,
    views = views,
    watchedEndTimeSeconds = watched?.endTime,
    watchedDateSeconds = watched?.date,
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
