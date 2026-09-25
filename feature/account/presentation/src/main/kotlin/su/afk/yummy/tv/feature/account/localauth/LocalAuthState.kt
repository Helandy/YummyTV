package su.afk.yummy.tv.feature.account.localauth

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.DiscoveredDevice
import su.afk.yummy.tv.domain.account.model.LocalAuthCode
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

class LocalAuthState {
    @Immutable
    data class State(
        val isSignedIn: Boolean = true,
        val devices: ImmutableList<DiscoveredDevice> = persistentListOf(),
        val selectedDevice: DiscoveredDevice? = null,
        val pin: String = "",
        /** ТВ из отсканированного QR, которого NSD-поиск ещё не нашёл: перенос стартует при появлении. */
        val pendingDeviceId: String? = null,
        val isSearching: Boolean = false,
        val isPermissionDenied: Boolean = false,
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

        /**
         * Итог запроса разрешения на поиск устройств: поиск стартует только при `granted`.
         *
         * [missing] — короткие имена невыданных разрешений через запятую: на чужих прошивках
         * нужное разрешение может вообще не выдаваться, и без этого в аналитике не разобрать,
         * какое именно.
         */
        data class PermissionResult(val granted: Boolean, val missing: String = "") : Event

        /** Повтор поиска с самого экрана: сам запуск придёт из [PermissionResult]. */
        data object RetrySearchSelected : Event
        data class DeviceSelected(val device: DiscoveredDevice) : Event
        data class PinChanged(val pin: String) : Event
        data object TransferSelected : Event

        /** Сырой текст из сканера QR: разбор и выбор ТВ — во ViewModel. */
        data class QrScanned(val raw: String) : Event

        /** Сканер не запустился: обычно нет сервисов Google Play. Отмена скана сюда не приходит. */
        data object QrScanFailed : Event
    }

    sealed interface Effect : UiEffect {
        data object TransferSuccess : Effect
    }

    companion object {
        const val PIN_LENGTH = LocalAuthCode.LENGTH
    }
}
