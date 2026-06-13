package su.afk.yummy.tv.core.storage.anime

data class AnimeDetailsCache(
    val entry: AnimeDetailsEntry,
    val otherTitles: List<AnimeDetailTitleEntry>,
    val genres: List<AnimeDetailNamedEntry>,
    val creators: List<AnimeDetailNamedEntry>,
    val studios: List<AnimeDetailNamedEntry>,
    val viewingOrder: List<AnimeViewingOrderEntry>,
    val screenshots: List<AnimeScreenshotEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AnimeVideosCache(
    val entry: AnimeVideoCacheEntry,
    val videos: List<AnimeVideoEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AnimeRecommendationsCache(
    val entry: AnimeRecommendationCacheEntry,
    val recommendations: List<AnimeRecommendationEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AnimeTrailersCache(
    val entry: AnimeTrailerCacheEntry,
    val trailers: List<AnimeTrailerEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun AnimeDetailsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AnimeVideosCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AnimeRecommendationsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AnimeTrailersCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
