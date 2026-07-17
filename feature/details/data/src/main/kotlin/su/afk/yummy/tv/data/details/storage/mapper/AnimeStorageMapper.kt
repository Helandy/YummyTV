package su.afk.yummy.tv.data.details.storage.mapper

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
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeEpisodes
import su.afk.yummy.tv.domain.anime.model.AnimeGenre
import su.afk.yummy.tv.domain.anime.model.AnimePerson
import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.domain.anime.model.AnimeRating
import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot
import su.afk.yummy.tv.domain.anime.model.AnimeStudio
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.model.AnimeViewingOrderItem

internal fun AnimeDetails.toAnimeDetailsCache(
    language: String,
    cachedAt: Long,
): AnimeDetailsCache {
    val animeId = id
    return AnimeDetailsCache(
        entry = AnimeDetailsEntry(
            animeId = animeId,
            language = language,
            animeUrl = animeUrl,
            title = title,
            description = description,
            posterSmallUrl = poster?.small,
            posterMediumUrl = poster?.medium,
            posterBigUrl = poster?.big,
            posterFullsizeUrl = poster?.fullsize,
            posterMegaUrl = poster?.mega,
            ratingAverage = rating.average,
            ratingCounters = rating.counters,
            ratingKinopoisk = rating.kinopoisk,
            ratingShikimori = rating.shikimori,
            ratingMyAnimeList = rating.myAnimeList,
            year = year,
            ageRating = ageRating,
            views = views,
            status = status,
            type = type,
            episodesCount = episodes?.count,
            episodesAired = episodes?.aired,
            episodesNextDateEpochSeconds = episodes?.nextDateEpochSeconds,
            episodesPrevDateEpochSeconds = episodes?.prevDateEpochSeconds,
            cachedAt = cachedAt,
        ),
        otherTitles = otherTitles.mapIndexed { index, title ->
            AnimeDetailTitleEntry(
                animeId = animeId,
                language = language,
                position = index,
                title = title,
            )
        },
        genres = genres.mapIndexed { index, genre ->
            AnimeDetailNamedEntry(
                animeId = animeId,
                language = language,
                kind = ANIME_DETAIL_NAMED_KIND_GENRE,
                position = index,
                itemId = genre.id,
                title = genre.title,
            )
        },
        creators = creators.mapIndexed { index, creator ->
            AnimeDetailNamedEntry(
                animeId = animeId,
                language = language,
                kind = ANIME_DETAIL_NAMED_KIND_CREATOR,
                position = index,
                itemId = creator.id,
                title = creator.title,
            )
        },
        studios = studios.mapIndexed { index, studio ->
            AnimeDetailNamedEntry(
                animeId = animeId,
                language = language,
                kind = ANIME_DETAIL_NAMED_KIND_STUDIO,
                position = index,
                itemId = studio.id,
                title = studio.title,
            )
        },
        viewingOrder = viewingOrder.mapIndexed { index, item ->
            AnimeViewingOrderEntry(
                animeId = animeId,
                language = language,
                position = index,
                relatedAnimeId = item.animeId,
                title = item.title,
                relation = item.relation,
                type = item.type,
                episodesCount = item.episodesCount,
                posterSmallUrl = item.poster?.small,
                posterMediumUrl = item.poster?.medium,
                posterBigUrl = item.poster?.big,
                posterFullsizeUrl = item.poster?.fullsize,
                posterMegaUrl = item.poster?.mega,
                year = item.year,
                rating = item.rating,
            )
        },
        screenshots = screenshots.mapIndexed { index, screenshot ->
            AnimeScreenshotEntry(
                animeId = animeId,
                language = language,
                position = index,
                screenshotId = screenshot.id,
                episode = screenshot.episode,
                smallUrl = screenshot.small,
                fullUrl = screenshot.full,
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

internal fun List<AnimeVideo>.toAnimeVideosCache(
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
            AnimeVideoEntry(
                animeId = animeId,
                language = language,
                position = index,
                videoId = video.id,
                episode = video.episode,
                dubbing = video.dubbing,
                player = video.player,
                playerId = video.playerId,
                iframeUrl = video.iframeUrl,
                durationSeconds = video.durationSeconds,
                views = video.views,
                watchedEndTimeSeconds = video.watchedEndTimeSeconds,
                watchedDateSeconds = video.watchedDateSeconds,
                openingStartMs = video.skips.opening?.startMs,
                openingEndMs = video.skips.opening?.endMs,
                endingStartMs = video.skips.ending?.startMs,
                endingEndMs = video.skips.ending?.endMs,
            )
        },
    )

internal fun AnimeVideosCache.toAnimeVideos(): List<AnimeVideo> =
    videos.map { it.toAnimeVideo() }

internal fun List<AnimeRecommendation>.toAnimeRecommendationsCache(
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
        recommendations = mapIndexed { index, recommendation ->
            AnimeRecommendationEntry(
                animeId = animeId,
                language = language,
                fromAi = fromAi,
                position = index,
                recommendationAnimeId = recommendation.animeId,
                title = recommendation.title,
                posterSmallUrl = recommendation.poster?.small,
                posterMediumUrl = recommendation.poster?.medium,
                posterBigUrl = recommendation.poster?.big,
                posterFullsizeUrl = recommendation.poster?.fullsize,
                posterMegaUrl = recommendation.poster?.mega,
                rating = recommendation.rating,
                type = recommendation.type,
                year = recommendation.year,
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

internal fun List<AnimeTrailer>.toAnimeTrailersCache(
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
        trailers = mapIndexed { index, trailer ->
            AnimeTrailerEntry(
                animeId = animeId,
                language = language,
                position = index,
                iframeUrl = trailer.iframeUrl,
            )
        },
    )

internal fun AnimeTrailersCache.toAnimeTrailers(): List<AnimeTrailer> =
    trailers
        .sortedBy { it.position }
        .map { AnimeTrailer(iframeUrl = it.iframeUrl) }
        .distinctBy { it.iframeUrl }

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
