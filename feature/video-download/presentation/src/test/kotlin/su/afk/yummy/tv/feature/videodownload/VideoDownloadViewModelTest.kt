package su.afk.yummy.tv.feature.videodownload

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadCacheKeyScheme
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import su.afk.yummy.tv.domain.videodownload.usecase.CancelOrDeleteVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.CancelVideoExportUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.CheckExportedFileExistsUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.EnqueueVideoExportUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadsUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoExportDestinationUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.PauseVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.RestartVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.SelectVideoExportDestinationUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.videodownload.VideoDownloadState.Effect
import su.afk.yummy.tv.feature.videodownload.VideoDownloadState.Event

class VideoDownloadViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val appLifecycleSettingsStore: AppLifecycleSettingsStore = mockk(relaxed = true)
    private val downloadRepository: VideoDownloadRepository = mockk(relaxed = true)
    private val exportRepository: VideoDownloadExportRepository = mockk(relaxed = true)
    private val playerNavigator: IPlayerNavigator = mockk()
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val items = MutableStateFlow<List<VideoDownloadItem>>(emptyList())
    private val destination = MutableStateFlow<VideoExportDestination?>(null)
    private val permissionRequested = MutableStateFlow(false)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { downloadRepository.observeDownloads() } returns items
        every { exportRepository.observeDestination() } returns destination
        every { appLifecycleSettingsStore.notificationPermissionRequested } returns permissionRequested
        every { playerNavigator.getDownloadedPlayerDest(any()) } returns navKey
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
    }

    private fun createViewModel() = VideoDownloadViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        appLifecycleSettingsStore = appLifecycleSettingsStore,
        observeVideoDownloads = ObserveVideoDownloadsUseCase(downloadRepository),
        cancelOrDeleteVideoDownload = CancelOrDeleteVideoDownloadUseCase(downloadRepository),
        pauseVideoDownload = PauseVideoDownloadUseCase(downloadRepository),
        restartVideoDownload = RestartVideoDownloadUseCase(downloadRepository),
        observeExportDestination = ObserveVideoExportDestinationUseCase(exportRepository),
        selectExportDestination = SelectVideoExportDestinationUseCase(exportRepository),
        enqueueVideoExport = EnqueueVideoExportUseCase(exportRepository),
        cancelVideoExport = CancelVideoExportUseCase(exportRepository),
        checkExportedFileExists = CheckExportedFileExistsUseCase(exportRepository),
        playerNavigator = playerNavigator,
        detailsNavigator = detailsNavigator,
    )

    private fun item(
        id: Long,
        status: VideoDownloadStatus = VideoDownloadStatus.Downloaded,
        exportStatus: VideoExportStatus = VideoExportStatus.Idle,
        exportDirectoryUri: String? = null,
        exportedFileUri: String? = null,
        bytes: Long = 100L,
    ) = VideoDownloadItem(
        id = id,
        animeId = 5,
        animeTitle = "Anime",
        posterUrl = "",
        episode = "1",
        videoId = 1,
        playerName = "Kodik",
        playerId = null,
        dubbing = "",
        iframeUrl = "",
        screenshotUrl = "",
        qualityLabel = "720p",
        streamUrl = "",
        headers = emptyMap(),
        cacheKey = "key$id",
        cacheKeyScheme = VideoDownloadCacheKeyScheme.entries.first(),
        status = status,
        progress = 1f,
        bytesDownloaded = bytes,
        totalBytes = null,
        errorMessage = null,
        exportStatus = exportStatus,
        exportProgress = 0f,
        exportDirectoryUri = exportDirectoryUri,
        exportedFileUri = exportedFileUri,
        exportErrorMessage = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `downloads destination and permission flag are mirrored`() {
        items.value = listOf(item(1, bytes = 10), item(2, bytes = 30))
        destination.value = DESTINATION
        permissionRequested.value = true

        val state = createViewModel().currentState

        assertEquals(listOf(1L, 2L), state.items.map { it.id })
        assertEquals(40L, state.occupiedBytes)
        assertEquals(DESTINATION, state.exportDestination)
        assertTrue(state.notificationPermissionRequested)
    }

    @Test
    fun `item and details selections navigate and invalid anime id is ignored`() {
        val vm = createViewModel()

        vm.setEvent(Event.ItemSelected(3))
        vm.setEvent(Event.DetailsSelected(5))
        vm.setEvent(Event.DetailsSelected(0))

        verify(exactly = 1) { playerNavigator.getDownloadedPlayerDest(3) }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(5) }
        verify(exactly = 2) { nav.navigate(navKey) }
    }

    @Test
    fun `delete asks for confirmation and removes the confirmed item`() {
        items.value = listOf(item(1))
        val vm = createViewModel()

        vm.setEvent(Event.DeleteSelected(1))
        assertEquals(1L, vm.currentState.pendingDeleteItem?.id)

        vm.setEvent(Event.DeleteConfirmed)

        assertNull(vm.currentState.pendingDeleteItem)
        coVerify(exactly = 1) { downloadRepository.cancelOrDelete(1) }
    }

    @Test
    fun `dismissed delete does nothing`() {
        items.value = listOf(item(1))
        val vm = createViewModel()
        vm.setEvent(Event.DeleteSelected(1))

        vm.setEvent(Event.DeleteDismissed)

        assertNull(vm.currentState.pendingDeleteItem)
        coVerify(exactly = 0) { downloadRepository.cancelOrDelete(any()) }
    }

    @Test
    fun `pause resume and restart are forwarded`() {
        val vm = createViewModel()

        vm.setEvent(Event.PauseSelected(1))
        vm.setEvent(Event.ResumeSelected(2))
        vm.setEvent(Event.RestartSelected(3))

        coVerify(exactly = 1) { downloadRepository.pause(1) }
        coVerify(exactly = 1) { downloadRepository.restart(2, null) }
        coVerify(exactly = 1) { downloadRepository.restart(3, null) }
    }

    @Test
    fun `export without a destination opens the folder picker`() = runTest {
        items.value = listOf(item(1))
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ExportSelected(1))

        assertEquals(listOf<Effect>(Effect.OpenExportDirectoryPicker), effects)
    }

    @Test
    fun `granted folder starts the pending export`() {
        coEvery { exportRepository.selectDestination("content://dir") } returns DESTINATION
        items.value = listOf(item(1))
        val vm = createViewModel()
        vm.setEvent(Event.ExportSelected(1))

        vm.setEvent(Event.ExportDirectoryGranted("content://dir"))

        coVerify(exactly = 1) { exportRepository.enqueue(listOf(1L), DESTINATION, any()) }
    }

    @Test
    fun `failed folder selection is reported`() = runTest {
        coEvery { exportRepository.selectDestination(any()) } throws IllegalStateException("denied")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ExportDirectoryGranted("content://dir"))

        assertEquals(listOf<Effect>(Effect.ExportDirectorySelectionFailed), effects)
    }

    @Test
    fun `export with a destination is enqueued directly`() {
        destination.value = DESTINATION
        items.value = listOf(item(1))
        val vm = createViewModel()

        vm.setEvent(Event.ExportSelected(1))

        coVerify(exactly = 1) { exportRepository.enqueue(listOf(1L), DESTINATION, any()) }
    }

    @Test
    fun `already exported item asks before exporting again`() {
        destination.value = DESTINATION
        items.value = listOf(
            item(
                id = 1,
                exportStatus = VideoExportStatus.Exported,
                exportDirectoryUri = DESTINATION.uri,
                exportedFileUri = "content://file",
            ),
        )
        coEvery { exportRepository.exportedFileExists("content://file") } returns true
        val vm = createViewModel()

        vm.setEvent(Event.ExportSelected(1))

        assertEquals(1L, vm.currentState.pendingReExportItem?.id)
        coVerify(exactly = 0) { exportRepository.enqueue(any(), any(), any()) }

        vm.setEvent(Event.ReExportConfirmed)

        assertNull(vm.currentState.pendingReExportItem)
        coVerify(exactly = 1) { exportRepository.enqueue(listOf(1L), DESTINATION, any()) }
    }

    @Test
    fun `export all counts only items that still need exporting`() {
        destination.value = DESTINATION
        items.value = listOf(
            item(1),
            item(2, status = VideoDownloadStatus.Downloading),
            item(3, exportStatus = VideoExportStatus.Copying),
            item(4, exportStatus = VideoExportStatus.Exported, exportDirectoryUri = DESTINATION.uri),
            item(5, exportStatus = VideoExportStatus.Failed),
        )
        val vm = createViewModel()

        vm.setEvent(Event.ExportAllSelected)

        assertEquals(2, vm.currentState.pendingBulkExportCount)

        vm.setEvent(Event.ExportAllConfirmed)

        assertEquals(0, vm.currentState.pendingBulkExportCount)
        coVerify(exactly = 1) { exportRepository.enqueue(listOf(1L, 5L), DESTINATION, any()) }
    }

    @Test
    fun `dismissed export all drops the pending export`() {
        destination.value = DESTINATION
        items.value = listOf(item(1))
        val vm = createViewModel()
        vm.setEvent(Event.ExportAllSelected)

        vm.setEvent(Event.ExportAllDismissed)
        vm.setEvent(Event.ExportAllConfirmed)

        assertEquals(0, vm.currentState.pendingBulkExportCount)
        coVerify(exactly = 0) { exportRepository.enqueue(any(), any(), any()) }
    }

    @Test
    fun `cancel export is forwarded`() {
        createViewModel().setEvent(Event.CancelExportSelected(4))

        coVerify(exactly = 1) { exportRepository.cancel(4) }
    }

    @Test
    fun `notification permission request is remembered and back leaves`() {
        val vm = createViewModel()

        vm.setEvent(Event.NotificationPermissionRequested)
        vm.setEvent(Event.BackSelected)

        coVerify(exactly = 1) { appLifecycleSettingsStore.markNotificationPermissionRequested() }
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        val DESTINATION = VideoExportDestination(uri = "content://dir", displayName = "Movies")
    }
}
