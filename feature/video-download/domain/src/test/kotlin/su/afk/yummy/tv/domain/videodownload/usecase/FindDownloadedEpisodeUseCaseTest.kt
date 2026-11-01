package su.afk.yummy.tv.domain.videodownload.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository

class FindDownloadedEpisodeUseCaseTest : BaseUnitTest() {

    private val repository: VideoDownloadRepository = mockk()

    private fun item(
        id: Long,
        episode: String = "1",
        dubbing: String = "AniLibria",
        status: VideoDownloadStatus = VideoDownloadStatus.Downloaded,
        animeId: Int = ANIME_ID,
    ) = mockk<VideoDownloadItem> {
        every { this@mockk.id } returns id
        every { this@mockk.episode } returns episode
        every { this@mockk.dubbing } returns dubbing
        every { this@mockk.status } returns status
        every { this@mockk.animeId } returns animeId
    }

    private fun useCase(vararg items: VideoDownloadItem): FindDownloadedEpisodeUseCase {
        every { repository.observeDownloads() } returns flowOf(items.toList())
        return FindDownloadedEpisodeUseCase(repository)
    }

    @Test
    fun `finds a finished download with the same dubbing`() = runTest {
        val found = useCase(item(id = 1, dubbing = "Anidub"), item(id = 2))(ANIME_ID, "1", "anilibria")

        assertEquals(2L, found?.id)
    }

    @Test
    fun `episode numbers with a leading zero are the same episode`() = runTest {
        val found = useCase(item(id = 3, episode = "01"))(ANIME_ID, "1", "AniLibria")

        assertEquals(3L, found?.id)
    }

    @Test
    fun `unfinished or foreign downloads are ignored`() = runTest {
        val useCase = useCase(
            item(id = 4, status = VideoDownloadStatus.Downloading),
            item(id = 5, animeId = 99),
        )

        assertNull(useCase(ANIME_ID, "1", "AniLibria"))
    }

    private companion object {
        const val ANIME_ID = 42
    }
}
