package su.afk.yummy.tv.feature.account.account.handler

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.usecase.DiscoverLocalAuthDevicesUseCase
import su.afk.yummy.tv.domain.account.usecase.StartLocalAuthServerUseCase
import su.afk.yummy.tv.domain.account.usecase.StopLocalAuthServerUseCase
import su.afk.yummy.tv.domain.account.usecase.TransferSessionUseCase

class AccountLocalAuthHandlerTest : BaseUnitTest() {

    private val startServer: StartLocalAuthServerUseCase = mockk()
    private val stopServer: StopLocalAuthServerUseCase = mockk(relaxed = true)
    private val discoverDevices: DiscoverLocalAuthDevicesUseCase = mockk()
    private val transferSession: TransferSessionUseCase = mockk(relaxed = true)
    private val authPreferences: YaniAuthPreferences = mockk()

    private val device = DiscoveredDevice(id = "tv", name = "Living room", host = "192.168.1.5", port = 8080)

    @Test
    fun `server states are forwarded and a crash becomes an error state`() = runTest {
        val states = mutableListOf<LocalAuthServerState>()
        every { startServer() } returns flow {
            emit(LocalAuthServerState.Idle)
            throw IllegalStateException("nsd")
        }

        createHandler().startServer(backgroundScope) { states += it }
        runCurrent()

        assertEquals(
            listOf(LocalAuthServerState.Idle, LocalAuthServerState.Error(LocalAuthError.SERVICE_UNAVAILABLE)),
            states,
        )
    }

    @Test
    fun `cancelled server stops forwarding and a full stop also stops the repository`() = runTest {
        val upstream = MutableSharedFlow<LocalAuthServerState>()
        every { startServer() } returns upstream
        val states = mutableListOf<LocalAuthServerState>()
        val handler = createHandler()
        handler.startServer(backgroundScope) { states += it }
        runCurrent()

        handler.stopServer(backgroundScope)
        upstream.emit(LocalAuthServerState.Idle)
        runCurrent()

        assertEquals(emptyList<LocalAuthServerState>(), states)
        coVerify { stopServer() }
    }

    @Test
    fun `discovery forwards devices and failures`() = runTest {
        every { discoverDevices() } returns flowOf(listOf(device))
        val found = mutableListOf<List<DiscoveredDevice>>()

        createHandler().startDiscovery(backgroundScope, onDevicesDiscovered = { found += it }, onFailure = {})
        runCurrent()

        assertEquals(listOf(listOf(device)), found)
    }

    @Test
    fun `session transfer sends the stored refresh token`() = runTest {
        every { authPreferences.refreshToken } returns flowOf("refresh")

        createHandler().transferSession(device, "1234")

        coVerify { transferSession(device, "1234", "refresh") }
    }

    @Test
    fun `session transfer without a sign-in is refused`() = runTest {
        every { authPreferences.refreshToken } returns flowOf("")
        val error = runCatching { createHandler().transferSession(device, "1234") }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        coVerify(exactly = 0) { transferSession(any(), any(), any()) }
    }

    private fun createHandler() = AccountLocalAuthHandler(
        startServerUseCase = startServer,
        stopServerUseCase = stopServer,
        discoverDevicesUseCase = discoverDevices,
        transferSessionUseCase = transferSession,
        authPreferences = authPreferences,
    )
}
