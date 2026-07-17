package su.afk.yummy.tv.data.home.storage.mapper

import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.storage.home.HOME_FEED_ACTION_COLLECTION
import su.afk.yummy.tv.core.storage.home.HOME_FEED_ACTION_SERIES
import su.afk.yummy.tv.core.storage.home.HOME_FEED_ACTION_VIDEO
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_COLLECTIONS
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_CONTINUE_WATCHING
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_HERO
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_NEW_RELEASES
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_RECOMMENDATIONS
import su.afk.yummy.tv.core.storage.home.HomeFeedCache
import su.afk.yummy.tv.core.storage.home.HomeFeedCacheEntry
import su.afk.yummy.tv.core.storage.home.HomeFeedItemEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.home.R
import su.afk.yummy.tv.data.home.dto.YaniAnimeDto
import su.afk.yummy.tv.data.home.dto.YaniCollectionDto
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniVideoDto
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.model.HomePoster

internal fun YaniFeedDto.toHomeFeedCache(
    language: String,
    watchSignature: String,
    cachedAt: Long,
): HomeFeedCache =
    HomeFeedCache(
        entry = HomeFeedCacheEntry(
            language = language,
            watchSignature = watchSignature,
            cachedAt = cachedAt,
        ),
        items = response.topCarousel.items.toSeriesEntries(
            language = language,
            watchSignature = watchSignature,
            container = HOME_FEED_CONTAINER_HERO,
        ) + response.newVideos.toNewVideoEntries(language, watchSignature) +
                response.recommends.toSeriesEntries(
                    language,
                    watchSignature,
                    HOME_FEED_CONTAINER_RECOMMENDATIONS,
                ) + response.collections.toCollectionEntries(language, watchSignature),
    )

internal fun HomeFeedCache.toHomeFeed(stringProvider: StringProvider): HomeFeed =
    HomeFeed(
        continueWatchingItems = items
            .filter { it.container == HOME_FEED_CONTAINER_CONTINUE_WATCHING }
            .sortedBy { it.position }
            .map { it.toContinueWatchingItem() },
        heroItems = items
            .filter { it.container == HOME_FEED_CONTAINER_HERO }
            .sortedBy { it.position }
            .map { it.toHomeFeedItem() },
        sections = listOfNotNull(
            section(
                type = HomeFeedSectionType.NEW_RELEASES,
                title = stringProvider.get(R.string.home_section_new),
                container = HOME_FEED_CONTAINER_NEW_RELEASES,
            ),
            section(
                type = HomeFeedSectionType.RECOMMENDATIONS,
                title = stringProvider.get(R.string.home_section_recommends),
                container = HOME_FEED_CONTAINER_RECOMMENDATIONS,
            ),
            section(
                type = HomeFeedSectionType.COLLECTIONS,
                title = stringProvider.get(R.string.home_section_collections),
                container = HOME_FEED_CONTAINER_COLLECTIONS,
            ),
        ),
    )

internal fun HomeContinueWatchingItem.toWatchProgressEntry(): WatchProgressEntry =
    WatchProgressEntry(
        animeId = animeId,
        episode = episode,
        videoId = videoId,
        episodeUrl = episodeUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
        animeTitle = animeTitle,
        posterUrl = poster?.bestUrl().orEmpty(),
        playerName = playerName,
        dubbing = dubbing,
        screenshotUrl = screenshotUrl,
    )

internal fun WatchProgressEntry.toHomeContinueWatchingItem(): HomeContinueWatchingItem =
    HomeContinueWatchingItem(
        animeId = animeId,
        animeTitle = animeTitle,
        description = "",
        poster = posterUrl.takeIf { it.isNotBlank() }?.let { it.toHomePoster() },
        videoId = videoId,
        episode = episode,
        episodeUrl = episodeUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
        playerName = playerName,
        dubbing = dubbing,
        screenshotUrl = screenshotUrl,
    )

private fun HomePoster.bestUrl(): String? =
    mega ?: fullsize ?: big ?: medium ?: small

private fun String.toHomePoster(): HomePoster =
    HomePoster(
        small = null,
        medium = null,
        big = null,
        fullsize = null,
        mega = this,
    )

private fun HomeFeedCache.section(
    type: HomeFeedSectionType,
    title: String,
    container: String,
): HomeFeedSection? {
    val sectionItems = items
        .filter { it.container == container }
        .sortedBy { it.position }
        .map { it.toHomeFeedItem() }
    return sectionItems
        .takeIf { it.isNotEmpty() }
        ?.let { HomeFeedSection(type = type, title = title, items = it) }
}

private fun List<YaniAnimeDto>.toSeriesEntries(
    language: String,
    watchSignature: String,
    container: String,
): List<HomeFeedItemEntry> =
    mapNotNull { item ->
        val id = item.animeId ?: return@mapNotNull null
        val title = item.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Triple(id, title, item)
    }.mapIndexed { index, (id, title, item) ->
        HomeFeedItemEntry(
            language = language,
            watchSignature = watchSignature,
            container = container,
            position = index,
            itemId = id,
            title = title,
            description = item.description,
            posterSmallUrl = item.poster?.small?.toHttpsUrl(),
            posterMediumUrl = item.poster?.medium?.toHttpsUrl(),
            posterBigUrl = item.poster?.big?.toHttpsUrl(),
            posterFullsizeUrl = item.poster?.fullsize?.toHttpsUrl(),
            posterMegaUrl = item.poster?.mega?.toHttpsUrl(),
            rating = item.rating?.average?.takeIf { it > 0.0 },
            year = item.year?.takeIf { it > 0 },
            actionType = HOME_FEED_ACTION_SERIES,
            actionId = id,
        )
    }

private fun List<YaniVideoDto>.toNewVideoEntries(
    language: String,
    watchSignature: String,
): List<HomeFeedItemEntry> =
    groupBy { it.animeId }.mapNotNull { (animeId, videos) ->
        val id = animeId ?: return@mapNotNull null
        val item = videos.first()
        val title = item.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Triple(id, title, item)
    }.mapIndexed { index, (id, title, item) ->
        HomeFeedItemEntry(
            language = language,
            watchSignature = watchSignature,
            container = HOME_FEED_CONTAINER_NEW_RELEASES,
            position = index,
            itemId = id,
            title = title,
            description = item.description,
            posterSmallUrl = item.poster?.small?.toHttpsUrl(),
            posterMediumUrl = item.poster?.medium?.toHttpsUrl(),
            posterBigUrl = item.poster?.big?.toHttpsUrl(),
            posterFullsizeUrl = item.poster?.fullsize?.toHttpsUrl(),
            posterMegaUrl = item.poster?.mega?.toHttpsUrl(),
            rating = null,
            year = null,
            actionType = HOME_FEED_ACTION_SERIES,
            actionId = id,
        )
    }

private fun List<YaniCollectionDto>.toCollectionEntries(
    language: String,
    watchSignature: String,
): List<HomeFeedItemEntry> =
    mapNotNull { item ->
        val id = item.id ?: return@mapNotNull null
        val title = item.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Triple(id, title, item)
    }.mapIndexed { index, (id, title, item) ->
        val poster = item.posterPreviews.firstOrNull()
        HomeFeedItemEntry(
            language = language,
            watchSignature = watchSignature,
            container = HOME_FEED_CONTAINER_COLLECTIONS,
            position = index,
            itemId = -id,
            title = title,
            description = item.description,
            posterSmallUrl = poster?.small?.toHttpsUrl(),
            posterMediumUrl = poster?.medium?.toHttpsUrl(),
            posterBigUrl = poster?.big?.toHttpsUrl(),
            posterFullsizeUrl = poster?.fullsize?.toHttpsUrl(),
            posterMegaUrl = poster?.mega?.toHttpsUrl(),
            rating = null,
            year = null,
            actionType = HOME_FEED_ACTION_COLLECTION,
            actionId = id,
        )
    }

private fun HomeFeedItemEntry.toHomeFeedItem(): HomeFeedItem =
    HomeFeedItem(
        id = itemId,
        title = title,
        description = description,
        poster = posterOrNull(
            small = posterSmallUrl,
            medium = posterMediumUrl,
            big = posterBigUrl,
            fullsize = posterFullsizeUrl,
            mega = posterMegaUrl,
        ),
        rating = rating?.takeIf { it > 0.0 },
        year = year?.takeIf { it > 0 },
        action = when (actionType) {
            HOME_FEED_ACTION_COLLECTION -> HomeFeedItemAction.OpenCollection(actionId)
            HOME_FEED_ACTION_VIDEO -> HomeFeedItemAction.OpenVideo(actionId)
            else -> HomeFeedItemAction.OpenSeries(actionId)
        },
    )

private fun HomeFeedItemEntry.toContinueWatchingItem(): HomeContinueWatchingItem =
    HomeContinueWatchingItem(
        animeId = itemId,
        animeTitle = title,
        description = description,
        poster = posterOrNull(
            small = posterSmallUrl,
            medium = posterMediumUrl,
            big = posterBigUrl,
            fullsize = posterFullsizeUrl,
            mega = posterMegaUrl,
        ),
        videoId = actionId.takeIf { actionType == HOME_FEED_ACTION_VIDEO } ?: 0,
        episode = episode,
        episodeUrl = episodeUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
        playerName = playerName,
        dubbing = dubbing,
        screenshotUrl = screenshotUrl,
    )

private fun posterOrNull(
    small: String?,
    medium: String?,
    big: String?,
    fullsize: String?,
    mega: String?,
): HomePoster? {
    if (small == null && medium == null && big == null && fullsize == null && mega == null) {
        return null
    }
    return HomePoster(
        small = small,
        medium = medium,
        big = big,
        fullsize = fullsize,
        mega = mega,
    )
}
