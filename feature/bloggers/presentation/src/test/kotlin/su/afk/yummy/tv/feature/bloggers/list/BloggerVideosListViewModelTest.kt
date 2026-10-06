package su.afk.yummy.tv.feature.bloggers.list

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerDirectory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import su.afk.yummy.tv.domain.bloggers.usecase.GetAnimeBloggerVideosUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggersDirectoryUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListState.Event

class BloggerVideosListViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val navigator: IBloggerVideosNavigator = mockk()
    private val repository: BloggerVideosRepository = mockk()
    private val videoKey: NavKey = mockk()
    private val bloggerKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { navigator.video(any()) } returns videoKey
        every { navigator.blogger(any()) } returns bloggerKey
        coEvery { repository.getDirectory(any()) } returns BloggerDirectory(
            categories = listOf(BloggerVideoCategory("all", "All"), BloggerVideoCategory("news", "News")),
            bloggers = listOf(Blogger(1, "blogger", null)),
        )
    }

    private fun createViewModel(animeId: Int? = null) = BloggerVideosListViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        getVideos = GetBloggerVideosUseCase(repository),
        getAnimeVideos = GetAnimeBloggerVideosUseCase(repository),
        getDirectory = GetBloggersDirectoryUseCase(repository),
        bloggerNavigator = navigator,
        animeId = animeId,
    )

    @Test
    fun `general feed loads the filter directory`() {
        val state = createViewModel().currentState

        assertNull(state.animeId)
        assertEquals(listOf("all", "news"), state.categories.map { it.id })
        assertEquals(listOf(1), state.bloggers.map { it.id })
    }

    @Test
    fun `anime feed does not load the directory`() {
        val vm = createViewModel(animeId = 5)

        assertEquals(5, vm.currentState.animeId)
        assertTrue(vm.currentState.categories.isEmpty())
    }

    @Test
    fun `selecting a category rebuilds the video flow`() {
        val vm = createViewModel()
        val before = vm.currentState.videos

        vm.setEvent(Event.CategorySelected("news"))

        assertEquals("news", vm.currentState.selectedCategory)
        assertTrue(before !== vm.currentState.videos)
    }

    @Test
    fun `blogger and sort selection update the filters`() {
        val vm = createViewModel()

        vm.setEvent(Event.BloggerSelected(1))
        vm.setEvent(Event.SortSelected(BloggerVideoSort.TOP))

        assertEquals(1, vm.currentState.selectedBloggerId)
        assertEquals(BloggerVideoSort.TOP, vm.currentState.sort)
    }

    @Test
    fun `reset restores the default filters`() {
        val vm = createViewModel()
        vm.setEvent(Event.CategorySelected("news"))
        vm.setEvent(Event.BloggerSelected(1))

        vm.setEvent(Event.FiltersReset)

        assertEquals("all", vm.currentState.selectedCategory)
        assertNull(vm.currentState.selectedBloggerId)
    }

    @Test
    fun `anime feed keeps its flow when filters change`() {
        val vm = createViewModel(animeId = 5)
        val before = vm.currentState.videos

        vm.setEvent(Event.CategorySelected("news"))

        assertSame(before, vm.currentState.videos)
    }

    @Test
    fun `video and blogger selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.VideoSelected(4))
        vm.setEvent(Event.BloggerDetailsSelected(1))

        verify(exactly = 1) { nav.navigate(videoKey) }
        verify(exactly = 1) { nav.navigate(bloggerKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }
}
