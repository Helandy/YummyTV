package su.afk.yummy.tv.source.anistar.data.source

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import su.afk.yummy.tv.source.anistar.domain.usecase.GetAnistarEpisodesUseCase
import su.afk.yummy.tv.source.anistar.domain.usecase.ResolveAnistarStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityAnswer
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant
import su.afk.yummy.tv.source.availability.domain.repository.AvailabilityRepository
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AnistarExtraSourceTest : BaseUnitTest() {

    private val repository: AnistarRepository = mockk()
    private val availability: AvailabilityRepository = mockk()
    private val messages = object : AnistarMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(28, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val anistar = SourceAvailability(
        sourceId = "anistar", key = "2658", url = "https://v31.astar.bz/test/player2/videoas_p2p_new.php?id=2658",
        episodesAvailable = 2, episodesTotal = 12, ongoing = true, qualities = emptyList(),
        variants = listOf(SourceVariant("2658", isDubbing = false, episodes = listOf(2, 1).map { SourceEpisode(it, null, null, null) })),
    )

    private fun answer(sources: List<SourceAvailability> = emptyList(), failed: Set<String> = emptySet()) = AvailabilityAnswer(sources, failed)

    private fun videoRepository() = AnistarExtraVideoRepository(FindSourceAvailabilityUseCase(availability), GetAnistarEpisodesUseCase())

    private fun streamRepository() = AnistarExtraStreamRepository(ResolveAnistarStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per episode and lookup carries the app data`() = runTest {
        coEvery { availability.find(AvailabilityLookup(52991, "Фрирен", listOf("Frieren"), 2023, "tv", 28)) } returns answer(listOf(anistar))

        val videos = videoRepository().videos(details(52991))

        assertEquals(listOf("1", "2"), videos.map { it.episode })
        assertEquals("source://anistar/2658/1", videos[0].iframeUrl)
        assertTrue(videos.all { it.player == "AniStar" && it.dubbing == "AniStar" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `no mal id or no title gives no videos`() = runTest {
        assertTrue(videoRepository().videos(details(null)).isEmpty())
        coEvery { availability.find(any()) } returns answer()
        assertTrue(videoRepository().videos(details(1)).isEmpty())
    }

    @Test
    fun `source that failed to answer is an error and not an empty list`() = runTest {
        coEvery { availability.find(any()) } returns answer(failed = setOf("anistar"))

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { videoRepository().videos(details(1)) } }
    }

    @Test
    fun `stream carries ascending qualities, the best one and the referer`() = runTest {
        coEvery { repository.getStreams(any()) } returns AnistarStreams(
            hlsByQuality = mapOf(720 to "https://cdn/720.m3u8", 360 to "https://cdn/360.m3u8"),
            referer = "https://v31.astar.bz/",
        )

        val result = streamRepository().resolve("source://anistar/2658/1") as PlayerStreamResolveResult.Stream

        assertEquals("https://cdn/720.m3u8", result.url)
        assertEquals(listOf("360p", "720p"), result.qualities?.keys?.toList())
        assertEquals(mapOf("Referer" to "https://v31.astar.bz/"), result.headers)
    }

    @Test
    fun `missing episode is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        val unavailable = streamRepository().resolve("source://anistar/2658/9")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://anistar/2658/1") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://anistar/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only anistar urls are supported`() {
        assertTrue(streamRepository().supports("source://anistar/2658/1"))
        assertEquals(false, streamRepository().supports("source://aniboom/0BzM07Pq71J/1/16"))
    }
}
