package su.afk.yummy.tv.source.anime4k.data.source

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlag
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kEpisode
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kFile
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTicket
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTitle
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import su.afk.yummy.tv.source.anime4k.domain.usecase.GetAnime4kTitleUseCase
import su.afk.yummy.tv.source.anime4k.domain.usecase.ResolveAnime4kStreamsUseCase
import su.afk.yummy.tv.source.anime4k.data.flag.Anime4kFeatureGate
import su.afk.yummy.tv.source.anime4k.data.signing.RequestSigner
import su.afk.yummy.tv.source.anime4k.data.utils.parseAnime4kSourceUrl

class Anime4kExtraSourceTest : BaseUnitTest() {

    private val repository: Anime4kRepository = mockk()
    private val ads: RewardedAdRepository = mockk()
    private val toggles: FeatureToggleProvider = mockk()
    private val signer: RequestSigner = mockk()
    private val messages = object : Anime4kMessages {
        override val declined = "declined-text"
        override val adNotShown = "ad-text"
        override val rateLimited = "rate-text"
        override val notFound = "nf-text"
    }

    private fun details(malId: Int?) = AnimeDetails(
        id = 1, animeUrl = "u", title = "t", description = "", poster = null,
        rating = AnimeRating(null, null, null, null, null), genres = emptyList(), year = null, ageRating = null,
        views = null, status = null, type = null, episodes = null, otherTitles = emptyList(), creators = emptyList(),
        studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(), malId = malId,
    )

    private val title = Anime4kTitle(
        malId = 1735,
        episodes = listOf(
            Anime4kEpisode("1", "1", listOf("AlexFilm", "Мосфильм-Мастер")),
            Anime4kEpisode("2", "1", listOf("AlexFilm")),
        ),
    )

    private fun videoRepository() = Anime4kExtraVideoRepository(GetAnime4kTitleUseCase(repository), ads, Anime4kFeatureGate(toggles, false), signer)

    private fun streamRepository() =
        Anime4kExtraStreamRepository(ResolveAnime4kStreamsUseCase(repository, ads), Anime4kFeatureGate(toggles, false), messages)

    private fun enabled(value: Boolean) {
        every { toggles.isEnabled(any<FeatureFlag.BooleanFlag>()) } returns value
        every { signer.isAvailable } returns true
    }

    @Test
    fun `videos are built per episode and dubbing without file urls`() = runTest {
        enabled(true)
        every { ads.isAvailable } returns true
        coEvery { repository.getTitle(any()) } returns title

        val videos = videoRepository().videos(details(1735))

        assertEquals(3, videos.size)
        assertTrue(videos.all { it.player == "4K" && it.id < 0 && it.iframeUrl.startsWith("source://anime4k/1735/") })
        assertEquals(3, videos.map { it.id }.toSet().size)
        val target = parseAnime4kSourceUrl(videos[1].iframeUrl)
        assertEquals("Мосфильм-Мастер", target?.team)
        assertEquals("1", target?.episode)
    }

    @Test
    fun `no title or no mal id or disabled gives nothing`() = runTest {
        every { ads.isAvailable } returns true
        coEvery { repository.getTitle(any()) } returns null

        enabled(true)
        assertTrue(videoRepository().videos(details(1735)).isEmpty())
        assertTrue(videoRepository().videos(details(null)).isEmpty())

        enabled(false)
        assertTrue(videoRepository().videos(details(1735)).isEmpty())
        coVerify(exactly = 1) { repository.getTitle(any()) }
    }

    @Test
    fun `source is hidden without an ad unit`() = runTest {
        enabled(true)
        every { ads.isAvailable } returns false

        assertTrue(videoRepository().videos(details(1735)).isEmpty())
        coVerify(exactly = 0) { repository.getTitle(any()) }
    }

    @Test
    fun `stream is resolved after the ad with referer header and quality map`() = runTest {
        enabled(true)
        val ticket = Anime4kTicket("t", 0, 300)
        coEvery { repository.requestTicket() } returns ticket
        coEvery { ads.askConsent() } returns true
        coEvery { ads.showRewardedAd() } returns true
        coEvery { repository.getStreams(any(), any()) } returns Anime4kStreams(
            "https://example.org/",
            listOf(Anime4kFile(2160, 7500, "https://v/x_2160.mp4"), Anime4kFile(1080, 3000, "https://v/x_1080.mp4")),
        )
        val url = "source://anime4k/1735/1/1/AlexFilm"

        val result = streamRepository().resolve(url) as PlayerStreamResolveResult.Stream

        assertEquals("https://v/x_2160.mp4", result.url)
        assertEquals("https://example.org/", result.headers["Referer"])
        // по возрастанию: плеер берёт последнее как лучшее
        assertEquals(listOf("1080p", "2160p"), result.qualities?.keys?.toList())
    }

    @Test
    fun `repeat resolve inside the window does not show the ad again`() = runTest {
        enabled(true)
        coEvery { repository.requestTicket() } returns Anime4kTicket("t", 0, 300)
        coEvery { ads.askConsent() } returns true
        coEvery { ads.showRewardedAd() } returns true
        coEvery { repository.getStreams(any(), any()) } returns
            Anime4kStreams("r", listOf(Anime4kFile(2160, 1, "https://v/x.mp4")))
        val repo = streamRepository()
        val url = "source://anime4k/1735/1/1/AlexFilm"

        repo.resolve(url)
        repo.resolve(url)

        coVerify(exactly = 1) { ads.showRewardedAd() }
    }

    @Test
    fun `skipped ad gives unavailable and foreign urls are not supported`() = runTest {
        enabled(true)
        coEvery { repository.requestTicket() } returns Anime4kTicket("t", 0, 300)
        coEvery { ads.askConsent() } returns true
        coEvery { ads.showRewardedAd() } returns false
        val repo = streamRepository()

        val result = repo.resolve("source://anime4k/1735/1/1/AlexFilm")

        assertTrue(result is PlayerStreamResolveResult.Unavailable)
        assertTrue(repo.supports("source://anime4k/1/1/1/X"))
        assertFalse(repo.supports("https://kodik.info/x"))
        enabled(false)
        assertFalse(repo.supports("source://anime4k/1/1/1/X"))
    }

    @Test
    fun `source is hidden when the build has no signing secret`() = runTest {
        enabled(true)
        every { ads.isAvailable } returns true
        every { signer.isAvailable } returns false

        assertTrue(videoRepository().videos(details(1735)).isEmpty())
        coVerify(exactly = 0) { repository.getTitle(any()) }
    }

    @Test
    fun `debug builds force the source on regardless of the remote flag`() = runTest {
        every { toggles.isEnabled(any<FeatureFlag.BooleanFlag>()) } returns false
        every { ads.isAvailable } returns true
        every { signer.isAvailable } returns true
        coEvery { repository.getTitle(any()) } returns title
        val debugRepo = Anime4kExtraVideoRepository(GetAnime4kTitleUseCase(repository), ads, Anime4kFeatureGate(toggles, true), signer)

        assertEquals(3, debugRepo.videos(details(1735)).size)
    }

    @Test
    fun `declined consent gives unavailable without asking the server`() = runTest {
        enabled(true)
        coEvery { ads.askConsent() } returns false
        val repo = streamRepository()

        val result = repo.resolve("source://anime4k/1735/1/1/AlexFilm")

        assertTrue(result is PlayerStreamResolveResult.Unavailable)
        coVerify(exactly = 0) { repository.requestTicket() }
        coVerify(exactly = 0) { ads.showRewardedAd() }
    }

    @Test
    fun `user facing messages are localized texts and a decline is not asked twice in a row`() = runTest {
        enabled(true)
        coEvery { ads.askConsent() } returns false
        val repo = streamRepository()
        val url = "source://anime4k/1735/1/1/AlexFilm"

        val first = repo.resolve(url) as PlayerStreamResolveResult.Unavailable
        val retry = repo.resolve(url) as PlayerStreamResolveResult.Unavailable

        assertEquals("declined-text", first.message)
        assertEquals("declined-text", retry.message)
        coVerify(exactly = 1) { ads.askConsent() }
    }

    @Test
    fun `manual retry after the silent one asks consent again`() = runTest {
        enabled(true)
        coEvery { ads.askConsent() } returns false
        val repo = streamRepository()
        val url = "source://anime4k/1735/1/1/AlexFilm"

        repo.resolve(url)
        repo.resolve(url) // тихий повтор плеера
        repo.resolve(url) // «Повторить» от пользователя

        coVerify(exactly = 2) { ads.askConsent() }
    }

    @Test
    fun `no ad and rate limit messages are localized`() = runTest {
        enabled(true)
        coEvery { ads.askConsent() } returns true
        coEvery { repository.requestTicket() } returns Anime4kTicket("t", 0, 300)
        coEvery { ads.showRewardedAd() } returns false

        val noAd = streamRepository().resolve("source://anime4k/1735/1/1/AlexFilm") as PlayerStreamResolveResult.Unavailable

        assertEquals("ad-text", noAd.message)
    }

    @Test
    fun `confirm before open asks only for 4k videos and follows the answer`() = runTest {
        coEvery { ads.confirmBeforeOpen() } returnsMany listOf(true, false)
        val repo = videoRepository()
        fun video(url: String) = AnimeVideo(
            id = -1, episode = "1", dubbing = "t", player = "4K", playerId = null, iframeUrl = url, durationSeconds = null,
        )

        assertTrue(repo.confirmOpen(video("https://kodik.example/x")))
        coVerify(exactly = 0) { ads.confirmBeforeOpen() }
        assertTrue(repo.confirmOpen(video("source://anime4k/1735/1/1/AlexFilm")))
        assertEquals(false, repo.confirmOpen(video("source://anime4k/1735/1/1/AlexFilm")))
    }
}
