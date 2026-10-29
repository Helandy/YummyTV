package su.afk.yummy.tv.source.mikai.data.source

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
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreams
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import su.afk.yummy.tv.source.ashdi.domain.repository.AshdiRepository
import su.afk.yummy.tv.source.ashdi.domain.usecase.ResolveAshdiStreamsUseCase
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreams
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import su.afk.yummy.tv.source.fenix.domain.repository.FenixRepository
import su.afk.yummy.tv.source.fenix.domain.usecase.ResolveFenixStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class MikaiExtraSourceTest : BaseUnitTest() {

    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val ashdi: AshdiRepository = mockk()
    private val fenix: FenixRepository = mockk()
    private val messages = object : MikaiMessages {
        override val notFound = "nf-text"
        override val regionBlocked = "geo-text"
        override val subsSuffix = " (subs)"
    }

    private fun details() = AnimeDetails(
        id = 1, animeUrl = "u", title = "Фрирен", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2023, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(28, 12, null, null), otherTitles = listOf("Frieren"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = 52991,
    )

    private fun episode(number: Int, id: String?, url: String? = null) = SourceEpisode(number, null, null, null, id, url)

    private val availability = SourceAvailability(
        sourceId = "mikai", key = "1", url = "https://mikai.me/anime/1-lazar", episodesAvailable = 2,
        episodesTotal = 2, ongoing = false, qualities = emptyList(),
        variants = listOf(
            SourceVariant("ashdi:44", false, listOf(episode(1, "100"), episode(2, null, "https://ashdi.vip/vod/101")), title = "QTV"),
            SourceVariant("ashdi:45", false, listOf(episode(1, "200")), title = "Amanogawa", isSubs = true),
            SourceVariant("fenix:44", false, listOf(episode(1, "eyJ0.sig-1"), episode(2, null, "https://fenixplay.xyz/vod/eyJ1.sig-2#embed=x")), title = "QTV"),
            SourceVariant("fenix:45", false, listOf(episode(1, "eyJ2.sig")), title = "Amanogawa", isSubs = true),
            SourceVariant("ashdi:46", false, listOf(episode(1, null, "https://fenixplay.xyz/vod/x")), title = "Broken"),
            SourceVariant("ashdi:47", false, listOf(episode(1, "400")), title = null),
        ),
    )

    private fun videoRepository() = MikaiExtraVideoRepository(findAvailability, messages)

    private fun streamRepository() = MikaiExtraStreamRepository(ResolveAshdiStreamsUseCase(ashdi), ResolveFenixStreamsUseCase(fenix), messages)

    @Test
    fun `ashdi and fenix teams with a playable id become videos, ashdi subtitles are marked`() = runTest {
        coEvery {
            findAvailability("mikai", AvailabilityLookup(52991, "Фрирен", listOf("Frieren"), 2023, "tv", 28))
        } returns availability

        val videos = videoRepository().videos(details())

        assertEquals(
            listOf(
                "QTV" to "source://mikai/ashdi/100",
                "QTV" to "source://mikai/ashdi/101",
                "Amanogawa (subs)" to "source://mikai/ashdi/200",
                "QTV" to "source://mikai/fenix/eyJ0.sig-1",
                "QTV" to "source://mikai/fenix/eyJ1.sig-2",
            ),
            videos.map { it.dubbing to it.iframeUrl },
        )
        assertEquals(listOf("1", "2", "1", "1", "2"), videos.map { it.episode })
        assertEquals(listOf("Ashdi", "Ashdi", "Ashdi", "Fenix", "Fenix"), videos.map { it.player })
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
        coEvery { ashdi.getStreams("https://ashdi.vip/vod/100") } returns
            AshdiStreamsResult.Success(AshdiStreams(linkedMapOf("480p" to "https://a/480.m3u8", "1080p" to "https://a/1080.m3u8"), "https://a/index.m3u8"))

        val result = streamRepository().resolve("source://mikai/ashdi/100") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/1080.m3u8", result.url)
        assertEquals(listOf("480p", "1080p"), result.qualities?.keys?.toList())
    }

    @Test
    fun `master without variants plays adaptively`() = runTest {
        coEvery { ashdi.getStreams(any()) } returns AshdiStreamsResult.Success(AshdiStreams(emptyMap(), "https://a/index.m3u8"))

        val result = streamRepository().resolve("source://mikai/ashdi/100") as PlayerStreamResolveResult.Stream

        assertEquals("https://a/index.m3u8", result.url)
        assertEquals(null, result.qualities)
    }

    @Test
    fun `not found and region block are unavailable with their own texts, network error and bad url fail`() = runTest {
        coEvery { ashdi.getStreams(any()) } returns AshdiStreamsResult.NotFound
        assertEquals("nf-text", (streamRepository().resolve("source://mikai/ashdi/9") as PlayerStreamResolveResult.Unavailable).message)

        coEvery { ashdi.getStreams(any()) } returns AshdiStreamsResult.RegionBlocked
        assertEquals("geo-text", (streamRepository().resolve("source://mikai/ashdi/9") as PlayerStreamResolveResult.Unavailable).message)

        coEvery { ashdi.getStreams(any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://mikai/ashdi/9") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://mikai/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `fenix url resolves to the signed master and plays adaptively`() = runTest {
        coEvery { fenix.getStreams("https://fenixplay.xyz/vod/eyJ0.sig-1", null) } returns
            FenixStreamsResult.Success(FenixStreams("https://media.fenixplay.xyz/m/master.m3u8?token=t"))

        val result = streamRepository().resolve("source://mikai/fenix/eyJ0.sig-1") as PlayerStreamResolveResult.Stream

        assertEquals("https://media.fenixplay.xyz/m/master.m3u8?token=t", result.url)
        assertEquals(null, result.qualities)
    }

    @Test
    fun `fenix not found is unavailable, network error and bad token fail`() = runTest {
        coEvery { fenix.getStreams(any(), any()) } returns FenixStreamsResult.NotFound
        assertEquals("nf-text", (streamRepository().resolve("source://mikai/fenix/eyJ0.sig") as PlayerStreamResolveResult.Unavailable).message)

        coEvery { fenix.getStreams(any(), any()) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://mikai/fenix/eyJ0.sig") is PlayerStreamResolveResult.Failed)
        assertTrue(streamRepository().resolve("source://mikai/fenix/a/b") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `only mikai urls are supported`() {
        assertTrue(streamRepository().supports("source://mikai/ashdi/100"))
        assertTrue(streamRepository().supports("source://mikai/fenix/eyJ0.sig"))
        assertEquals(false, streamRepository().supports("source://animeon/924/1/1093/1209"))
    }
}
