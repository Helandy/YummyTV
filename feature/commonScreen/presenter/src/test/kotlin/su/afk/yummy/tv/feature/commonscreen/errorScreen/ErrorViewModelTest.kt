package su.afk.yummy.tv.feature.commonscreen.errorScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.commonscreen.CommonScreenAnalytics
import su.afk.yummy.tv.feature.commonscreen.navigator.CommonScreenDestination

class ErrorViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val tracker: AnalyticsTracker = mockk(relaxed = true)

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
    }

    private fun createViewModel(retryKey: String? = "key") = ErrorViewModel(
        dest = CommonScreenDestination.ErrorNavigatorDest(ErrorItem("title", "message", retryKey = retryKey)),
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        navManager = nav,
        analytics = CommonScreenAnalytics(tracker),
    )

    @Test
    fun `shows the error and tracks it`() {
        val vm = createViewModel()

        assertEquals("message", vm.currentState.error?.message)
        verify(exactly = 1) { tracker.track(CommonScreenAnalytics.EVENT_ERROR_SHOWN, any()) }
    }

    @Test
    fun `retry runs the stored action and goes back`() {
        var retried = 0
        every { retryStorage.consume("key") } returns { retried++ }
        val vm = createViewModel()

        vm.setEvent(ErrorScreenState.Event.Retry)

        assertEquals(1, retried)
        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `retry without a key neither navigates nor consumes anything`() {
        val vm = createViewModel(retryKey = null)

        vm.setEvent(ErrorScreenState.Event.Retry)

        verify(exactly = 0) { nav.back() }
        verify(exactly = 0) { retryStorage.consume(any()) }
    }

    @Test
    fun `back pops both the error and the failed screen`() {
        createViewModel().setEvent(ErrorScreenState.Event.Back)

        verify(exactly = 1) { nav.backTwo() }
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `unused retry action is dropped when the screen is cleared`() {
        val store = ViewModelStore()
        ViewModelProvider.create(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = createViewModel() as T
            },
        )[ErrorViewModel::class.java]

        store.clear()

        verify(exactly = 1) { retryStorage.remove("key") }
    }
}
