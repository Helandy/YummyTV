package su.afk.yummy.tv.core.storage.comments

data class CommentsPageCache(
    val entry: CommentPageEntry,
    val items: List<CommentItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun CommentsPageCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
