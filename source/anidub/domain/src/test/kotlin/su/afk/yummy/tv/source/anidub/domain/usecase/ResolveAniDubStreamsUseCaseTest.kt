package su.afk.yummy.tv.source.anidub.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreams
import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreamsResult
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget
import su.afk.yummy.tv.source.anidub.domain.repository.AniDubRepository
import java.io.IOException

class ResolveAniDubStreamsUseCaseTest : BaseUnitTest() {

    private val repository: AniDubRepository = mockk()
    private val target = AniDubTarget("123-psyren", 1)

    private fun createUseCase() = ResolveAniDubStreamsUseCase(repository)

    @Test
    fun `streams are wrapped into success`() = runTest {
        val streams = AniDubStreams(mapOf("480p" to "https://a/480.m3u8"), "https://p/vid.php?v=/s1/x")
        coEvery { repository.getStreams(target) } returns streams

        assertEquals(AniDubStreamsResult.Success(streams), createUseCase()(target))
    }

    @Test
    fun `missing episode is not found and network error is failed`() = runTest {
        coEvery { repository.getStreams(target) } returns null
        assertEquals(AniDubStreamsResult.NotFound, createUseCase()(target))

        coEvery { repository.getStreams(target) } throws IOException("down")
        assertTrue(createUseCase()(target) is AniDubStreamsResult.Failed)
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        coEvery { repository.getStreams(target) } throws CancellationException("stop")

        assertThrows(CancellationException::class.java) { kotlinx.coroutines.runBlocking { createUseCase()(target) } }
    }
}
