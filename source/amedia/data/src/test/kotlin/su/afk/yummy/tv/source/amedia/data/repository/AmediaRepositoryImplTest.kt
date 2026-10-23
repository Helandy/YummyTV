package su.afk.yummy.tv.source.amedia.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.data.network.AmediaApi
import su.afk.yummy.tv.source.amedia.data.dto.AmediaEpisodeDto
import su.afk.yummy.tv.source.amedia.data.dto.AmediaTitleResponseDto
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import java.io.IOException

class AmediaRepositoryImplTest : BaseUnitTest() {

    private val api: AmediaApi = mockk()
    private val lookup = AmediaLookup(59088, "Токийские мстители: Битва", listOf("Tokyo Revengers"), 2026, "tv", 12)
    private val pageUrl = "https://amd.online/5753-b.html"

    private val serverEpisodes = listOf(AmediaEpisodeDto(1, vodId = 47997))
    private val title = AmediaTitle(pageUrl, listOf(AmediaEpisode(1, AmediaEpisodeSource.Vod(47997))))

    private fun createRepository() = AmediaRepositoryImpl(api)

    @Test
    fun `title comes from the server and is cached per mal id including the negative answer`() = runTest {
        coEvery { api.findTitle(any()) } returns AmediaTitleResponseDto(true, pageUrl, serverEpisodes)
        val repository = createRepository()

        assertEquals(title, repository.findTitle(lookup))
        repository.findTitle(lookup)
        coVerify(exactly = 1) { api.findTitle(any()) }

        coEvery { api.findTitle(any()) } returns AmediaTitleResponseDto(available = false)
        assertNull(repository.findTitle(lookup.copy(malId = 2)))
        repository.findTitle(lookup.copy(malId = 2))
        coVerify(exactly = 2) { api.findTitle(any()) }
    }

    @Test
    fun `server failure is not cached`() = runTest {
        coEvery { api.findTitle(any()) } throws IOException("down")
        val repository = createRepository()

        assertThrows(IOException::class.java) { runBlocking { repository.findTitle(lookup) } }

        coEvery { api.findTitle(any()) } returns AmediaTitleResponseDto(true, pageUrl, serverEpisodes)
        assertEquals(title, repository.findTitle(lookup))
    }

    @Test
    fun `title the server could not read episodes for is a failure and is not cached`() = runTest {
        coEvery { api.findTitle(any()) } returns AmediaTitleResponseDto(true, pageUrl, episodes = null)
        val repository = createRepository()

        assertThrows(IOException::class.java) { runBlocking { repository.findTitle(lookup) } }

        coEvery { api.findTitle(any()) } returns AmediaTitleResponseDto(true, pageUrl, serverEpisodes)
        assertEquals(title, repository.findTitle(lookup))
    }

    @Test
    fun `streams carry parsed variants and survive a broken master playlist`() = runTest {
        val master = "https://aser.pro/a/hls/index.m3u8"
        coEvery { api.getVodPage(47997) } returns """Playerjs({file: "$master"})"""
        coEvery { api.getPlaylist(master) } returns "#EXT-X-STREAM-INF:RESOLUTION=1280x720\n720/index.m3u8"
        val repository = createRepository()

        assertEquals(mapOf(720 to "https://aser.pro/a/hls/720/index.m3u8"), repository.getStreams(AmediaTarget(47997))!!.hlsByQuality)

        coEvery { api.getPlaylist(master) } throws IOException("down")
        val degraded = repository.getStreams(AmediaTarget(47997))!!
        assertEquals(master, degraded.masterUrl)
        assertEquals(emptyMap<Int, String>(), degraded.hlsByQuality)
    }

    @Test
    fun `vod page without a file gives no streams`() = runTest {
        coEvery { api.getVodPage(1) } returns "<html></html>"

        assertNull(createRepository().getStreams(AmediaTarget(1)))
    }
}
