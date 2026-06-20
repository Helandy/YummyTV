package su.afk.yummy.tv.core.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState

data class OffsetPage<T>(
    val items: List<T>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)

class OffsetPagingSource<T : Any>(
    private val initialOffset: Int = 0,
    private val loadPage: suspend (limit: Int, offset: Int) -> OffsetPage<T>,
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> =
        runCatching {
            val offset = params.key ?: initialOffset
            var currentOffset = offset
            var page = loadPage(params.loadSize, currentOffset)

            while (page.items.isEmpty() && page.canLoadMore && page.nextOffset > currentOffset) {
                currentOffset = page.nextOffset
                page = loadPage(params.loadSize, currentOffset)
            }

            LoadResult.Page(
                data = page.items,
                prevKey = null,
                nextKey = if (page.canLoadMore && page.nextOffset > currentOffset) {
                    page.nextOffset
                } else {
                    null
                },
            )
        }.getOrElse { error -> LoadResult.Error(error) }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            (anchorPosition - state.config.initialLoadSize / 2).coerceAtLeast(initialOffset)
        }
}
