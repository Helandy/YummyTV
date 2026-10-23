package su.afk.yummy.tv.source.amedia.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreamsResult
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import java.io.IOException

class AmediaUseCasesTest : BaseUnitTest() {

    private val repository: AmediaRepository = mockk()
    private val target = AmediaTarget(47997)

    private fun episode(number: Int, vodId: Long) = AmediaEpisode(number, AmediaEpisodeSource.Vod(vodId))

    @Test
    fun `episodes are sorted and duplicates by number are collapsed`() {
        val title = AmediaTitle(
            "https://amd.online/5753-x.html",
            listOf(episode(3, 30), episode(1, 10), episode(3, 31), episode(2, 20)),
        )

        assertEquals(listOf(1, 2, 3), GetAmediaEpisodesUseCase()(title).map { it.number })
        assertEquals(episode(3, 30), GetAmediaEpisodesUseCase()(title).last())
    }

    @Test
    fun `streams become success, missing stream not found, failure failed`() = runTest {
        val streams = AmediaStreams("https://aser.pro/a/index.m3u8", mapOf(720 to "https://aser.pro/a/720/index.m3u8"))
        coEvery { repository.getStreams(target) } returns streams
        assertEquals(AmediaStreamsResult.Success(streams), ResolveAmediaStreamsUseCase(repository)(target))

        coEvery { repository.getStreams(target) } returns null
        assertEquals(AmediaStreamsResult.NotFound, ResolveAmediaStreamsUseCase(repository)(target))

        coEvery { repository.getStreams(target) } throws IOException("down")
        assertTrue(ResolveAmediaStreamsUseCase(repository)(target) is AmediaStreamsResult.Failed)
    }
}
