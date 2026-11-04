package su.afk.yummy.tv.source.sameband.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreams
import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreamsResult
import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget
import su.afk.yummy.tv.source.sameband.domain.repository.SamebandRepository
import java.io.IOException

class ResolveSamebandStreamsUseCaseTest : BaseUnitTest() {

    private val repository: SamebandRepository = mockk()
    private val target = SamebandTarget("123-psyren", 1)

    private fun createUseCase() = ResolveSamebandStreamsUseCase(repository)

    @Test
    fun `streams are wrapped into success`() = runTest {
        val streams = SamebandStreams(mapOf("480p" to "https://a/480.m3u8"), mapOf("Referer" to "https://p/"))
        coEvery { repository.getStreams(target) } returns streams

        assertEquals(SamebandStreamsResult.Success(streams), createUseCase()(target))
    }

    @Test
    fun `missing episode is not found and network error is failed`() = runTest {
        coEvery { repository.getStreams(target) } returns null
        assertEquals(SamebandStreamsResult.NotFound, createUseCase()(target))

        coEvery { repository.getStreams(target) } throws IOException("down")
        assertTrue(createUseCase()(target) is SamebandStreamsResult.Failed)
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        coEvery { repository.getStreams(target) } throws CancellationException("stop")

        assertThrows(CancellationException::class.java) { kotlinx.coroutines.runBlocking { createUseCase()(target) } }
    }
}
