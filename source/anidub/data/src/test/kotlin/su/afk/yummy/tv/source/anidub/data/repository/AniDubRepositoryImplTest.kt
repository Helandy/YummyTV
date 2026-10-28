package su.afk.yummy.tv.source.anidub.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.source.anidub.data.network.AniDubApi
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget
import java.io.IOException

class AniDubRepositoryImplTest : BaseUnitTest() {

    private val api: AniDubApi = mockk()
    private val userAgents = object : BrowserUserAgentProvider {
        override val userAgent = "UA-TEST"
    }
    private val target = AniDubTarget("12254-blich", 3)
    private val host = "https://player26.ladonyvesna2005.info"
    private val site = "https://online.anidub.com/"
    private val masterUrl = "$host/vid.php?v=/s11/8bb973f5692359d62675ab918277b609"

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private fun createRepository() = AniDubRepositoryImpl(api, userAgents)

    /** Мастер с узлом [node]: ссылки на варианты у него одинаковые, как у настоящего плеера. */
    private fun master(node: String) = """
        #EXTM3U
        #EXT-X-STREAM-INF:BANDWIDTH=3139300,RESOLUTION=1280x720
        https://$node/vod9/s11/x/fhd.mp4/chunk.m3u8?md5=A&expires=1
        #EXT-X-STREAM-INF:BANDWIDTH=1639300,RESOLUTION=854x480
        https://$node/vod9/s11/x/sd.mp4/chunk.m3u8?md5=B&expires=1
    """.trimIndent()

    private fun fhd(node: String) = "https://$node/vod9/s11/x/fhd.mp4/chunk.m3u8?md5=A&expires=1"

    private fun givenPageAndPlaylist() {
        coEvery { api.getText("https://online.anidub.com/12254-blich.html", null, "UA-TEST") } returns resource("anidub-page.html")
        coEvery { api.getText("$host/index.php?v=/s10/87925c141377bb17a5ecd1a06374b524&playlist", site, "UA-TEST") } returns
            resource("anidub-playlist.html")
    }

    private fun givenMasters(vararg nodes: String) {
        var index = 0
        coEvery { api.getText(masterUrl, "$host/", "UA-TEST") } answers { master(nodes[minOf(index++, nodes.lastIndex)]) }
    }

    private fun givenProbe(node: String, body: String?) {
        coEvery { api.getText(fhd(node), "$host/", "UA-TEST") } returns body
    }

    @Test
    fun `chain gives qualities, master url and headers carrying the same user agent`() = runTest {
        givenPageAndPlaylist()
        givenMasters("cdn5.anivids.link")
        givenProbe("cdn5.anivids.link", "#EXTM3U\n#EXTINF:10,")

        val streams = requireNotNull(createRepository().getStreams(target))

        assertEquals(listOf("480p", "1080p"), streams.qualities.keys.toList())
        assertEquals(masterUrl, streams.masterUrl)
        assertEquals("UA-TEST", streams.headers["User-Agent"])
        assertEquals("$host/", streams.headers["Referer"])
        assertEquals(host, streams.headers["Origin"])
        assertEquals("*/*", streams.headers["Accept"])
    }

    @Test
    fun `known bad node is skipped and the master is asked again`() = runTest {
        givenPageAndPlaylist()
        givenMasters("cdn1.anivids.link", "cdn5.anivids.link")
        givenProbe("cdn5.anivids.link", "#EXTM3U")

        val streams = requireNotNull(createRepository().getStreams(target))

        assertTrue(streams.qualities.values.all { it.contains("cdn5.anivids.link") })
        coVerify(exactly = 2) { api.getText(masterUrl, "$host/", "UA-TEST") }
        coVerify(exactly = 0) { api.getText(fhd("cdn1.anivids.link"), any(), any()) }
    }

    @Test
    fun `node that fails the probe is remembered as dead and skipped next time`() = runTest {
        givenPageAndPlaylist()
        givenMasters("cdn3.anivids.link", "cdn5.anivids.link", "cdn3.anivids.link", "cdn5.anivids.link")
        coEvery { api.getText(fhd("cdn3.anivids.link"), "$host/", "UA-TEST") } throws IOException("403")
        givenProbe("cdn5.anivids.link", "#EXTM3U")
        val repository = createRepository()

        repository.getStreams(target)
        repository.getStreams(target)

        coVerify(exactly = 1) { api.getText(fhd("cdn3.anivids.link"), any(), any()) }
        coVerify(exactly = 1) { api.getText(fhd("cdn5.anivids.link"), any(), any()) }
    }

    @Test
    fun `when only a bad node ever answers its master is still returned after the attempts`() = runTest {
        givenPageAndPlaylist()
        givenMasters("cdn1.anivids.link")

        val streams = requireNotNull(createRepository().getStreams(target))

        assertTrue(streams.qualities.values.all { it.contains("cdn1.anivids.link") })
        coVerify(exactly = 8) { api.getText(masterUrl, "$host/", "UA-TEST") }
    }

    @Test
    fun `missing page, title without a player and absent episode are not found`() = runTest {
        coEvery { api.getText(any(), any(), any()) } returns null
        assertNull(createRepository().getStreams(target))

        coEvery { api.getText("https://online.anidub.com/12254-blich.html", null, "UA-TEST") } returns "<html>sibnet only</html>"
        assertNull(createRepository().getStreams(target))

        givenPageAndPlaylist()
        assertNull(createRepository().getStreams(target.copy(episode = 9)))
    }

    @Test
    fun `network error of the master is not swallowed`() = runTest {
        givenPageAndPlaylist()
        coEvery { api.getText(masterUrl, "$host/", "UA-TEST") } throws IOException("down")

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { createRepository().getStreams(target) } }
    }
}
