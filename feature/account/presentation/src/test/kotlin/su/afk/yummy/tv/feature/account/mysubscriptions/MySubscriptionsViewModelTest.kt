package su.afk.yummy.tv.feature.account.mysubscriptions

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetVideoSubscriptionsUseCase
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.details.IDetailsNavigator

class MySubscriptionsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val subscriptionRepository: VideoSubscriptionRepository = mockk()
    private val detailsKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        every { detailsNavigator.getDetailsDest(any()) } returns detailsKey
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = true, userId = USER_ID)
        coEvery { subscriptionRepository.getSubscriptions(USER_ID) } returns listOf(subscription(animeId = 1))
    }

    private fun createViewModel() = MySubscriptionsViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getAccountSession = GetAccountSessionUseCase(accountRepository),
        getVideoSubscriptions = GetVideoSubscriptionsUseCase(subscriptionRepository),
    )

    @Test
    fun `starts in loading until the screen is shown`() {
        val state = createViewModel().currentState

        assertTrue(state.isLoading)
        assertTrue(state.subscriptions.isEmpty())
    }

    @Test
    fun `shown screen loads the subscriptions`() {
        val vm = createViewModel()

        vm.setEvent(MySubscriptionsState.Event.ScreenShown)

        val state = vm.currentState
        assertFalse(state.isLoading)
        assertTrue(state.isSignedIn)
        assertNull(state.error)
        assertEquals(listOf(1), state.subscriptions.map { it.animeId })
    }

    @Test
    fun `guest sees the sign-in state without requesting subscriptions`() {
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = false, userId = 0)
        val vm = createViewModel()

        vm.setEvent(MySubscriptionsState.Event.ScreenShown)

        assertFalse(vm.currentState.isSignedIn)
        assertFalse(vm.currentState.isLoading)
        coVerify(exactly = 0) { subscriptionRepository.getSubscriptions(any()) }
    }

    @Test
    fun `failed load shows the error and an empty list`() {
        coEvery { subscriptionRepository.getSubscriptions(USER_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()

        vm.setEvent(MySubscriptionsState.Event.ScreenShown)

        assertEquals(AccountUiError.LOAD_SUBSCRIPTIONS_FAILED, vm.currentState.error)
        assertTrue(vm.currentState.subscriptions.isEmpty())
        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `retry reloads after a failure`() {
        coEvery { subscriptionRepository.getSubscriptions(USER_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        vm.setEvent(MySubscriptionsState.Event.ScreenShown)
        coEvery { subscriptionRepository.getSubscriptions(USER_ID) } returns listOf(subscription(animeId = 2))

        vm.setEvent(MySubscriptionsState.Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(listOf(2), vm.currentState.subscriptions.map { it.animeId })
    }

    @Test
    fun `repeated shown refreshes silently without the loader`() {
        val vm = createViewModel()
        vm.setEvent(MySubscriptionsState.Event.ScreenShown)
        coEvery { subscriptionRepository.getSubscriptions(USER_ID) } returns listOf(subscription(animeId = 3))

        vm.setEvent(MySubscriptionsState.Event.ScreenShown)

        assertFalse(vm.currentState.isLoading)
        assertEquals(listOf(3), vm.currentState.subscriptions.map { it.animeId })
    }

    @Test
    fun `selecting a subscription opens the title`() {
        createViewModel().setEvent(MySubscriptionsState.Event.SubscriptionSelected(animeId = 9))

        verify(exactly = 1) { detailsNavigator.getDetailsDest(9) }
        verify(exactly = 1) { nav.navigate(detailsKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(MySubscriptionsState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private fun subscription(animeId: Int) = VideoSubscription(
        animeId = animeId,
        animeUrl = "url",
        playerId = 1,
        player = "Kodik",
        dubbing = "",
        posterUrl = null,
        title = "Title $animeId",
    )

    private companion object {
        const val USER_ID = 5
    }
}
