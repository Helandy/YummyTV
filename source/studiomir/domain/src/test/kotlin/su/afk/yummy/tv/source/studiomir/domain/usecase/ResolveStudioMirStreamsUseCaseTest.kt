package su.afk.yummy.tv.source.studiomir.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirStreams
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirStreamsResult
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget
import su.afk.yummy.tv.source.studiomir.domain.repository.StudioMirRepository
import java.io.IOException

class ResolveStudioMirStreamsUseCaseTest : BaseUnitTest() {

    private val repository: StudioMirRepository = mockk()
    private val target = StudioMirTarget("123-psyren", 1)

    private fun createUseCase() = ResolveStudioMirStreamsUseCase(repository)

    @Test
    fun `streams are wrapped into success`() = runTest {
        val streams = StudioMirStreams(mapOf("480p" to "https://a/480.m3u8"), "https://p/vid.php?v=/s1/x")
        coEvery { repository.getStreams(target) } returns streams

        assertEquals(StudioMirStreamsResult.Success(streams), createUseCase()(target))
    }

    @Test
    fun `missing episode is not found and network error is failed`() = runTest {
        coEvery { repository.getStreams(target) } returns null
        assertEquals(StudioMirStreamsResult.NotFound, createUseCase()(target))

        coEvery { repository.getStreams(target) } throws IOException("down")
        assertTrue(createUseCase()(target) is StudioMirStreamsResult.Failed)
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        coEvery { repository.getStreams(target) } throws CancellationException("stop")

        assertThrows(CancellationException::class.java) { kotlinx.coroutines.runBlocking { createUseCase()(target) } }
    }
}
