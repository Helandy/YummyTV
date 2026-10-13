package su.afk.yummy.tv.domain.player.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerSourceData
import su.afk.yummy.tv.domain.player.model.PlayerSourceRequest
import su.afk.yummy.tv.domain.player.model.PlayerSourceVideo
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository

class GetPlayerSourceGraphUseCaseTest : BaseUnitTest() {

    private val repository: PlayerSourceRepository = mockk()

    private val ep1 = video(id = 1, episode = "1")
    private val ep2 = video(id = 2, episode = "2")

    @Test
    fun `graph selects the requested video`() = runTest {
        coEvery { repository.getSources(ANIME_ID, false) } returns PlayerSourceData(listOf(ep1, ep2))

        val graph = GetPlayerSourceGraphUseCase(repository)(request(selectedVideoId = 2))

        val dubbing = graph.balancers[graph.selection.balancerIndex].dubbings[graph.selection.dubbingIndex]
        assertEquals("2", dubbing.episodes[graph.selection.episodeIndex].number)
    }

    @Test
    fun `missing video id falls back to the iframe, then to the episode`() = runTest {
        coEvery { repository.getSources(ANIME_ID, false) } returns PlayerSourceData(listOf(ep1, ep2))
        val useCase = GetPlayerSourceGraphUseCase(repository)

        val byIframe = useCase(request(selectedVideoId = 0, iframeUrl = ep2.iframeUrl))
        val byEpisode = useCase(request(selectedVideoId = 0, episode = "2"))

        assertEquals(1, byIframe.selection.episodeIndex)
        assertEquals(1, byEpisode.selection.episodeIndex)
    }

    @Test
    fun `source without a title plays only the requested video`() = runTest {
        val graph = GetPlayerSourceGraphUseCase(repository)(
            request(animeId = 0, selectedVideoId = 9, iframeUrl = "https://kodik.info/seria/9", episode = "5"),
        )

        assertEquals(listOf("5"), graph.balancers.single().dubbings.single().episodes.map { it.number })
        coVerify(exactly = 0) { repository.getSources(any(), any()) }
    }

    @Test
    fun `forced refresh is passed to the repository`() = runTest {
        coEvery { repository.getSources(ANIME_ID, true) } returns PlayerSourceData(listOf(ep1))

        GetPlayerSourceGraphUseCase(repository)(request(selectedVideoId = 1), forceRefreshVideos = true)

        coVerify { repository.getSources(ANIME_ID, true) }
    }

    private fun request(
        animeId: Int = ANIME_ID,
        selectedVideoId: Int,
        iframeUrl: String = "",
        episode: String = "",
    ) = PlayerSourceRequest(
        animeId = animeId,
        iframeUrl = iframeUrl,
        animeTitle = "Title",
        episode = episode,
        playerName = "Kodik",
        dubbing = "AniLibria",
        selectedVideoId = selectedVideoId,
        selectedScreenshotUrl = "",
    )

    private fun video(id: Int, episode: String) = PlayerSourceVideo(
        id = id,
        episode = episode,
        dubbing = "AniLibria",
        player = "Kodik",
        iframeUrl = "https://kodik.info/seria/$id",
    )

    private companion object {
        const val ANIME_ID = 42
    }
}
