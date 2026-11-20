package su.afk.yummy.tv.feature.account.account

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.YaniAccount
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.account.AccountState.AccountTab
import su.afk.yummy.tv.feature.account.account.AccountState.Effect
import su.afk.yummy.tv.feature.account.account.AccountState.Event
import su.afk.yummy.tv.feature.account.account.handler.AccountCachedHub
import su.afk.yummy.tv.feature.account.account.handler.AccountHubHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountLocalAuthHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountLoginResult
import su.afk.yummy.tv.feature.account.account.handler.AccountRefreshResult
import su.afk.yummy.tv.feature.account.account.handler.AccountSessionHandler
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.localauth.LocalAuthAnalytics
import su.afk.yummy.tv.feature.faq.IFaqNavigator
import su.afk.yummy.tv.feature.glossary.IGlossaryNavigator
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator

class AccountViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val settingsStore: YaniAccountSettingsStore = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk()
    private val videoDownloadNavigator: IVideoDownloadNavigator = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk(relaxed = true)
    private val faqNavigator: IFaqNavigator = mockk(relaxed = true)
    private val glossaryNavigator: IGlossaryNavigator = mockk(relaxed = true)
    private val sitePagesNavigator: ISitePagesNavigator = mockk(relaxed = true)
    private val settingsNavigator: ISettingsNavigator = mockk(relaxed = true)
    private val sessionHandler: AccountSessionHandler = mockk()
    private val hubHandler: AccountHubHandler = mockk()
    private val localAuthHandler: AccountLocalAuthHandler = mockk(relaxed = true)
    private val analytics: AccountAnalytics = mockk(relaxed = true)
    private val localAuthAnalytics: LocalAuthAnalytics = mockk(relaxed = true)

    private val session = MutableStateFlow(AccountSession(isAuthorized = true, userId = USER_ID))
    private val nickname = MutableStateFlow("")
    private val avatarUrl = MutableStateFlow("")
    private val unreadCount = MutableStateFlow(0)

    @Before
    fun setUp() {
        every { accountRepository.observeSession() } returns session
        every { settingsStore.yaniNickname } returns nickname
        every { settingsStore.yaniAvatarUrl } returns avatarUrl
        every { settingsStore.yaniUnreadNotificationsCount } returns unreadCount
        every { sessionHandler.onSessionSnapshot(any()) } just Runs
        every { sessionHandler.isAuthorized() } answers { session.value.isAuthorized }
        every { sessionHandler.beginMissingProfileRecoveryIfNeeded(any()) } returns false
        every { sessionHandler.markHubLoadIfNeeded(any(), any()) } returns true
        every { sessionHandler.isHubLoadedFor(any()) } returns false
        every { sessionHandler.markProfileChanged() } just Runs
        every { sessionHandler.completeMissingProfileRecovery() } just Runs
        stubHub()
    }

    private fun createViewModel() = AccountViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        settingsStore = settingsStore,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        videoDownloadNavigator = videoDownloadNavigator,
        accountNavigator = accountNavigator,
        faqNavigator = faqNavigator,
        glossaryNavigator = glossaryNavigator,
        sitePagesNavigator = sitePagesNavigator,
        settingsNavigator = settingsNavigator,
        sessionHandler = sessionHandler,
        hubHandler = hubHandler,
        localAuthHandler = localAuthHandler,
        analytics = analytics,
        localAuthAnalytics = localAuthAnalytics,
    )

    private fun stubHub(
        summary: Result<UserProfileSummary> = Result.success(UserProfileSummary(userId = USER_ID, nickname = "sandy")),
        stats: Result<UserStats> = Result.failure(IllegalStateException("stats")),
    ) {
        coEvery { hubHandler.loadCached(any()) } returns AccountCachedHub(null, null)
        coEvery { hubHandler.loadProfileSummary(any()) } returns summary
        coEvery { hubHandler.loadStats(any()) } returns stats
        coEvery { hubHandler.loadNotificationCounts() } returns Result.success(emptyList())
    }

    @Test
    fun `signed-in session resolves the screen and loads the hub`() {
        val state = createViewModel().currentState

        assertTrue(state.isSessionResolved)
        assertTrue(state.isSignedIn)
        assertEquals(USER_ID, state.userId)
        assertEquals("sandy", state.profileSummary?.nickname)
        assertFalse(state.isStatsLoading)
        verify(exactly = 1) { analytics.eventScreenOpened() }
    }

    @Test
    fun `guest session resets the account data`() {
        every { sessionHandler.markHubLoadIfNeeded(any(), any()) } returns false
        session.value = AccountSession(isAuthorized = false, userId = 0)

        val state = createViewModel().currentState

        assertTrue(state.isSessionResolved)
        assertFalse(state.isSignedIn)
        assertEquals(0, state.userId)
        assertNull(state.profileSummary)
    }

    @Test
    fun `stored nickname and avatar are mirrored into the state`() {
        val vm = createViewModel()

        nickname.value = "sandy"
        avatarUrl.value = "avatar"

        assertEquals("sandy", vm.currentState.nickname)
        assertEquals("avatar", vm.currentState.avatarUrl)
    }

    @Test
    fun `hub failure without any data shows the hub error`() {
        stubHub(summary = Result.failure(IllegalStateException("summary")))

        val state = createViewModel().currentState

        assertEquals(AccountUiError.LOAD_PROFILE_STATISTICS_FAILED, state.hubError)
        assertFalse(state.isStatsLoading)
    }

    @Test
    fun `unread notifications count is mirrored from the shared setting`() {
        val vm = createViewModel()

        unreadCount.value = 4

        assertEquals(4, vm.currentState.unreadNotificationCount)
    }

    @Test
    fun `empty credentials are rejected without a request`() {
        val vm = createViewModel()

        vm.setEvent(Event.LoginSelected)

        assertEquals(AccountUiError.CREDENTIALS_REQUIRED, vm.currentState.error)
        coVerify(exactly = 0) { sessionHandler.login(any(), any()) }
    }

    @Test
    fun `successful login stores the account and reloads the hub`() {
        coEvery { sessionHandler.login(any(), any()) } returns
            AccountLoginResult.Success(YaniAccount(id = 8, nickname = "sandy", avatarUrl = "a"))
        val vm = createViewModel()
        vm.setEvent(Event.LoginChanged(" sandy "))
        vm.setEvent(Event.PasswordChanged("secret"))

        vm.setEvent(Event.LoginSelected)

        assertTrue(vm.currentState.isSignedIn)
        assertEquals(8, vm.currentState.userId)
        assertEquals("a", vm.currentState.avatarUrl)
        assertFalse(vm.currentState.isLoading)
        verify(exactly = 1) { analytics.eventLoginSuccess() }
        coVerify(exactly = 1) { sessionHandler.login(match { it.login == "sandy" && it.password == "secret" }, null) }
    }

    @Test
    fun `captcha demand asks for a captcha and emits keyboard and hint effects`() = runTest {
        coEvery { sessionHandler.login(any(), any()) } returns AccountLoginResult.CaptchaRequired(rejected = false)
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.LoginChanged("sandy"))
        vm.setEvent(Event.PasswordChanged("secret"))

        vm.setEvent(Event.LoginSelected)

        assertTrue(vm.currentState.isCaptchaRequired)
        assertNull(vm.currentState.captchaError)
        assertEquals(listOf(Effect.HideKeyboard, Effect.ShowCaptchaHint), effects)
    }

    @Test
    fun `rejected captcha token is reported`() {
        coEvery { sessionHandler.login(any(), any()) } returns AccountLoginResult.CaptchaRequired(rejected = true)
        val vm = createViewModel()
        vm.setEvent(Event.LoginChanged("sandy"))
        vm.setEvent(Event.PasswordChanged("secret"))

        vm.setEvent(Event.CaptchaSolved("token"))

        assertEquals(AccountUiError.CAPTCHA_REJECTED, vm.currentState.captchaError)
        coVerify(exactly = 1) { sessionHandler.login(any(), "token") }
    }

    @Test
    fun `blank captcha token is an error`() {
        val vm = createViewModel()

        vm.setEvent(Event.CaptchaSolved(" "))

        assertEquals(AccountUiError.CAPTCHA_RESPONSE_EMPTY, vm.currentState.error)
    }

    @Test
    fun `failed login keeps the server message and discards autofill`() = runTest {
        coEvery { sessionHandler.login(any(), any()) } returns AccountLoginResult.Failure("Wrong password")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.LoginChanged("sandy"))
        vm.setEvent(Event.PasswordChanged("secret"))

        vm.setEvent(Event.LoginSelected)

        assertEquals(AccountUiError.SIGN_IN_FAILED, vm.currentState.error)
        assertEquals("Wrong password", vm.currentState.errorMessage)
        assertEquals(listOf<Effect>(Effect.DiscardAutofill), effects)
    }

    @Test
    fun `expired captcha refreshes the challenge`() {
        val vm = createViewModel()

        vm.setEvent(Event.CaptchaExpired)

        assertEquals(AccountUiError.CAPTCHA_EXPIRED, vm.currentState.captchaError)
        assertEquals(1, vm.currentState.captchaChallengeId)
    }

    @Test
    fun `logout clears the profile and leaves the screen`() {
        coEvery { sessionHandler.logout() } returns true
        val vm = createViewModel()

        vm.setEvent(Event.LogoutSelected)

        assertNull(vm.currentState.profileSummary)
        assertFalse(vm.currentState.isLoading)
        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `failed logout stays on the screen with an error`() {
        coEvery { sessionHandler.logout() } returns false
        val vm = createViewModel()

        vm.setEvent(Event.LogoutSelected)

        assertEquals(AccountUiError.LOGOUT_FAILED, vm.currentState.error)
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `profile refresh reloads the hub and failure reports an error`() {
        coEvery { sessionHandler.refreshProfile() } returns AccountRefreshResult.Success(null)
        val vm = createViewModel()

        vm.setEvent(Event.RefreshProfileSelected)

        verify(exactly = 1) { sessionHandler.markProfileChanged() }
        coVerify(atLeast = 2) { hubHandler.loadProfileSummary(USER_ID) }

        coEvery { sessionHandler.refreshProfile() } returns AccountRefreshResult.Failure
        vm.setEvent(Event.RefreshProfileSelected)

        assertEquals(AccountUiError.REFRESH_FAILED, vm.currentState.error)
    }

    @Test
    fun `menu events open their destinations`() {
        val vm = createViewModel()

        vm.setEvent(Event.DownloadedEpisodesSelected)
        vm.setEvent(Event.FaqSelected)
        vm.setEvent(Event.GlossarySelected)
        vm.setEvent(Event.SitePagesSelected)
        vm.setEvent(Event.SettingsSelected)
        vm.setEvent(Event.NotificationsSelected)
        vm.setEvent(Event.ProfileEditSelected)
        vm.setEvent(Event.PasswordResetSelected)
        vm.setEvent(Event.RegistrationSelected)

        verify(exactly = 9) { nav.navigate(any()) }
    }

    @Test
    fun `guest cannot open the signed-in only screens`() {
        session.value = AccountSession(isAuthorized = false, userId = 0)
        val vm = createViewModel()

        vm.setEvent(Event.ProfileEditSelected)

        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `tab selection is tracked once per change`() {
        val vm = createViewModel()

        vm.setEvent(Event.TabSelected(AccountTab.NOTIFICATIONS))
        vm.setEvent(Event.TabSelected(AccountTab.NOTIFICATIONS))

        assertEquals(AccountTab.NOTIFICATIONS, vm.currentState.selectedTab)
        verify(exactly = 1) { analytics.eventTabSelected(AccountTab.NOTIFICATIONS) }
    }

    @Test
    fun `local auth server states are mirrored and a terminal state stops the server`() {
        val callback = slot<(LocalAuthServerState) -> Unit>()
        every { localAuthHandler.startServer(any(), capture(callback)) } just Runs
        val vm = createViewModel()

        vm.setEvent(Event.StartLocalAuthServerSelected)
        val pairing = LocalAuthServerState.Pairing(pin = "PIN", port = 1, serviceName = "tv", attemptsLeft = 3)
        callback.captured(pairing)

        assertEquals(pairing, vm.currentState.localAuthServerState)
        verify(exactly = 1) { localAuthAnalytics.eventTvPinShown() }

        callback.captured(LocalAuthServerState.Success)

        verify(exactly = 1) { localAuthAnalytics.eventTvTransferSuccess() }
        verify(exactly = 1) { localAuthHandler.cancelServer() }
    }

    @Test
    fun `denied local auth permission becomes an error state`() {
        val vm = createViewModel()

        vm.setEvent(Event.LocalAuthPermissionDenied)

        assertEquals(
            LocalAuthServerState.Error(LocalAuthError.PERMISSION_DENIED),
            vm.currentState.localAuthServerState,
        )
        verify(exactly = 1) { localAuthHandler.cancelServer() }
    }

    @Test
    fun `stopping the local auth server returns to idle`() {
        val vm = createViewModel()

        vm.setEvent(Event.StopLocalAuthServerSelected)

        assertEquals(LocalAuthServerState.Idle, vm.currentState.localAuthServerState)
        verify(exactly = 1) { localAuthHandler.stopServer(any()) }
    }

    @Test
    fun `back leaves the screen`() {
        val vm = createViewModel()

        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val USER_ID = 5
    }
}
