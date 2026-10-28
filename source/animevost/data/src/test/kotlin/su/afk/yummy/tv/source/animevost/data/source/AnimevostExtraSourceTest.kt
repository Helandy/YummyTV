package su.afk.yummy.tv.source.animevost.data.source

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
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostStreams
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget
import su.afk.yummy.tv.source.animevost.domain.repository.AnimevostRepository
import su.afk.yummy.tv.source.animevost.domain.usecase.ResolveAnimevostStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AnimevostExtraSourceTest : BaseUnitTest() {

    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val repository: AnimevostRepository = mockk()
    private val messages = object : AnimevostMessages {
        override val notFound = "nf-text"
    }

    private fun details() = AnimeDetails(
        id = 1, animeUrl = "u", title = "Кикансha", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2024, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(12, 4, null, null), otherTitles = listOf("Kikansha"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = 7,
    )

    private val availability = SourceAvailability(
        sourceId = "animevost", key = "4004-kikansha", url = "https://animevost.org/tip/tv/4004-kikansha.html",
        episodesAvailable = 3, episodesTotal = 12, ongoing = true, qualities = emptyList(),
    )

    private fun videoRepository() = AnimevostExtraVideoRepository(findAvailability)

    private fun streamRepository() = AnimevostExtraStreamRepository(ResolveAnimevostStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per available episode from the page url`() = runTest {
        coEvery { findAvailability("animevost", AvailabilityLookup(7, "Кикансha", listOf("Kikansha"), 2024, "tv", 12)) } returns availability

        val videos = videoRepository().videos(details())

        assertEquals(listOf("1", "2", "3"), videos.map { it.episode })
        assertEquals("source://animevost/tv/4004-kikansha/2", videos[1].iframeUrl)
        assertTrue(videos.all { it.player == "AnimeVost" && it.dubbing == "AnimeVost" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `no title or unknown page url gives no videos`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null
        assertTrue(videoRepository().videos(details()).isEmpty())

        coEvery { findAvailability(any(), any()) } returns availability.copy(url = "https://animevost.org/")
        assertTrue(videoRepository().videos(details()).isEmpty())
    }

    @Test
    fun `stream plays the best quality and carries the map`() = runTest {
        coEvery { repository.getStreams(AnimevostTarget("tv", "4004-kikansha", 2)) } returns
            AnimevostStreams(linkedMapOf("480p" to "https://a/480.mp4", "720p" to "https://a/720.mp4"))

        val result = streamRepository().resolve("source://animevost/tv/4004-kikansha/2") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/720.mp4", result.url)
        assertEquals(listOf("480p", "720p"), result.qualities?.keys?.toList())
    }

    @Test
    fun `missing episode is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        val unavailable = streamRepository().resolve("source://animevost/tv/4004-kikansha/9")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://animevost/tv/4004-kikansha/1") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://animevost/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only animevost urls are supported`() {
        assertTrue(streamRepository().supports("source://animevost/tv/4004-kikansha/1"))
        assertEquals(false, streamRepository().supports("source://animeon/924/1/1093/1209"))
    }
}
