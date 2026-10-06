package su.afk.yummy.tv.feature.messages.chat

import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.repository.MessagesMutationRepository
import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import su.afk.yummy.tv.domain.messages.usecase.GetDialogsUseCase
import su.afk.yummy.tv.domain.messages.usecase.GetMessagesUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.messages.ME_ID
import su.afk.yummy.tv.feature.messages.PEER_ID
import su.afk.yummy.tv.feature.messages.chat.ChatState.Effect
import su.afk.yummy.tv.feature.messages.chat.ChatState.Event
import su.afk.yummy.tv.feature.messages.chat.ChatState.MessageType
import su.afk.yummy.tv.feature.messages.chat.handler.ChatMutationHandler
import su.afk.yummy.tv.feature.messages.chat.handler.ChatPollingHandler
import su.afk.yummy.tv.feature.messages.chatMessage
import su.afk.yummy.tv.feature.messages.dialog

class ChatViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val messagesRepository: MessagesRepository = mockk()
    private val pollingHandler: ChatPollingHandler = mockk()
    private val mutationHandler: ChatMutationHandler = mockk()
    private val mutationNotifier: MessagesMutationRepository = mockk(relaxed = true)
    private val session = MutableStateFlow(AccountSession(isAuthorized = true, userId = ME_ID))
    private val updates = MutableSharedFlow<List<ChatMessage>>()
    private val savedStateHandle = SavedStateHandle()
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { accountRepository.observeSession() } returns session
        every { accountNavigator.getAccountDest() } returns navKey
        every { accountNavigator.getUserProfileDest(any()) } returns navKey
        every { pollingHandler.updates(any()) } returns updates
        coEvery { messagesRepository.dialogs(any(), any(), any()) } returns listOf(dialog())
        coEvery { messagesRepository.messages(PEER_ID, any(), any()) } returns listOf(
            chatMessage(1, from = PEER_ID, to = ME_ID),
            chatMessage(2, from = ME_ID, to = PEER_ID),
        )
    }

    private fun createViewModel() = ChatViewModel(
        userId = PEER_ID,
        nickname = "peer",
        avatarUrl = null,
        savedStateHandle = savedStateHandle,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        getDialogs = GetDialogsUseCase(messagesRepository),
        getMessages = GetMessagesUseCase(messagesRepository),
        pollingHandler = pollingHandler,
        mutationHandler = mutationHandler,
        mutationNotifier = mutationNotifier,
    )

    @Test
    fun `authorized session loads the peer and messages`() {
        val state = createViewModel().currentState

        assertTrue(state.isAuthResolved)
        assertTrue(state.isAuthorized)
        assertEquals(ME_ID, state.currentUserId)
        assertEquals(listOf(1, 2), state.messages.map { it.id })
        assertEquals(PEER_ID, state.peer?.userId)
        assertFalse(state.isLoading)
        assertFalse(state.canLoadOlder)
    }

    @Test
    fun `guest does not load anything`() {
        session.value = AccountSession(isAuthorized = false, userId = 0)

        val state = createViewModel().currentState

        assertTrue(state.isAuthResolved)
        assertFalse(state.isAuthorized)
        assertTrue(state.messages.isEmpty())
        coVerify(exactly = 0) { messagesRepository.messages(any(), any(), any()) }
    }

    @Test
    fun `failed initial load is marked as a load error`() {
        coEvery { messagesRepository.messages(any(), any(), any()) } throws IllegalStateException("boom")

        val state = createViewModel().currentState

        assertTrue(state.hasLoadError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `incoming unread messages are marked read`() {
        coEvery { messagesRepository.messages(PEER_ID, any(), any()) } returns
            listOf(chatMessage(1, from = PEER_ID, to = ME_ID, isRead = false))
        coEvery { mutationHandler.markRead(PEER_ID) } returns true

        val state = createViewModel().currentState

        assertTrue(state.messages.single().isRead)
        verify(exactly = 1) { mutationNotifier.notifyChanged() }
    }

    @Test
    fun `polling merges new messages only while the screen is started`() = runTest {
        val vm = createViewModel()

        vm.setEvent(Event.ScreenStarted)
        updates.emit(listOf(chatMessage(3, from = PEER_ID, to = ME_ID)))
        assertEquals(listOf(1, 2, 3), vm.currentState.messages.map { it.id })

        vm.setEvent(Event.ScreenStopped)
        updates.emit(listOf(chatMessage(4, from = PEER_ID, to = ME_ID)))
        assertEquals(listOf(1, 2, 3), vm.currentState.messages.map { it.id })
    }

    @Test
    fun `draft is stored in the saved state and restored`() {
        val vm = createViewModel()

        vm.setEvent(Event.DraftChanged("hello"))

        assertEquals("hello", vm.currentState.draft)
        assertEquals("hello", savedStateHandle.get<String>("messages_chat_draft_$PEER_ID"))
        assertEquals("hello", createViewModel().currentState.draft)
    }

    @Test
    fun `send appends the message clears the draft and notifies`() {
        coEvery { mutationHandler.send(PEER_ID, "hi", 0) } returns chatMessage(3, from = ME_ID, to = PEER_ID, text = "hi")
        val vm = createViewModel()
        vm.setEvent(Event.DraftChanged("  hi  "))

        vm.setEvent(Event.SendSelected)

        assertEquals(listOf(1, 2, 3), vm.currentState.messages.map { it.id })
        assertEquals("", vm.currentState.draft)
        assertFalse(vm.currentState.isMutating)
        verify(exactly = 1) { mutationNotifier.notifyChanged() }
    }

    @Test
    fun `send answers the message being replied to`() {
        coEvery { mutationHandler.send(PEER_ID, "re", 1) } returns chatMessage(3, from = ME_ID, to = PEER_ID)
        val vm = createViewModel()
        vm.setEvent(Event.ReplySelected(1))
        vm.setEvent(Event.DraftChanged("re"))

        vm.setEvent(Event.SendSelected)

        coVerify(exactly = 1) { mutationHandler.send(PEER_ID, "re", 1) }
        assertNull(vm.currentState.replyingTo)
    }

    @Test
    fun `failed send keeps the draft and reports the failure`() = runTest {
        coEvery { mutationHandler.send(any(), any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.DraftChanged("hi"))

        vm.setEvent(Event.SendSelected)

        assertEquals("hi", vm.currentState.draft)
        assertFalse(vm.currentState.isMutating)
        assertEquals(listOf<Effect>(Effect.ShowMessage(MessageType.SEND_FAILED)), effects)
    }

    @Test
    fun `blank draft and banned peer do not send`() {
        coEvery { messagesRepository.dialogs(any(), any(), any()) } returns listOf(dialog(isBanned = true))
        val vm = createViewModel()
        vm.setEvent(Event.DraftChanged("hi"))

        vm.setEvent(Event.SendSelected)

        coVerify(exactly = 0) { mutationHandler.send(any(), any(), any()) }
    }

    @Test
    fun `own message can be edited and saved`() {
        coEvery { mutationHandler.edit(2, "new") } returns chatMessage(2, from = ME_ID, to = PEER_ID, text = "new")
        val vm = createViewModel()

        vm.setEvent(Event.EditSelected(2))
        assertEquals(2, vm.currentState.editingMessageId)
        vm.setEvent(Event.EditTextChanged("new"))
        vm.setEvent(Event.EditConfirmed)

        assertNull(vm.currentState.editingMessageId)
        assertEquals("new", vm.currentState.messages.first { it.id == 2 }.text)
    }

    @Test
    fun `foreign message cannot be edited or deleted but can be claimed`() {
        val vm = createViewModel()

        vm.setEvent(Event.EditSelected(1))
        vm.setEvent(Event.DeleteSelected(1))
        vm.setEvent(Event.ClaimSelected(1))

        assertNull(vm.currentState.editingMessageId)
        assertNull(vm.currentState.pendingDeleteMessageId)
        assertEquals(1, vm.currentState.pendingClaimMessageId)
    }

    @Test
    fun `confirmed delete replaces the message and notifies`() {
        coEvery { mutationHandler.delete(2) } returns chatMessage(2, from = ME_ID, to = PEER_ID, isDeleted = true)
        val vm = createViewModel()
        vm.setEvent(Event.DeleteSelected(2))

        vm.setEvent(Event.DeleteConfirmed)

        assertTrue(vm.currentState.messages.first { it.id == 2 }.isDeleted)
        assertNull(vm.currentState.pendingDeleteMessageId)
    }

    @Test
    fun `failed delete reports the failure`() = runTest {
        coEvery { mutationHandler.delete(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.DeleteSelected(2))

        vm.setEvent(Event.DeleteConfirmed)

        assertEquals(listOf<Effect>(Effect.ShowMessage(MessageType.DELETE_FAILED)), effects)
    }

    @Test
    fun `deleted own message can be restored`() {
        coEvery { messagesRepository.messages(PEER_ID, any(), any()) } returns
            listOf(chatMessage(2, from = ME_ID, to = PEER_ID, isDeleted = true))
        coEvery { mutationHandler.restore(2) } returns chatMessage(2, from = ME_ID, to = PEER_ID)
        val vm = createViewModel()

        vm.setEvent(Event.RestoreSelected(2))

        assertFalse(vm.currentState.messages.single().isDeleted)
    }

    @Test
    fun `confirmed claim reports that it was sent`() = runTest {
        coEvery { mutationHandler.claim(1) } returns true
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.ClaimSelected(1))

        vm.setEvent(Event.ClaimConfirmed)

        assertEquals(listOf<Effect>(Effect.ShowMessage(MessageType.CLAIM_SENT)), effects)
        assertNull(vm.currentState.pendingClaimMessageId)
    }

    @Test
    fun `history loads and can be dismissed`() {
        coEvery { mutationHandler.history(2) } returns emptyList()
        val vm = createViewModel()

        vm.setEvent(Event.HistorySelected(2))
        assertEquals(2, vm.currentState.historyMessageId)
        assertFalse(vm.currentState.isHistoryLoading)

        vm.setEvent(Event.HistoryDismissed)
        assertNull(vm.currentState.historyMessageId)
    }

    @Test
    fun `failed history is marked with an error`() {
        coEvery { mutationHandler.history(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()

        vm.setEvent(Event.HistorySelected(2))

        assertTrue(vm.currentState.hasHistoryError)
    }

    @Test
    fun `ban toggle flips the banned flag of the peer`() {
        coEvery { mutationHandler.setBanned(PEER_ID, true) } returns true
        val vm = createViewModel()
        vm.setEvent(Event.BanToggleSelected)
        assertTrue(vm.currentState.isBanConfirmationVisible)

        vm.setEvent(Event.BanToggleConfirmed)

        assertTrue(vm.currentState.peer!!.isBanned)
        assertFalse(vm.currentState.isBanConfirmationVisible)
    }

    @Test
    fun `refused ban reports the failure`() = runTest {
        coEvery { mutationHandler.setBanned(any(), any()) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.BanToggleConfirmed)

        assertEquals(listOf<Effect>(Effect.ShowMessage(MessageType.BAN_FAILED)), effects)
    }

    @Test
    fun `full page enables loading older messages`() {
        val page = (1..30).map { chatMessage(it + 100, from = PEER_ID, to = ME_ID) }
        coEvery { messagesRepository.messages(PEER_ID, any(), 0) } returns page
        coEvery { messagesRepository.messages(PEER_ID, any(), 101) } returns listOf(chatMessage(50))
        val vm = createViewModel()
        assertTrue(vm.currentState.canLoadOlder)

        vm.setEvent(Event.LoadOlderSelected)

        assertEquals(50, vm.currentState.messages.first().id)
        assertFalse(vm.currentState.canLoadOlder)
    }

    @Test
    fun `author login and back events navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.AuthorSelected(PEER_ID))
        vm.setEvent(Event.AuthorSelected(0))
        vm.setEvent(Event.LoginSelected)
        vm.setEvent(Event.BackSelected)

        verify(exactly = 2) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
    }
}
