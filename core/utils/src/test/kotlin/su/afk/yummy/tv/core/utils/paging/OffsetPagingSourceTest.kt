package su.afk.yummy.tv.core.utils.paging

import androidx.paging.PagingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A repeat across pages is what crashes a lazy list with `Key "..." was already used`,
 * so de-duplication by [itemKey] is covered on its own.
 */
class OffsetPagingSourceTest {

    private data class Item(val id: Int)

    private fun source(pages: List<List<Item>>) = OffsetPagingSource<Item>(
        itemKey = { it.id },
        loadPage = { limit, offset ->
            val items = pages.getOrElse(offset / limit) { emptyList() }
            OffsetPage(items = items, nextOffset = offset + limit, canLoadMore = items.size >= limit)
        },
    )

    private suspend fun PagingSource<Int, Item>.page(offset: Int?, limit: Int) = load(
        if (offset == null) {
            PagingSource.LoadParams.Refresh(null, limit, false)
        } else {
            PagingSource.LoadParams.Append(offset, limit, false)
        },
    ) as PagingSource.LoadResult.Page

    @Test
    fun `drops an item the previous page already returned`() = runTest {
        val source = source(
            listOf(
                listOf(Item(1), Item(2)),
                listOf(Item(2), Item(3)),
            ),
        )

        val first = source.page(offset = null, limit = 2)
        val second = source.page(offset = first.nextKey, limit = 2)

        assertEquals(listOf(Item(1), Item(2)), first.data)
        assertEquals(listOf(Item(3)), second.data)
    }

    @Test
    fun `collapses duplicates inside a single page`() = runTest {
        val source = source(listOf(listOf(Item(1), Item(1), Item(2))))

        assertEquals(listOf(Item(1), Item(2)), source.page(offset = null, limit = 3).data)
    }

    @Test
    fun `refresh forgets the keys seen so far`() = runTest {
        val pages = listOf(listOf(Item(1), Item(2)))
        val source = source(pages)

        source.page(offset = null, limit = 2)
        val refreshed = source.page(offset = null, limit = 2)

        assertEquals(listOf(Item(1), Item(2)), refreshed.data)
    }
}
