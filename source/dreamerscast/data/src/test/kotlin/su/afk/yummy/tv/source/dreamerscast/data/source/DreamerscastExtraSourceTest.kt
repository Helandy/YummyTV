package su.afk.yummy.tv.source.dreamerscast.data.source

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
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastStreams
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget
import su.afk.yummy.tv.source.dreamerscast.domain.repository.DreamerscastRepository
import su.afk.yummy.tv.source.dreamerscast.domain.usecase.ResolveDreamerscastStreamsUseCase
import java.io.IOException

class DreamerscastExtraSourceTest : BaseUnitTest() {

    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val repository: DreamerscastRepository = mockk()
    private val messages = object : DreamerscastMessages {
        override val notFound = "nf-text"
    }

    private fun details() = AnimeDetails(
        id = 1, animeUrl = "u", title = "Я перевоплотился в гоблина", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2026, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(12, 5, null, null), otherTitles = listOf("Tensei Goblin"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = 7,
    )

    private val availability = SourceAvailability(
        sourceId = "dreamerscast", key = "618-tensei-goblin", url = "https://dreamerscast.com/home/release/618-tensei-goblin",
        episodesAvailable = 3, episodesTotal = 12, ongoing = true, qualities = emptyList(),
    )

    private fun videoRepository() = DreamerscastExtraVideoRepository(findAvailability)

    private fun streamRepository() = DreamerscastExtraStreamRepository(ResolveDreamerscastStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per available episode from the release page url`() = runTest {
        coEvery {
            findAvailability("dreamerscast", AvailabilityLookup(7, "Я перевоплотился в гоблина", listOf("Tensei Goblin"), 2026, "tv", 12))
        } returns availability

        val videos = videoRepository().videos(details())

        assertEquals(listOf("1", "2", "3"), videos.map { it.episode })
        assertEquals("source://dreamerscast/618-tensei-goblin/2", videos[1].iframeUrl)
        assertTrue(videos.all { it.player == "Dream Cast" && it.dubbing == "Dream Cast" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `no title or unknown page url gives no videos`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null
        assertTrue(videoRepository().videos(details()).isEmpty())

        coEvery { findAvailability(any(), any()) } returns availability.copy(url = "https://dreamerscast.com/")
        assertTrue(videoRepository().videos(details()).isEmpty())
    }

    @Test
    fun `stream plays the best quality and carries the map`() = runTest {
        coEvery { repository.getStreams(DreamerscastTarget("618-tensei-goblin", 2)) } returns
            DreamerscastStreams(linkedMapOf("720p" to "https://a/720.m3u8", "1080p" to "https://a/1080.m3u8"), "https://a/master.m3u8")

        val result = streamRepository().resolve("source://dreamerscast/618-tensei-goblin/2") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/1080.m3u8", result.url)
        assertEquals(listOf("720p", "1080p"), result.qualities?.keys?.toList())
    }

    @Test
    fun `master without variants plays adaptively`() = runTest {
        coEvery { repository.getStreams(any()) } returns DreamerscastStreams(emptyMap(), "https://a/master.m3u8")

        val result = streamRepository().resolve("source://dreamerscast/618-tensei-goblin/1") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/master.m3u8", result.url)
        assertEquals(null, result.qualities)
    }

    @Test
    fun `missing episode is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        val unavailable = streamRepository().resolve("source://dreamerscast/618-tensei-goblin/9")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://dreamerscast/618-tensei-goblin/1") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://dreamerscast/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only dreamerscast urls are supported`() {
        assertTrue(streamRepository().supports("source://dreamerscast/618-tensei-goblin/1"))
        assertEquals(false, streamRepository().supports("source://animebesst/957-kikansha/1?h=anime1.best"))
    }
}
