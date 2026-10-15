package su.afk.yummy.tv.source.anime4k.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kApiException
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kFile
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreamsResult
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTicket
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import java.io.IOException

class ResolveAnime4kStreamsUseCaseTest : BaseUnitTest() {

    private val repository: Anime4kRepository = mockk()
    private val ads: RewardedAdRepository = mockk()

    private val target = Anime4kTarget(malId = 1735, season = "1", episode = "1", team = "AlexFilm")
    private val streams = Anime4kStreams("https://example.org/", listOf(Anime4kFile(2160, 7500, "https://v/x_2160.mp4")))

    private fun ticket(notBeforeSec: Int = 0) = Anime4kTicket("t", notBeforeSec, 300)

    private fun createUseCase() = ResolveAnime4kStreamsUseCase(repository, ads)

    private fun consent(value: Boolean = true) {
        coEvery { ads.askConsent() } returns value
    }

    @Test
    fun `ticket is requested before the ad and links after it`() = runTest {
        consent()
        coEvery { repository.requestTicket() } returns ticket()
        coEvery { ads.showRewardedAd() } returns true
        coEvery { repository.getStreams(any(), target) } returns streams

        val result = createUseCase()(target)

        assertEquals(Anime4kStreamsResult.Success(streams), result)
        coVerifyOrder {
            repository.requestTicket()
            ads.showRewardedAd()
            repository.getStreams(any(), target)
        }
    }

    @Test
    fun `no reward means no links`() = runTest {
        consent()
        coEvery { repository.requestTicket() } returns ticket()
        coEvery { ads.showRewardedAd() } returns false

        val result = createUseCase()(target)

        assertEquals(Anime4kStreamsResult.AdNotRewarded, result)
        coVerify(exactly = 0) { repository.getStreams(any(), any()) }
    }

    @Test
    fun `waits out the remaining minimum time after a quick ad`() = runTest {
        consent()
        coEvery { repository.requestTicket() } returns ticket(notBeforeSec = 20)
        coEvery { ads.showRewardedAd() } returns true
        coEvery { repository.getStreams(any(), any()) } returns streams

        createUseCase()(target)

        assertTrue("должно выдержать паузу", testScheduler.currentTime >= 19_000L)
    }

    @Test
    fun `server answers are mapped to results`() = runTest {
        consent()
        coEvery { ads.showRewardedAd() } returns true
        coEvery { repository.requestTicket() } returns ticket()

        coEvery { repository.getStreams(any(), any()) } throws Anime4kApiException.RateLimited()
        assertEquals(Anime4kStreamsResult.RateLimited, createUseCase()(target))

        coEvery { repository.getStreams(any(), any()) } throws Anime4kApiException.Rejected()
        assertEquals(Anime4kStreamsResult.Rejected, createUseCase()(target))

        coEvery { repository.getStreams(any(), any()) } throws Anime4kApiException.NotFound()
        assertEquals(Anime4kStreamsResult.NotFound, createUseCase()(target))
    }

    @Test
    fun `transport failure becomes failed and is not thrown`() = runTest {
        consent()
        coEvery { repository.requestTicket() } throws IOException("offline")

        val result = createUseCase()(target)

        assertEquals(Anime4kStreamsResult.Failed("offline"), result)
        coVerify(exactly = 0) { ads.showRewardedAd() }
    }

    @Test
    fun `declined consent stops before ticket and ad`() = runTest {
        consent(false)

        val result = createUseCase()(target)

        assertEquals(Anime4kStreamsResult.Declined, result)
        coVerify(exactly = 0) { repository.requestTicket() }
        coVerify(exactly = 0) { ads.showRewardedAd() }
    }

    @Test
    fun `consent is asked before the ticket is requested`() = runTest {
        consent()
        coEvery { repository.requestTicket() } returns ticket()
        coEvery { ads.showRewardedAd() } returns true
        coEvery { repository.getStreams(any(), any()) } returns streams

        createUseCase()(target)

        coVerifyOrder {
            ads.askConsent()
            repository.requestTicket()
            ads.showRewardedAd()
        }
    }
}
