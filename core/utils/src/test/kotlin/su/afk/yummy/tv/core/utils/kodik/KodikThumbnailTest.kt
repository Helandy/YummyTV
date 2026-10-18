package su.afk.yummy.tv.core.utils.kodik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class KodikThumbnailTest : BaseUnitTest() {

    @Test
    fun `different dubbings of one episode share cache key`() {
        val watched = KodikThumbnail("//kodikplayer.com/season/1/a/720p?episode=1", 10, "01")
        val popular = KodikThumbnail("//kodikplayer.com/season/2/b/720p?episode=1", 10, "1")

        assertEquals(watched.cacheKey, popular.cacheKey)
        assertEquals(episodeThumbnailCacheKey(10, "1"), watched.cacheKey)
        assertNotEquals(watched.legacyCacheKey, popular.legacyCacheKey)
    }

    @Test
    fun `without episode the cache key stays iframe based`() {
        val thumbnail = KodikThumbnail("//kodikplayer.com/season/1/a/720p?episode=1")

        assertEquals("kodik_thumb:https://kodikplayer.com/season/1/a/720p?episode=1", thumbnail.cacheKey)
        assertEquals(thumbnail.legacyCacheKey, thumbnail.cacheKey)
    }
}
