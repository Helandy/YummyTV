package su.afk.yummy.tv.feature.glossary

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
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.glossary.model.GlossaryCategory

class GlossaryViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
    }

    private fun createViewModel() = GlossaryViewModel(errorHandler, retryStorage, nav)

    @Test
    fun `back event leaves the screen`() {
        createViewModel().setEvent(GlossaryState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `query change is stored in state`() {
        val vm = createViewModel()

        vm.setEvent(GlossaryState.Event.QueryChanged("ова"))

        assertEquals("ова", vm.currentState.query)
    }

    @Test
    fun `category toggle collapses and expands it again`() {
        val vm = createViewModel()

        vm.setEvent(GlossaryState.Event.CategoryToggled(GlossaryCategory.SLANG))
        assertTrue(GlossaryCategory.SLANG in vm.currentState.collapsedCategories)

        vm.setEvent(GlossaryState.Event.CategoryToggled(GlossaryCategory.SLANG))
        assertFalse(GlossaryCategory.SLANG in vm.currentState.collapsedCategories)
    }
}
