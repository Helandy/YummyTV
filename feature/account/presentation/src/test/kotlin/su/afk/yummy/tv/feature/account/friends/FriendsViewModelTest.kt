package su.afk.yummy.tv.feature.account.friends

import androidx.navigation3.runtime.NavKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.repository.UserProfileContentRepository
import su.afk.yummy.tv.domain.account.usecase.GetUserFriendsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator

class FriendsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val repository: UserProfileContentRepository = mockk(relaxed = true)
    private val profileKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { accountNavigator.getUserProfileDest(any()) } returns profileKey
        every { accountNavigator.getUserSearchDest() } returns profileKey
    }

    private fun createViewModel() = FriendsViewModel(
        userId = 7,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        getUserFriends = GetUserFriendsUseCase(repository),
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
}
