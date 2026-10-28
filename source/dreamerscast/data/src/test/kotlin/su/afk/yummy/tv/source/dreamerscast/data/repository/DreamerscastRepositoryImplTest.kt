package su.afk.yummy.tv.source.dreamerscast.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.dreamerscast.data.network.DreamerscastApi
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget
import java.io.IOException

class DreamerscastRepositoryImplTest : BaseUnitTest() {

    private val api: DreamerscastApi = mockk()
    private val target = DreamerscastTarget("618-tensei-goblin", 1)
    private val pageUrl = "https://dreamerscast.com/home/release/618-tensei-goblin"
    private val masterUrl =
        "https://play.dreamerscast.com/hls/312db18b-c489-4fa4-8ec2-8a79c698822c/88a30fac-2e1f-4390-9a46-6a9e81018ea3_,1080,720,low,aac,.mp4.urlset/master.m3u8"

    private fun resource(name: String) = requireNotNull(javaClass.classLoader?.getResourceAsStream(name)).readBytes().decodeToString()

    private fun createRepository() = DreamerscastRepositoryImpl(api)

    @Test
    fun `chain gives the master and its qualities`() = runTest {
        coEvery { api.getText(pageUrl) } returns resource("dreamerscast-release.html")
        coEvery { api.getText(masterUrl) } returns resource("dreamerscast-master.m3u8")

        val streams = requireNotNull(createRepository().getStreams(target))

        assertEquals(masterUrl, streams.masterUrl)
        assertEquals(listOf("480p", "720p", "1080p"), streams.qualities.keys.toList())
    }

    @Test
    fun `missing page, episode or player gives no stream`() = runTest {
        coEvery { api.getText(pageUrl) } returns null
        assertNull(createRepository().getStreams(target))

        coEvery { api.getText(pageUrl) } returns resource("dreamerscast-release.html")
        assertNull(createRepository().getStreams(target.copy(episode = 9)))

        coEvery { api.getText(pageUrl) } returns "<html>no player</html>"
        assertNull(createRepository().getStreams(target))
    }

    @Test
    fun `network error propagates`() = runTest {
        coEvery { api.getText(any()) } throws IOException("down")

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { createRepository().getStreams(target) } }
    }
}
