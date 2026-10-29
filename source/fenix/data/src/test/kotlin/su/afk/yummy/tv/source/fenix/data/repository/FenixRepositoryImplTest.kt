package su.afk.yummy.tv.source.fenix.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.fenix.data.network.FenixApi
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreams
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import java.io.IOException

class FenixRepositoryImplTest : BaseUnitTest() {

    private val api: FenixApi = mockk()

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private fun createRepository() = FenixRepositoryImpl(api)

    @Before
    fun mainHostIsReachable() {
        coEvery { api.isReachable(any(), any()) } returns true
    }

    @Test
    fun `embed page gives the signed master of the chosen track`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/embed/abc.def") } returns resource("fenix-embed-page.html")

        val result = createRepository().getStreams("https://fenixplay.xyz/embed/abc.def?ep=2&edition=tv", "FanVoxUa")

        assertEquals(
            FenixStreamsResult.Success(
                FenixStreams("https://media.fenixplay.xyz/anime/friren/ep002/fanvox/master.m3u8?v=2&token=media.tok.sig"),
            ),
            result,
        )
    }

    @Test
    fun `slow main host gives the master on the edge host`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/embed/abc.def") } returns resource("fenix-embed-page.html")
        coEvery { api.isReachable("https://media.fenixplay.xyz/anime/friren/ep002/fanvox/master.m3u8?v=2&token=media.tok.sig", any()) } returns false
        coEvery { api.isReachable("https://cf-media.fenixplay.xyz/anime/friren/ep002/fanvox/master.m3u8?v=2&edge=edge.tok.sig", any()) } returns true

        val result = createRepository().getStreams("https://fenixplay.xyz/embed/abc.def?ep=2&edition=tv", "FanVoxUa")

        assertEquals(
            FenixStreamsResult.Success(
                FenixStreams("https://cf-media.fenixplay.xyz/anime/friren/ep002/fanvox/master.m3u8?v=2&edge=edge.tok.sig"),
            ),
            result,
        )
    }

    @Test
    fun `unreachable hosts fall back to the main master`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/embed/abc.def") } returns resource("fenix-embed-page.html")
        coEvery { api.isReachable(any(), any()) } returns false

        val result = createRepository().getStreams("https://fenixplay.xyz/embed/abc.def?ep=2&edition=tv", "FanVoxUa")

        assertEquals(
            FenixStreamsResult.Success(
                FenixStreams("https://media.fenixplay.xyz/anime/friren/ep002/fanvox/master.m3u8?v=2&token=media.tok.sig"),
            ),
            result,
        )
    }

    @Test
    fun `page without an edge keeps the main master and probes nothing`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/vod/qtv.sig") } returns resource("fenix-vod-page.html")

        val result = createRepository().getStreams("https://fenixplay.xyz/vod/qtv.sig", null)
        coVerify(exactly = 0) { api.isReachable(any(), any()) }

        assertEquals(
            FenixStreamsResult.Success(
                FenixStreams("https://media.fenixplay.xyz/anime/naruto/ep001/qtv/master.m3u8?v=9&token=vod.tok.sig"),
            ),
            result,
        )
    }

    @Test
    fun `vod page is read without the fragment and plays its own track`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/vod/qtv.sig") } returns resource("fenix-vod-page.html")

        val result = createRepository().getStreams("https://fenixplay.xyz/vod/qtv.sig#embed=x&ep=1", null)

        assertEquals(
            FenixStreamsResult.Success(
                FenixStreams("https://media.fenixplay.xyz/anime/naruto/ep001/qtv/master.m3u8?v=9&token=vod.tok.sig"),
            ),
            result,
        )
    }

    @Test
    fun `indexed page takes the streams of its own track from the playback api`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/vod/own.sig") } returns resource("fenix-vod-page-indexed.html")
        coEvery {
            api.getPlayback("/api/playback/chorna-konyushyna-sezon-2/episodes/tv/1", "playback.tok.sig")
        } returns resource("fenix-playback-episode.json")

        val result = createRepository().getStreams("https://fenixplay.xyz/vod/own.sig#embed=x&ep=1", null)

        assertEquals(
            FenixStreamsResult.Success(
                FenixStreams("https://media.fenixplay.xyz/anime/s2/ep001/own/master.m3u8?v=3&token=vod.tok.sig"),
            ),
            result,
        )
    }

    @Test
    fun `indexed page without the playback episode means not found`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/vod/own.sig") } returns resource("fenix-vod-page-indexed.html")
        coEvery { api.getPlayback(any(), any()) } returns null

        assertEquals(FenixStreamsResult.NotFound, createRepository().getStreams("https://fenixplay.xyz/vod/own.sig", null))
    }

    @Test
    fun `missing page, unknown episode, page without a player and a foreign url mean not found`() = runTest {
        coEvery { api.getText("https://fenixplay.xyz/embed/abc.def") } returns null
        assertEquals(FenixStreamsResult.NotFound, createRepository().getStreams("https://fenixplay.xyz/embed/abc.def?ep=1", null))

        coEvery { api.getText("https://fenixplay.xyz/embed/abc.def") } returns resource("fenix-embed-page.html")
        assertEquals(FenixStreamsResult.NotFound, createRepository().getStreams("https://fenixplay.xyz/embed/abc.def?ep=99", null))
        assertEquals(FenixStreamsResult.NotFound, createRepository().getStreams("https://fenixplay.xyz/embed/abc.def", null))

        coEvery { api.getText("https://fenixplay.xyz/embed/abc.def") } returns "<html>nothing here</html>"
        assertEquals(FenixStreamsResult.NotFound, createRepository().getStreams("https://fenixplay.xyz/embed/abc.def?ep=1", null))

        assertEquals(FenixStreamsResult.NotFound, createRepository().getStreams("https://evil.example/embed/abc", null))
    }

    @Test
    fun `network error is thrown for the use case to wrap`() = runTest {
        coEvery { api.getText(any()) } throws IOException("down")

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { createRepository().getStreams("https://fenixplay.xyz/vod/a.b", null) }
        }
    }
}
