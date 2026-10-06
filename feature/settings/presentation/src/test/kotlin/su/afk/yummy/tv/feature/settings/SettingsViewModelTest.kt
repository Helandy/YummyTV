package su.afk.yummy.tv.feature.settings

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsDeviceIdProvider
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.SettingsSnapshot
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.auth.TokenStorageMode
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.core.utils.logging.AppLogExporter
import su.afk.yummy.tv.core.utils.system.CacheStorageInspector
import su.afk.yummy.tv.core.utils.system.CacheStorageReport
import su.afk.yummy.tv.domain.update.model.AppReleaseNotes
import su.afk.yummy.tv.domain.update.repository.UpdateRepository
import su.afk.yummy.tv.domain.update.usecase.GetAppReleaseHistoryUseCase
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoExportDestinationUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.SelectVideoExportDestinationUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.settings.SettingsState.Effect
import su.afk.yummy.tv.feature.settings.SettingsState.Event
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.model.ReleaseNotesStatus
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategory
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategoryDestination
import su.afk.yummy.tv.feature.settings.navigator.SettingsDetailsButtonOrderDestination

class SettingsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk(relaxed = true)
    private val yaniAuthPreferences: YaniAuthPreferences = mockk()
    private val interfaceModePreferences: AppInterfaceModePreferences = mockk(relaxed = true)
    private val tvIntegration: ITvIntegration = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk(relaxed = true)
    private val analytics: SettingsAnalytics = mockk(relaxed = true)
    private val exportRepository: VideoDownloadExportRepository = mockk(relaxed = true)
    private val cacheStorageInspector: CacheStorageInspector = mockk()
    private val appLogExporter: AppLogExporter = mockk()
    private val updateRepository: UpdateRepository = mockk()
    private val analyticsDeviceIdProvider: AnalyticsDeviceIdProvider = mockk()

    private val snapshot = MutableStateFlow(snapshot())
    private val saveLastSearch = MutableStateFlow(false)
    private val betaUpdates = MutableStateFlow(false)
    private val storageMode = MutableStateFlow(TokenStorageMode.entries.first())
    private val exportDestination = MutableStateFlow<VideoExportDestination?>(null)

    @Before
    fun setUp() {
        every { settingsStore.settingsSnapshot } returns snapshot
        every { settingsStore.playerSubtitleStyle } returns MutableStateFlow(mockk<PlayerSubtitleStyleSettings>())
        every { settingsStore.mobilePlayerGestureTutorialDismissed } returns MutableStateFlow(false)
        every { settingsStore.tvPlayerControlsTutorialDismissed } returns MutableStateFlow(false)
        every { settingsStore.saveLastSearchEnabled } returns saveLastSearch
        every { settingsStore.betaUpdatesEnabled } returns betaUpdates
        every { yaniAuthPreferences.storageMode } returns storageMode
        every { tvIntegration.previewChannelBrowsable } returns MutableStateFlow(true)
        every { exportRepository.observeDestination() } returns exportDestination
        every { interfaceModePreferences.selectedMode } returns AppInterfaceMode.MOBILE
        coEvery { cacheStorageInspector.inspect() } returns CacheStorageReport(entries = emptyList(), totalBytes = 1_024L)
        coEvery { analyticsDeviceIdProvider.deviceId() } returns "abcdefgh"
    }

    private fun createViewModel() = SettingsViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        settingsStore = settingsStore,
        yaniAuthPreferences = yaniAuthPreferences,
        interfaceModePreferences = interfaceModePreferences,
        tvIntegration = tvIntegration,
        nav = nav,
        accountNavigator = accountNavigator,
        analytics = analytics,
        observeVideoExportDestination = ObserveVideoExportDestinationUseCase(exportRepository),
        selectVideoExportDestination = SelectVideoExportDestinationUseCase(exportRepository),
        cacheStorageInspector = cacheStorageInspector,
        appLogExporter = appLogExporter,
        getAppReleaseHistory = GetAppReleaseHistoryUseCase(updateRepository),
        analyticsDeviceIdProvider = analyticsDeviceIdProvider,
        versionName = "1.0.0",
    )

    private fun snapshot(
        autoPlay: Boolean = false,
        order: List<DetailsButtonAction> = DetailsButtonAction.entries,
    ) = SettingsSnapshot(
        appTheme = AppTheme.entries.first(),
        backgroundStyle = BackgroundStyle.entries.first(),
        posterQuality = PosterQuality.entries.first(),
        posterCardSize = PosterCardSize.entries.first(),
        showTopTitleYear = false,
        showLibraryTitleYear = false,
        libraryContinueWatchingCardSize = LibraryContinueWatchingCardSize.entries.first(),
        preferredPlayer = PreferredPlayer.entries.first(),
        preferredVideoQuality = PreferredVideoQuality.entries.first(),
        watchNextEnabled = false,
        previewCacheSize = 1,
        autoSkipOpeningsEndings = false,
        autoSkipDelaySeconds = 1,
        showOpeningOnTimeline = false,
        autoPlayNextEpisode = autoPlay,
        nextEpisodeSwitchDelaySeconds = 1,
        playerControlsAutoHideSeconds = 1,
        askDubbingOnWatch = false,
        pictureInPictureEnabled = false,
        playerOrientationMode = PlayerOrientationMode.entries.first(),
        suggestNextEpisodeOnWatched = false,
        watchedThresholds = WatchedThresholds(),
        refreshContinueWatchingProgressOnLaunch = false,
        tvPlayerVolumeKeysEnabled = false,
        advancedPlayerVolumeEnabled = false,
        volumeStabilizationEnabled = false,
        playerBufferProfile = PlayerBufferProfile.entries.first(),
        videoExportAutoEnabled = false,
        yaniApplicationToken = "token",
        contentLanguage = YaniContentLanguage.entries.first(),
        detailsButtonOrder = order,
    )

    @Test
    fun `snapshot and the stored values are mirrored into the state`() {
        val state = createViewModel().currentState

        assertEquals("token", state.yaniApplicationToken)
        assertEquals(DetailsButtonAction.entries, state.detailsButtonOrder)
        assertFalse(state.isFallbackSessionStorage.xor(storageMode.value == TokenStorageMode.FALLBACK))
        assertTrue(state.isPreviewChannelBrowsable)
        assertEquals(AppInterfaceMode.MOBILE, state.interfaceMode)
        verify(exactly = 1) { analytics.eventScreenOpened() }
    }

    @Test
    fun `later snapshot updates reach the state`() {
        val vm = createViewModel()

        snapshot.value = snapshot(autoPlay = true)

        assertTrue(vm.currentState.autoPlayNextEpisode)
    }

    @Test
    fun `cache storage and device id are loaded`() {
        val state = createViewModel().currentState

        assertFalse(state.isCacheStorageLoading)
        assertEquals(1_024L, state.cacheStorageTotalBytes)
        assertEquals("abc def gh", state.analyticsDeviceId)
    }

    @Test
    fun `failed cache inspection keeps the previous values and stops loading`() {
        coEvery { cacheStorageInspector.inspect() } throws IllegalStateException("boom")

        val state = createViewModel().currentState

        assertFalse(state.isCacheStorageLoading)
        assertEquals(0L, state.cacheStorageTotalBytes)
    }

    @Test
    fun `refresh request inspects the cache again`() {
        val vm = createViewModel()

        vm.setEvent(Event.CacheStorageRefreshRequested)

        coVerify(exactly = 2) { cacheStorageInspector.inspect() }
    }

    @Test
    fun `video export directory name follows the destination`() {
        exportDestination.value = VideoExportDestination("content://dir", "Movies")

        val state = createViewModel().currentState

        assertEquals("Movies", state.videoExportDirectoryName)
    }

    @Test
    fun `changing the interface mode saves it and asks for a restart`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.InterfaceModeSelected(AppInterfaceMode.MOBILE))
        assertTrue(effects.isEmpty())

        val other = AppInterfaceMode.entries.first { it != AppInterfaceMode.MOBILE }
        vm.setEvent(Event.InterfaceModeSelected(other))

        verify(exactly = 1) { interfaceModePreferences.select(other) }
        assertEquals(other, vm.currentState.interfaceMode)
        assertEquals(listOf<Effect>(Effect.RestartApplication), effects)
    }

    @Test
    fun `toggles write the opposite value to the store`() {
        val vm = createViewModel()

        vm.setEvent(Event.AutoPlayNextEpisodeToggled)
        vm.setEvent(Event.WatchNextToggled)
        vm.setEvent(Event.SaveLastSearchToggled)
        vm.setEvent(Event.BetaUpdatesToggled)

        coVerify(exactly = 1) { settingsStore.setAutoPlayNextEpisode(true) }
        coVerify(exactly = 1) { settingsStore.setWatchNextEnabled(true) }
        coVerify(exactly = 1) { settingsStore.setSaveLastSearchEnabled(true) }
        coVerify(exactly = 1) { settingsStore.setBetaUpdatesEnabled(true) }
        verify(exactly = 1) { analytics.eventAutoPlayNextEpisodeToggled(true) }
    }

    @Test
    fun `selections are saved`() {
        val vm = createViewModel()

        vm.setEvent(Event.AppThemeSelected(AppTheme.entries.last()))
        vm.setEvent(Event.PreferredPlayerSelected(PreferredPlayer.entries.last()))
        vm.setEvent(Event.AutoSkipDelayChanged(7))

        coVerify(exactly = 1) { settingsStore.setAppTheme(AppTheme.entries.last()) }
        coVerify(exactly = 1) { settingsStore.setPreferredPlayer(PreferredPlayer.entries.last()) }
        coVerify(exactly = 1) { settingsStore.setAutoSkipDelaySeconds(7) }
    }

    @Test
    fun `application token is shown immediately and saved`() {
        val vm = createViewModel()

        vm.setEvent(Event.YaniApplicationTokenChanged("new"))

        assertEquals("new", vm.currentState.yaniApplicationToken)
        coVerify(exactly = 1) { settingsStore.setYaniApplicationToken("new") }
    }

    @Test
    fun `details button is moved within the saved order`() {
        val vm = createViewModel()
        val order = DetailsButtonAction.entries
        val second = order[1]

        vm.setEvent(Event.DetailsButtonMoved(second, DetailsButtonMoveDirection.UP))

        coVerify(exactly = 1) {
            settingsStore.setDetailsButtonOrder(match { it.first() == second })
        }
    }

    @Test
    fun `details button order can be reset to the default`() {
        createViewModel().setEvent(Event.DetailsButtonOrderReset)

        coVerify(exactly = 1) { settingsStore.setDetailsButtonOrder(SettingsStore.defaultDetailsButtonOrder) }
    }

    @Test
    fun `video export directory picker is requested`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VideoExportDirectorySelected)

        assertEquals(listOf<Effect>(Effect.OpenVideoExportDirectoryPicker), effects)
    }

    @Test
    fun `failed export directory selection is reported`() = runTest {
        coEvery { exportRepository.selectDestination(any()) } throws IllegalStateException("denied")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VideoExportDirectoryGranted("content://dir"))

        assertEquals(listOf<Effect>(Effect.VideoExportDirectorySelectionFailed), effects)
    }

    @Test
    fun `release notes are loaded once and failures can be retried`() {
        coEvery { updateRepository.releaseHistory("1.0.0", false) } throws IllegalStateException("boom")
        val vm = createViewModel()

        vm.setEvent(Event.ReleaseNotesRequested)
        assertEquals(ReleaseNotesStatus.Error, vm.currentState.releaseNotes)

        coEvery { updateRepository.releaseHistory("1.0.0", false) } returns
            listOf(AppReleaseNotes("1.0.0", "2026-09-20", "notes", isPrerelease = false))
        vm.setEvent(Event.ReleaseNotesRequested)
        val loaded = vm.currentState.releaseNotes as ReleaseNotesStatus.Loaded
        assertEquals("20.09.2026", loaded.items.single().date)
        assertTrue(loaded.items.single().isCurrent)

        vm.setEvent(Event.ReleaseNotesRequested)
        coVerify(exactly = 2) { updateRepository.releaseHistory(any(), any()) }
    }

    @Test
    fun `beta channel toggle invalidates the loaded release notes`() {
        coEvery { updateRepository.releaseHistory(any(), any()) } returns emptyList()
        val vm = createViewModel()
        vm.setEvent(Event.ReleaseNotesRequested)

        vm.setEvent(Event.BetaUpdatesToggled)

        assertEquals(ReleaseNotesStatus.Idle, vm.currentState.releaseNotes)
    }

    @Test
    fun `shared logs become an effect and failures report a logs error`() = runTest {
        val file = mockk<java.io.File>()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://logs"
        coEvery { appLogExporter.buildDump() } returns file
        every { appLogExporter.shareUri(file) } returns uri
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ShareLogsClicked)
        assertEquals(listOf<Effect>(Effect.ShareLogs("content://logs")), effects)

        coEvery { appLogExporter.buildDump() } throws IllegalStateException("boom")
        vm.setEvent(Event.ShareLogsClicked)
        assertEquals(Effect.LogsFailed, effects.last())
    }

    @Test
    fun `navigation events open their destinations`() {
        val vm = createViewModel()

        vm.setEvent(Event.DetailsButtonOrderSelected)
        vm.setEvent(Event.CategorySelected(SettingsCategory.entries.first()))
        vm.setEvent(Event.LoginOnTvSelected)
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.navigate(SettingsDetailsButtonOrderDestination) }
        verify(exactly = 1) { nav.navigate(SettingsCategoryDestination(SettingsCategory.entries.first())) }
        verify(exactly = 1) { accountNavigator.getLocalAuthDest() }
        verify(exactly = 1) { nav.back() }
    }
}
