package su.afk.yummy.tv.data.details.storage.mapper

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeGenre
import su.afk.yummy.tv.core.model.anime.AnimePerson
import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.core.model.anime.AnimeStudio
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.storage.account.AccountUserRatingEntry
import su.afk.yummy.tv.core.storage.anime.ANIME_DETAIL_NAMED_KIND_CREATOR
import su.afk.yummy.tv.core.storage.anime.ANIME_DETAIL_NAMED_KIND_GENRE
import su.afk.yummy.tv.core.storage.anime.ANIME_DETAIL_NAMED_KIND_STUDIO
import su.afk.yummy.tv.core.storage.anime.AnimeDetailNamedEntry
import su.afk.yummy.tv.core.storage.anime.AnimeDetailTitleEntry
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsCache
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsEntry
import su.afk.yummy.tv.core.storage.anime.AnimeRecommendationCacheEntry
import su.afk.yummy.tv.core.storage.anime.AnimeRecommendationEntry
import su.afk.yummy.tv.core.storage.anime.AnimeRecommendationsCache
import su.afk.yummy.tv.core.storage.anime.AnimeScreenshotEntry
import su.afk.yummy.tv.core.storage.anime.AnimeTrailerCacheEntry
import su.afk.yummy.tv.core.storage.anime.AnimeTrailerEntry
import su.afk.yummy.tv.core.storage.anime.AnimeTrailersCache
import su.afk.yummy.tv.core.storage.anime.AnimeVideoCacheEntry
import su.afk.yummy.tv.core.storage.anime.AnimeVideoEntry
import su.afk.yummy.tv.core.storage.anime.AnimeVideosCache
import su.afk.yummy.tv.core.storage.anime.AnimeViewingOrderEntry
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.details.dto.YaniAgeRatingDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeVideoDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationItemDto
import su.afk.yummy.tv.data.details.dto.YaniScreenshotDto
import su.afk.yummy.tv.data.details.dto.YaniTrailerDto

internal fun YaniAnimeDetailsDto.toAnimeDetailsCache(
    language: String,
    cachedAt: Long,
): AnimeDetailsCache? {
    val source = response
    val animeId = source.animeId ?: return null
    return AnimeDetailsCache(
        entry = AnimeDetailsEntry(
            animeId = animeId,
            language = language,
            animeUrl = source.animeUrl,
            title = source.title,
            description = source.description,
            posterSmallUrl = source.poster?.small?.toHttpsUrl(),
            posterMediumUrl = source.poster?.medium?.toHttpsUrl(),
            posterBigUrl = source.poster?.big?.toHttpsUrl(),
            posterFullsizeUrl = source.poster?.fullsize?.toHttpsUrl(),
            posterMegaUrl = source.poster?.mega?.toHttpsUrl(),
            ratingAverage = source.rating.average?.takeIf { it > 0.0 },
            ratingCounters = source.rating.counters,
            ratingKinopoisk = source.rating.kinopoisk?.takeIf { it > 0.0 },
            ratingShikimori = source.rating.shikimori?.takeIf { it > 0.0 },
            ratingMyAnimeList = source.rating.myAnimeList?.takeIf { it > 0.0 },
            year = source.year?.takeIf { it > 0 },
            ageRating = source.minAge.toShortAgeRating(),
            views = source.views,
            status = source.animeStatus?.title.knownText(),
            type = source.type?.name.knownText() ?: source.type?.shortname.knownText(),
            episodesCount = source.episodes?.count?.takeIf { it > 0 },
            episodesAired = source.episodes?.aired?.takeIf { it > 0 },
            episodesNextDateEpochSeconds = source.episodes?.nextDate?.takeIf { it > 0 },
            episodesPrevDateEpochSeconds = source.episodes?.prevDate?.takeIf { it > 0 },
            cachedAt = cachedAt,
        ),
        otherTitles = source.otherTitles.filter { it.isNotBlank() }.mapIndexed { index, title ->
            AnimeDetailTitleEntry(
                animeId = animeId,
                language = language,
                position = index,
                title = title,
            )
        },
        genres = source.genres.mapNotNull { item ->
            item.title.knownText()?.let { item to it }
        }.mapIndexed { index, (genre, title) ->
            AnimeDetailNamedEntry(
                animeId = animeId,
                language = language,
                kind = ANIME_DETAIL_NAMED_KIND_GENRE,
                position = index,
                itemId = genre.id,
                title = title,
            )
        },
        creators = source.creators.mapNotNull { item ->
            item.title.knownText()?.let { item to it }
        }.mapIndexed { index, (creator, title) ->
            AnimeDetailNamedEntry(
                animeId = animeId,
                language = language,
                kind = ANIME_DETAIL_NAMED_KIND_CREATOR,
                position = index,
                itemId = creator.id,
                title = title,
            )
        },
        studios = source.studios.mapNotNull { item ->
            item.title.knownText()?.let { item to it }
        }.mapIndexed { index, (studio, title) ->
            AnimeDetailNamedEntry(
                animeId = animeId,
                language = language,
                kind = ANIME_DETAIL_NAMED_KIND_STUDIO,
                position = index,
                itemId = studio.id,
                title = title,
            )
        },
        viewingOrder = source.viewingOrder.mapNotNull { item ->
            val relatedId = item.animeId ?: return@mapNotNull null
            val title = item.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Triple(relatedId, title, item)
        }.mapIndexed { index, (relatedId, title, item) ->
            AnimeViewingOrderEntry(
                animeId = animeId,
                language = language,
                position = index,
                relatedAnimeId = relatedId,
                title = title,
                relation = item.data?.text.knownText(),
                type = item.type?.name.knownText() ?: item.type?.shortname.knownText(),
                episodesCount = item.type?.value?.takeIf { it > 0 },
                posterSmallUrl = item.poster?.small?.toHttpsUrl(),
                posterMediumUrl = item.poster?.medium?.toHttpsUrl(),
                posterBigUrl = item.poster?.big?.toHttpsUrl(),
                posterFullsizeUrl = item.poster?.fullsize?.toHttpsUrl(),
                posterMegaUrl = item.poster?.mega?.toHttpsUrl(),
                year = item.year?.takeIf { it > 0 },
                rating = item.rating?.takeIf { it > 0.0 },
            )
        },
        screenshots = source.randomScreenshots.map { screenshot ->
            Triple(
                screenshot,
                screenshot.sizes.small?.toHttpsUrl(),
                screenshot.sizes.full?.toHttpsUrl(),
            )
        }.filterDistinctScreenshots().mapIndexed { index, (screenshot, small, full) ->
            AnimeScreenshotEntry(
                animeId = animeId,
                language = language,
                position = index,
                screenshotId = screenshot.id,
                episode = screenshot.episode,
                smallUrl = small,
                fullUrl = full,
            )
        },
    )
}

internal fun Int?.toAccountUserRatingEntry(
    userId: Int,
    animeId: Int,
    cachedAt: Long,
): AccountUserRatingEntry =
    AccountUserRatingEntry(
        userId = userId,
        animeId = animeId,
        rating = this,
        cachedAt = cachedAt,
    )

internal fun AnimeDetailsCache.toAnimeDetails(): AnimeDetails {
    val source = entry
    return AnimeDetails(
        id = source.animeId,
        animeUrl = source.animeUrl,
        title = source.title,
        description = source.description,
        poster = source.toPosterOrNull(),
        rating = AnimeRating(
            average = source.ratingAverage,
            counters = source.ratingCounters,
            kinopoisk = source.ratingKinopoisk,
            shikimori = source.ratingShikimori,
            myAnimeList = source.ratingMyAnimeList,
        ),
        genres = genres.map { AnimeGenre(id = it.itemId, title = it.title) },
        year = source.year,
        ageRating = source.ageRating,
        views = source.views,
        status = source.status,
        type = source.type,
        episodes = source.toEpisodesOrNull(),
        otherTitles = otherTitles.map { it.title },
        creators = creators.map { AnimePerson(id = it.itemId, title = it.title) },
        studios = studios.map { AnimeStudio(id = it.itemId, title = it.title) },
        viewingOrder = viewingOrder.map { it.toAnimeViewingOrderItem() },
        screenshots = screenshots.map {
            AnimeScreenshot(
                id = it.screenshotId,
                episode = it.episode,
                small = it.smallUrl,
                full = it.fullUrl,
            )
        },
    )
}

internal fun List<YaniAnimeVideoDto>.toAnimeVideosCache(
    animeId: Int,
    language: String,
    cachedAt: Long,
): AnimeVideosCache =
    AnimeVideosCache(
        entry = AnimeVideoCacheEntry(
            animeId = animeId,
            language = language,
            cachedAt = cachedAt,
        ),
        videos = mapIndexed { index, video ->
            val opening = video.skips?.opening.toSkipSegment()
            val ending = video.skips?.ending.toSkipSegment()
            AnimeVideoEntry(
                animeId = animeId,
                language = language,
                position = index,
                videoId = video.videoId,
                episode = video.number,
                dubbing = video.data.dubbing,
                player = video.data.player,
                playerId = video.data.playerId,
                iframeUrl = video.iframeUrl.toHttpsUrl(),
                durationSeconds = video.duration,
                views = video.views,
                watchedEndTimeSeconds = video.watched?.endTime,
                watchedDateSeconds = video.watched?.date,
                openingStartMs = opening?.first,
                openingEndMs = opening?.second,
                endingStartMs = ending?.first,
                endingEndMs = ending?.second,
            )
        },
    )

internal fun AnimeVideosCache.toAnimeVideos(): List<AnimeVideo> =
    videos.map { it.toAnimeVideo() }

internal fun List<YaniRecommendationItemDto>.toAnimeRecommendationsCache(
    animeId: Int,
    language: String,
    fromAi: Boolean,
    cachedAt: Long,
): AnimeRecommendationsCache =
    AnimeRecommendationsCache(
        entry = AnimeRecommendationCacheEntry(
            animeId = animeId,
            language = language,
            fromAi = fromAi,
            cachedAt = cachedAt,
        ),
        recommendations = mapNotNull { item ->
            val id = item.animeId ?: return@mapNotNull null
            val title = item.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Triple(id, title, item)
        }.mapIndexed { index, (id, title, recommendation) ->
            AnimeRecommendationEntry(
                animeId = animeId,
                language = language,
                fromAi = fromAi,
                position = index,
                recommendationAnimeId = id,
                title = title,
                posterSmallUrl = recommendation.poster?.small?.toHttpsUrl(),
                posterMediumUrl = recommendation.poster?.medium?.toHttpsUrl(),
                posterBigUrl = recommendation.poster?.big?.toHttpsUrl(),
                posterFullsizeUrl = recommendation.poster?.fullsize?.toHttpsUrl(),
                posterMegaUrl = recommendation.poster?.mega?.toHttpsUrl(),
                rating = recommendation.rating.average?.takeIf { it > 0.0 },
                type = recommendation.type?.name.knownText()
                    ?: recommendation.type?.shortname.knownText(),
                year = recommendation.year?.takeIf { it > 0 },
            )
        },
    )

internal fun AnimeRecommendationsCache.toAnimeRecommendations(): List<AnimeRecommendation> =
    recommendations
        .sortedBy { it.position }
        .map {
            AnimeRecommendation(
                animeId = it.recommendationAnimeId,
                title = it.title,
                poster = posterOrNull(
                    small = it.posterSmallUrl,
                    medium = it.posterMediumUrl,
                    big = it.posterBigUrl,
                    fullsize = it.posterFullsizeUrl,
                    mega = it.posterMegaUrl,
                ),
                rating = it.rating,
                type = it.type,
                year = it.year,
            )
        }

internal fun List<YaniTrailerDto>.toAnimeTrailersCache(
    animeId: Int,
    language: String,
    cachedAt: Long,
): AnimeTrailersCache =
    AnimeTrailersCache(
        entry = AnimeTrailerCacheEntry(
            animeId = animeId,
            language = language,
            cachedAt = cachedAt,
        ),
        trailers = map { it.iframeUrl.toHttpsUrl() }.distinct().mapIndexed { index, iframeUrl ->
            AnimeTrailerEntry(
                animeId = animeId,
                language = language,
                position = index,
                iframeUrl = iframeUrl,
            )
        },
    )

internal fun AnimeTrailersCache.toAnimeTrailers(): List<AnimeTrailer> =
    trailers
        .sortedBy { it.position }
        .map { AnimeTrailer(iframeUrl = it.iframeUrl) }
        .distinctBy { it.iframeUrl }

private fun List<Triple<YaniScreenshotDto, String?, String?>>.filterDistinctScreenshots():
        List<Triple<YaniScreenshotDto, String?, String?>> {
    val seenImageUrls = mutableSetOf<String>()
    return filter { (_, small, full) ->
        val imageUrl = full?.takeIf { it.isNotBlank() } ?: small?.takeIf { it.isNotBlank() }
        imageUrl == null || seenImageUrls.add(imageUrl)
    }
}

private fun JsonElement?.toSkipSegment(): Pair<Long, Long>? {
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
    return start * 1_000L to end * 1_000L
}

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

private fun String.toShortAgeRatingTitle(): String = when (val code = substringBefore("(").trim()) {
    "G", "PG", "PG-13", "R+" -> code
    "R-17", "R-17+" -> "R-17"
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

private fun AnimeDetailsEntry.toPosterOrNull(): AnimePoster? =
    posterOrNull(
        small = posterSmallUrl,
        medium = posterMediumUrl,
        big = posterBigUrl,
        fullsize = posterFullsizeUrl,
        mega = posterMegaUrl,
    )

private fun AnimeDetailsEntry.toEpisodesOrNull(): AnimeEpisodes? {
    if (
        episodesCount == null &&
        episodesAired == null &&
        episodesNextDateEpochSeconds == null &&
        episodesPrevDateEpochSeconds == null
    ) {
        return null
    }
    return AnimeEpisodes(
        count = episodesCount,
        aired = episodesAired,
        nextDateEpochSeconds = episodesNextDateEpochSeconds,
        prevDateEpochSeconds = episodesPrevDateEpochSeconds,
    )
}

private fun AnimeViewingOrderEntry.toAnimeViewingOrderItem(): AnimeViewingOrderItem =
    AnimeViewingOrderItem(
        animeId = relatedAnimeId,
        title = title,
        relation = relation,
        type = type,
        episodesCount = episodesCount,
        poster = posterOrNull(
            small = posterSmallUrl,
            medium = posterMediumUrl,
            big = posterBigUrl,
            fullsize = posterFullsizeUrl,
            mega = posterMegaUrl,
        ),
        year = year,
        rating = rating,
    )

private fun AnimeVideoEntry.toAnimeVideo(): AnimeVideo =
    AnimeVideo(
        id = videoId,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = playerId,
        iframeUrl = iframeUrl,
        durationSeconds = durationSeconds,
        views = views,
        watchedEndTimeSeconds = watchedEndTimeSeconds,
        watchedDateSeconds = watchedDateSeconds,
        skips = AnimeVideoSkips(
            opening = skipSegment(openingStartMs, openingEndMs),
            ending = skipSegment(endingStartMs, endingEndMs),
        ),
    )

private fun posterOrNull(
    small: String?,
    medium: String?,
    big: String?,
    fullsize: String?,
    mega: String?,
): AnimePoster? {
    if (small == null && medium == null && big == null && fullsize == null && mega == null) {
        return null
    }
    return AnimePoster(
        small = small,
        medium = medium,
        big = big,
        fullsize = fullsize,
        mega = mega,
    )
}

private fun skipSegment(startMs: Long?, endMs: Long?): AnimeVideoSkipSegment? {
    if (startMs == null || endMs == null || endMs <= startMs) return null
    return AnimeVideoSkipSegment(startMs = startMs, endMs = endMs)
}
