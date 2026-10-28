package su.afk.yummy.tv.source.animebesst.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.animebesst.data.network.AnimebesstApi
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget
import java.io.IOException

class AnimebesstRepositoryImplTest : BaseUnitTest() {

    private val api: AnimebesstApi = mockk()
    private val target = AnimebesstTarget("957-kikansha", 1, listOf("anime1.best", "ab2.animebesst.org"))
    private val mainPage = "https://anime1.best/anime1-online/957-kikansha.html"
    private val mirrorPage = "https://ab2.animebesst.org/anime1-online/957-kikansha.html"
    private val playerUrl = "https://plus4.aff1best.org/vod/96032"
    private val masterUrl =
        "https://plus4.aff1best.org/content/vod/serials/kikansha_no_mahou_wa_tokubetsu_desu/s02/kikansha_no_mahou_wa_tokubetsu_desu_1/hls/index.m3u8"

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private fun createRepository() = AnimebesstRepositoryImpl(api)

    private fun givenPlayer() {
        coEvery { api.getText(playerUrl) } returns resource("animebesst-player.html")
        coEvery { api.getText(masterUrl) } returns resource("animebesst-master.m3u8")
    }

    @Test
    fun `chain gives qualities resolved against the master`() = runTest {
        coEvery { api.getText(mainPage) } returns resource("animebesst-page.html")
        givenPlayer()

        val streams = requireNotNull(createRepository().getStreams(target))

        assertEquals(masterUrl, streams.masterUrl)
        assertEquals(listOf("720p", "1080p"), streams.qualities.keys.toList())
        assertEquals(masterUrl.replace("index.m3u8", "720/index.m3u8"), streams.qualities["720p"])
    }

    @Test
    fun `a mirror is used when the main host fails`() = runTest {
        coEvery { api.getText(mainPage) } throws IOException("timeout")
        coEvery { api.getText(mirrorPage) } returns resource("animebesst-page.html")
        givenPlayer()

        assertEquals(masterUrl, requireNotNull(createRepository().getStreams(target)).masterUrl)
    }

    @Test
    fun `network error on every host propagates, a missing page on every host is not found`() = runTest {
        coEvery { api.getText(any()) } throws IOException("down")
        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { createRepository().getStreams(target) } }

        coEvery { api.getText(any()) } returns null
        assertNull(createRepository().getStreams(target))
    }

    @Test
    fun `missing episode or player file gives no stream`() = runTest {
        coEvery { api.getText(mainPage) } returns resource("animebesst-page.html")
        assertNull(createRepository().getStreams(target.copy(episode = 9)))

        coEvery { api.getText(playerUrl) } returns "<html></html>"
        assertNull(createRepository().getStreams(target))
    }
}
