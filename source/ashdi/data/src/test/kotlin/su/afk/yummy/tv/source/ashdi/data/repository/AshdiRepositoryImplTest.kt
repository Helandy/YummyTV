package su.afk.yummy.tv.source.ashdi.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.ashdi.data.network.AshdiApi
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import java.io.IOException

class AshdiRepositoryImplTest : BaseUnitTest() {

    private val api: AshdiApi = mockk()
    private val pageUrl = "https://ashdi.vip/vod/104245"
    private val masterUrl =
        "https://ashdi.vip/video28/1/serials/fanvox_sousou_no_frieren/frren_knczya_shlyaxu__01_104245/hls/BKaKlHKJjudZnhH+Aoo=/index.m3u8"

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private fun createRepository() = AshdiRepositoryImpl(api)

    @Test
    fun `page gives the master and its qualities`() = runTest {
        coEvery { api.getText(pageUrl) } returns resource("ashdi-page.html")
        coEvery { api.getText(masterUrl) } returns resource("ashdi-master.m3u8")

        val streams = (createRepository().getStreams(pageUrl) as AshdiStreamsResult.Success).streams

        assertEquals(masterUrl, streams.masterUrl)
        assertEquals(listOf("480p", "720p", "1080p"), streams.qualities.keys.toList())
    }

    @Test
    fun `geo stub page means region blocked`() = runTest {
        coEvery { api.getText(pageUrl) } returns "<html><head><title>Змініть країну перегляду</title></head><body>в цій країні</body></html>"

        assertEquals(AshdiStreamsResult.RegionBlocked, createRepository().getStreams(pageUrl))
    }

    @Test
    fun `page without a file and without the stub, a missing page or a foreign url mean not found`() = runTest {
        coEvery { api.getText(pageUrl) } returns "<html>nothing here</html>"
        assertEquals(AshdiStreamsResult.NotFound, createRepository().getStreams(pageUrl))

        coEvery { api.getText(pageUrl) } returns null
        assertEquals(AshdiStreamsResult.NotFound, createRepository().getStreams(pageUrl))

        assertEquals(AshdiStreamsResult.NotFound, createRepository().getStreams("https://evil.example/vod/1"))
    }

    @Test
    fun `network error propagates`() = runTest {
        coEvery { api.getText(any()) } throws IOException("down")

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { createRepository().getStreams(pageUrl) } }
    }
}
