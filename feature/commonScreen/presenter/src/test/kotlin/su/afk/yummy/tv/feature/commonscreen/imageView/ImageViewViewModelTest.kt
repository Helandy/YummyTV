package su.afk.yummy.tv.feature.commonscreen.imageView

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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

class ImageViewViewModelTest : BaseUnitTest() {

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

    private fun createViewModel(selectedIndex: Int = 0, images: List<String> = listOf("a", "b", "c")) =
        ImageViewViewModel(
            dest = CommonScreenDestination.ImageViewDest(images, selectedIndex),
            errorHandler = errorHandler,
            retryStorage = retryStorage,
            navManager = nav,
            analytics = CommonScreenAnalytics(tracker),
        )

    @Test
    fun `initial index is clamped into the image range`() {
        assertEquals(2, createViewModel(selectedIndex = 10).currentState.selectedIndex)
        assertEquals(0, createViewModel(selectedIndex = -3).currentState.selectedIndex)
        assertEquals(0, createViewModel(selectedIndex = 5, images = emptyList()).currentState.selectedIndex)
    }

    @Test
    fun `next stops at the last image`() {
        val vm = createViewModel(selectedIndex = 1)

        vm.setEvent(ImageViewState.Event.Next)
        vm.setEvent(ImageViewState.Event.Next)

        assertEquals(2, vm.currentState.selectedIndex)
        assertEquals(false, vm.currentState.hasNext)
    }

    @Test
    fun `previous stops at the first image`() {
        val vm = createViewModel(selectedIndex = 1)

        vm.setEvent(ImageViewState.Event.Previous)
        vm.setEvent(ImageViewState.Event.Previous)

        assertEquals(0, vm.currentState.selectedIndex)
        assertEquals(false, vm.currentState.hasPrevious)
    }

    @Test
    fun `select index is clamped`() {
        val vm = createViewModel()

        vm.setEvent(ImageViewState.Event.SelectIndex(99))
        assertEquals(2, vm.currentState.selectedIndex)

        vm.setEvent(ImageViewState.Event.SelectIndex(-1))
        assertEquals(0, vm.currentState.selectedIndex)
        assertEquals("a", vm.currentState.currentImage)
    }

    @Test
    fun `navigation events are tracked`() {
        val vm = createViewModel()

        vm.setEvent(ImageViewState.Event.Next)
        vm.setEvent(ImageViewState.Event.Previous)
        vm.setEvent(ImageViewState.Event.SelectIndex(1))

        verifyOrder {
            tracker.track(CommonScreenAnalytics.EVENT_IMAGE_NEXT, any())
            tracker.track(CommonScreenAnalytics.EVENT_IMAGE_PREVIOUS, any())
            tracker.track(CommonScreenAnalytics.EVENT_IMAGE_SELECT_INDEX, any())
        }
    }

    @Test
    fun `back leaves the viewer`() {
        createViewModel().setEvent(ImageViewState.Event.Back)

        verify(exactly = 1) { nav.back() }
    }
}
