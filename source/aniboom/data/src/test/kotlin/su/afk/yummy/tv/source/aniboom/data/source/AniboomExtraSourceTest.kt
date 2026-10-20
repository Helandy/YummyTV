package su.afk.yummy.tv.source.aniboom.data.source

import io.mockk.coEvery
import io.mockk.coVerify
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
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomDubbing
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreams
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTitle
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.usecase.FindAniboomTitleUseCase
import su.afk.yummy.tv.source.aniboom.domain.usecase.GetAniboomEpisodeCountsUseCase
import su.afk.yummy.tv.source.aniboom.domain.usecase.ResolveAniboomStreamsUseCase
import java.io.IOException

class AniboomExtraSourceTest : BaseUnitTest() {

    private val repository: AniboomRepository = mockk()
    private val messages = object : AniboomMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?, total: Int? = 28) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(total, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val title = AniboomTitle("0BzM07Pq71J", 2, listOf(AniboomDubbing("JAM CLUB", 16), AniboomDubbing("Dream Cast", 30)))

    private fun videoRepository() =
        AniboomExtraVideoRepository(FindAniboomTitleUseCase(repository), GetAniboomEpisodeCountsUseCase(repository))

    private fun streamRepository() = AniboomExtraStreamRepository(ResolveAniboomStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per dubbing and episode and lookup carries the app data`() = runTest {
        coEvery { repository.findTitle(AniboomLookup(52991, "Фрирен", listOf("Frieren"), 2023, "tv", 28)) } returns title
        coEvery { repository.hasEpisode(any()) } returns true

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
    fun `no mal id or no title gives no videos`() = runTest {
        assertTrue(videoRepository().videos(details(null)).isEmpty())
        coEvery { repository.findTitle(any()) } returns null
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

    @Test
    fun `stub episode is not offered for the dubbing that has no video for it`() = runTest {
        coEvery { repository.findTitle(any()) } returns title
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 2, 16)) } returns false
        coEvery { repository.hasEpisode(AniboomTarget("0BzM07Pq71J", 2, 30)) } returns true

        val videos = videoRepository().videos(details(52991, total = 12))

        assertEquals(
            listOf("JAM CLUB" to "1", "Dream Cast" to "1", "Dream Cast" to "2"),
            videos.map { it.dubbing to it.episode },
        )
    }

    @Test
    fun `finished title is not probed and keeps every episode`() = runTest {
        coEvery { repository.findTitle(any()) } returns title

        val videos = videoRepository().videos(details(52991, total = 2))

        assertEquals(4, videos.size)
        coVerify(exactly = 0) { repository.hasEpisode(any()) }
    }

    @Test
    fun `unknown total is probed like an ongoing title`() = runTest {
        coEvery { repository.findTitle(any()) } returns title
        coEvery { repository.hasEpisode(any()) } returns false

        val videos = videoRepository().videos(details(52991, total = null))

        assertEquals(listOf("1", "1"), videos.map { it.episode })
    }
}
