package su.afk.yummy.tv.source.anilibria.data.source

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaEpisode
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaSkip
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import su.afk.yummy.tv.source.anilibria.domain.usecase.ResolveAniLibriaStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityAnswer
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant
import su.afk.yummy.tv.source.availability.domain.repository.AvailabilityRepository
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AniLibriaExtraSourceTest : BaseUnitTest() {

    private val repository: AniLibriaRepository = mockk()
    private val availability: AvailabilityRepository = mockk()
    private val messages = object : AniLibriaMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = null, ageRating = null,
        views = null, status = null, type = null, episodes = null, otherTitles = listOf("Frieren"), creators = emptyList(),
        studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val release = AniLibriaRelease(
        id = 9542,
        malId = 52991,
        episodes = listOf(
            AniLibriaEpisode(
                1, null, "https://cache.libria.fun/a480.m3u8?x=1", null, "https://other.host/a1080.m3u8", 1560,
                AniLibriaSkip(5, 95),
            ),
            AniLibriaEpisode(2, null, null, "b720", null, null, null),
        ),
    )

    private fun variant(id: Int, dubbing: Boolean, vararg episodes: Int) = SourceVariant(
        key = id.toString(),
        isDubbing = dubbing,
        episodes = episodes.map { SourceEpisode(number = it, durationSec = 1560, openingStartSec = 5, openingEndSec = 95) },
    )

    private fun anilibria(vararg variants: SourceVariant) = SourceAvailability(
        sourceId = "anilibria", key = variants.first().key, url = "https://x", episodesAvailable = 2,
        episodesTotal = null, ongoing = false, qualities = emptyList(), variants = variants.toList(),
    )

    private fun answer(sources: List<SourceAvailability> = emptyList(), failed: Set<String> = emptySet()) = AvailabilityAnswer(sources, failed)

    private fun videoRepository() = AniLibriaExtraVideoRepository(FindSourceAvailabilityUseCase(availability))

    private fun streamRepository() =
        AniLibriaExtraStreamRepository(ResolveAniLibriaStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per episode from the variants with source urls and skips`() = runTest {
        coEvery { availability.find(any()) } returns answer(listOf(anilibria(variant(9542, false, 2, 1), variant(9600, true, 1))))

        val videos = videoRepository().videos(details(52991))

        assertEquals(listOf("2", "1", "1").sorted(), videos.map { it.episode }.sorted())
        val first = videos.first { it.iframeUrl == "source://anilibria/9542/1" }
        assertEquals("Озвучка AniLibria", first.dubbing)
        assertEquals(5_000L, first.skips.opening?.startMs)
        assertEquals(95_000L, first.skips.opening?.endMs)
        assertEquals(1560, first.durationSeconds)
        assertEquals("Дубляж AniLibria", videos.first { it.iframeUrl == "source://anilibria/9600/1" }.dubbing)
        assertTrue(videos.all { it.id < 0 })
        assertTrue(videoRepository().requiresMatchingDubbing)
    }

    @Test
    fun `lookup carries mal id and the app data`() = runTest {
        coEvery { availability.find(any()) } returns answer()

        videoRepository().videos(details(52991))

        io.mockk.coVerify { availability.find(AvailabilityLookup(52991, "Фрирен", listOf("Frieren"), null, null, null)) }
    }

    @Test
    fun `no mal id or no source gives no videos`() = runTest {
        assertTrue(videoRepository().videos(details(null)).isEmpty())
        coEvery { availability.find(any()) } returns answer()
        assertTrue(videoRepository().videos(details(1)).isEmpty())
    }

    @Test
    fun `source that failed to answer is an error and not an empty list`() = runTest {
        coEvery { availability.find(any()) } returns answer(failed = setOf("anilibria"))

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { videoRepository().videos(details(1)) } }
    }

    @Test
    fun `variant whose key is not a release id is skipped`() = runTest {
        coEvery { availability.find(any()) } returns answer(listOf(anilibria(variant(9542, false, 1), SourceVariant("abc", false, emptyList()))))

        assertEquals(listOf("source://anilibria/9542/1"), videoRepository().videos(details(1)).map { it.iframeUrl })
    }

    @Test
    fun `stream resolves best quality last`() = runTest {
        coEvery { repository.getRelease(9542) } returns release
        val result = streamRepository().resolve("source://anilibria/9542/1") as PlayerStreamResolveResult.Stream
        assertEquals("https://other.host/a1080.m3u8", result.url)
        assertEquals(listOf("480p", "1080p"), result.qualities?.keys?.toList())
        assertEquals("https://cache-rfn.libria.fun/a480.m3u8?x=1", result.qualities?.get("480p"))
    }

    @Test
    fun `unknown episode is unavailable and bad url fails`() = runTest {
        coEvery { repository.getRelease(9542) } returns release
        val unavailable = streamRepository().resolve("source://anilibria/9542/7")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)
        assertTrue(streamRepository().resolve("source://anilibria/x") is PlayerStreamResolveResult.Failed)
    }
}
