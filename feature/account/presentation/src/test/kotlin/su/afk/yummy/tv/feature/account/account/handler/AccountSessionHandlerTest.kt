package su.afk.yummy.tv.feature.account.account.handler

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.account.account.AccountState

class AccountSessionHandlerTest : BaseUnitTest() {

    private val authHandler: AccountAuthHandler = mockk()

    @Test
    fun `missing profile is recovered once per session`() {
        val handler = AccountSessionHandler(authHandler)
        handler.onSessionSnapshot(isAuthorized = true)
        val noProfile = AccountState.State(isSignedIn = true, userId = 0)

        assertTrue(handler.beginMissingProfileRecoveryIfNeeded(noProfile))
        handler.completeMissingProfileRecovery()

        assertFalse(handler.beginMissingProfileRecoveryIfNeeded(noProfile))
        assertFalse(handler.beginMissingProfileRecoveryIfNeeded(AccountState.State(isSignedIn = true, userId = 5)))
    }

    @Test
    fun `no recovery without a session or while one is running`() {
        val handler = AccountSessionHandler(authHandler)
        val noProfile = AccountState.State(isSignedIn = true, userId = 0)

        assertFalse(handler.beginMissingProfileRecoveryIfNeeded(noProfile))

        handler.onSessionSnapshot(isAuthorized = true)
        handler.beginMissingProfileRecoveryIfNeeded(noProfile)
        handler.markProfileChanged()
        assertTrue(handler.beginMissingProfileRecoveryIfNeeded(noProfile))
    }

    @Test
    fun `hub is loaded once per user unless forced`() {
        val handler = AccountSessionHandler(authHandler)
        val user = AccountState.State(isSignedIn = true, userId = 5)

        assertTrue(handler.markHubLoadIfNeeded(user, force = false))
        assertTrue(handler.isHubLoadedFor(5))
        assertFalse(handler.markHubLoadIfNeeded(user, force = false))
        assertTrue(handler.markHubLoadIfNeeded(user, force = true))
        assertTrue(handler.markHubLoadIfNeeded(user.copy(userId = 6), force = false))
        assertFalse(handler.markHubLoadIfNeeded(AccountState.State(isSignedIn = false, userId = 7), force = true))
    }

    @Test
    fun `sign-out forgets the loaded hub`() = runTest {
        coEvery { authHandler.logout() } returns true
        val handler = AccountSessionHandler(authHandler)
        handler.onSessionSnapshot(isAuthorized = true)
        handler.markHubLoadIfNeeded(AccountState.State(isSignedIn = true, userId = 5), force = false)

        assertTrue(handler.logout())

        assertFalse(handler.isAuthorized())
        assertFalse(handler.isHubLoadedFor(5))
    }

    @Test
    fun `failed sign-out keeps the session`() = runTest {
        coEvery { authHandler.logout() } returns false
        val handler = AccountSessionHandler(authHandler)
        handler.onSessionSnapshot(isAuthorized = true)

        assertFalse(handler.logout())
        assertTrue(handler.isAuthorized())
    }
}
