package su.afk.yummy.tv.data.library.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.testing.BaseUnitTest

class LocalWatchedCountUtilsTest : BaseUnitTest() {

    @Test
    fun `counts unique watched main episodes and ignores partial or fractional rows`() {
        val entries = listOf(
            entry(animeId = 10, episode = "1", positionMs = FULL_MS, durationMs = FULL_MS),
            entry(animeId = 10, episode = "01", positionMs = FULL_MS, durationMs = FULL_MS),
            entry(animeId = 10, episode = "2", positionMs = FULL_MS, durationMs = FULL_MS),
            entry(animeId = 10, episode = "2.5", positionMs = FULL_MS, durationMs = FULL_MS),
            entry(animeId = 10, episode = "3", positionMs = 120_000L, durationMs = FULL_MS),
            entry(animeId = 20, episode = "1", positionMs = FULL_MS, durationMs = FULL_MS),
        )

        val counts = entries.countWatchedEpisodesByAnimeId()

        assertEquals(mapOf(10 to 2, 20 to 1), counts)
    }

    private fun entry(
        animeId: Int,
        episode: String,
        positionMs: Long,
        durationMs: Long,
    ) = WatchProgressEntry(
        animeId = animeId,
        episode = episode,
        videoId = 0,
        episodeUrl = "",
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = 1L,
    )

    private companion object {
        const val FULL_MS = 24 * 60 * 1_000L
    }
}
