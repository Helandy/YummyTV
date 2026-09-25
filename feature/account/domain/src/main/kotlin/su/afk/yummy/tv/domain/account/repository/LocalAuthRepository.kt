package su.afk.yummy.tv.domain.account.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState

/**
 * Repository for local network authentication (session transfer).
 */
interface LocalAuthRepository {

    // TV Side (Server)

    /**
     * Starts the local auth server and advertises it via NSD.
     * Emits states from [LocalAuthServerState].
     */
    fun startServer(): Flow<LocalAuthServerState>

    /**
     * Stops the local auth server and unregisters the NSD service.
     */
    suspend fun stopServer()

    // Mobile Side (Discovery & Transfer)

    /**
     * Discovers TV devices on the local network.
     */
    fun discoverDevices(): Flow<List<DiscoveredDevice>>

    /**
     * Transfers the session to the specified device using the PIN for encryption.
     * @param device The target device.
     * @param pin Код сопряжения с экрана ТВ.
     * @param refreshToken The session refresh token to transfer.
     */
    suspend fun transferSession(device: DiscoveredDevice, pin: String, refreshToken: String)
}
