package su.afk.yummy.tv.feature.details.details.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.animeDetails

class DetailsSubscriptionHandlerTest : BaseUnitTest() {

    private val getAccountSession: GetAccountSessionUseCase = mockk()
    private val getAnimeDetails: GetAnimeDetailsUseCase = mockk()
    private val getAnimeVideos: GetAnimeVideosUseCase = mockk()
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase = mockk()
    private val setVideoSubscription: SetVideoSubscriptionUseCase = mockk()

    private val videos = listOf(
        video(id = 1, episode = "1"),
        video(id = 2, episode = "2", subscribed = true),
    )

    @Test
    fun `guest gets no subscriptions`() = runTest {
        coEvery { getAccountSession() } returns AccountSession(isAuthorized = false, userId = 0)

        assertEquals(ScreenSubscriptionBaseResult.SignedOut, createHandler().loadScreenSubscriptionBase(ANIME_ID))
        coVerify(exactly = 0) { getAnimeVideos(any()) }
    }

    @Test
    fun `subscriptions load even when the details fail`() = runTest {
        signedIn()
        coEvery { getAnimeDetails(ANIME_ID) } throws IllegalStateException("details")
        coEvery { getAnimeVideos(ANIME_ID) } returns videos

        val result = createHandler().loadScreenSubscriptionBase(ANIME_ID) as ScreenSubscriptionBaseResult.Content

        assertNull(result.base.details)
        val option = result.base.subscriptions.single()
        assertTrue(option.isSubscribed)
        // Отписываемся тем видео, которое сервер считает подписанным.
        assertEquals(2, option.subscriptionVideoId)
        assertEquals(2, option.episodesCount)
    }

    @Test
    fun `failed video load is reported`() = runTest {
        signedIn()
        coEvery { getAnimeDetails(ANIME_ID) } returns animeDetails(id = ANIME_ID)
        coEvery { getAnimeVideos(ANIME_ID) } throws IllegalStateException("videos")

        val result = createHandler().loadScreenSubscriptionBase(ANIME_ID)

        assertEquals("videos", (result as ScreenSubscriptionBaseResult.Failure).message)
    }

    @Test
    fun `in-flight change is shown until the server answers`() = runTest {
        coEvery { refreshAnimeVideos(ANIME_ID) } returns listOf(video(id = 1, episode = "1"))
        val handler = createHandler()
        val option = handler.reloadSubscriptions(ANIME_ID).getOrThrow().single()
        var duringRequest: Boolean? = null
        coEvery { setVideoSubscription(1, true) } coAnswers {
            duringRequest = handler.reloadSubscriptions(ANIME_ID).getOrThrow().single().isSubscribed
            true
        }

        val changed = handler.commitSubscriptionChange(ANIME_ID, option, subscribed = true)

        assertTrue(changed)
        assertEquals(true, duringRequest)
        assertTrue(handler.pendingSubscriptionStates(ANIME_ID).isEmpty())
    }

    @Test
    fun `failed change clears the pending state and reports failure`() = runTest {
        coEvery { refreshAnimeVideos(ANIME_ID) } returns videos
        val handler = createHandler()
        val option = handler.reloadSubscriptions(ANIME_ID).getOrThrow().single()
        coEvery { setVideoSubscription(any(), any()) } throws IllegalStateException("500")

        assertFalse(handler.commitSubscriptionChange(ANIME_ID, option, subscribed = false))
        assertTrue(handler.pendingSubscriptionStates(ANIME_ID).isEmpty())
    }

    private fun signedIn() {
        coEvery { getAccountSession() } returns AccountSession(isAuthorized = true, userId = 5)
    }

    private fun video(id: Int, episode: String, subscribed: Boolean = false) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = "AniLibria",
        player = "Kodik",
        playerId = 7,
        iframeUrl = "https://kodik.info/$id",
        durationSeconds = 1_440,
        isSubscribed = subscribed,
    )

    private fun createHandler() = DetailsSubscriptionHandler(
        getAccountSession = getAccountSession,
        getAnimeDetails = getAnimeDetails,
        getAnimeVideos = getAnimeVideos,
        refreshAnimeVideos = refreshAnimeVideos,
        setVideoSubscription = setVideoSubscription,
    )

    private companion object {
        const val ANIME_ID = 42
    }
}
