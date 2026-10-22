package su.afk.yummy.tv.source.aniboom.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomDubbingResponseDto
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomTitleResponseDto
import su.afk.yummy.tv.source.aniboom.data.network.AniboomApi
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import java.io.IOException

class AniboomRepositoryImplTest : BaseUnitTest() {

    private val api: AniboomApi = mockk()
    private val lookup = AniboomLookup(1, "A", emptyList(), null, null, null)

    private fun createRepository() = AniboomRepositoryImpl(api)

    private val available = AniboomTitleResponseDto(true, "0BzM07Pq71J", 2, listOf(AniboomDubbingResponseDto("JAM", 16)))

    @Test
    fun `title is cached per mal id including the negative answer`() = runTest {
        coEvery { api.findTitle(any()) } returns available
        val repository = createRepository()

        assertEquals("0BzM07Pq71J", repository.findTitle(lookup)?.aniboomId)
        repository.findTitle(lookup)
        coVerify(exactly = 1) { api.findTitle(any()) }

        coEvery { api.findTitle(any()) } returns AniboomTitleResponseDto(available = false)
        assertNull(repository.findTitle(lookup.copy(malId = 2)))
        repository.findTitle(lookup.copy(malId = 2))
        coVerify(exactly = 2) { api.findTitle(any()) }
    }

    @Test
    fun `network failure is not cached`() = runTest {
        coEvery { api.findTitle(any()) } throws IOException("down")
        val repository = createRepository()

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { repository.findTitle(lookup) } }

        coEvery { api.findTitle(any()) } returns available
        assertEquals("0BzM07Pq71J", repository.findTitle(lookup)?.aniboomId)
    }

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
