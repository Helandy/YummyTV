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
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaEpisodeDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaReleaseDto
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AniLibriaRepositoryImplTest : BaseUnitTest() {

    private val api: AniLibriaApi = mockk()
    private val findAvailability: FindSourceAvailabilityUseCase = mockk()

    private fun createRepository() = AniLibriaRepositoryImpl(api, findAvailability)

    private fun variant(id: Int, dubbing: Boolean = false, vararg episodes: Int) = SourceVariant(
        key = id.toString(),
        isDubbing = dubbing,
        episodes = episodes.map { SourceEpisode(number = it, durationSec = 1440, openingStartSec = 5, openingEndSec = 95) },
    )

    private fun availability(vararg variants: SourceVariant) = SourceAvailability(
        sourceId = "anilibria", key = variants.first().key, url = "https://x", episodesAvailable = 2,
        episodesTotal = null, ongoing = false, qualities = emptyList(), variants = variants.toList(),
    )

    @Test
    fun `releases come from the availability answer with episodes and without stream links`() = runTest {
        coEvery { findAvailability("anilibria", any()) } returns availability(variant(1, false, 2, 1), variant(2, true, 1))

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
    fun `lookup carries mal id, the latin title first and the year`() = runTest {
        coEvery { findAvailability("anilibria", any()) } returns null

        createRepository().findReleases(52991, listOf("Фрирен", "Sousou no Frieren"), 2023)

        coVerify { findAvailability("anilibria", AvailabilityLookup(52991, "Sousou no Frieren", listOf("Фрирен"), 2023, null, null)) }
    }

    @Test
    fun `source without the title means no releases`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null

        assertEquals(emptyList<Any>(), createRepository().findReleases(1, listOf("Some Show"), 2020))
    }

    @Test
    fun `unreachable server is a failure without any fallback to AniLibria`() = runTest {
        coEvery { findAvailability(any(), any()) } throws IOException("offline")

        assertTrue(runCatching { createRepository().findReleases(1, listOf("Some Show"), 2020) }.exceptionOrNull() is IOException)
        coVerify(exactly = 0) { api.getRelease(any()) }
    }

    @Test
    fun `source that did not answer is not cached as empty`() = runTest {
        coEvery { findAvailability(any(), any()) } throws IOException("не ответил") andThen availability(variant(1, false, 1), variant(2, true, 1))
        val repository = createRepository()

        assertTrue(runCatching { repository.findReleases(5, listOf("Show"), null) }.exceptionOrNull() is IOException)

        assertEquals(listOf(1, 2), repository.findReleases(5, listOf("Show"), null).map { it.id })
    }

    @Test
    fun `result is cached per mal id`() = runTest {
        coEvery { findAvailability(any(), any()) } returns availability(variant(1, false, 1))
        val repository = createRepository()

        repository.findReleases(5, listOf("Show"), null)
        repository.findReleases(5, listOf("Show"), null)

        coVerify(exactly = 1) { findAvailability(any(), any()) }
    }

    @Test
    fun `variant whose key is not a release id is skipped`() = runTest {
        coEvery { findAvailability(any(), any()) } returns availability(variant(1, false, 1), SourceVariant("abc", false, emptyList()))

        assertEquals(listOf(1), createRepository().findReleases(5, listOf("Show"), null).map { it.id })
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
