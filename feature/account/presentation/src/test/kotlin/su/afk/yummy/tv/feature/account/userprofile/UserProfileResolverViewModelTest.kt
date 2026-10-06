package su.afk.yummy.tv.feature.account.userprofile

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileByNicknameUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator

class UserProfileResolverViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val repository: UserDirectoryRepository = mockk()
    private val profileKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        every { accountNavigator.getUserProfileDest(any()) } returns profileKey
    }

    private fun createViewModel(nickname: String = NICKNAME) = UserProfileResolverViewModel(
        nickname = nickname,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        getProfile = GetUserProfileByNicknameUseCase(repository),
    )

    @Test
    fun `known nickname replaces the screen with the user profile`() {
        coEvery { repository.getProfileByNickname(NICKNAME) } returns UserProfileSummary(userId = 42)

        createViewModel()

        verify(exactly = 1) { accountNavigator.getUserProfileDest(42) }
        verify(exactly = 1) { nav.replace(profileKey) }
    }

    @Test
    fun `profile without an id is shown as an error`() {
        coEvery { repository.getProfileByNickname(NICKNAME) } returns UserProfileSummary(userId = 0)

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertTrue(state.hasError)
        verify(exactly = 0) { nav.replace(any()) }
    }

    @Test
    fun `failed lookup is shown as an error`() {
        coEvery { repository.getProfileByNickname(NICKNAME) } throws IllegalStateException("boom")

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertTrue(state.hasError)
    }

    @Test
    fun `retry resolves the nickname again`() {
        coEvery { repository.getProfileByNickname(NICKNAME) } throws IllegalStateException("boom")
        val vm = createViewModel()
        coEvery { repository.getProfileByNickname(NICKNAME) } returns UserProfileSummary(userId = 7)

        vm.setEvent(UserProfileResolverState.Event.RetrySelected)

        verify(exactly = 1) { nav.replace(profileKey) }
    }

    @Test
    fun `back leaves the screen`() {
        coEvery { repository.getProfileByNickname(NICKNAME) } throws IllegalStateException("boom")

        createViewModel().setEvent(UserProfileResolverState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val NICKNAME = "sandy"
    }
}
