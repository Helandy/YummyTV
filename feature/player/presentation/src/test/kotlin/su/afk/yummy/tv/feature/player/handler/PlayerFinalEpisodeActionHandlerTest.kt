package su.afk.yummy.tv.feature.player.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction

class PlayerFinalEpisodeActionHandlerTest : BaseUnitTest() {

    private val getAnimeDetails: GetAnimeDetailsUseCase = mockk()
    private val getAccountSession: GetAccountSessionUseCase = mockk()

    @Test
    fun `finished title suggests rating it`() = runTest {
        coEvery { getAnimeDetails(ANIME_ID) } returns details(status = "Вышел")

        assertEquals(PlayerFinalEpisodeAction.RateTitle, createHandler().resolve(ANIME_ID))
        coVerify(exactly = 0) { getAccountSession() }
    }

    @Test
    fun `ongoing title suggests subscriptions to an authorized user`() = runTest {
        coEvery { getAnimeDetails(ANIME_ID) } returns details(status = "ongoing")
        coEvery { getAccountSession() } returns AccountSession(isAuthorized = true, userId = USER_ID)

        assertEquals(PlayerFinalEpisodeAction.ManageSubscriptions, createHandler().resolve(ANIME_ID))
    }

    @Test
    fun `ongoing title suggests nothing to a guest`() = runTest {
        coEvery { getAnimeDetails(ANIME_ID) } returns details(status = "Онгоинг")
        coEvery { getAccountSession() } returns AccountSession(isAuthorized = false, userId = 0)

        assertEquals(PlayerFinalEpisodeAction.None, createHandler().resolve(ANIME_ID))
    }

    @Test
    fun `failed loads and unknown title suggest nothing`() = runTest {
        coEvery { getAnimeDetails(ANIME_ID) } throws IllegalStateException("network")

        val handler = createHandler()

        assertEquals(PlayerFinalEpisodeAction.None, handler.resolve(ANIME_ID))
        assertEquals(PlayerFinalEpisodeAction.None, handler.resolve(0))
        coVerify(exactly = 1) { getAnimeDetails(any()) }
    }

    @Test
    fun `failed session load suggests nothing`() = runTest {
        coEvery { getAnimeDetails(ANIME_ID) } returns details(status = "ongoing")
        coEvery { getAccountSession() } throws IllegalStateException("storage")

        assertEquals(PlayerFinalEpisodeAction.None, createHandler().resolve(ANIME_ID))
    }

    private fun details(status: String): AnimeDetails = mockk {
        every { this@mockk.status } returns status
    }

    private fun createHandler() = PlayerFinalEpisodeActionHandler(
        getAnimeDetails = getAnimeDetails,
        getAccountSession = getAccountSession,
    )

    private companion object {
        const val ANIME_ID = 42
        const val USER_ID = 7
    }
}
