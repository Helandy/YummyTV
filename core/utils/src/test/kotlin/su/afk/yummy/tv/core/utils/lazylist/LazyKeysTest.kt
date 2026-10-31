package su.afk.yummy.tv.core.utils.lazylist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class LazyKeysTest : BaseUnitTest() {

    @Test
    fun `the same id in different sections gives different keys`() {
        assertNotEquals(lazyKey("similar", 855389), lazyKey("relation", 855389))
    }

    @Test
    fun `a placeholder never lands in the id space`() {
        assertNotEquals(lazyKey("post", null, index = 7), lazyKey("post", 7))
    }

    @Test
    fun `one id in one section gives one key regardless of position`() {
        assertEquals(lazyKey("post", 7, index = 0), lazyKey("post", 7, index = 3))
    }
}

class PagingItemKeyTest : BaseUnitTest() {

    private val items = listOf(10, 11)

    private fun keyAt(index: Int, itemCount: Int = items.size): Any =
        pagingItemKey(index, itemCount, peek = { items.getOrNull(it) }, placeholderPrefix = "p") { it }

    @Test
    fun `a loaded item gives its own key`() {
        assertEquals(11, keyAt(1))
    }

    @Test
    fun `an index past the snapshot never reads the list`() {
        var peeked = false
        val key = pagingItemKey<Int>(20, itemCount = 20, peek = { peeked = true; null }, placeholderPrefix = "p") { it }

        assertEquals(lazyKey("p", null, 20), key)
        assertEquals(false, peeked)
    }

    @Test
    fun `a negative index gives a placeholder key`() {
        assertEquals(lazyKey("p", null, -1), keyAt(-1))
    }

    @Test
    fun `a placeholder inside the snapshot gives a placeholder key`() {
        val key = pagingItemKey<Int>(1, itemCount = 5, peek = { null }, placeholderPrefix = "p") { it }

        assertEquals(lazyKey("p", null, 1), key)
    }
}
