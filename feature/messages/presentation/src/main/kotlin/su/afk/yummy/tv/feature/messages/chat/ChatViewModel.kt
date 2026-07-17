package su.afk.yummy.tv.feature.messages.chat

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.messages.MessagesMutationNotifier
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.usecase.GetDialogsUseCase
import su.afk.yummy.tv.domain.messages.usecase.GetMessagesUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.messages.chat.handler.ChatMutationHandler
import su.afk.yummy.tv.feature.messages.chat.handler.ChatPollingHandler

private const val CHAT_PAGE_SIZE = 30
private const val DRAFT_KEY_PREFIX = "messages_chat_draft_"

@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted private val userId: Int,
    @Assisted("nickname") private val nickname: String,
    @Assisted("avatarUrl") private val avatarUrl: String?,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val getDialogs: GetDialogsUseCase,
    private val getMessages: GetMessagesUseCase,
    private val pollingHandler: ChatPollingHandler,
    private val mutationHandler: ChatMutationHandler,
    private val mutationNotifier: MessagesMutationNotifier,
) : BaseViewModelNew<ChatState.State, ChatState.Event, ChatState.Effect>(savedStateHandle) {
    @AssistedFactory
    interface Factory {
        fun create(
            userId: Int,
            @Assisted("nickname") nickname: String,
            @Assisted("avatarUrl") avatarUrl: String?,
        ): ChatViewModel
    }

    private var pollingJob: Job? = null
    private var isScreenStarted = false
    private var readJob: Job? = null
    private val draftKey = "$DRAFT_KEY_PREFIX$userId"

    override fun createInitialState() = ChatState.State(
        userId = userId,
        fallbackNickname = nickname,
        fallbackAvatarUrl = avatarUrl,
        draft = savedStateHandle[draftKey] ?: "",
    )

    override fun saveToSavedState(state: ChatState.State) {
        savedStateHandle[draftKey] = state.draft
    }

    init {
        observeAccountSession()
            .onEach { session ->
                val becameAuthorized = session.isAuthorized && !currentState.isAuthorized
                setState {
                    copy(
                        isAuthResolved = true,
                        isAuthorized = session.isAuthorized,
                        currentUserId = session.userId,
                    )
                }
                if (becameAuthorized) {
                    loadInitial()
                    if (isScreenStarted) startPolling()
                } else if (!session.isAuthorized) {
                    stopPolling()
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ChatState.Event) {
        when (event) {
            ChatState.Event.BackSelected -> nav.back()
            ChatState.Event.LoginSelected -> nav.navigate(accountNavigator.getAccountDest())
            ChatState.Event.ScreenStarted -> {
                isScreenStarted = true
                startPolling()
            }

            ChatState.Event.ScreenStopped -> {
                isScreenStarted = false
                stopPolling()
            }

            ChatState.Event.RefreshSelected -> refresh()
            ChatState.Event.LoadOlderSelected -> loadOlder()
            is ChatState.Event.DraftChanged -> setState { copy(draft = event.text) }
            ChatState.Event.SendSelected -> send()
            is ChatState.Event.EditSelected -> beginEdit(event.messageId)
            is ChatState.Event.EditTextChanged -> setState { copy(editingText = event.text) }
            ChatState.Event.EditCancelled -> setState {
                copy(editingMessageId = null, editingText = "")
            }

            ChatState.Event.EditConfirmed -> confirmEdit()
            is ChatState.Event.DeleteSelected -> selectDelete(event.messageId)
            ChatState.Event.DeleteDismissed -> setState { copy(pendingDeleteMessageId = null) }
            ChatState.Event.DeleteConfirmed -> confirmDelete()
            is ChatState.Event.RestoreSelected -> restore(event.messageId)
            is ChatState.Event.HistorySelected -> loadHistory(event.messageId)
            ChatState.Event.HistoryDismissed -> setState {
                copy(
                    historyMessageId = null,
                    messageHistory = emptyList(),
                    isHistoryLoading = false,
                    hasHistoryError = false,
                )
            }

            ChatState.Event.HistoryRetrySelected -> currentState.historyMessageId?.let(::loadHistory)
            is ChatState.Event.ClaimSelected -> selectClaim(event.messageId)
            ChatState.Event.ClaimDismissed -> setState { copy(pendingClaimMessageId = null) }
            ChatState.Event.ClaimConfirmed -> confirmClaim()
            ChatState.Event.BanToggleSelected -> setState { copy(isBanConfirmationVisible = true) }
            ChatState.Event.BanToggleDismissed -> setState { copy(isBanConfirmationVisible = false) }
            ChatState.Event.BanToggleConfirmed -> confirmBanToggle()
        }
    }

    private fun loadInitial() {
        if (!currentState.isAuthorized || userId <= 0 || currentState.isLoading) return
        viewModelScope.launch {
            setState { copy(isLoading = true, hasLoadError = false) }
            runCatching {
                coroutineScope {
                    val peer = async {
                        runCatching { getDialogs(1, 0, needUserId = userId).firstOrNull() }
                            .getOrNull()
                    }
                    val messages = async { getMessages(userId, CHAT_PAGE_SIZE) }
                    peer.await() to messages.await()
                }
            }.fold(
                onSuccess = { (peer, messages) ->
                    setState {
                        copy(
                            peer = peer ?: this.peer,
                            messages = mergeMessages(emptyList(), messages),
                            isLoading = false,
                            canLoadOlder = messages.size >= CHAT_PAGE_SIZE,
                            hasLoadError = false,
                        )
                    }
                    markIncomingReadIfNeeded()
                },
                onFailure = {
                    setState { copy(isLoading = false, hasLoadError = true) }
                },
            )
        }
    }

    private fun refresh() {
        if (!currentState.isAuthorized || currentState.isRefreshing) return
        viewModelScope.launch {
            setState { copy(isRefreshing = true, hasLoadError = false) }
            runCatching {
                coroutineScope {
                    val peer = async {
                        runCatching { getDialogs(1, 0, needUserId = userId).firstOrNull() }
                            .getOrNull()
                    }
                    val messages = async { getMessages(userId, CHAT_PAGE_SIZE) }
                    peer.await() to messages.await()
                }
            }.fold(
                onSuccess = { (peer, messages) ->
                    setState {
                        copy(
                            peer = peer ?: this.peer,
                            messages = mergeMessages(this.messages, messages),
                            isRefreshing = false,
                            hasLoadError = false,
                        )
                    }
                    markIncomingReadIfNeeded()
                },
                onFailure = {
                    setState {
                        copy(isRefreshing = false, hasLoadError = messages.isEmpty())
                    }
                },
            )
        }
    }

    private fun loadOlder() {
        val state = currentState
        if (!state.isAuthorized || state.isLoadingOlder || !state.canLoadOlder || state.messages.isEmpty()) return
        val cursor = state.messages.minOf { it.id }
        viewModelScope.launch {
            setState { copy(isLoadingOlder = true) }
            runCatching { getMessages(userId, CHAT_PAGE_SIZE, cursor) }.fold(
                onSuccess = { older ->
                    setState {
                        copy(
                            messages = mergeMessages(messages, older),
                            isLoadingOlder = false,
                            canLoadOlder = older.size >= CHAT_PAGE_SIZE &&
                                    older.any { it.id < cursor },
                        )
                    }
                },
                onFailure = { setState { copy(isLoadingOlder = false) } },
            )
        }
    }

    private fun startPolling() {
        if (!isScreenStarted || !currentState.isAuthorized || pollingJob?.isActive == true) return
        pollingJob = pollingHandler.updates(userId)
            .onEach { latest ->
                setState { copy(messages = mergeMessages(messages, latest)) }
                markIncomingReadIfNeeded()
            }
            .catch { /* Polling failures must not replace already loaded content. */ }
            .launchIn(viewModelScope)
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun send() {
        val text = currentState.draft.trim()
        if (
            text.isBlank() || currentState.isMutating || !currentState.isAuthorized ||
            currentState.peer?.isBanned == true
        ) return
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { mutationHandler.send(userId, text) }.fold(
                onSuccess = { saved ->
                    setState {
                        copy(
                            messages = mergeMessages(messages, listOf(saved)),
                            draft = "",
                            isMutating = false,
                        )
                    }
                    mutationNotifier.notifyChanged()
                },
                onFailure = {
                    setState { copy(isMutating = false) }
                    setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.SEND_FAILED))
                },
            )
        }
    }

    private fun beginEdit(messageId: Int) {
        val message = currentState.messages.firstOrNull { it.id == messageId } ?: return
        if (message.fromUserId != currentState.currentUserId || message.isDeleted) return
        setState { copy(editingMessageId = message.id, editingText = message.text) }
    }

    private fun confirmEdit() {
        val messageId = currentState.editingMessageId ?: return
        val text = currentState.editingText.trim()
        val original = currentState.messages.firstOrNull { it.id == messageId } ?: return
        if (
            text.isBlank() || currentState.isMutating || original.isDeleted ||
            original.fromUserId != currentState.currentUserId
        ) return
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { mutationHandler.edit(messageId, text) }.fold(
                onSuccess = { saved ->
                    setState {
                        copy(
                            messages = mergeMessages(messages, listOf(saved)),
                            editingMessageId = null,
                            editingText = "",
                            isMutating = false,
                        )
                    }
                    mutationNotifier.notifyChanged()
                },
                onFailure = {
                    setState { copy(isMutating = false) }
                    setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.EDIT_FAILED))
                },
            )
        }
    }

    private fun selectDelete(messageId: Int) {
        val message = currentState.messages.firstOrNull { it.id == messageId } ?: return
        if (message.fromUserId != currentState.currentUserId || message.isDeleted) return
        setState { copy(pendingDeleteMessageId = messageId) }
    }

    private fun confirmDelete() {
        val messageId = currentState.pendingDeleteMessageId ?: return
        if (currentState.isMutating) return
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { mutationHandler.delete(messageId) }.fold(
                onSuccess = { saved ->
                    setState {
                        copy(
                            messages = mergeMessages(messages, listOf(saved)),
                            pendingDeleteMessageId = null,
                            isMutating = false,
                        )
                    }
                    mutationNotifier.notifyChanged()
                },
                onFailure = {
                    setState { copy(isMutating = false) }
                    setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.DELETE_FAILED))
                },
            )
        }
    }

    private fun restore(messageId: Int) {
        val message = currentState.messages.firstOrNull { it.id == messageId } ?: return
        if (currentState.isMutating || !message.isDeleted || message.fromUserId != currentState.currentUserId) return
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { mutationHandler.restore(messageId) }.fold(
                onSuccess = { saved ->
                    setState {
                        copy(
                            messages = mergeMessages(messages, listOf(saved)),
                            isMutating = false
                        )
                    }
                    mutationNotifier.notifyChanged()
                },
                onFailure = {
                    setState { copy(isMutating = false) }
                    setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.RESTORE_FAILED))
                },
            )
        }
    }

    private fun loadHistory(messageId: Int) {
        if (currentState.isHistoryLoading) return
        viewModelScope.launch {
            setState {
                copy(
                    historyMessageId = messageId,
                    messageHistory = emptyList(),
                    isHistoryLoading = true,
                    hasHistoryError = false,
                )
            }
            runCatching { mutationHandler.history(messageId) }.fold(
                onSuccess = { history ->
                    setState { copy(messageHistory = history, isHistoryLoading = false) }
                },
                onFailure = { setState { copy(isHistoryLoading = false, hasHistoryError = true) } },
            )
        }
    }

    private fun selectClaim(messageId: Int) {
        val message = currentState.messages.firstOrNull { it.id == messageId } ?: return
        if (message.fromUserId == currentState.currentUserId || message.isDeleted) return
        setState { copy(pendingClaimMessageId = messageId) }
    }

    private fun confirmClaim() {
        val messageId = currentState.pendingClaimMessageId ?: return
        if (currentState.isMutating) return
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { mutationHandler.claim(messageId) }.fold(
                onSuccess = { sent ->
                    setState { copy(pendingClaimMessageId = null, isMutating = false) }
                    setEffect(
                        ChatState.Effect.ShowMessage(
                            if (sent) ChatState.MessageType.CLAIM_SENT else ChatState.MessageType.CLAIM_FAILED
                        )
                    )
                },
                onFailure = {
                    setState { copy(isMutating = false) }
                    setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.CLAIM_FAILED))
                },
            )
        }
    }

    private fun confirmBanToggle() {
        val peer = currentState.peer ?: return
        if (currentState.isMutating) return
        val shouldBan = !peer.isBanned
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { mutationHandler.setBanned(userId, shouldBan) }.fold(
                onSuccess = { changed ->
                    if (changed) {
                        setState {
                            copy(
                                peer = peer.copy(isBanned = shouldBan),
                                isBanConfirmationVisible = false,
                                isMutating = false,
                            )
                        }
                        mutationNotifier.notifyChanged()
                    } else {
                        setState { copy(isMutating = false) }
                        setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.BAN_FAILED))
                    }
                },
                onFailure = {
                    setState { copy(isMutating = false) }
                    setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.BAN_FAILED))
                },
            )
        }
    }

    private fun markIncomingReadIfNeeded() {
        val state = currentState
        if (
            readJob?.isActive == true || state.currentUserId <= 0 ||
            state.messages.none { !it.isRead && it.toUserId == state.currentUserId }
        ) return
        readJob = viewModelScope.launch {
            runCatching { mutationHandler.markRead(userId) }.fold(
                onSuccess = { read ->
                    if (read) {
                        setState {
                            copy(messages = messages.map { message ->
                                if (message.toUserId == currentUserId) message.copy(isRead = true)
                                else message
                            })
                        }
                        mutationNotifier.notifyChanged()
                    }
                },
                onFailure = {
                    setEffect(ChatState.Effect.ShowMessage(ChatState.MessageType.READ_FAILED))
                },
            )
        }
    }
}

private fun mergeMessages(
    current: List<ChatMessage>,
    incoming: List<ChatMessage>,
): List<ChatMessage> = buildMap {
    current.forEach { put(it.id, it) }
    incoming.forEach { put(it.id, it) }
}.values.sortedWith(compareBy(ChatMessage::dateSeconds, ChatMessage::id))
