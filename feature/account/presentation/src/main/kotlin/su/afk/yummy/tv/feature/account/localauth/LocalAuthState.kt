package su.afk.yummy.tv.feature.account.localauth

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

class LocalAuthState {
    @Immutable
    data class State(
        val isSignedIn: Boolean = true,
        val devices: ImmutableList<DiscoveredDevice> = persistentListOf(),
        val selectedDevice: DiscoveredDevice? = null,
        val pin: String = "",
        val isTransferring: Boolean = false,
        val isTransferred: Boolean = false,
        val error: AccountUiError? = null,
    ) : UiState {
        val canTransfer: Boolean
            get() = selectedDevice != null && pin.length == PIN_LENGTH && !isTransferring
    }

    sealed interface Event : UiEvent {
        data object ScreenOpened : Event
        data object BackSelected : Event

        /** Итог запроса разрешения на поиск устройств: поиск стартует только при `granted`. */
        data class PermissionResult(val granted: Boolean) : Event
        data class DeviceSelected(val device: DiscoveredDevice) : Event
        data class PinChanged(val pin: String) : Event
        data object TransferSelected : Event
    }

    sealed interface Effect : UiEffect {
        data object TransferSuccess : Effect
    }

    companion object {
        const val PIN_LENGTH = 6
    }
}
