package su.afk.yummy.tv.source.anistar.data.source

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
import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import su.afk.yummy.tv.source.anistar.domain.usecase.FindAnistarTitleUseCase
import su.afk.yummy.tv.source.anistar.domain.usecase.GetAnistarEpisodesUseCase
import su.afk.yummy.tv.source.anistar.domain.usecase.ResolveAnistarStreamsUseCase
import java.io.IOException

class AnistarExtraSourceTest : BaseUnitTest() {

    private val repository: AnistarRepository = mockk()
    private val messages = object : AnistarMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(28, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val title = AnistarTitle(2658, "https://v31.astar.bz")

    private fun videoRepository() = AnistarExtraVideoRepository(FindAnistarTitleUseCase(repository), GetAnistarEpisodesUseCase(repository))

    private fun streamRepository() = AnistarExtraStreamRepository(ResolveAnistarStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per episode and lookup carries the app data`() = runTest {
        coEvery { repository.findTitle(AnistarLookup(52991, "Фрирен", listOf("Frieren"), 2023, "tv", 28)) } returns title
        coEvery { repository.getEpisodes(title) } returns listOf(AnistarEpisode(2), AnistarEpisode(1))

        val videos = videoRepository().videos(details(52991))

        assertEquals(listOf("1", "2"), videos.map { it.episode })
        assertEquals("source://anistar/2658/1", videos[0].iframeUrl)
        assertTrue(videos.all { it.player == "AniStar" && it.dubbing == "AniStar" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `no mal id or no title gives no videos`() = runTest {
        assertTrue(videoRepository().videos(details(null)).isEmpty())
        coEvery { repository.findTitle(any()) } returns null
        assertTrue(videoRepository().videos(details(1)).isEmpty())
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
