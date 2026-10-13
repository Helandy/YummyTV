package su.afk.yummy.tv.feature.account.account.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.LoginException
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.usecase.LoginUseCase
import su.afk.yummy.tv.domain.account.usecase.LogoutUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.SyncVideoWatchesUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import su.afk.yummy.tv.domain.player.usecase.GetMeaningfulVideoProgressUseCase
import su.afk.yummy.tv.feature.account.utils.AccountLoginCredentials
import java.io.IOException
import java.security.KeyStoreException

class AccountAuthHandlerTest : BaseUnitTest() {

    private val login: LoginUseCase = mockk()
    private val logout: LogoutUseCase = mockk(relaxed = true)
    private val refreshAccount: RefreshAccountUseCase = mockk()
    private val syncVideoWatches: SyncVideoWatchesUseCase = mockk()
    private val refreshHomeFeed: RefreshHomeFeedUseCase = mockk(relaxed = true)
    private val getMeaningfulVideoProgress: GetMeaningfulVideoProgressUseCase = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val errorHandler: ErrorHandler = mockk()

    private val credentials = AccountLoginCredentials(login = "user", password = "secret")
    private val account = YaniAccount(id = 5, nickname = "user")

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed")
        coEvery { getMeaningfulVideoProgress() } returns emptyList()
        coEvery { syncVideoWatches(any()) } returns true
    }

    @Test
    fun `successful login syncs local watches and refreshes the feed`() = runTest {
        coEvery { login("user", "secret", null) } returns account
        coEvery { getMeaningfulVideoProgress() } returns listOf(progress(videoId = 7, positionMs = 90_000L, updatedAt = 5_000L))

        val result = createHandler().login(credentials, captchaResponse = null)

        assertEquals(AccountLoginResult.Success(account), result)
        coVerify { syncVideoWatches(match { it.single().videoId == 7 && it.single().timeSeconds == 90 && it.single().dateSeconds == 5 }) }
        coVerify { refreshHomeFeed() }
    }

    @Test
    fun `login succeeds even when post-login sync and refresh fail`() = runTest {
        coEvery { login(any(), any(), any()) } returns account
        coEvery { getMeaningfulVideoProgress() } throws IllegalStateException("db")
        coEvery { refreshHomeFeed() } throws IOException("offline")

        assertEquals(AccountLoginResult.Success(account), createHandler().login(credentials, null))
    }

    @Test
    fun `captcha is requested, and a rejected answer is flagged`() = runTest {
        coEvery { login(any(), any(), any()) } throws AccountCaptchaRequiredException()
        val handler = createHandler()

        assertEquals(AccountLoginResult.CaptchaRequired(rejected = false), handler.login(credentials, null))
        assertEquals(AccountLoginResult.CaptchaRequired(rejected = true), handler.login(credentials, "answer"))
    }

    @Test
    fun `server refusal keeps its message`() = runTest {
        coEvery { login(any(), any(), any()) } throws LoginException("Wrong password")

        assertEquals(AccountLoginResult.Failure("Wrong password"), createHandler().login(credentials, null))
    }

    @Test
    fun `unexpected failure names its cause and is reported, network failure is not named`() = runTest {
        val handler = createHandler()

        coEvery { login(any(), any(), any()) } throws KeyStoreException("broken")
        assertEquals(AccountLoginResult.Failure("parsed (KeyStoreException)"), handler.login(credentials, null))
        verify { analyticsTracker.reportError(any(), any<KeyStoreException>(), any()) }

        coEvery { login(any(), any(), any()) } throws IOException("offline")
        assertEquals(AccountLoginResult.Failure("parsed"), handler.login(credentials, null))
    }

    @Test
    fun `logout and refresh report their outcome`() = runTest {
        val handler = createHandler()
        coEvery { refreshAccount() } returns account

        assertTrue(handler.logout())
        assertEquals(AccountRefreshResult.Success(account), handler.refreshProfile())

        coEvery { logout() } throws IllegalStateException("io")
        coEvery { refreshAccount() } throws IOException("offline")
        assertFalse(handler.logout())
        assertEquals(AccountRefreshResult.Failure, handler.refreshProfile())
    }

    private fun progress(videoId: Int, positionMs: Long, updatedAt: Long) = AnimeWatchProgress(
        animeId = 1,
        episode = "1",
        videoId = videoId,
        episodeUrl = "",
        positionMs = positionMs,
        durationMs = 1_440_000L,
        updatedAt = updatedAt,
    )

    private fun createHandler() = AccountAuthHandler(
        loginUseCase = login,
        logoutUseCase = logout,
        refreshAccountUseCase = refreshAccount,
        syncVideoWatches = syncVideoWatches,
        refreshHomeFeed = refreshHomeFeed,
        getMeaningfulVideoProgress = getMeaningfulVideoProgress,
        analyticsTracker = analyticsTracker,
        errorHandler = errorHandler,
    )
}
