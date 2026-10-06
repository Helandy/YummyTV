package su.afk.yummy.tv.feature.messages.dialogs

import androidx.navigation3.runtime.NavKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.messages.repository.MessagesMutationRepository
import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import su.afk.yummy.tv.domain.messages.usecase.GetDialogsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.messages.dialogs.DialogsState.Event

class DialogsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val navigator: IMessagesNavigator = mockk()
    private val accountNavigator: IAccountNavigator = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val messagesRepository: MessagesRepository = mockk()
    private val mutationNotifier: MessagesMutationRepository = mockk()
    private val session = MutableStateFlow(AccountSession(isAuthorized = true, userId = 1))
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { accountRepository.observeSession() } returns session
        every { mutationNotifier.version } returns MutableStateFlow(0L)
        every { navigator.chat(any(), any(), any()) } returns navKey
        every { accountNavigator.getAccountDest() } returns navKey
    }

    private fun createViewModel() = DialogsViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        navigator = navigator,
        accountNavigator = accountNavigator,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        getDialogs = GetDialogsUseCase(messagesRepository),
        mutationNotifier = mutationNotifier,
    )

    @Test
    fun `authorized session resolves auth`() {
        val state = createViewModel().currentState

        assertTrue(state.isAuthResolved)
        assertTrue(state.isAuthorized)
    }

    @Test
    fun `guest session is resolved as not authorized`() {
        session.value = AccountSession(isAuthorized = false, userId = 0)

        val state = createViewModel().currentState

        assertTrue(state.isAuthResolved)
        assertFalse(state.isAuthorized)
    }

    @Test
    fun `signing in creates the dialogs flow once`() {
        session.value = AccountSession(isAuthorized = false, userId = 0)
        val vm = createViewModel()
        val guestFlow = vm.currentState.dialogs

        session.value = AccountSession(isAuthorized = true, userId = 1)
        val signedInFlow = vm.currentState.dialogs
        session.value = AccountSession(isAuthorized = true, userId = 1)

        assertNotSame(guestFlow, signedInFlow)
        assertSame(signedInFlow, vm.currentState.dialogs)
    }

    @Test
    fun `dialog selection opens the chat and negative id is ignored`() {
        val vm = createViewModel()

        vm.setEvent(Event.DialogSelected(5))
        vm.setEvent(Event.DialogSelected(-1))

        verify(exactly = 1) { navigator.chat(5, any(), any()) }
        verify(exactly = 1) { nav.navigateDetail(navKey) }
    }

    @Test
    fun `login and back events navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.LoginSelected)
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
    }
}
