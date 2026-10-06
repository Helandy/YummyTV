package su.afk.yummy.tv.feature.details.collections

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.DetailsAnalytics

class CollectionsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private lateinit var repository: AnimeExtrasRepository
    private val collectionNavigator: ICollectionNavigator = mockk()

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        repository = mockk<AnimeExtrasRepository>()
        with(collectionNavigator) {
            every { getCollectionDest(any()) } answers { CollectionKey(firstArg()) }
        }
    }

    private fun createViewModel() = CollectionsViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        collectionNavigator = collectionNavigator,
        getAnimeCollections = GetAnimeCollectionsUseCase(repository),
        analytics = DetailsAnalytics(tracker),
    )

    private fun givenCollections(vararg collections: AnimeCollectionSummary) {
        coEvery { repository.getCollections(ANIME_ID, any(), any()) } returns collections.toList()
    }

    @Test
    fun `loads collections without duplicates`() {
        givenCollections(collection(1), collection(2), collection(1))

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf(1, 2), state.collections.map { it.id })
    }

    @Test
    fun `failure clears the collections and shows the error`() {
        coEvery { repository.getCollections(ANIME_ID, any(), any()) } throws IllegalStateException("boom")

        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("boom", state.error)
        assertTrue(state.collections.isEmpty())
    }

    @Test
    fun `retry reloads after a failure`() {
        coEvery { repository.getCollections(ANIME_ID, any(), any()) } throws IllegalStateException("boom") andThen
            listOf(collection(3))
        val vm = createViewModel()

        vm.setEvent(CollectionsState.Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(listOf(3), vm.currentState.collections.map { it.id })
    }

    @Test
    fun `selecting a collection opens it`() {
        givenCollections()

        createViewModel().setEvent(CollectionsState.Event.CollectionSelected(9))

        verify(exactly = 1) { nav.navigate(CollectionKey(9)) }
    }

    @Test
    fun `back leaves the screen`() {
        givenCollections()

        createViewModel().setEvent(CollectionsState.Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private data class CollectionKey(val id: Int) : NavKey

    private companion object {
        const val ANIME_ID = 10

        fun collection(id: Int) = AnimeCollectionSummary(
            id = id,
            title = "collection $id",
            description = "",
            posterUrl = null,
            poster = null,
            views = null,
        )
    }
}
