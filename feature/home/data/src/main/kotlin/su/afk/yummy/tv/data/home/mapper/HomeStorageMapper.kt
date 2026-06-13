package su.afk.yummy.tv.data.home.mapper

import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.storage.home.HOME_FEED_ACTION_COLLECTION
import su.afk.yummy.tv.core.storage.home.HOME_FEED_ACTION_SERIES
import su.afk.yummy.tv.core.storage.home.HOME_FEED_ACTION_VIDEO
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_COLLECTIONS
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_HERO
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_NEW_RELEASES
import su.afk.yummy.tv.core.storage.home.HOME_FEED_CONTAINER_RECOMMENDATIONS
import su.afk.yummy.tv.core.storage.home.HomeFeedCache
import su.afk.yummy.tv.core.storage.home.HomeFeedCacheEntry
import su.afk.yummy.tv.core.storage.home.HomeFeedItemEntry
import su.afk.yummy.tv.data.home.R
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.model.HomePoster

internal fun HomeFeed.toHomeFeedCache(
    language: String,
    cachedAt: Long,
): HomeFeedCache =
    HomeFeedCache(
        entry = HomeFeedCacheEntry(
            language = language,
            cachedAt = cachedAt,
        ),
        items = heroItems.toHomeFeedItemEntries(language, HOME_FEED_CONTAINER_HERO) +
                sections.flatMap { section ->
                    section.items.toHomeFeedItemEntries(
                        language = language,
                        container = section.type.toStorageContainer(),
                    )
                },
    )

internal fun HomeFeedCache.toHomeFeed(stringProvider: StringProvider): HomeFeed =
    HomeFeed(
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

private fun List<HomeFeedItem>.toHomeFeedItemEntries(
    language: String,
    container: String,
): List<HomeFeedItemEntry> =
    mapIndexed { index, item ->
        HomeFeedItemEntry(
            language = language,
            container = container,
            position = index,
            itemId = item.id,
            title = item.title,
            description = item.description,
            posterSmallUrl = item.poster?.small,
            posterMediumUrl = item.poster?.medium,
            posterBigUrl = item.poster?.big,
            posterFullsizeUrl = item.poster?.fullsize,
            posterMegaUrl = item.poster?.mega,
            rating = item.rating,
            actionType = item.action.storageType,
            actionId = item.action.storageId,
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
        rating = rating,
        action = when (actionType) {
            HOME_FEED_ACTION_COLLECTION -> HomeFeedItemAction.OpenCollection(actionId)
            HOME_FEED_ACTION_VIDEO -> HomeFeedItemAction.OpenVideo(actionId)
            else -> HomeFeedItemAction.OpenSeries(actionId)
        },
    )

private fun HomeFeedSectionType.toStorageContainer(): String =
    when (this) {
        HomeFeedSectionType.NEW_RELEASES -> HOME_FEED_CONTAINER_NEW_RELEASES
        HomeFeedSectionType.RECOMMENDATIONS -> HOME_FEED_CONTAINER_RECOMMENDATIONS
        HomeFeedSectionType.COLLECTIONS -> HOME_FEED_CONTAINER_COLLECTIONS
    }

private val HomeFeedItemAction.storageType: String
    get() = when (this) {
        is HomeFeedItemAction.OpenCollection -> HOME_FEED_ACTION_COLLECTION
        is HomeFeedItemAction.OpenVideo -> HOME_FEED_ACTION_VIDEO
        is HomeFeedItemAction.OpenSeries -> HOME_FEED_ACTION_SERIES
    }

private val HomeFeedItemAction.storageId: Int
    get() = when (this) {
        is HomeFeedItemAction.OpenCollection -> collectionId
        is HomeFeedItemAction.OpenVideo -> videoId
        is HomeFeedItemAction.OpenSeries -> seriesId
    }

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
