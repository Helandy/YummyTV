package su.afk.yummy.tv.feature.videodownload.mapper

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus

class VideoDownloadWatchProgressMapperTest {

    private fun item(
        id: Long,
        episode: String = "1",
        animeId: Int = 5,
        status: VideoDownloadStatus = VideoDownloadStatus.Downloaded,
    ) = mockk<VideoDownloadItem> {
        every { this@mockk.id } returns id
        every { this@mockk.episode } returns episode
        every { this@mockk.animeId } returns animeId
        every { this@mockk.status } returns status
    }

    private fun progress(
        episode: String = "1",
        animeId: Int = 5,
        positionMs: Long = 600_000L,
        durationMs: Long = 1_200_000L,
        updatedAt: Long = 0L,
    ) = AnimeWatchProgress(
        animeId = animeId,
        episode = episode,
        episodeUrl = "url",
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
    )

    @Test
    fun `matches progress by anime and episode number ignoring leading zero`() {
        val result = listOf(item(id = 1, episode = "01")).toWatchProgressByDownload(listOf(progress(episode = "1")))

        assertEquals(600_000L, result.getValue(1L).positionMs)
        assertEquals(0.5f, result.getValue(1L).fraction, 0.001f)
        assertFalse(result.getValue(1L).isWatched)
    }

    @Test
    fun `episode watched to the end is marked watched with a full bar`() {
        val result = listOf(item(id = 1)).toWatchProgressByDownload(
            listOf(progress(positionMs = 1_190_000L, durationMs = 1_200_000L)),
        )

        assertTrue(result.getValue(1L).isWatched)
        assertEquals(1f, result.getValue(1L).fraction, 0f)
    }

    @Test
    fun `skips episodes without meaningful progress or from other anime and unfinished downloads`() {
        val result = listOf(
            item(id = 1, episode = "1"),
            item(id = 2, episode = "2", animeId = 9),
            item(id = 3, episode = "3", status = VideoDownloadStatus.Downloading),
        ).toWatchProgressByDownload(
            listOf(
                progress(episode = "1", positionMs = 5_000L),
                progress(episode = "2"),
                progress(episode = "3"),
            ),
        )

        assertNull(result[1L])
        assertNull(result[2L])
        assertNull(result[3L])
    }

    @Test
    fun `latest entry wins when the same episode has several`() {
        val result = listOf(item(id = 1)).toWatchProgressByDownload(
            listOf(
                progress(positionMs = 300_000L, updatedAt = 1),
                progress(positionMs = 700_000L, updatedAt = 2),
            ),
        )

        assertEquals(700_000L, result.getValue(1L).positionMs)
    }
}
