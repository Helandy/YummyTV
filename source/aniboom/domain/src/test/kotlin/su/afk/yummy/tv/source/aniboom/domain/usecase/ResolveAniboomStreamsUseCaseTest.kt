package su.afk.yummy.tv.source.aniboom.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreams
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreamsResult
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import java.io.IOException

class ResolveAniboomStreamsUseCaseTest : BaseUnitTest() {

    private val repository: AniboomRepository = mockk()
    private val target = AniboomTarget("0BzM07Pq71J", 1, 16)

    private fun createUseCase() = ResolveAniboomStreamsUseCase(repository)

    @Test
    fun `stream is wrapped into success`() = runTest {
        coEvery { repository.getStreams(target) } returns AniboomStreams("https://cdn/master.m3u8")

        val result = createUseCase()(target)

        assertEquals("https://cdn/master.m3u8", (result as AniboomStreamsResult.Success).streams.hlsUrl)
    }

    @Test
    fun `missing stream is not found`() = runTest {
        coEvery { repository.getStreams(target) } returns null

        assertTrue(createUseCase()(target) is AniboomStreamsResult.NotFound)
    }

    @Test
    fun `network error is failed with its message`() = runTest {
        coEvery { repository.getStreams(target) } throws IOException("boom")

        assertEquals("boom", (createUseCase()(target) as AniboomStreamsResult.Failed).reason)
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        coEvery { repository.getStreams(target) } throws CancellationException("cancel")

        assertThrows(CancellationException::class.java) { kotlinx.coroutines.runBlocking { createUseCase()(target) } }
    }
}
