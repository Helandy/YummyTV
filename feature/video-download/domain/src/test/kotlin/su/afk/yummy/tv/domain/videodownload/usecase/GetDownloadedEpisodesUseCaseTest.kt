package su.afk.yummy.tv.domain.videodownload.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository

class GetDownloadedEpisodesUseCaseTest : BaseUnitTest() {

    private val repository: VideoDownloadRepository = mockk()

    private fun item(
        id: Long,
        episode: String,
        dubbing: String = "AniLibria",
        status: VideoDownloadStatus = VideoDownloadStatus.Downloaded,
        animeId: Int = ANIME_ID,
        updatedAt: Long = 0L,
    ) = mockk<VideoDownloadItem> {
        every { this@mockk.id } returns id
        every { this@mockk.episode } returns episode
        every { this@mockk.dubbing } returns dubbing
        every { this@mockk.status } returns status
        every { this@mockk.animeId } returns animeId
        every { this@mockk.updatedAt } returns updatedAt
    }

    private fun useCase(vararg items: VideoDownloadItem): GetDownloadedEpisodesUseCase {
        every { repository.observeDownloads() } returns flowOf(items.toList())
        return GetDownloadedEpisodesUseCase(repository)
    }

    @Test
    fun `returns finished episodes of the dubbing ordered by number`() = runTest {
        val result = useCase(
            item(id = 3, episode = "10"),
            item(id = 1, episode = "2"),
            item(id = 2, episode = "1", dubbing = "Anidub"),
            item(id = 4, episode = "3", status = VideoDownloadStatus.Downloading),
            item(id = 5, episode = "1", animeId = 99),
            item(id = 6, episode = "1"),
        )(ANIME_ID, "anilibria")

        assertEquals(listOf(6L, 1L, 3L), result.map { it.id })
    }

    @Test
    fun `duplicate downloads of one episode keep the latest`() = runTest {
        val result = useCase(
            item(id = 1, episode = "1", updatedAt = 10),
            item(id = 2, episode = "01", updatedAt = 20),
        )(ANIME_ID, "AniLibria")

        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `unknown anime has no episodes`() = runTest {
        assertTrue(useCase(item(id = 1, episode = "1"))(0, "AniLibria").isEmpty())
    }

    private companion object {
        const val ANIME_ID = 5
    }
}
