package su.afk.yummy.tv.source.amedia.data.source

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import su.afk.yummy.tv.source.amedia.domain.usecase.GetAmediaEpisodesUseCase
import su.afk.yummy.tv.source.amedia.domain.usecase.ResolveAmediaStreamsUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.model.SourceEpisode
import su.afk.yummy.tv.source.availability.domain.model.SourceVariant
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import java.io.IOException

class AmediaExtraSourceTest : BaseUnitTest() {

    private val repository: AmediaRepository = mockk()
    private val findAvailability: FindSourceAvailabilityUseCase = mockk()
    private val messages = object : AmediaMessages {
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "Мстители", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = 2026, ageRating = null,
        views = null, status = null, type = "tv", episodes = AnimeEpisodes(12, 3, null, null), otherTitles = listOf("Revengers"),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private fun availability(vararg episodes: SourceEpisode, url: String = "https://amd.online/5753-x.html") = SourceAvailability(
        sourceId = "amedia", key = "5753-x", url = url, episodesAvailable = episodes.size, episodesTotal = 12, ongoing = true,
        qualities = emptyList(), variants = listOf(SourceVariant("5753", false, episodes.toList())),
    )

    private fun vod(number: Int, vodId: Long) = SourceEpisode(number, null, null, null, id = vodId.toString())

    private fun rutube(number: Int, url: String) = SourceEpisode(number, null, null, null, url = url)

    private fun videoRepository() = AmediaExtraVideoRepository(findAvailability, GetAmediaEpisodesUseCase())

    private fun streamRepository() = AmediaExtraStreamRepository(ResolveAmediaStreamsUseCase(repository), messages)

    @Test
    fun `videos are built per episode and lookup carries the app data`() = runTest {
        coEvery { findAvailability("amedia", AvailabilityLookup(59088, "Мстители", listOf("Revengers"), 2026, "tv", 12)) } returns
            availability(vod(2, 48111), vod(1, 47997))

        val videos = videoRepository().videos(details(59088))

        assertEquals(listOf("1", "2"), videos.map { it.episode })
        assertEquals("source://amedia/47997", videos[0].iframeUrl)
        assertTrue(videos.all { it.player == "Amedia" && it.dubbing == "Amedia" && it.id < 0 })
        assertEquals(videos.size, videos.map { it.id }.toSet().size)
    }

    @Test
    fun `rutube episode keeps the rutube embed as its url`() = runTest {
        val embed = "https://rutube.ru/play/embed/be753fcea5b1c586363cf8a220c47167"
        coEvery { findAvailability(any(), any()) } returns availability(rutube(1, embed), vod(2, 30296))

        val videos = videoRepository().videos(details(1))

        assertEquals(listOf(embed, "source://amedia/30296"), videos.map { it.iframeUrl })
        assertTrue(videos.all { it.player == "Amedia" })
    }

    @Test
    fun `no mal id, no title or a foreign page gives no videos`() = runTest {
        assertTrue(videoRepository().videos(details(null)).isEmpty())
        coEvery { findAvailability(any(), any()) } returns null
        assertTrue(videoRepository().videos(details(1)).isEmpty())
        coEvery { findAvailability(any(), any()) } returns availability(vod(1, 1), url = "https://evil.example/1.html")
        assertTrue(videoRepository().videos(details(1)).isEmpty())
    }

    @Test
    fun `a failed source is not swallowed so the old list stays cached`() = runTest {
        coEvery { findAvailability(any(), any()) } throws IOException("amedia не ответил")

        assertTrue(runCatching { videoRepository().videos(details(1)) }.exceptionOrNull() is IOException)
    }

    @Test
    fun `stream is best quality with ascending quality map and no headers`() = runTest {
        coEvery { repository.getStreams(AmediaTarget(47997)) } returns
            AmediaStreams("https://m/index.m3u8", mapOf(720 to "https://m/720.m3u8", 360 to "https://m/360.m3u8"))

        val stream = streamRepository().resolve("source://amedia/47997") as PlayerStreamResolveResult.Stream

        assertEquals("https://m/720.m3u8", stream.url)
        assertEquals(listOf("360p", "720p"), stream.qualities!!.keys.toList())
        assertTrue(stream.headers.isEmpty())
    }

    @Test
    fun `stream without variants falls back to the master playlist`() = runTest {
        coEvery { repository.getStreams(AmediaTarget(1)) } returns AmediaStreams("https://m/index.m3u8", emptyMap())

        val stream = streamRepository().resolve("source://amedia/1") as PlayerStreamResolveResult.Stream

        assertEquals("https://m/index.m3u8", stream.url)
        assertNull(stream.qualities)
    }

    @Test
    fun `missing stream is unavailable, failure failed, bad url failed`() = runTest {
        coEvery { repository.getStreams(AmediaTarget(2)) } returns null
        assertEquals(PlayerStreamResolveResult.Unavailable("nf-text"), streamRepository().resolve("source://amedia/2"))

        coEvery { repository.getStreams(AmediaTarget(3)) } throws IOException("down")
        assertTrue(streamRepository().resolve("source://amedia/3") is PlayerStreamResolveResult.Failed)

        assertTrue(streamRepository().resolve("source://amedia/x") is PlayerStreamResolveResult.Failed)
    }

    @Test
    fun `supports only amedia urls`() {
        assertTrue(streamRepository().supports("source://amedia/1"))
        assertFalse(streamRepository().supports("source://anistar/1/1"))
    }
}
