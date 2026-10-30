package su.afk.yummy.tv.source.animeon.data.source

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreams
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreamsResult
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.animeon.domain.repository.AnimeOnRepository
import su.afk.yummy.tv.source.animeon.domain.usecase.ResolveAnimeOnStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AnimeOnExtraSourceTest : BaseUnitTest() {

    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val repository: AnimeOnRepository = mockk()
    private val messages = object : AnimeOnMessages {
        override val notFound = "nf-text"
        override val regionBlocked = "geo-text"
    }

    private fun details() = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(28, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = 52991,
    )

    private fun variant(key: String, title: String?, episodes: Int) =
        SourceVariant(key, isDubbing = false, episodes = (1..episodes).map { SourceEpisode(it, null, null, null) }, title = title)

    private val availability = SourceAvailability(
        sourceId = "animeon", key = "924", url = "https://animeon.club/anime/924-frieren", episodesAvailable = 2,
        episodesTotal = 28, ongoing = false, qualities = emptyList(),
        variants = listOf(
            variant("1093:1209", "FanVoxUA (Ashdi)", 2),
            variant("1093:3917", "FanVoxUA (Moon)", 2),
            variant("1093:9061", "FanVoxUA (Fenix)", 2),
            variant("bad", "Broken (Ashdi)", 1),
            variant("1094:1209", null, 1),
        ),
    )

    private fun videoRepository() = AnimeOnExtraVideoRepository(findAvailability)

    private fun streamRepository() = AnimeOnExtraStreamRepository(ResolveAnimeOnStreamsUseCase(repository), messages)

    @Test
    fun `ashdi, fenix and moon voices become videos with their own source urls`() = runTest {
        coEvery {
            findAvailability("animeon", AvailabilityLookup(52991, "Фрирен", listOf("Frieren"), 2023, "tv", 28))
        } returns availability

        val videos = videoRepository().videos(details())

        assertEquals(listOf("1", "2", "1", "2", "1", "2"), videos.map { it.episode })
        assertEquals("source://animeon/924/2/1093/1209", videos[1].iframeUrl)
        assertEquals("source://animeon/924/1/1093/3917", videos[2].iframeUrl)
        assertEquals("source://animeon/924/1/1093/9061", videos[4].iframeUrl)
        assertEquals(List(6) { "FanVoxUA" }, videos.map { it.dubbing })
        assertEquals(listOf("Ashdi", "Ashdi", "Moon", "Moon", "Fenix", "Fenix"), videos.map { it.player })
        assertTrue(videos.all { it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `no title gives no videos`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null

        assertTrue(videoRepository().videos(details()).isEmpty())
    }

    @Test
    fun `stream carries the quality map and plays the best one`() = runTest {
        coEvery { repository.getStreams(AnimeOnTarget("924", 1093, 1209, 2)) } returns
            AnimeOnStreamsResult.Success(
                AnimeOnStreams("https://a/index.m3u8", linkedMapOf("480p" to "https://a/480.m3u8", "1080p" to "https://a/1080.m3u8")),
            )

        val result = streamRepository().resolve("source://animeon/924/2/1093/1209") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/1080.m3u8", result.url)
        assertEquals(listOf("480p", "1080p"), result.qualities?.keys?.toList())
    }

    @Test
    fun `master without variants plays adaptively`() = runTest {
        coEvery { repository.getStreams(any()) } returns AnimeOnStreamsResult.Success(AnimeOnStreams("https://a/index.m3u8"))

        val result = streamRepository().resolve("source://animeon/924/1/1093/1209") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/index.m3u8", result.url)
        assertEquals(null, result.qualities)
    }

    @Test
    fun `stream headers reach the player`() = runTest {
        coEvery { repository.getStreams(any()) } returns
            AnimeOnStreamsResult.Success(AnimeOnStreams("https://s/v.m3u8", headers = mapOf("Referer" to "https://moonanime.art/iframe/a/")))

        val result = streamRepository().resolve("source://animeon/924/1/1093/3917") as PlayerStreamResolveResult.Stream

        assertEquals(mapOf("Referer" to "https://moonanime.art/iframe/a/"), result.headers)
    }

    @Test
    fun `missing file is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns AnimeOnStreamsResult.NotFound
        val unavailable = streamRepository().resolve("source://animeon/924/9/1093/1209")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://animeon/924/1/1093/1209") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://animeon/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `region block is unavailable with its own text`() = runTest {
        coEvery { repository.getStreams(any()) } returns AnimeOnStreamsResult.RegionBlocked

        val result = streamRepository().resolve("source://animeon/924/1/1093/1209")

        assertEquals("geo-text", (result as PlayerStreamResolveResult.Unavailable).message)
    }

    @Test
    fun `only animeon urls are supported`() {
        assertTrue(streamRepository().supports("source://animeon/924/1/1093/1209"))
        assertEquals(false, streamRepository().supports("source://anidub/123-psyren/1"))
    }
}
