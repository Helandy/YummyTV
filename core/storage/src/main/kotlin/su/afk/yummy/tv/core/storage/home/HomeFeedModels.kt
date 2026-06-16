package su.afk.yummy.tv.core.storage.home

const val HOME_FEED_CONTAINER_HERO = "hero"
const val HOME_FEED_CONTAINER_CONTINUE_WATCHING = "continue_watching"
const val HOME_FEED_CONTAINER_NEW_RELEASES = "new_releases"
const val HOME_FEED_CONTAINER_RECOMMENDATIONS = "recommendations"
const val HOME_FEED_CONTAINER_COLLECTIONS = "collections"

const val HOME_FEED_ACTION_SERIES = "series"
const val HOME_FEED_ACTION_VIDEO = "video"
const val HOME_FEED_ACTION_COLLECTION = "collection"
const val HOME_FEED_GENERIC_WATCH_SIGNATURE = ""

data class HomeFeedCache(
    val entry: HomeFeedCacheEntry,
    val items: List<HomeFeedItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun HomeFeedCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
