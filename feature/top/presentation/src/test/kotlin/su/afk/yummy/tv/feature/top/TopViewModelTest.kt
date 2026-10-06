package su.afk.yummy.tv.feature.top

import androidx.navigation3.runtime.NavKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.repository.AnimeTopRepository
import su.afk.yummy.tv.domain.top.usecase.GetAnimeTopUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator

class TopViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private lateinit var showTopTitleYear: MutableStateFlow<Boolean>
    private val settings: AppearanceSettingsStore = mockk()
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val tracker: AnalyticsTracker = mockk(relaxed = true)

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        showTopTitleYear = MutableStateFlow(false)
        every { settings.showTopTitleYear } returns showTopTitleYear
        with(detailsNavigator) {
            every { getDetailsDest(any()) } answers { DetailsKey(firstArg()) }
        }
    }

    private fun createViewModel() = TopViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getAnimeTop = GetAnimeTopUseCase(mockk<AnimeTopRepository>()),
        settingsStore = settings,
        analytics = TopAnalytics(tracker),
    )

    @Test
    fun `starts on the TV top`() {
        val state = createViewModel().currentState

        assertEquals(AnimeTopType.TV, state.selectedType)
        verify(exactly = 1) { tracker.track(TopAnalytics.EVENT_SCREEN_OPENED, any()) }
    }

    @Test
    fun `title year flag follows the settings`() {
        val vm = createViewModel()
        assertEquals(false, vm.currentState.showTitleYear)

        showTopTitleYear.value = true

        assertEquals(true, vm.currentState.showTitleYear)
    }

    @Test
    fun `selecting another type switches the selected type and the paging flow`() {
        val vm = createViewModel()
        val before = vm.currentState.items

        vm.setEvent(TopState.Event.TypeSelected(AnimeTopType.MOVIE))

        assertEquals(AnimeTopType.MOVIE, vm.currentState.selectedType)
        assertNotSame(before, vm.currentState.items)
        verify(exactly = 1) { tracker.track(TopAnalytics.EVENT_TYPE_SELECTED, any()) }
    }

    @Test
    fun `selecting the current type changes nothing`() {
        val vm = createViewModel()
        val before = vm.currentState.items

        vm.setEvent(TopState.Event.TypeSelected(AnimeTopType.TV))

        assertSame(before, vm.currentState.items)
        verify(exactly = 0) { tracker.track(TopAnalytics.EVENT_TYPE_SELECTED, any()) }
    }

    @Test
    fun `selecting an anime opens its details`() {
        createViewModel().setEvent(TopState.Event.AnimeSelected(5))

        verify(exactly = 1) { nav.navigate(DetailsKey(5)) }
    }

    private data class DetailsKey(val animeId: Int) : NavKey
}
