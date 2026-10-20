package su.afk.yummy.tv.source.aniboom.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomDubbing
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTitle
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import java.io.IOException

class GetAniboomEpisodeCountsUseCaseTest : BaseUnitTest() {

    private val repository: AniboomRepository = mockk()
    private val title = AniboomTitle(
        "0BzM07Pq71J", 5, listOf(AniboomDubbing("A", 16), AniboomDubbing("B", 30), AniboomDubbing("C", 41)),
    )

    private fun createUseCase() = GetAniboomEpisodeCountsUseCase(repository)

    @Test
    fun `dubbing without the last episode loses one episode`() = runTest {
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 5, 16)) } returns true
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 5, 30)) } returns false
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 5, 41)) } returns true

        assertEquals(mapOf(16 to 5, 30 to 4, 41 to 5), createUseCase()(title, checkLast = true))
    }

    @Test
    fun `network error and timeout keep the episode`() = runTest {
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 5, 16)) } throws IOException("down")
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 5, 30)) } coAnswers { delay(60_000); false }
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 5, 41)) } returns false

        assertEquals(mapOf(16 to 5, 30 to 5, 41 to 4), createUseCase()(title, checkLast = true))
    }

    @Test
    fun `without check there are no requests`() = runTest {
        assertEquals(mapOf(16 to 5, 30 to 5, 41 to 5), createUseCase()(title, checkLast = false))
        coVerify(exactly = 0) { repository.hasEpisode(any()) }
    }

    @Test
    fun `single episode title is never probed`() = runTest {
        val single = title.copy(episodesAvailable = 1)

        assertEquals(mapOf(16 to 1, 30 to 1, 41 to 1), createUseCase()(single, checkLast = true))
        coVerify(exactly = 0) { repository.hasEpisode(any()) }
    }
}
