package su.afk.yummy.tv.feature.account.usersearch

import androidx.navigation3.runtime.NavKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import su.afk.yummy.tv.domain.account.usecase.SearchUsersUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator

class UserSearchViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val repository: UserDirectoryRepository = mockk(relaxed = true)
    private val profileKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { accountNavigator.getUserProfileByNicknameDest(any()) } returns profileKey
    }

    private fun createViewModel() = UserSearchViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        searchUsers = SearchUsersUseCase(repository),
    )

    @Test
    fun `starts with an empty inactive search`() {
        val state = createViewModel().currentState

        assertEquals("", state.query)
        assertFalse(state.isSearchActive)
    }

    @Test
    fun `typing updates the query but waits for the debounce`() {
        val vm = createViewModel()

        vm.setEvent(UserSearchState.Event.QueryChanged("sa"))

        assertEquals("sa", vm.currentState.query)
        assertFalse(vm.currentState.isSearchActive)
    }

    @Test
    fun `query is searched after the debounce`() {
        val vm = createViewModel()

        vm.setEvent(UserSearchState.Event.QueryChanged("sandy"))
        testScheduler.advanceTimeBy(501)
        testScheduler.runCurrent()

        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `too short query never starts a search`() {
        val vm = createViewModel()

        vm.setEvent(UserSearchState.Event.QueryChanged("s"))
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()

        assertFalse(vm.currentState.isSearchActive)
    }

    @Test
    fun `new typing cancels the pending search`() {
        val vm = createViewModel()

        vm.setEvent(UserSearchState.Event.QueryChanged("sandy"))
        testScheduler.advanceTimeBy(300)
        vm.setEvent(UserSearchState.Event.QueryChanged("s"))
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()

        assertFalse(vm.currentState.isSearchActive)
    }

    @Test
    fun `submit searches immediately with the trimmed query`() {
        val vm = createViewModel()
        vm.setEvent(UserSearchState.Event.QueryChanged("  sandy "))

        vm.setEvent(UserSearchState.Event.SearchSubmitted)

        assertEquals("sandy", vm.currentState.query)
        assertTrue(vm.currentState.isSearchActive)
    }

    @Test
    fun `selecting a user opens the profile by trimmed nickname`() {
        createViewModel().setEvent(UserSearchState.Event.UserSelected(" sandy "))

        verify(exactly = 1) { accountNavigator.getUserProfileByNicknameDest("sandy") }
        verify(exactly = 1) { nav.navigate(profileKey) }
    }

    @Test
    fun `blank nickname is ignored`() {
        createViewModel().setEvent(UserSearchState.Event.UserSelected("   "))

        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(UserSearchState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }
}
