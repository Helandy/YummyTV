package su.afk.yummy.tv.source.anidub.data.source

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
import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreams
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget
import su.afk.yummy.tv.source.anidub.domain.repository.AniDubRepository
import su.afk.yummy.tv.source.anidub.domain.usecase.ResolveAniDubStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AniDubExtraSourceTest : BaseUnitTest() {

    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val repository: AniDubRepository = mockk()
    private val messages = object : AniDubMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(28, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val availability = SourceAvailability(
        sourceId = "anidub", key = "12294-frieren", url = "https://online.anidub.com/12294-frieren.html",
        episodesAvailable = 3, episodesTotal = 28, ongoing = true, qualities = listOf("480p", "1080p"),
    )

    private fun videoRepository() = AniDubExtraVideoRepository(findAvailability)

    private fun streamRepository() = AniDubExtraStreamRepository(ResolveAniDubStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per available episode and lookup carries the app data`() = runTest {
        coEvery {
            findAvailability("anidub", AvailabilityLookup(52991, "Фрирен", listOf("Frieren"), 2023, "tv", 28))
        } returns availability

        val videos = videoRepository().videos(details(52991))

        assertEquals(listOf("1", "2", "3"), videos.map { it.episode })
        assertEquals("source://anidub/12294-frieren/2", videos[1].iframeUrl)
        assertTrue(videos.all { it.player == "AniDub" && it.dubbing == "AniDub" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `title is looked up without mal id too`() = runTest {
        coEvery { findAvailability("anidub", AvailabilityLookup(null, "Фрирен", listOf("Frieren"), 2023, "tv", 28)) } returns availability

        assertEquals(3, videoRepository().videos(details(null)).size)
    }

    @Test
    fun `no title or no episodes gives no videos`() = runTest {
        coEvery { findAvailability(any(), any()) } returns null
        assertTrue(videoRepository().videos(details(1)).isEmpty())

        coEvery { findAvailability(any(), any()) } returns availability.copy(episodesAvailable = 0)
        assertTrue(videoRepository().videos(details(1)).isEmpty())
    }

    @Test
    fun `stream carries the quality map and plays the best one`() = runTest {
        coEvery { repository.getStreams(AniDubTarget("123-psyren", 2)) } returns
            AniDubStreams(
                linkedMapOf("480p" to "https://a/480.m3u8", "1080p" to "https://a/1080.m3u8"),
                "https://p/vid.php?v=/s1/x",
                mapOf("User-Agent" to "UA-TEST", "Referer" to "https://p/"),
            )

        val result = streamRepository().resolve("source://anidub/123-psyren/2") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/1080.m3u8", result.url)
        assertEquals(listOf("480p", "1080p"), result.qualities?.keys?.toList())
        assertEquals(mapOf("User-Agent" to "UA-TEST", "Referer" to "https://p/"), result.headers)
    }

    @Test
    fun `master without parsed variants plays adaptively`() = runTest {
        coEvery { repository.getStreams(any()) } returns AniDubStreams(emptyMap(), "https://p/vid.php?v=/s1/x", mapOf("User-Agent" to "UA-TEST"))

        val result = streamRepository().resolve("source://anidub/123-psyren/1") as PlayerStreamResolveResult.Stream

        assertEquals("https://p/vid.php?v=/s1/x", result.url)
        assertEquals(null, result.qualities)
        assertEquals("UA-TEST", result.headers["User-Agent"])
    }

    @Test
    fun `missing episode is unavailable, network error and bad url fail`() = runTest {
        coEvery { repository.getStreams(any()) } returns null
        val unavailable = streamRepository().resolve("source://anidub/123-psyren/9")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)

        coEvery { repository.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://anidub/123-psyren/1") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://anidub/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only anidub urls are supported`() {
        assertTrue(streamRepository().supports("source://anidub/123-psyren/1"))
        assertEquals(false, streamRepository().supports("source://aniboom/0BzM07Pq71J/1/16"))
    }
}
