package su.afk.yummy.tv.feature.account.localauth

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.SessionTransferException
import su.afk.yummy.tv.domain.account.utils.encodeLocalAuthPairingPayload
import su.afk.yummy.tv.feature.account.account.handler.AccountLocalAuthHandler
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

class LocalAuthViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val localAuthHandler: AccountLocalAuthHandler = mockk()
    private val analytics: LocalAuthAnalytics = mockk(relaxed = true)

    private val tv = DiscoveredDevice(id = "TV-1", name = "Living room", host = "192.168.0.2", port = 8080)
    private val onDevices = slot<(List<DiscoveredDevice>) -> Unit>()
    private val onFailure = slot<(Throwable) -> Unit>()

    @Before
    fun setUp() {
        every { localAuthHandler.startDiscovery(any(), capture(onDevices), capture(onFailure)) } just Runs
        every { localAuthHandler.stopDiscovery() } just Runs
        coEvery { localAuthHandler.transferSession(any(), any()) } just Runs
    }

    private fun createViewModel() = LocalAuthViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        localAuthHandler = localAuthHandler,
        analytics = analytics,
    )

    private fun LocalAuthViewModel.startDiscovery() =
        setEvent(LocalAuthState.Event.PermissionResult(granted = true))

    @Test
    fun `granted permission starts the discovery`() {
        val vm = createViewModel()

        vm.startDiscovery()

        assertTrue(vm.currentState.isSearching)
        verify(exactly = 1) { localAuthHandler.startDiscovery(any(), any(), any()) }
    }

    @Test
    fun `denied permission shows the denied state and skips the discovery`() {
        val vm = createViewModel()

        vm.setEvent(LocalAuthState.Event.PermissionResult(granted = false, missing = "NEARBY"))

        assertTrue(vm.currentState.isPermissionDenied)
        assertFalse(vm.currentState.isSearching)
        verify(exactly = 0) { localAuthHandler.startDiscovery(any(), any(), any()) }
    }

    @Test
    fun `discovered devices are shown`() {
        val vm = createViewModel()
        vm.startDiscovery()

        onDevices.captured(listOf(tv))

        assertEquals(listOf(tv), vm.currentState.devices)
    }

    @Test
    fun `discovery failure stops the search with a transfer error`() {
        val vm = createViewModel()
        vm.startDiscovery()

        onFailure.captured(IllegalStateException("nsd"))

        assertFalse(vm.currentState.isSearching)
        assertEquals(AccountUiError.TRANSFER_FAILED, vm.currentState.error)
    }

    @Test
    fun `pin input is normalized and capped`() {
        val vm = createViewModel()

        vm.setEvent(LocalAuthState.Event.PinChanged("acde-fhjk-mnpr-0000"))

        assertEquals("ACDEFHJKMNPR", vm.currentState.pin)
    }

    @Test
    fun `transfer needs a selected device`() {
        val vm = createViewModel()

        vm.setEvent(LocalAuthState.Event.TransferSelected)

        coVerify(exactly = 0) { localAuthHandler.transferSession(any(), any()) }
    }

    @Test
    fun `successful transfer clears the form and emits success`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(LocalAuthState.Event.DeviceSelected(tv))
        vm.setEvent(LocalAuthState.Event.PinChanged(PIN))

        vm.setEvent(LocalAuthState.Event.TransferSelected)

        val state = vm.currentState
        assertTrue(state.isTransferred)
        assertFalse(state.isTransferring)
        assertEquals("", state.pin)
        assertNull(state.selectedDevice)
        assertEquals(listOf(LocalAuthState.Effect.TransferSuccess), effects)
        coVerify(exactly = 1) { localAuthHandler.transferSession(tv, PIN) }
        verify(exactly = 1) { localAuthHandler.stopDiscovery() }
    }

    @Test
    fun `wrong pin is reported as an invalid pin`() {
        coEvery { localAuthHandler.transferSession(any(), any()) } throws
            SessionTransferException("bad", LocalAuthError.INVALID_PIN)
        val vm = createViewModel()
        vm.setEvent(LocalAuthState.Event.DeviceSelected(tv))
        vm.setEvent(LocalAuthState.Event.PinChanged(PIN))

        vm.setEvent(LocalAuthState.Event.TransferSelected)

        assertEquals(AccountUiError.LOCAL_AUTH_INVALID_PIN, vm.currentState.error)
        assertFalse(vm.currentState.isTransferring)
        assertFalse(vm.currentState.isTransferred)
    }

    @Test
    fun `unknown failure is a generic transfer error`() {
        coEvery { localAuthHandler.transferSession(any(), any()) } throws IllegalStateException("net")
        val vm = createViewModel()
        vm.setEvent(LocalAuthState.Event.DeviceSelected(tv))

        vm.setEvent(LocalAuthState.Event.TransferSelected)

        assertEquals(AccountUiError.TRANSFER_FAILED, vm.currentState.error)
    }

    @Test
    fun `scanned qr of a known tv transfers immediately`() {
        val vm = createViewModel()
        vm.startDiscovery()
        onDevices.captured(listOf(tv))

        vm.setEvent(LocalAuthState.Event.QrScanned(encodeLocalAuthPairingPayload("tv-1", PIN)))

        assertTrue(vm.currentState.isTransferred)
        coVerify(exactly = 1) { localAuthHandler.transferSession(tv, PIN) }
    }

    @Test
    fun `scanned qr of an undiscovered tv waits for it to appear`() {
        val vm = createViewModel()
        vm.startDiscovery()

        vm.setEvent(LocalAuthState.Event.QrScanned(encodeLocalAuthPairingPayload("TV-1", PIN)))

        assertEquals("TV-1", vm.currentState.pendingDeviceId)
        assertEquals(PIN, vm.currentState.pin)
        coVerify(exactly = 0) { localAuthHandler.transferSession(any(), any()) }

        onDevices.captured(listOf(tv))

        assertTrue(vm.currentState.isTransferred)
        coVerify(exactly = 1) { localAuthHandler.transferSession(tv, PIN) }
    }

    @Test
    fun `bare code fills the pin and picks the only device`() {
        val vm = createViewModel()
        vm.startDiscovery()
        onDevices.captured(listOf(tv))

        vm.setEvent(LocalAuthState.Event.QrScanned(PIN))

        assertTrue(vm.currentState.isTransferred)
    }

    @Test
    fun `garbage qr is reported as invalid`() {
        val vm = createViewModel()

        vm.setEvent(LocalAuthState.Event.QrScanned("hello"))

        assertEquals(AccountUiError.LOCAL_AUTH_INVALID_QR, vm.currentState.error)
    }

    @Test
    fun `unavailable scanner is reported`() {
        val vm = createViewModel()

        vm.setEvent(LocalAuthState.Event.QrScanFailed)

        assertEquals(AccountUiError.LOCAL_AUTH_SCANNER_UNAVAILABLE, vm.currentState.error)
    }

    @Test
    fun `selecting a device clears the error and pending id`() {
        val vm = createViewModel()
        vm.setEvent(LocalAuthState.Event.QrScanFailed)

        vm.setEvent(LocalAuthState.Event.DeviceSelected(tv))

        assertEquals(tv, vm.currentState.selectedDevice)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `retry clears the error and the denied flag`() {
        val vm = createViewModel()
        vm.setEvent(LocalAuthState.Event.PermissionResult(granted = false))

        vm.setEvent(LocalAuthState.Event.RetrySearchSelected)

        assertFalse(vm.currentState.isPermissionDenied)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(LocalAuthState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val PIN = "ACDEFHJKMNPR"
    }
}
