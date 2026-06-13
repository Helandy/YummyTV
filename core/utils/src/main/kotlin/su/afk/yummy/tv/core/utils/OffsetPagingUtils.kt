package su.afk.yummy.tv.core.utils

suspend fun <P, I> loadFirstNonEmptyOffsetPage(
    initialOffset: Int,
    loadPage: suspend (offset: Int) -> P,
    items: (P) -> List<I>,
    nextOffset: (P) -> Int,
    canLoadMore: (P) -> Boolean,
): P {
    var currentOffset = initialOffset
    var page = loadPage(currentOffset)

    while (items(page).isEmpty() && canLoadMore(page)) {
        val followingOffset = nextOffset(page)
        if (followingOffset <= currentOffset) break

        currentOffset = followingOffset
        page = loadPage(currentOffset)
    }

    return page
}
