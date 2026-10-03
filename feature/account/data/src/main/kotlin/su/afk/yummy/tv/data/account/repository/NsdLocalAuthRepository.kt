package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.localauth.LocalAuthServer
import su.afk.yummy.tv.data.account.localauth.NsdAdvertiser
import su.afk.yummy.tv.data.account.localauth.NsdDeviceDiscovery
import su.afk.yummy.tv.data.account.localauth.PairingSession
import su.afk.yummy.tv.data.account.localauth.SessionTransferClient
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.repository.LocalAuthRepository
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Сводит воедино сеанс сопряжения: локальный сервер, анонс в NSD, поиск устройств и передачу сессии.
 * Вся механика живёт в `data.account.localauth`, здесь только порядок шагов и время жизни.
 */
internal class NsdLocalAuthRepository @Inject constructor(
    private val server: LocalAuthServer,
    private val advertiser: NsdAdvertiser,
    private val discovery: NsdDeviceDiscovery,
    private val transferClient: SessionTransferClient,
) : LocalAuthRepository {

    /** Держим ссылки только ради [stopServer], который приходит извне потока. */
    private val running = CopyOnWriteArrayList<PairingRun>()

    override fun startServer(): Flow<LocalAuthServerState> = callbackFlow {
        val session = PairingSession()
        val serverHandle = server.start(session) { trySend(it) }

        val registration = advertiser.register(
            port = serverHandle.port,
            onRegistered = { serviceName ->
                session.serviceName = serviceName
                trySend(
                    LocalAuthServerState.Pairing(
                        pin = session.pin,
                        port = serverHandle.port,
                        serviceName = serviceName,
                        attemptsLeft = session.attemptsLeft,
                    ),
                )
            },
            onFailed = {
                trySend(LocalAuthServerState.Error(LocalAuthError.SERVICE_UNAVAILABLE))
            },
        )

        val run = PairingRun(serverHandle, registration)
        running += run

        awaitClose { run.stop() }
    }.flowOn(Dispatchers.IO)

    override suspend fun stopServer() = withContext(Dispatchers.IO) {
        running.toList().forEach { it.stop() }
    }

    override fun discoverDevices(): Flow<List<DiscoveredDevice>> = discovery.devices()

    override suspend fun transferSession(
        device: DiscoveredDevice,
        pin: String,
        refreshToken: String,
    ) = transferClient.transfer(device, pin, refreshToken)

    /** Один сеанс: сервер плюс его анонс. Остановка идемпотентна и снимает обе части. */
    private inner class PairingRun(
        private val serverHandle: LocalAuthServer.Handle,
        private val registration: NsdAdvertiser.Registration,
    ) {
        fun stop() {
            running -= this
            registration.unregister()
            serverHandle.stop()
        }
    }
}
