package su.afk.yummy.tv.source.animevost.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.animevost.data.network.AnimevostApi
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget
import java.io.IOException

class AnimevostRepositoryImplTest : BaseUnitTest() {

    private val api: AnimevostApi = mockk()
    private val target = AnimevostTarget("tv", "4004-kikansha", 2)
    private val pageUrl = "https://animevost.org/tip/tv/4004-kikansha.html"

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private fun createRepository() = AnimevostRepositoryImpl(api)

    @Test
    fun `chain gives qualities from the episode player`() = runTest {
        coEvery { api.getText(pageUrl) } returns resource("animevost-page.html")
        coEvery { api.getText("https://animevost.org/frame5.php?play=1606941982&old=1") } returns resource("animevost-frame.html")

        val streams = requireNotNull(createRepository().getStreams(target))

        assertEquals(listOf("480p", "720p"), streams.qualities.keys.toList())
    }

    @Test
    fun `no page, no episode or no links gives no stream`() = runTest {
        coEvery { api.getText(pageUrl) } returns null
        assertNull(createRepository().getStreams(target))

        coEvery { api.getText(pageUrl) } returns resource("animevost-page.html")
        assertNull(createRepository().getStreams(target.copy(episode = 9)))

        coEvery { api.getText("https://animevost.org/frame5.php?play=1606941982&old=1") } returns "<html>empty</html>"
        assertNull(createRepository().getStreams(target))
    }

    @Test
    fun `network error propagates`() = runTest {
        coEvery { api.getText(any()) } throws IOException("down")

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { createRepository().getStreams(target) } }
    }
}
