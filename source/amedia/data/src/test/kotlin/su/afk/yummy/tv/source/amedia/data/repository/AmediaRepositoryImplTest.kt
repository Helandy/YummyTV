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
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import java.io.IOException

class AmediaRepositoryImplTest : BaseUnitTest() {

    private val api: AmediaApi = mockk()
    private val lookup = AmediaLookup(59088, "Токийские мстители: Битва", listOf("Tokyo Revengers"))
    private val wrongUrl = "https://amd.online/843-a.html"
    private val rightUrl = "https://amd.online/5753-b.html"

    private fun page(malId: Int) = """
        <video-player data-title-id="$malId" data-aggregator="mali"></video-player>
        <a data-vid="1" data-vlnk="https://aser.pro/vod/47997" class="x">1</a>
    """.trimIndent()

    private fun createRepository() = AmediaRepositoryImpl(api)

    @Test
    fun `title is the first search hit with matching mal id and its episodes are reused`() = runTest {
        coEvery { api.search(any()) } returns """<a href="$wrongUrl">a</a><a href="$rightUrl">b</a>"""
        coEvery { api.getTitlePage(wrongUrl) } returns page(42249)
        coEvery { api.getTitlePage(rightUrl) } returns page(59088)
        val repository = createRepository()

        val title = repository.findTitle(lookup)

        assertEquals(AmediaTitle(rightUrl), title)
        assertEquals(listOf(AmediaEpisode(1, 47997)), repository.getEpisodes(title!!))
        coVerify(exactly = 1) { api.getTitlePage(rightUrl) }
    }

    @Test
    fun `no match is cached and network failure is not`() = runTest {
        coEvery { api.search(any()) } returns """<a href="$wrongUrl">a</a>"""
        coEvery { api.getTitlePage(wrongUrl) } returns page(42249)
        val repository = createRepository()

        assertNull(repository.findTitle(lookup))
        repository.findTitle(lookup)
        coVerify(exactly = 1) { api.getTitlePage(wrongUrl) }

        coEvery { api.search(any()) } throws IOException("down")
        assertThrows(IOException::class.java) { runBlocking { createRepository().findTitle(lookup) } }
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
