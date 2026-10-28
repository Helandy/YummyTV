package su.afk.yummy.tv.source.aniboom.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.aniboom.data.network.AniboomApi
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import java.io.IOException

class AniboomRepositoryImplTest : BaseUnitTest() {

    private val api: AniboomApi = mockk()
    private fun createRepository() = AniboomRepositoryImpl(api)

    @Test
    fun `stream comes from the embed and 404 is null`() = runTest {
        val html = requireNotNull(javaClass.getResourceAsStream("/aniboom-embed.html")).readBytes().decodeToString()
        coEvery { api.getEmbed("https://aniboom.one/embed/0BzM07Pq71J?episode=1&translation=16&episodes=1", "https://animego.me/") } returns html
        coEvery { api.getEmbed("https://aniboom.one/embed/0BzM07Pq71J?episode=9&translation=16&episodes=1", any()) } returns null

        val repository = createRepository()

        assertEquals(
            "https://poppy.ya-ligh123.site/lk/lk8qW4J5Mmo/master.m3u8",
            repository.getStreams(AniboomTarget("0BzM07Pq71J", 1, 16))?.hlsUrl,
        )
        assertNull(repository.getStreams(AniboomTarget("0BzM07Pq71J", 9, 16)))
    }

    @Test
    fun `playlist failure keeps the stream without qualities`() = runTest {
        val html = requireNotNull(javaClass.getResourceAsStream("/aniboom-embed.html")).readBytes().decodeToString()
        coEvery { api.getEmbed(any(), any()) } returns html
        coEvery { api.getPlaylist(any()) } throws IOException("down")

        val streams = createRepository().getStreams(AniboomTarget("0BzM07Pq71J", 1, 16))

        assertEquals("https://poppy.ya-ligh123.site/lk/lk8qW4J5Mmo/master.m3u8", streams?.hlsUrl)
        assertEquals(emptyMap<String, String>(), streams?.qualities)
    }

    @Test
    fun `embed without a link is an error`() = runTest {
        coEvery { api.getEmbed(any(), any()) } returns "<html>no params</html>"

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { createRepository().getStreams(AniboomTarget("0BzM07Pq71J", 1, 16)) }
        }
    }
}
