package su.afk.yummy.tv.feature.account.localauth

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.LocalAuthCode
import su.afk.yummy.tv.domain.account.model.SessionTransferException
import su.afk.yummy.tv.feature.account.account.handler.AccountLocalAuthHandler
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.localauth.mapper.toUiError
import javax.inject.Inject

@HiltViewModel
class LocalAuthViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val localAuthHandler: AccountLocalAuthHandler,
    private val analytics: LocalAuthAnalytics,
) : BaseViewModel<LocalAuthState.State, LocalAuthState.Event, LocalAuthState.Effect>() {

    override fun createInitialState() = LocalAuthState.State()

    override fun onEvent(event: LocalAuthState.Event) {
        when (event) {
            LocalAuthState.Event.ScreenOpened -> analytics.eventMobileScreenOpened()

            LocalAuthState.Event.BackSelected -> nav.back()

            is LocalAuthState.Event.PermissionResult -> {
                analytics.eventMobilePermissionResult(event.granted, event.missing)
                if (event.granted) {
                    startDiscovery()
                } else {
                    setState { copy(isSearching = false, isPermissionDenied = true) }
                }
            }

            LocalAuthState.Event.RetrySearchSelected -> {
                analytics.eventMobileSearchRetry()
                setState { copy(error = null, isPermissionDenied = false) }
            }

            is LocalAuthState.Event.DeviceSelected -> {
                analytics.eventMobileDeviceSelected()
                setState { copy(selectedDevice = event.device, error = null) }
            }

            is LocalAuthState.Event.PinChanged -> setState {
                copy(
                    pin = LocalAuthCode.normalize(event.pin).take(LocalAuthState.PIN_LENGTH),
                    error = null,
                )
            }

            LocalAuthState.Event.TransferSelected -> transfer()
        }
    }

    private fun startDiscovery() {
        setState {
            copy(
                error = null,
                devices = persistentListOf(),
                isSearching = true,
                isPermissionDenied = false,
            )
        }
        localAuthHandler.startDiscovery(
            scope = viewModelScope,
            onDevicesDiscovered = { devices ->
                setState { copy(devices = devices.toImmutableList()) }
            },
            onFailure = {
                analytics.eventMobileDiscoveryFailed()
                setState { copy(isSearching = false, error = AccountUiError.TRANSFER_FAILED) }
            },
        )
    }

    private fun transfer() {
        val device = currentState.selectedDevice ?: return
        val pin = currentState.pin
        analytics.eventMobileTransferSelected()
        viewModelScope.launch {
            setState { copy(isTransferring = true, error = null) }
            runSuspendCatching { localAuthHandler.transferSession(device, pin) }
                .onSuccess {
                    analytics.eventMobileTransferSuccess()
                    localAuthHandler.stopDiscovery()
                    setState {
                        copy(
                            pin = "",
                            selectedDevice = null,
                            isSearching = false,
                            isTransferring = false,
                            isTransferred = true,
                        )
                    }
                    setEffect(LocalAuthState.Effect.TransferSuccess)
                }
                .onFailure { error ->
                    analytics.eventMobileTransferFailure(
                        (error as? SessionTransferException)?.reason,
                    )
                    setState {
                        copy(isTransferring = false, error = error.toUiError())
                    }
                }
        }
    }

    override fun onCleared() {
        analytics.eventMobileDiscoveryFinished(currentState.devices.size)
        localAuthHandler.stopDiscovery()
        super.onCleared()
    }
}
