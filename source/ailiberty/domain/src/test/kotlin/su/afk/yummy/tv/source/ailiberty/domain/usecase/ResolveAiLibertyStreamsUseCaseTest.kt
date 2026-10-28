package su.afk.yummy.tv.source.ailiberty.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyStreams
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyStreamsResult
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyTarget
import su.afk.yummy.tv.source.ailiberty.domain.repository.AiLibertyRepository
import java.io.IOException

class ResolveAiLibertyStreamsUseCaseTest : BaseUnitTest() {

    private val repository: AiLibertyRepository = mockk()
    private val target = AiLibertyTarget("psyren", 1)

    private fun createUseCase() = ResolveAiLibertyStreamsUseCase(repository)

    @Test
    fun `streams are wrapped into success`() = runTest {
        val streams = AiLibertyStreams(mapOf("480p" to "https://a/480.m3u8"))
        coEvery { repository.getStreams(target) } returns streams

        assertEquals(AiLibertyStreamsResult.Success(streams), createUseCase()(target))
    }

    @Test
    fun `missing episode is not found and network error is failed`() = runTest {
        coEvery { repository.getStreams(target) } returns null
        assertEquals(AiLibertyStreamsResult.NotFound, createUseCase()(target))

        coEvery { repository.getStreams(target) } throws IOException("down")
        assertTrue(createUseCase()(target) is AiLibertyStreamsResult.Failed)
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        coEvery { repository.getStreams(target) } throws CancellationException("stop")

        assertThrows(CancellationException::class.java) { kotlinx.coroutines.runBlocking { createUseCase()(target) } }
    }
}
