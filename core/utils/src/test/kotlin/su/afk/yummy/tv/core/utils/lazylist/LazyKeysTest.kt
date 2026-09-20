package su.afk.yummy.tv.core.utils.lazylist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LazyKeysTest {

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
