package su.afk.yummy.tv.source.anilibria.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaEpisode
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaStreamsResult
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaTarget
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository

class ResolveAniLibriaStreamsUseCaseTest : BaseUnitTest() {

    private val repository: AniLibriaRepository = mockk()

    private fun episode(ordinal: Int, hls480: String? = null, hls720: String? = null, hls1080: String? = null) =
        AniLibriaEpisode(ordinal, null, hls480, hls720, hls1080, null, null)

    private fun createUseCase() = ResolveAniLibriaStreamsUseCase(repository)

    @Test
    fun `available qualities are returned ascending`() = runTest {
        coEvery { repository.getRelease(9542) } returns
            AniLibriaRelease(9542, 52991, listOf(episode(1, "a", null, "c")))
        val result = createUseCase()(AniLibriaTarget(9542, 1))
        assertEquals(listOf(480, 1080), (result as AniLibriaStreamsResult.Success).streams.files.keys.toList())
    }

    @Test
    fun `missing release or episode is not found`() = runTest {
        coEvery { repository.getRelease(1) } returns null
        coEvery { repository.getRelease(2) } returns AniLibriaRelease(2, 1, listOf(episode(1, "a")))
        assertTrue(createUseCase()(AniLibriaTarget(1, 1)) is AniLibriaStreamsResult.NotFound)
        assertTrue(createUseCase()(AniLibriaTarget(2, 5)) is AniLibriaStreamsResult.NotFound)
    }

    @Test
    fun `episode without links is not found`() = runTest {
        coEvery { repository.getRelease(3) } returns AniLibriaRelease(3, 1, listOf(episode(1, " ", null, "")))
        assertTrue(createUseCase()(AniLibriaTarget(3, 1)) is AniLibriaStreamsResult.NotFound)
    }
}
