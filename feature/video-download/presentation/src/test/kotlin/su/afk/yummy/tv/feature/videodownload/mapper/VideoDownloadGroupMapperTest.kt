package su.afk.yummy.tv.feature.videodownload.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadCacheKeyScheme
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus

class VideoDownloadGroupMapperTest {

    private fun item(
        id: Long,
        animeId: Int = 5,
        title: String = "Anime",
        episode: String = "1",
        status: VideoDownloadStatus = VideoDownloadStatus.Downloaded,
        bytes: Long = 100L,
        updatedAt: Long = 0L,
        posterUrl: String = "",
    ) = VideoDownloadItem(
        id = id,
        animeId = animeId,
        animeTitle = title,
        posterUrl = posterUrl,
        episode = episode,
        videoId = 1,
        playerName = "Kodik",
        playerId = null,
        dubbing = "",
        iframeUrl = "",
        screenshotUrl = "",
        qualityLabel = "720p",
        streamUrl = "",
        headers = emptyMap(),
        cacheKey = "key$id",
        cacheKeyScheme = VideoDownloadCacheKeyScheme.entries.first(),
        status = status,
        progress = 1f,
        bytesDownloaded = bytes,
        totalBytes = null,
        errorMessage = null,
        exportStatus = VideoExportStatus.Idle,
        exportProgress = 0f,
        exportDirectoryUri = null,
        exportedFileUri = null,
        exportErrorMessage = null,
        createdAt = 0L,
        updatedAt = updatedAt,
    )

    @Test
    fun `episodes of one anime collapse into one group`() {
        val groups = listOf(item(1, episode = "2"), item(2, episode = "1"), item(3, animeId = 9)).toGroups()

        assertEquals(2, groups.size)
        assertEquals(listOf(2L, 1L), groups.first { it.animeId == 5 }.episodes.map { it.id })
    }

    @Test
    fun `groups are ordered by latest update`() {
        val groups = listOf(
            item(1, animeId = 1, updatedAt = 10),
            item(2, animeId = 2, updatedAt = 30),
            item(3, animeId = 1, updatedAt = 20),
        ).toGroups()

        assertEquals(listOf(2, 1), groups.map { it.animeId })
    }

    @Test
    fun `episodes are sorted numerically with leading zeros and non numeric last`() {
        val episodes = listOf(
            item(1, episode = "10"),
            item(2, episode = "02"),
            item(3, episode = "Special"),
            item(4, episode = "1"),
        ).toGroups().single().episodes

        assertEquals(listOf(4L, 2L, 1L, 3L), episodes.map { it.id })
    }

    @Test
    fun `items without anime id are grouped by title`() {
        val groups = listOf(
            item(1, animeId = 0, title = "A"),
            item(2, animeId = 0, title = "B"),
            item(3, animeId = 0, title = "A"),
        ).toGroups()

        assertEquals(2, groups.size)
        assertEquals(2, groups.first { it.title == "A" }.episodeCount)
    }

    @Test
    fun `aggregates sum bytes and count active episodes`() {
        val group = listOf(
            item(1, bytes = 10),
            item(2, bytes = 30, status = VideoDownloadStatus.Downloading),
            item(3, bytes = -5),
        ).toGroups().single()

        assertEquals(3, group.episodeCount)
        assertEquals(40L, group.occupiedBytes)
        assertEquals(1, group.activeCount)
    }

    @Test
    fun `poster is taken from first episode that has one`() {
        val group = listOf(item(1), item(2, posterUrl = "p")).toGroups().single()

        assertEquals("p", group.posterUrl)
    }
}
