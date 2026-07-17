package su.afk.yummy.tv.feature.messages.chat

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.domain.messages.model.MessageHistoryEntry

object ChatState {
    data class State(
        val userId: Int = 0,
        val currentUserId: Int = 0,
        val isAuthResolved: Boolean = false,
        val isAuthorized: Boolean = false,
        val fallbackNickname: String = "",
        val fallbackAvatarUrl: String? = null,
        val peer: DialogSummary? = null,
        val messages: List<ChatMessage> = emptyList(),
        val draft: String = "",
        val editingMessageId: Int? = null,
        val editingText: String = "",
        val pendingDeleteMessageId: Int? = null,
        val pendingClaimMessageId: Int? = null,
        val historyMessageId: Int? = null,
        val messageHistory: List<MessageHistoryEntry> = emptyList(),
        val isHistoryLoading: Boolean = false,
        val hasHistoryError: Boolean = false,
        val isBanConfirmationVisible: Boolean = false,
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingOlder: Boolean = false,
        val canLoadOlder: Boolean = true,
        val isMutating: Boolean = false,
        val hasLoadError: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object LoginSelected : Event
        data object ScreenStarted : Event
        data object ScreenStopped : Event
        data object RefreshSelected : Event
        data object LoadOlderSelected : Event
        data class DraftChanged(val text: String) : Event
        data object SendSelected : Event
        data class EditSelected(val messageId: Int) : Event
        data class EditTextChanged(val text: String) : Event
        data object EditCancelled : Event
        data object EditConfirmed : Event
        data class DeleteSelected(val messageId: Int) : Event
        data object DeleteDismissed : Event
        data object DeleteConfirmed : Event
        data class RestoreSelected(val messageId: Int) : Event
        data class HistorySelected(val messageId: Int) : Event
        data object HistoryDismissed : Event
        data object HistoryRetrySelected : Event
        data class ClaimSelected(val messageId: Int) : Event
        data object ClaimDismissed : Event
        data object ClaimConfirmed : Event
        data object BanToggleSelected : Event
        data object BanToggleDismissed : Event
        data object BanToggleConfirmed : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowMessage(val type: MessageType) : Effect
    }

    enum class MessageType {
        SEND_FAILED,
        EDIT_FAILED,
        READ_FAILED,
        DELETE_FAILED,
        RESTORE_FAILED,
        CLAIM_FAILED,
        CLAIM_SENT,
        BAN_FAILED,
    }
}
