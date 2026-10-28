package su.afk.yummy.tv.source.animebesst.data.source

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
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstStreams
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget
import su.afk.yummy.tv.source.animebesst.domain.repository.AnimebesstRepository
import su.afk.yummy.tv.source.animebesst.domain.usecase.ResolveAnimebesstStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AnimebesstExtraSourceTest : BaseUnitTest() {

    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val repository: AnimebesstRepository = mockk()
    private val messages = object : AnimebesstMessages {
        override val notFound = "nf-text"
    }

    private fun details() = AnimeDetails(
        id = 1, animeUrl = "u", title = "Кикансha", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2024, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(12, 4, null, null), otherTitles = listOf("Kikansha"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = 7,
    )

    private val availability = SourceAvailability(
        sourceId = "animebesst", key = "957-kikansha", url = "https://anime1.best/anime1-online/957-kikansha.html",
        episodesAvailable = 3, episodesTotal = 12, ongoing = true, qualities = emptyList(),
        mirrors = listOf("https://ab2.animebesst.org/anime1-online/957-kikansha.html"),
    )

    private fun videoRepository() = AnimebesstExtraVideoRepository(findAvailability)

    private fun streamRepository() = AnimebesstExtraStreamRepository(ResolveAnimebesstStreamsUseCase(repository), messages)

    @Test
    fun `videos carry the site hosts so a mirror can be used at play time`() = runTest {
        coEvery { findAvailability("animebesst", AvailabilityLookup(7, "Кикансha", listOf("Kikansha"), 2024, "tv", 12)) } returns availability

        val videos = videoRepository().videos(details())

        assertEquals(listOf("1", "2", "3"), videos.map { it.episode })
        assertEquals("source://animebesst/957-kikansha/2?h=anime1.best,ab2.animebesst.org", videos[1].iframeUrl)
        assertTrue(videos.all { it.player == "Animebesst" && it.dubbing == "Animebesst" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `no title or unknown page url gives no videos`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null
        assertTrue(videoRepository().videos(details()).isEmpty())

        coEvery { findAvailability(any(), any()) } returns availability.copy(url = "https://anime1.best/")
        assertTrue(videoRepository().videos(details()).isEmpty())
    }

    @Test
    fun `stream plays the best quality and carries the map`() = runTest {
        coEvery { repository.getStreams(AnimebesstTarget("957-kikansha", 2, listOf("anime1.best"))) } returns
            AnimebesstStreams(linkedMapOf("720p" to "https://a/720.m3u8", "1080p" to "https://a/1080.m3u8"), "https://a/index.m3u8")

        val result = streamRepository().resolve("source://animebesst/957-kikansha/2?h=anime1.best") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/1080.m3u8", result.url)
        assertEquals(listOf("720p", "1080p"), result.qualities?.keys?.toList())
    }

    @Test
    fun `master without variants plays adaptively`() = runTest {
        coEvery { repository.getStreams(any()) } returns AnimebesstStreams(emptyMap(), "https://a/index.m3u8")

        val result = streamRepository().resolve("source://animebesst/957-kikansha/1?h=anime1.best") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/index.m3u8", result.url)
        assertEquals(null, result.qualities)
    }

    @Test
    fun `missing episode is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        val unavailable = streamRepository().resolve("source://animebesst/957-kikansha/9?h=anime1.best")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://animebesst/957-kikansha/1?h=anime1.best") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://animebesst/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only animebesst urls are supported`() {
        assertTrue(streamRepository().supports("source://animebesst/957-kikansha/1?h=anime1.best"))
        assertEquals(false, streamRepository().supports("source://animevost/tv/4004-kikansha/1"))
    }
}
