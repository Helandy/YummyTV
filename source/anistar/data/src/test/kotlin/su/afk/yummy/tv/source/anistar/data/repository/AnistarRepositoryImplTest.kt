package su.afk.yummy.tv.source.anistar.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anistar.data.dto.AnistarMirrorResponseDto
import su.afk.yummy.tv.source.anistar.data.dto.AnistarTitleResponseDto
import su.afk.yummy.tv.source.anistar.data.network.AnistarApi
import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import java.io.IOException

class AnistarRepositoryImplTest : BaseUnitTest() {

    private val api: AnistarApi = mockk()
    private val lookup = AnistarLookup(1, "A", emptyList(), null, null, null)
    private val playlist = requireNotNull(javaClass.getResourceAsStream("/anistar-playlist.html")).readBytes().decodeToString()
    private val title = AnistarTitle(2658, "https://v31.astar.bz")

    private fun createRepository() = AnistarRepositoryImpl(api)

    private val available = AnistarTitleResponseDto(true, 2658, "https://v31.astar.bz")

    @Test
    fun `title is cached per mal id including the negative answer`() = runTest {
        coEvery { api.findTitle(any()) } returns available
        val repository = createRepository()

        assertEquals(title, repository.findTitle(lookup))
        repository.findTitle(lookup)
        coVerify(exactly = 1) { api.findTitle(any()) }

        coEvery { api.findTitle(any()) } returns AnistarTitleResponseDto(available = false)
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
        assertEquals(title, repository.findTitle(lookup))
    }

    @Test
    fun `episodes come from the playlist and are cached`() = runTest {
        coEvery { api.getPlayerPage("https://v31.astar.bz", 2658) } returns playlist
        val repository = createRepository()

        assertEquals(listOf(391, 392, 393, 4), repository.getEpisodes(title).map { it.number })
        repository.getEpisodes(title)

        coVerify(exactly = 1) { api.getPlayerPage(any(), any()) }
    }

    @Test
    fun `streams are fetched fresh every time with the mirror as referer`() = runTest {
        coEvery { api.findTitle(any()) } returns available
        coEvery { api.getPlayerPage("https://v31.astar.bz", 2658) } returns playlist
        val repository = createRepository()
        repository.findTitle(lookup)

        val first = repository.getStreams(AnistarTarget(2658, 392))
        repository.getStreams(AnistarTarget(2658, 392))

        assertEquals(setOf(360, 720), first?.hlsByQuality?.keys)
        assertEquals("https://v31.astar.bz/", first?.referer)
        coVerify(exactly = 2) { api.getPlayerPage(any(), any()) }
    }

    @Test
    fun `missing episode is null`() = runTest {
        coEvery { api.getMirror() } returns AnistarMirrorResponseDto("https://v31.astar.bz")
        coEvery { api.getPlayerPage(any(), any()) } returns playlist

        assertNull(createRepository().getStreams(AnistarTarget(2658, 999)))
    }

    @Test
    fun `cold start asks the server for the mirror and a dead mirror is refreshed once`() = runTest {
        coEvery { api.getMirror() } returnsMany listOf(
            AnistarMirrorResponseDto("https://v30.astar.bz"),
            AnistarMirrorResponseDto("https://v31.astar.bz"),
        )
        coEvery { api.getPlayerPage("https://v30.astar.bz", 2658) } throws IOException("dead")
        coEvery { api.getPlayerPage("https://v31.astar.bz", 2658) } returns playlist

        val streams = createRepository().getStreams(AnistarTarget(2658, 391))

        assertEquals("https://v31.astar.bz/", streams?.referer)
        coVerify(exactly = 2) { api.getMirror() }
    }

    @Test
    fun `server without a mirror is an error`() = runTest {
        coEvery { api.getMirror() } returns AnistarMirrorResponseDto(null)

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { createRepository().getStreams(AnistarTarget(2658, 1)) }
        }
    }
}
