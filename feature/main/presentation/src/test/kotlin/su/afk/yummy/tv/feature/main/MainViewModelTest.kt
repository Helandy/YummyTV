package su.afk.yummy.tv.feature.main

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.MainSettingsSnapshot
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.YaniApplicationTokenState
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.model.AccountMutationErrorEvent
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.feature.main.MainState.Effect
import su.afk.yummy.tv.feature.main.MainState.Event
import su.afk.yummy.tv.feature.main.handler.MainSideEffectsHandler
import su.afk.yummy.tv.feature.main.handler.MainUpdateCheckResult
import su.afk.yummy.tv.feature.update.navigator.UpdateDestination

class MainViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val analytics: MainAnalytics = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk()
    private val nav: INavigationManager = mockk(relaxed = true)
    private val featureToggleUpdateObserver: FeatureToggleUpdateObserver = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val mainSideEffectsHandler: MainSideEffectsHandler = mockk()
    private val accountMutationErrorRepository: AccountMutationErrorRepository = mockk()
    private val stringProvider: StringProvider = mockk()
    private val networkConnectivityMonitor: NetworkConnectivityMonitor = mockk()

    private val snapshot = MutableStateFlow(snapshot(nickname = "sandy", unread = 4))
    private val tokenState = MutableStateFlow(YaniApplicationTokenState.DEFAULT)
    private val session = MutableStateFlow(AccountSession(isAuthorized = true, userId = 1))
    private val isOnline = MutableStateFlow(true)
    private val mutationErrors = MutableSharedFlow<AccountMutationErrorEvent>()
    private val toggleUpdates = MutableSharedFlow<Long>()

    @Before
    fun setUp() {
        every { settingsStore.mainSettingsSnapshot } returns snapshot
        every { settingsStore.yaniApplicationTokenState } returns tokenState
        every { accountRepository.observeSession() } returns session
        every { networkConnectivityMonitor.isOnline } returns isOnline
        every { accountMutationErrorRepository.events } returns mutationErrors
        every { featureToggleUpdateObserver.currentActivationId } returns 1L
        every { featureToggleUpdateObserver.updates } returns toggleUpdates
        every { stringProvider.get(any<Int>()) } returns "mutation error"
        coEvery { mainSideEffectsHandler.checkForUpdates() } returns MainUpdateCheckResult.NotAvailable
        coEvery { mainSideEffectsHandler.restoreAccountIfMissing() } returns Unit
        coEvery { mainSideEffectsHandler.refreshAccountIfStale(any()) } returns Unit
    }

    private fun createViewModel() = MainViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        analytics = analytics,
        settingsStore = settingsStore,
        nav = nav,
        featureToggleUpdateObserver = featureToggleUpdateObserver,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        mainSideEffectsHandler = mainSideEffectsHandler,
        accountMutationErrorRepository = accountMutationErrorRepository,
        stringProvider = stringProvider,
        networkConnectivityMonitor = networkConnectivityMonitor,
        interfaceModePreferences = mockk(relaxed = true),
    )

    private fun snapshot(nickname: String, unread: Int) = MainSettingsSnapshot(
        appTheme = AppTheme.entries.first(),
        backgroundStyle = BackgroundStyle.entries.first(),
        posterQuality = PosterQuality.entries.first(),
        posterCardSize = PosterCardSize.entries.first(),
        yaniNickname = nickname,
        yaniAvatarUrl = "avatar",
        yaniUnreadNotificationsCount = unread,
    )

    private fun update(required: Boolean) = MainUpdateCheckResult.Available(
        version = "2.0",
        apkUrl = "url",
        changelog = "log",
        required = required,
        updatesCount = 2,
        isPrerelease = false,
    )

    @Test
    fun `settings and the signed-in session are mirrored into the state`() {
        val state = createViewModel().currentState

        assertEquals("sandy", state.yaniNickname)
        assertEquals("avatar", state.yaniAvatarUrl)
        assertEquals(4, state.unreadNotificationsCount)
        assertTrue(state.isYaniSignedIn)
        assertTrue(state.isYaniAuthResolved)
        verify(exactly = 1) { analytics.eventScreenOpened() }
        verify(exactly = 1) { analytics.eventAppSession(true, YaniApplicationTokenState.DEFAULT) }
    }

    @Test
    fun `guest has no unread notifications`() {
        session.value = AccountSession(isAuthorized = false, userId = 0)

        val state = createViewModel().currentState

        assertFalse(state.isYaniSignedIn)
        assertEquals(0, state.unreadNotificationsCount)
    }

    @Test
    fun `connectivity changes are mirrored`() {
        val vm = createViewModel()

        isOnline.value = false

        assertFalse(vm.currentState.isOnline)
    }

    @Test
    fun `startup restores and refreshes the account`() {
        createViewModel()

        coVerify(exactly = 1) { mainSideEffectsHandler.restoreAccountIfMissing() }
        coVerify(exactly = 1) { mainSideEffectsHandler.refreshAccountIfStale(any()) }
    }

    @Test
    fun `optional update opens the update screen on top`() {
        coEvery { mainSideEffectsHandler.checkForUpdates() } returns update(required = false)
        val destination = slot<UpdateDestination>()

        createViewModel()

        verify(exactly = 1) { nav.navigate(capture(destination)) }
        assertEquals("2.0", destination.captured.version)
        verify(exactly = 0) { nav.replace(any()) }
    }

    @Test
    fun `required update replaces the current screen`() {
        coEvery { mainSideEffectsHandler.checkForUpdates() } returns update(required = true)

        createViewModel()

        verify(exactly = 1) { nav.replace(any()) }
        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `newer feature toggle activation rechecks updates`() = runTest {
        createViewModel()
        coVerify(exactly = 1) { mainSideEffectsHandler.checkForUpdates() }

        toggleUpdates.emit(1L)
        coVerify(exactly = 1) { mainSideEffectsHandler.checkForUpdates() }

        toggleUpdates.emit(2L)
        coVerify(exactly = 2) { mainSideEffectsHandler.checkForUpdates() }
    }

    @Test
    fun `account mutation error becomes a toast`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        mutationErrors.emit(AccountMutationErrorEvent(AccountMutationAction.entries.first(), null))

        assertEquals(listOf<Effect>(Effect.ShowToast("mutation error")), effects)
    }

    @Test
    fun `root selection switches the tab`() {
        createViewModel().setEvent(Event.RootSelected(RootTab.HOME, popToRootOnReselect = true))

        verify(exactly = 1) { nav.switchRoot(RootTab.HOME, true) }
    }
}
