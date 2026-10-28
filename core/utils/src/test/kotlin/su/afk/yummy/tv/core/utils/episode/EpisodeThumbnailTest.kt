package su.afk.yummy.tv.core.utils.episode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class EpisodeThumbnailTest : BaseUnitTest() {

    @Test
    fun `different dubbings of one episode share cache key`() {
        val watched = EpisodeThumbnail(10, "01", iframeUrl = "//kodikplayer.com/season/1/a/720p?episode=1")
        val popular = EpisodeThumbnail(10, "1", iframeUrl = "//kodikplayer.com/season/2/b/720p?episode=1")

        assertEquals(watched.cacheKey, popular.cacheKey)
        assertEquals(episodeThumbnailCacheKey(10, "1"), watched.cacheKey)
        assertNotEquals(watched.legacyCacheKey, popular.legacyCacheKey)
    }

    @Test
    fun `without episode the cache key stays iframe based`() {
        val thumbnail = EpisodeThumbnail(iframeUrl = "//kodikplayer.com/season/1/a/720p?episode=1")

        assertEquals("kodik_thumb:https://kodikplayer.com/season/1/a/720p?episode=1", thumbnail.cacheKey)
        assertTrue(thumbnail.fallbackCacheKeys.isEmpty())
    }

    @Test
    fun `known episode also reads the legacy keys`() {
        val thumbnail = EpisodeThumbnail(10, "1", iframeUrl = "//kodikplayer.com/season/1/a/720p?episode=1")

        assertEquals(
            listOf(tenraiThumbnailCacheKey(10, "1"), thumbnail.legacyCacheKey),
            thumbnail.fallbackCacheKeys,
        )
    }

    @Test
    fun `thumbnail without any hint is not resolvable`() {
        assertFalse(EpisodeThumbnail().isResolvable)
        assertTrue(EpisodeThumbnail(animeId = 1, episode = "2").isResolvable)
    }
}
