package su.afk.yummy.tv.feature.update

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.update.model.UpdateDownloadState
import su.afk.yummy.tv.feature.update.UpdateState.Event
import su.afk.yummy.tv.feature.update.UpdateState.State.Status
import su.afk.yummy.tv.feature.update.handler.UpdateInstallHandler
import su.afk.yummy.tv.feature.update.handler.UpdateInstallResult
import java.io.File

class UpdateViewModelTest : BaseUnitTest() {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val updateInstallHandler: UpdateInstallHandler = mockk()
    private val stringProvider: StringProvider = mockk()
    private val analytics: UpdateAnalytics = mockk(relaxed = true)
    private val download = MutableSharedFlow<UpdateDownloadState>(replay = 1)

    @Before
    fun setUp() {
        every { stringProvider.get(any<Int>()) } returns "fallback error"
        every { updateInstallHandler.observeDownload(any()) } returns download
        every { updateInstallHandler.startDownload(any()) } just Runs
        every { updateInstallHandler.reportDownloadError(any(), any()) } just Runs
    }

    private fun createViewModel() = UpdateViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        updateInstallHandler = updateInstallHandler,
        stringProvider = stringProvider,
        analytics = analytics,
    )

    private fun UpdateViewModel.init(required: Boolean = false) = setEvent(
        Event.Init(version = "2.0", apkUrl = APK, changelog = "log", required = required, updatesCount = 3),
    )

    @Test
    fun `starts idle`() {
        assertEquals(Status.Idle, createViewModel().currentState.status)
    }

    @Test
    fun `init offers the update`() {
        val vm = createViewModel()

        vm.init()

        val status = vm.currentState.status as Status.Available
        assertEquals("2.0", status.version)
        assertEquals(3, status.updatesCount)
    }

    @Test
    fun `init is applied only once`() {
        val vm = createViewModel()
        vm.init()

        vm.setEvent(Event.Init(version = "3.0", apkUrl = APK, changelog = ""))

        assertEquals("2.0", (vm.currentState.status as Status.Available).version)
    }

    @Test
    fun `optional update can be dismissed`() {
        val vm = createViewModel()
        vm.init()

        vm.setEvent(Event.Dismiss)

        assertEquals(Status.Idle, vm.currentState.status)
        verify(exactly = 1) { nav.back() }
        verify(exactly = 1) { analytics.eventDismiss("2.0") }
    }

    @Test
    fun `required update cannot be dismissed`() {
        val vm = createViewModel()
        vm.init(required = true)

        vm.setEvent(Event.Dismiss)

        assertTrue(vm.currentState.status is Status.Available)
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `confirm starts the download and shows its progress`() = runTest {
        val vm = createViewModel()
        vm.init()

        vm.setEvent(Event.ConfirmUpdate(APK))
        download.emit(UpdateDownloadState.Downloading(0.5f))

        verify(exactly = 1) { updateInstallHandler.startDownload(APK) }
        assertEquals(Status.Downloading(0.5f), vm.currentState.status)
    }

    @Test
    fun `finished download is installed once`() = runTest {
        val file = temporaryFolder.newFile("update.apk")
        coEvery { updateInstallHandler.install(file, "2.0") } returns UpdateInstallResult.Success(file)
        val vm = createViewModel()
        vm.init()
        vm.setEvent(Event.ConfirmUpdate(APK))

        download.emit(UpdateDownloadState.Downloaded(file))
        download.emit(UpdateDownloadState.Downloaded(file))

        assertEquals(Status.Installing, vm.currentState.status)
        coVerify(exactly = 1) { updateInstallHandler.install(file, "2.0") }
    }

    @Test
    fun `install failure becomes an error with the apk url`() = runTest {
        val file = temporaryFolder.newFile("update.apk")
        coEvery { updateInstallHandler.install(any(), any()) } returns
            UpdateInstallResult.Failure(IllegalStateException("cannot install"))
        val vm = createViewModel()
        vm.init()
        vm.setEvent(Event.ConfirmUpdate(APK))

        download.emit(UpdateDownloadState.Downloaded(file))

        assertEquals(Status.Error("cannot install", APK), vm.currentState.status)
    }

    @Test
    fun `failed download reports the error and shows the fallback text for an empty message`() = runTest {
        val vm = createViewModel()
        vm.init()
        vm.setEvent(Event.ConfirmUpdate(APK))

        download.emit(UpdateDownloadState.Failed(null))

        verify(exactly = 1) { updateInstallHandler.reportDownloadError("2.0", null) }
        assertEquals(Status.Error("fallback error", APK), vm.currentState.status)
    }

    @Test
    fun `download cancelled from outside returns to the offer`() = runTest {
        val vm = createViewModel()
        vm.init()
        vm.setEvent(Event.ConfirmUpdate(APK))
        download.emit(UpdateDownloadState.Downloading(0.2f))

        download.emit(UpdateDownloadState.Idle)

        assertTrue(vm.currentState.status is Status.Available)
    }

    @Test
    fun `retry without a downloaded file downloads again`() {
        val vm = createViewModel()
        vm.init()

        vm.setEvent(Event.RetryUpdate(APK))

        verify(exactly = 1) { updateInstallHandler.startDownload(APK) }
        verify(exactly = 1) { analytics.eventRetry("2.0") }
    }

    @Test
    fun `retry with a downloaded file only repeats the install`() = runTest {
        val file: File = temporaryFolder.newFile("update.apk")
        coEvery { updateInstallHandler.install(file, "2.0") } returns
            UpdateInstallResult.Failure(IllegalStateException("first"))
        val vm = createViewModel()
        vm.init()
        vm.setEvent(Event.ConfirmUpdate(APK))
        download.emit(UpdateDownloadState.Downloaded(file))
        coEvery { updateInstallHandler.install(file, "2.0") } returns UpdateInstallResult.Success(file)

        vm.setEvent(Event.RetryUpdate(APK))

        assertEquals(Status.Installing, vm.currentState.status)
        verify(exactly = 1) { updateInstallHandler.startDownload(APK) }
    }

    private companion object {
        const val APK = "https://apk"
    }
}
