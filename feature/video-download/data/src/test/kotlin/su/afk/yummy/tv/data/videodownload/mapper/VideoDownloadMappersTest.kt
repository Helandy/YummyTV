package su.afk.yummy.tv.data.videodownload.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadCacheKeyScheme
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadQualityOption
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest

class VideoDownloadMappersTest : BaseUnitTest() {

    private fun request(skips: AnimeVideoSkips) = VideoDownloadRequest(
        animeId = 1,
        animeTitle = "Title",
        posterUrl = "",
        episode = "1",
        videoId = 10,
        playerName = "Kodik",
        playerId = null,
        dubbing = "Dub",
        iframeUrl = "https://iframe",
        screenshotUrl = "",
        quality = VideoDownloadQualityOption(label = "720p", url = "https://stream"),
        headers = emptyMap(),
        skips = skips,
    )

    @Test
    fun `тайминги пропуска переживают запись и чтение загрузки`() {
        val skips = AnimeVideoSkips(
            opening = AnimeVideoSkipSegment(startMs = 5_000, endMs = 95_000),
            ending = AnimeVideoSkipSegment(startMs = 1_300_000, endMs = 1_390_000),
        )

        val item = request(skips)
            .toEntry(now = 1L, cacheKeyScheme = VideoDownloadCacheKeyScheme.entries.first())
            .toDomain()

        assertEquals(skips, item.skips)
    }

    @Test
    fun `без таймингов сегменты остаются пустыми`() {
        val item = request(AnimeVideoSkips())
            .toEntry(now = 1L, cacheKeyScheme = VideoDownloadCacheKeyScheme.entries.first())
            .toDomain()

        assertNull(item.skips.opening)
        assertNull(item.skips.ending)
    }
}
