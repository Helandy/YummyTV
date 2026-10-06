package su.afk.yummy.tv.feature.pages

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
import su.afk.yummy.tv.domain.pages.model.SitePage
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.domain.pages.repository.SitePagesRepository
import su.afk.yummy.tv.domain.pages.usecase.GetSitePageUseCase

class SitePagesViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private lateinit var repository: SitePagesRepository

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        repository = mockk<SitePagesRepository>()
    }

    private fun createViewModel() = SitePagesViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        getSitePage = GetSitePageUseCase(repository),
    )

    private fun givenPage(type: SitePageType) {
        coEvery { repository.getPage(type) } returns SitePage(type.apiValue, "text")
    }

    @Test
    fun `selecting a page loads it`() {
        givenPage(SitePageType.RULES)
        val vm = createViewModel()

        vm.setEvent(SitePagesState.Event.PageSelected(SitePageType.RULES))

        val state = vm.currentState
        assertEquals(SitePageType.RULES, state.selectedType)
        assertEquals(SitePage("rules", "text"), state.page)
        assertFalse(state.loading)
        assertFalse(state.usingFallback)
    }

    @Test
    fun `failed load switches to the fallback`() {
        coEvery { repository.getPage(SitePageType.FAQ) } throws IllegalStateException("boom")
        val vm = createViewModel()

        vm.setEvent(SitePagesState.Event.PageSelected(SitePageType.FAQ))

        val state = vm.currentState
        assertEquals(SitePageType.FAQ, state.selectedType)
        assertNull(state.page)
        assertFalse(state.loading)
        assertTrue(state.usingFallback)
    }

    @Test
    fun `retry reloads the selected page and clears the fallback`() {
        coEvery { repository.getPage(SitePageType.PRIVACY) } throws IllegalStateException("boom") andThen
            SitePage("privacy", "text")
        val vm = createViewModel()
        vm.setEvent(SitePagesState.Event.PageSelected(SitePageType.PRIVACY))

        vm.setEvent(SitePagesState.Event.RetrySelected)

        coVerify(exactly = 2) { repository.getPage(SitePageType.PRIVACY) }
        assertFalse(vm.currentState.usingFallback)
        assertEquals(SitePage("privacy", "text"), vm.currentState.page)
    }

    @Test
    fun `retry without a selected page does nothing`() {
        val vm = createViewModel()

        vm.setEvent(SitePagesState.Event.RetrySelected)

        coVerify(exactly = 0) { repository.getPage(any()) }
    }

    @Test
    fun `back from a page returns to the list`() {
        givenPage(SitePageType.RULES)
        val vm = createViewModel()
        vm.setEvent(SitePagesState.Event.PageSelected(SitePageType.RULES))

        vm.setEvent(SitePagesState.Event.BackSelected)

        assertEquals(SitePagesState.State(), vm.currentState)
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `back from the list leaves the screen`() {
        val vm = createViewModel()

        vm.setEvent(SitePagesState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }
}
