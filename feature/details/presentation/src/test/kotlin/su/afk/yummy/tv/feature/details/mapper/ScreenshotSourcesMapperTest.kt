package su.afk.yummy.tv.feature.details.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

class ScreenshotSourcesMapperTest {

    private fun yani(vararg urls: String) = urls.mapIndexed { i, url -> AnimeScreenshot(id = i + 100, episode = null, url = url) }

    @Test
    fun `without frames the yani screenshots stay as they are`() {
        val screenshots = yani("y1", "y2")

        assertEquals(screenshots, mergeScreenshotSources(ANIME_ID, AnimeEpisodeFrames(), screenshots))
    }

    @Test
    fun `tmdb sets the count and every card falls back to tenrai then yani`() {
        val frames = AnimeEpisodeFrames(tmdbStillUrls = listOf("t1", "t2", "t3"), tenraiFrameUrls = listOf("n1", "n2"))

        val merged = mergeScreenshotSources(ANIME_ID, frames, yani("y1"))

        assertEquals(listOf("t1", "t2", "t3"), merged.map { it.url })
        assertEquals(listOf("n1", "y1"), merged[0].fallbackUrls)
        assertEquals(listOf("n2"), merged[1].fallbackUrls)
        assertEquals(emptyList<String>(), merged[2].fallbackUrls)
        assertEquals(listOf("1", "1", "1"), merged.map { it.episode })
    }

    @Test
    fun `without tmdb tenrai sets the count and falls back to yani`() {
        val frames = AnimeEpisodeFrames(tenraiFrameUrls = listOf("n1", "n2"))

        val merged = mergeScreenshotSources(ANIME_ID, frames, yani("y1", "y2", "y3"))

        assertEquals(listOf("n1", "n2"), merged.map { it.url })
        assertEquals(listOf(listOf("y1"), listOf("y2")), merged.map { it.fallbackUrls })
        assertEquals(listOf("1", "2"), merged.map { it.episode })
        assertEquals(listOf(100, 101), merged.map { it.id })
    }

    @Test
    fun `repeated and blank urls are dropped from the chain`() {
        val frames = AnimeEpisodeFrames(tmdbStillUrls = listOf("same"), tenraiFrameUrls = listOf("same"))

        val merged = mergeScreenshotSources(ANIME_ID, frames, yani(" "))

        assertEquals("same", merged.single().url)
        assertEquals(emptyList<String>(), merged.single().fallbackUrls)
    }

    @Test
    fun `every card has one stable cache key for all its urls`() {
        val frames = AnimeEpisodeFrames(tmdbStillUrls = listOf("t1", "t2"), tenraiFrameUrls = listOf("n1"))

        val merged = mergeScreenshotSources(ANIME_ID, frames, yani("y1", "y2"))

        assertEquals(listOf("screenshot_v1:7:0", "screenshot_v1:7:1"), merged.map { it.cacheKey })
        // ключ не зависит от того, какой источник выиграл: тот же при другом наборе ссылок
        val tenraiOnly = mergeScreenshotSources(ANIME_ID, AnimeEpisodeFrames(tenraiFrameUrls = listOf("n1", "n2")), emptyList())
        assertEquals(listOf("screenshot_v1:7:0", "screenshot_v1:7:1"), tenraiOnly.map { it.cacheKey })
    }

    private companion object {
        const val ANIME_ID = 7
    }
}
