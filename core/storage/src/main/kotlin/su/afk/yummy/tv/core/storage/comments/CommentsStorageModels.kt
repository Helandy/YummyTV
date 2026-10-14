package su.afk.yummy.tv.core.storage.comments

import su.afk.yummy.tv.core.storage.offlinefirst.isCacheFresh

data class CommentsPageCache(
    val entry: CommentPageEntry,
    val items: List<CommentItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun CommentsPageCache.isFresh(ttlMs: Long): Boolean =
    isCacheFresh(cachedAt, ttlMs)
