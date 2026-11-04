package su.afk.yummy.tv.source.sameband.data.source

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
import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreams
import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget
import su.afk.yummy.tv.source.sameband.domain.repository.SamebandRepository
import su.afk.yummy.tv.source.sameband.domain.usecase.ListSamebandEpisodesUseCase
import su.afk.yummy.tv.source.sameband.domain.usecase.ResolveSamebandStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class SamebandExtraSourceTest : BaseUnitTest() {

    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val repository: SamebandRepository = mockk()
    private val messages = object : SamebandMessages {
        override val notFound = "nf-text"
    }

    private val details = AnimeDetails(
        id = 1, animeUrl = "u", title = "Необъятный океан 3", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2026, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(null, 0, null, null), otherTitles = emptyList(),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = 1,
    )

    private val availability = SourceAvailability(
        sourceId = "sameband", key = "1234-grand-blue-3", url = "https://sameband.studio/anime/1234-grand-blue-3.html",
        episodesAvailable = 0, episodesTotal = null, ongoing = true, qualities = emptyList(),
    )

    private fun videoRepository() = SamebandExtraVideoRepository(findAvailability, ListSamebandEpisodesUseCase(repository))

    private fun streamRepository() = SamebandExtraStreamRepository(ResolveSamebandStreamsUseCase(repository), messages)

    @Test
    fun `episodes come from the playlist even when the server count is unknown`() = runTest {
        coEvery { findAvailability("sameband", any()) } returns availability
        coEvery { repository.getEpisodes("1234-grand-blue-3") } returns listOf(1, 2, 3)

        val videos = videoRepository().videos(details)

        assertEquals(listOf("1", "2", "3"), videos.map { it.episode })
        assertEquals("source://sameband/1234-grand-blue-3/2", videos[1].iframeUrl)
        assertTrue(videos.all { it.player == "Sameband" && it.dubbing == "Студийная Банда" && it.id < 0 })
    }

    @Test
    fun `no title or playlist failure gives no videos`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null
        assertTrue(videoRepository().videos(details).isEmpty())

        coEvery { findAvailability(any(), any()) } returns availability
        coEvery { repository.getEpisodes(any()) } throws IOException("down")
        assertTrue(videoRepository().videos(details).isEmpty())
    }

    @Test
    fun `stream carries the quality map and plays the best one`() = runTest {
        coEvery { repository.getStreams(SamebandTarget("1234-x", 2)) } returns
            SamebandStreams(linkedMapOf("480p" to "https://s/480.m3u8", "1080p" to "https://s/1080.m3u8"), mapOf("Referer" to "https://s/"))

        val result = streamRepository().resolve("source://sameband/1234-x/2") as PlayerStreamResolveResult.Stream

        assertEquals("https://s/1080.m3u8", result.url)
        assertEquals(listOf("480p", "1080p"), result.qualities?.keys?.toList())
        assertEquals(mapOf("Referer" to "https://s/"), result.headers)
    }

    @Test
    fun `missing episode is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        assertEquals("nf-text", (streamRepository().resolve("source://sameband/1234-x/9") as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://sameband/1234-x/1") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://sameband/x") is PlayerStreamResolveResult.Failed)
    }
}
