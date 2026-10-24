package su.afk.yummy.tv.source.anilibria.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaCatalogEpisodeDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaCatalogReleaseDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaEpisodeDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaLookupResponseDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaReleaseDto
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi
import java.io.IOException

class AniLibriaRepositoryImplTest : BaseUnitTest() {

    private val api: AniLibriaApi = mockk()

    private fun createRepository() = AniLibriaRepositoryImpl(api)

    private fun release(id: Int, dubbing: Boolean = false, vararg episodes: Int) = AniLibriaCatalogReleaseDto(
        id = id,
        dubbing = dubbing,
        episodes = episodes.map { AniLibriaCatalogEpisodeDto(ordinal = it, durationSec = 1440, openingStartSec = 5, openingEndSec = 95) },
    )

    @Test
    fun `releases come from the server catalog with episodes and without stream links`() = runTest {
        coEvery { api.findReleases(any()) } returns AniLibriaLookupResponseDto(
            releaseIds = listOf(1, 2),
            releases = listOf(release(1, false, 2, 1), release(2, true, 1)),
        )

        val releases = createRepository().findReleases(52991, listOf("Фрирен", "Sousou no Frieren"), 2023)

        assertEquals(listOf(1, 2), releases.map { it.id })
        assertEquals(listOf(false, true), releases.map { it.isDubbing })
        assertEquals(listOf(1, 2), releases[0].episodes.map { it.ordinal })
        assertEquals(5, releases[0].episodes[0].opening?.startSec)
        assertEquals(1440, releases[0].episodes[0].durationSec)
        assertNull(releases[0].episodes[0].hls720)
        // AniLibria при этом не опрашивается
        coVerify(exactly = 0) { api.getRelease(any()) }
    }

    @Test
    fun `empty server answer means no releases`() = runTest {
        coEvery { api.findReleases(any()) } returns AniLibriaLookupResponseDto()

        assertEquals(emptyList<Any>(), createRepository().findReleases(1, listOf("Some Show"), 2020))
    }

    @Test
    fun `unreachable server is a failure without any fallback to AniLibria`() = runTest {
        coEvery { api.findReleases(any()) } throws IOException("offline")

        assertTrue(runCatching { createRepository().findReleases(1, listOf("Some Show"), 2020) }.exceptionOrNull() is IOException)
        coVerify(exactly = 0) { api.getRelease(any()) }
    }

    @Test
    fun `release whose episodes the server has not read yet is not cached as empty`() = runTest {
        coEvery { api.findReleases(any()) } returnsMany listOf(
            AniLibriaLookupResponseDto(releaseIds = listOf(1, 2), releases = listOf(release(1, false, 1))),
            AniLibriaLookupResponseDto(releaseIds = listOf(1, 2), releases = listOf(release(1, false, 1), release(2, true, 1))),
        )
        val repository = createRepository()

        assertTrue(runCatching { repository.findReleases(5, listOf("Show"), null) }.exceptionOrNull() is IOException)

        assertEquals(listOf(1, 2), repository.findReleases(5, listOf("Show"), null).map { it.id })
    }

    @Test
    fun `result is cached per mal id`() = runTest {
        coEvery { api.findReleases(any()) } returns AniLibriaLookupResponseDto(listOf(1), listOf(release(1, false, 1)))
        val repository = createRepository()

        repository.findReleases(5, listOf("Show"), null)
        repository.findReleases(5, listOf("Show"), null)

        coVerify(exactly = 1) { api.findReleases(any()) }
    }

    @Test
    fun `stream links are requested from AniLibria only by getRelease`() = runTest {
        coEvery { api.getRelease(9542) } returns AniLibriaReleaseDto(
            id = 9542,
            episodes = listOf(AniLibriaEpisodeDto(ordinal = 1.0, hls_720 = "https://cache.libria.fun/a.m3u8")),
        )

        val release = createRepository().getRelease(9542)

        assertEquals("https://cache.libria.fun/a.m3u8", release?.episodes?.single()?.hls720)
    }
}
