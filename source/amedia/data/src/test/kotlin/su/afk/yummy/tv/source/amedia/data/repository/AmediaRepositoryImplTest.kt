package su.afk.yummy.tv.source.amedia.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.data.network.AmediaApi
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import java.io.IOException

class AmediaRepositoryImplTest : BaseUnitTest() {

    private val api: AmediaApi = mockk()
    private fun createRepository() = AmediaRepositoryImpl(api)

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
