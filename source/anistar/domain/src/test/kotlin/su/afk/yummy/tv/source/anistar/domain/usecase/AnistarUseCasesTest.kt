package su.afk.yummy.tv.source.anistar.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreamsResult
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import java.io.IOException

class AnistarUseCasesTest : BaseUnitTest() {

    private val repository: AnistarRepository = mockk()
    private val target = AnistarTarget(2658, 1)

    @Test
    fun `episodes are sorted and duplicates by number are collapsed`() = runTest {
        val title = AnistarTitle(2658, "https://v31.astar.bz")
        coEvery { repository.getEpisodes(title) } returns listOf(AnistarEpisode(3), AnistarEpisode(1), AnistarEpisode(3), AnistarEpisode(2))

        assertEquals(listOf(1, 2, 3), GetAnistarEpisodesUseCase(repository)(title).map { it.number })
    }

    @Test
    fun `streams become success, missing episode not found, failure failed`() = runTest {
        val streams = AnistarStreams(mapOf(720 to "https://cdn/720.m3u8"), "https://v31.astar.bz/")
        coEvery { repository.getStreams(target) } returns streams
        assertEquals(AnistarStreamsResult.Success(streams), ResolveAnistarStreamsUseCase(repository)(target))

        coEvery { repository.getStreams(target) } returns null
        assertEquals(AnistarStreamsResult.NotFound, ResolveAnistarStreamsUseCase(repository)(target))

        coEvery { repository.getStreams(target) } returns AnistarStreams(emptyMap(), "r")
        assertEquals(AnistarStreamsResult.NotFound, ResolveAnistarStreamsUseCase(repository)(target))

        coEvery { repository.getStreams(target) } throws IOException("down")
        assertTrue(ResolveAnistarStreamsUseCase(repository)(target) is AnistarStreamsResult.Failed)
    }
}
