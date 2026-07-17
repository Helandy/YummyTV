package su.afk.yummy.tv.data.details.mapper

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeGenre
import su.afk.yummy.tv.core.model.anime.AnimePerson
import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.core.model.anime.AnimeStudio
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.details.dto.YaniAgeRatingDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniAnimePosterDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeRatingDto
import su.afk.yummy.tv.data.details.dto.YaniEpisodesDto
import su.afk.yummy.tv.data.details.dto.YaniNamedDto
import su.afk.yummy.tv.data.details.dto.YaniScreenshotDto
import su.afk.yummy.tv.data.details.dto.YaniViewingOrderItemDto

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
        viewingOrder = source.viewingOrder.mapNotNull {
            it.toViewingOrderItem(
                currentAnimeId = source.animeId,
                currentEpisodes = source.episodes
            )
        },
        screenshots = source.randomScreenshots.toAnimeScreenshots(),
        reviewsCount = source.reviewsCount.coerceAtLeast(0),
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
    title.knownText()?.let { AnimeStudio(id = id, title = it, url = url.knownText()) }

private fun YaniEpisodesDto.toAnimeEpisodes(): AnimeEpisodes = AnimeEpisodes(
    count = count?.takeIf { it > 0 },
    aired = aired?.takeIf { it > 0 },
    nextDateEpochSeconds = nextDate?.takeIf { it > 0 },
    prevDateEpochSeconds = prevDate?.takeIf { it > 0 },
)

private fun YaniViewingOrderItemDto.toViewingOrderItem(
    currentAnimeId: Int?,
    currentEpisodes: YaniEpisodesDto?,
): AnimeViewingOrderItem? {
    val id = animeId ?: return null
    val safeTitle = title.takeIf { it.isNotBlank() } ?: return null

    return AnimeViewingOrderItem(
        animeId = id,
        title = safeTitle,
        relation = data?.text.knownText(),
        type = type?.name.knownText() ?: type?.shortname.knownText(),
        // API не отдаёт количество серий для элементов viewing_order — только код типа
        // (TV/OVA/фильм и т.д.). Для текущего тайтла берём реальное число серий из
        // основного ответа; для остальных элементов честно оставляем null.
        episodesCount = if (id == currentAnimeId) {
            currentEpisodes?.count?.takeIf { it > 0 } ?: currentEpisodes?.aired?.takeIf { it > 0 }
        } else {
            null
        },
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

private fun List<YaniScreenshotDto>.toAnimeScreenshots(): List<AnimeScreenshot> {
    val seenImageUrls = mutableSetOf<String>()
    return map { it.toAnimeScreenshot() }
        .filter { screenshot ->
            val imageUrl = screenshot.dedupeImageUrl()
            imageUrl == null || seenImageUrls.add(imageUrl)
        }
}

private fun AnimeScreenshot.dedupeImageUrl(): String? =
    full?.takeIf { it.isNotBlank() }
        ?: small?.takeIf { it.isNotBlank() }

private fun String?.knownText(): String? {
    val value = this?.trim().orEmpty()
    return value.takeIf {
        it.isNotBlank() &&
                !it.equals("unknown", ignoreCase = true) &&
                !it.equals("unknow", ignoreCase = true)
    }
}
