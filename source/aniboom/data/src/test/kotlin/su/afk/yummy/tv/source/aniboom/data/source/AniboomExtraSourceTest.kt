package su.afk.yummy.tv.source.aniboom.data.source

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
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreams
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import su.afk.yummy.tv.source.aniboom.domain.usecase.ResolveAniboomStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AniboomExtraSourceTest : BaseUnitTest() {

    private val repository: AniboomRepository = mockk()
    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val messages = object : AniboomMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(28, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private fun variant(key: String, title: String?, vararg episodes: Int) =
        SourceVariant(key, isDubbing = false, episodes = episodes.map { SourceEpisode(it, null, null, null) }, title = title)

    private val availability = SourceAvailability(
        sourceId = "aniboom", key = "0BzM07Pq71J", url = "https://animego.me/anime/x", episodesAvailable = 2, episodesTotal = 28,
        ongoing = false, qualities = emptyList(),
        variants = listOf(variant("16", "JAM CLUB", 1, 2), variant("30", "Dream Cast", 1, 2), variant("x", "Bad", 1), variant("5", null, 1)),
    )

    private fun videoRepository() =
        AniboomExtraVideoRepository(findAvailability)

    private fun streamRepository() = AniboomExtraStreamRepository(ResolveAniboomStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per dubbing and episode, unusable variants are skipped`() = runTest {
        coEvery { findAvailability("aniboom", AvailabilityLookup(52991, "Фрирен", listOf("Frieren"), 2023, "tv", 28)) } returns availability

        val videos = videoRepository().videos(details(52991))

        assertEquals(4, videos.size)
        assertEquals(listOf("1", "2", "1", "2"), videos.map { it.episode })
        assertEquals(listOf("JAM CLUB", "JAM CLUB", "Dream Cast", "Dream Cast"), videos.map { it.dubbing })
        assertEquals("source://aniboom/0BzM07Pq71J/1/16", videos[0].iframeUrl)
        assertEquals("source://aniboom/0BzM07Pq71J/2/30", videos[3].iframeUrl)
        assertTrue(videos.all { it.player == "AniBoom" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `no title gives no videos`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null
        assertTrue(videoRepository().videos(details(1)).isEmpty())
    }

    @Test
    fun `stream is the master playlist without headers or fixed qualities`() = runTest {
        coEvery { repository.getStreams(any()) } returns AniboomStreams("https://cdn/master.m3u8")

        val result = streamRepository().resolve("source://aniboom/0BzM07Pq71J/1/16") as PlayerStreamResolveResult.Stream

        assertEquals("https://cdn/master.m3u8", result.url)
        assertTrue(result.headers.isEmpty())
        assertEquals(null, result.qualities)
    }

    @Test
    fun `missing episode is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        val unavailable = streamRepository().resolve("source://aniboom/0BzM07Pq71J/9/16")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://aniboom/0BzM07Pq71J/1/16") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://aniboom/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only aniboom urls are supported`() {
        assertTrue(streamRepository().supports("source://aniboom/0BzM07Pq71J/1/16"))
        assertEquals(false, streamRepository().supports("source://anime4k/1/1/1/x"))
    }
}
