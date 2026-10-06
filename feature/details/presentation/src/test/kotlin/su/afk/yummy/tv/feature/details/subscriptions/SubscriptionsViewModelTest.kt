package su.afk.yummy.tv.feature.details.subscriptions

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.details.handler.DetailsSubscriptionHandler
import su.afk.yummy.tv.feature.details.details.handler.ScreenSubscriptionBase
import su.afk.yummy.tv.feature.details.details.handler.ScreenSubscriptionBaseResult
import su.afk.yummy.tv.feature.details.details.model.SubscriptionOption
import su.afk.yummy.tv.feature.details.subscriptions.SubscriptionsState.Event

class SubscriptionsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val subscriptionHandler: DetailsSubscriptionHandler = mockk()
    private val tracker: AnalyticsTracker = mockk(relaxed = true)

    private fun option(key: String, subscribed: Boolean = false) = SubscriptionOption(
        key = key,
        playerId = 1,
        player = "Kodik",
        dubbing = key,
        episodesCount = 12,
        subscriptionVideoId = 100,
        isSubscribed = subscribed,
    )

    private fun content(vararg options: SubscriptionOption) =
        ScreenSubscriptionBaseResult.Content(ScreenSubscriptionBase(details = null, subscriptions = options.toList()))

    private fun createViewModel() = SubscriptionsViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        subscriptionHandler = subscriptionHandler,
        analytics = DetailsAnalytics(tracker),
    )

    @Test
    fun `loads the subscription options`() {
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns content(option("a"), option("b"))

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals(listOf("a", "b"), state.subscriptions.map { it.key })
    }

    @Test
    fun `signed out user sees no subscriptions`() {
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns ScreenSubscriptionBaseResult.SignedOut

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertTrue(state.subscriptions.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns
            ScreenSubscriptionBaseResult.Failure("offline", IllegalStateException("offline"))
        val vm = createViewModel()
        assertEquals("offline", vm.currentState.error)
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns content(option("a"))

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(1, vm.currentState.subscriptions.size)
    }

    @Test
    fun `toggle shows the new state immediately and takes the reloaded list`() {
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns content(option("a"))
        coEvery { subscriptionHandler.commitSubscriptionChange(ANIME_ID, any(), true) } returns true
        coEvery { subscriptionHandler.reloadSubscriptions(ANIME_ID) } returns
            Result.success(listOf(option("a", subscribed = true)))
        val vm = createViewModel()

        vm.setEvent(Event.SubscriptionToggled("a"))

        assertTrue(vm.currentState.subscriptions.single().isSubscribed)
        coVerify(exactly = 1) { subscriptionHandler.reloadSubscriptions(ANIME_ID) }
    }

    @Test
    fun `failed toggle is rolled back`() {
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns content(option("a"))
        coEvery { subscriptionHandler.commitSubscriptionChange(any(), any(), any()) } returns false
        val vm = createViewModel()

        vm.setEvent(Event.SubscriptionToggled("a"))

        assertFalse(vm.currentState.subscriptions.single().isSubscribed)
        coVerify(exactly = 0) { subscriptionHandler.reloadSubscriptions(any()) }
    }

    @Test
    fun `unknown key is ignored`() {
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns content(option("a"))
        val vm = createViewModel()

        vm.setEvent(Event.SubscriptionToggled("zzz"))

        coVerify(exactly = 0) { subscriptionHandler.commitSubscriptionChange(any(), any(), any()) }
    }

    @Test
    fun `back leaves the screen`() {
        coEvery { subscriptionHandler.loadScreenSubscriptionBase(ANIME_ID) } returns ScreenSubscriptionBaseResult.SignedOut

        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val ANIME_ID = 4
    }
}
