package su.afk.yummy.tv.feature.account.account.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.usecase.DiscoverLocalAuthDevicesUseCase
import su.afk.yummy.tv.domain.account.usecase.StartLocalAuthServerUseCase
import su.afk.yummy.tv.domain.account.usecase.StopLocalAuthServerUseCase
import su.afk.yummy.tv.domain.account.usecase.TransferSessionUseCase
import javax.inject.Inject

internal class AccountLocalAuthHandler @Inject constructor(
    private val startServerUseCase: StartLocalAuthServerUseCase,
    private val stopServerUseCase: StopLocalAuthServerUseCase,
    private val discoverDevicesUseCase: DiscoverLocalAuthDevicesUseCase,
    private val transferSessionUseCase: TransferSessionUseCase,
    private val authPreferences: YaniAuthPreferences,
) {
    private var serverJob: Job? = null
    private var discoveryJob: Job? = null

    fun startServer(
        scope: CoroutineScope,
        onStateChanged: (LocalAuthServerState) -> Unit,
    ) {
        serverJob?.cancel()
        serverJob = startServerUseCase()
            .catch { onStateChanged(LocalAuthServerState.Error(LocalAuthError.SERVICE_UNAVAILABLE)) }
            .onEach { onStateChanged(it) }
            .launchIn(scope)
    }

    /**
     * Гасит только текущий сервер: отмена подписки запускает `awaitClose`, который снимает
     * NSD-регистрацию и останавливает движок. Безопасно перед немедленным перезапуском.
     */
    fun cancelServer() {
        serverJob?.cancel()
        serverJob = null
    }

    /**
     * Полная остановка: помимо отмены подписки добивает все живые серверы репозитория.
     * Не вызывать прямо перед перезапуском — отложенная корутина может погасить новый сервер.
     */
    fun stopServer(scope: CoroutineScope) {
        cancelServer()
        scope.launch { stopServerUseCase() }
    }

    fun startDiscovery(
        scope: CoroutineScope,
        onDevicesDiscovered: (List<DiscoveredDevice>) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        discoveryJob?.cancel()
        discoveryJob = discoverDevicesUseCase()
            .catch { error -> onFailure(error) }
            .onEach { onDevicesDiscovered(it) }
            .launchIn(scope)
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    suspend fun transferSession(device: DiscoveredDevice, pin: String) {
        val token = authPreferences.refreshToken.first()
        check(token.isNotBlank()) { "Not signed in" }
        transferSessionUseCase(device, pin, token)
    }
}
