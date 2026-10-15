package su.afk.yummy.tv.feature.main.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.featuretoggle.api.VersionSupportChecker
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.RestoreAccountSessionUseCase
import su.afk.yummy.tv.domain.update.model.AppRelease
import su.afk.yummy.tv.domain.update.usecase.GetLatestAppReleaseUseCase
import kotlin.time.Duration.Companion.hours

class MainSideEffectsHandlerTest : BaseUnitTest() {

    private val getLatestAppRelease: GetLatestAppReleaseUseCase = mockk()
    private val versionSupportChecker: VersionSupportChecker = mockk()
    private val getAccountSession: GetAccountSessionUseCase = mockk()
    private val refreshAccount: RefreshAccountUseCase = mockk(relaxed = true)
    private val restoreAccountSession: RestoreAccountSessionUseCase = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk()

    @Before
    fun setUp() {
        every { versionSupportChecker.isCurrentVersionSupported() } returns true
        every { settingsStore.betaUpdatesEnabled } returns flowOf(false)
    }

    @Test
    fun `newer release is offered as optional`() = runTest {
        coEvery { getLatestAppRelease(CURRENT, false) } returns release("1.26.0")

        val result = createHandler().checkForUpdates() as MainUpdateCheckResult.Available

        assertEquals("1.26.0", result.version)
        assertEquals(false, result.required)
    }

    @Test
    fun `same version is not offered unless the current one is no longer supported`() = runTest {
        coEvery { getLatestAppRelease(any(), any()) } returns release(CURRENT)
        val handler = createHandler()

        assertEquals(MainUpdateCheckResult.NotAvailable, handler.checkForUpdates())

        every { versionSupportChecker.isCurrentVersionSupported() } returns false
        assertEquals(true, (handler.checkForUpdates() as MainUpdateCheckResult.Available).required)
    }

    @Test
    fun `beta channel asks for prereleases`() = runTest {
        every { settingsStore.betaUpdatesEnabled } returns flowOf(true)
        coEvery { getLatestAppRelease(CURRENT, true) } returns null

        createHandler().checkForUpdates()

        coVerify { getLatestAppRelease(CURRENT, true) }
    }

    @Test
    fun `slow or failing release check gives no update`() = runTest {
        val handler = createHandler()

        coEvery { getLatestAppRelease(any(), any()) } coAnswers { awaitCancellation() }
        assertEquals(MainUpdateCheckResult.NotAvailable, handler.checkForUpdates())

        coEvery { getLatestAppRelease(any(), any()) } throws IllegalStateException("github")
        assertEquals(MainUpdateCheckResult.NotAvailable, handler.checkForUpdates())
    }

    @Test
    fun `session is refreshed only when stale and signed in`() = runTest {
        val handler = createHandler()
        coEvery { getAccountSession() } returns AccountSession(isAuthorized = true, userId = 5)

        every { settingsStore.yaniTokenRefreshAt } returns flowOf(System.currentTimeMillis())
        handler.refreshAccountIfStale(maxAge = 48.hours)
        coVerify(exactly = 0) { refreshAccount() }

        every { settingsStore.yaniTokenRefreshAt } returns flowOf(0L)
        handler.refreshAccountIfStale(maxAge = 48.hours)
        coVerify(exactly = 1) { refreshAccount() }

        coEvery { getAccountSession() } returns AccountSession(isAuthorized = false, userId = 0)
        handler.refreshAccountIfStale(maxAge = 48.hours)
        coVerify(exactly = 1) { refreshAccount() }
    }

    @Test
    fun `failed session restore does not crash the start`() = runTest {
        coEvery { restoreAccountSession() } throws IllegalStateException("keystore")

        createHandler().restoreAccountIfMissing()
    }

    private fun release(version: String) = AppRelease(version = version, changelog = "notes", apkUrl = "https://apk")

    private fun createHandler() = MainSideEffectsHandler(
        getLatestAppRelease = getLatestAppRelease,
        versionSupportChecker = versionSupportChecker,
        getAccountSession = getAccountSession,
        refreshAccount = refreshAccount,
        restoreAccountSession = restoreAccountSession,
        settingsStore = settingsStore,
        syncRemoteLibrary = mockk(relaxed = true),
        versionName = CURRENT,
    )

    private companion object {
        const val CURRENT = "1.25.1"
    }
}
