package su.afk.yummy.tv.source.anime4k.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kApiException
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kFile
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreamsResult
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import java.io.IOException

class ResolveAnime4kStreamsUseCaseTest : BaseUnitTest() {

    private val repository: Anime4kRepository = mockk()

    private val target = Anime4kTarget(malId = 1735, season = "1", episode = "1", team = "AlexFilm")
    private val streams = Anime4kStreams("https://example.org/", listOf(Anime4kFile(2160, 7500, "https://v/x_2160.mp4")))

    private fun createUseCase() = ResolveAnime4kStreamsUseCase(repository)

    @Test
    fun `links are returned straight away`() = runTest {
        coEvery { repository.getStreams(target) } returns streams

        assertEquals(Anime4kStreamsResult.Success(streams), createUseCase()(target))
    }

    @Test
    fun `server refusals map to results`() = runTest {
        coEvery { repository.getStreams(target) } throws Anime4kApiException.RateLimited()
        assertEquals(Anime4kStreamsResult.RateLimited, createUseCase()(target))

        coEvery { repository.getStreams(target) } throws Anime4kApiException.Rejected()
        assertEquals(Anime4kStreamsResult.Rejected, createUseCase()(target))

        coEvery { repository.getStreams(target) } throws Anime4kApiException.NotFound()
        assertEquals(Anime4kStreamsResult.NotFound, createUseCase()(target))
    }

    @Test
    fun `transport error becomes a failed result`() = runTest {
        coEvery { repository.getStreams(target) } throws IOException("boom")

        assertEquals(Anime4kStreamsResult.Failed("boom"), createUseCase()(target))
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        coEvery { repository.getStreams(target) } throws CancellationException("stop")

        val error = runCatching { createUseCase()(target) }.exceptionOrNull()

        assertTrue(error is CancellationException)
    }
}
