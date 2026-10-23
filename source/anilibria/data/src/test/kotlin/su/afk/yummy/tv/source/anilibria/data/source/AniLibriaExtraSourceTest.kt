package su.afk.yummy.tv.source.anilibria.data.source

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaEpisode
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaSkip
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import su.afk.yummy.tv.source.anilibria.domain.usecase.FindAniLibriaReleasesUseCase
import su.afk.yummy.tv.source.anilibria.domain.usecase.ResolveAniLibriaStreamsUseCase

class AniLibriaExtraSourceTest : BaseUnitTest() {

    private val repository: AniLibriaRepository = mockk()
    private val messages = object : AniLibriaMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = null, ageRating = null,
        views = null, status = null, type = null, episodes = null, otherTitles = listOf("Frieren"), creators = emptyList(),
        studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val release = AniLibriaRelease(
        id = 9542,
        malId = 52991,
        episodes = listOf(
            AniLibriaEpisode(
                1, null, "https://cache.libria.fun/a480.m3u8?x=1", null, "https://other.host/a1080.m3u8", 1560,
                AniLibriaSkip(5, 95),
            ),
            AniLibriaEpisode(2, null, null, "b720", null, null, null),
        ),
    )

    private fun videoRepository() = AniLibriaExtraVideoRepository(FindAniLibriaReleasesUseCase(repository))

    private fun streamRepository() =
        AniLibriaExtraStreamRepository(ResolveAniLibriaStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per episode with source urls and skips`() = runTest {
        coEvery { repository.findReleases(52991, listOf("Фрирен", "Frieren"), null) } returns listOf(release)
        val videos = videoRepository().videos(details(52991))
        assertEquals(listOf("1", "2"), videos.map { it.episode })
        assertEquals("source://anilibria/9542/1", videos[0].iframeUrl)
        assertEquals("Озвучка AniLibria", videos[0].dubbing)
        assertTrue(videos.all { it.id < 0 })
        assertEquals(5_000L, videos[0].skips.opening?.startMs)
        assertEquals(95_000L, videos[0].skips.opening?.endMs)
        assertTrue(videoRepository().requiresMatchingDubbing)
    }

    @Test
    fun `no mal id or no release gives no videos`() = runTest {
        assertTrue(videoRepository().videos(details(null)).isEmpty())
        coEvery { repository.findReleases(1, any(), any()) } returns emptyList()
        assertTrue(videoRepository().videos(details(1)).isEmpty())
    }

    @Test
    fun `stream resolves best quality last`() = runTest {
        coEvery { repository.getRelease(9542) } returns release
        val result = streamRepository().resolve("source://anilibria/9542/1") as PlayerStreamResolveResult.Stream
        assertEquals("https://other.host/a1080.m3u8", result.url)
        assertEquals(listOf("480p", "1080p"), result.qualities?.keys?.toList())
        assertEquals("https://cache-rfn.libria.fun/a480.m3u8?x=1", result.qualities?.get("480p"))
    }

    @Test
    fun `unknown episode is unavailable and bad url fails`() = runTest {
        coEvery { repository.getRelease(9542) } returns release
        val unavailable = streamRepository().resolve("source://anilibria/9542/7")
        assertEquals("nf-text", (unavailable as PlayerStreamResolveResult.Unavailable).message)
        assertTrue(streamRepository().resolve("source://anilibria/x") is PlayerStreamResolveResult.Failed)
    }
}
