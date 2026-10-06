package su.afk.yummy.tv.feature.faq

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest

class FaqViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
    }

    private fun createViewModel() = FaqViewModel(errorHandler, retryStorage, nav)

    @Test
    fun `back event leaves the screen`() {
        createViewModel().setEvent(FaqState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }
}
