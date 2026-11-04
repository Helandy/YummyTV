package su.afk.yummy.tv.feature.account.friends

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.repository.UserProfileContentRepository
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFriendsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator

class FriendsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val repository: UserProfileContentRepository = mockk(relaxed = true)
    private val profileKey: NavKey = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val messagesNavigator: IMessagesNavigator = mockk()

    @Before
    fun setUp() {
        every { accountNavigator.getUserProfileDest(any()) } returns profileKey
        every { accountNavigator.getUserSearchDest() } returns profileKey
        every { messagesNavigator.dialogs() } returns profileKey
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = true, userId = 7)
    }

    private fun createViewModel() = FriendsViewModel(
        userId = 7,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        getUserFriends = GetUserFriendsUseCase(repository),
        getAccountSession = GetAccountSessionUseCase(accountRepository),
        messagesNavigator = messagesNavigator,
    )

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(FriendsState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `selecting a friend opens their profile`() {
        createViewModel().setEvent(FriendsState.Event.FriendSelected(42))

        verify(exactly = 1) { accountNavigator.getUserProfileDest(42) }
        verify(exactly = 1) { nav.navigate(profileKey) }
    }

    @Test
    fun `empty state opens the user search`() {
        createViewModel().setEvent(FriendsState.Event.UserSearchSelected)

        verify(exactly = 1) { accountNavigator.getUserSearchDest() }
        verify(exactly = 1) { nav.navigate(profileKey) }
    }

    @Test
    fun `invalid friend id is ignored`() {
        createViewModel().setEvent(FriendsState.Event.FriendSelected(0))

        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `messages open dialogs on the own list`() {
        createViewModel().setEvent(FriendsState.Event.MessagesSelected)

        verify(exactly = 1) { nav.navigate(profileKey) }
    }

    @Test
    fun `messages are ignored on someone else's list`() {
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = true, userId = 99)

        createViewModel().setEvent(FriendsState.Event.MessagesSelected)

        verify(exactly = 0) { nav.navigate(any()) }
    }
}
