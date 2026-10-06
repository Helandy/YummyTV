package su.afk.yummy.tv.feature.posts.list

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.posts.model.PostCategory
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import su.afk.yummy.tv.domain.posts.usecase.GetPostCategoriesUseCase
import su.afk.yummy.tv.domain.posts.usecase.GetPostsUseCase
import su.afk.yummy.tv.feature.posts.IPostsNavigator

class PostsListViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val repository: PostsRepository = mockk()
    private val navigator: IPostsNavigator = mockk()

    @Before
    fun setUp() {
        with(errorHandler) {
            every { parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        }
        with(repository) {
            coEvery { categories() } returns emptyList()
        }
        with(navigator) {
            every { details(any()) } answers { PostKey(firstArg()) }
        }
    }

    private fun createViewModel() = PostsListViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        navigator = navigator,
        getPostCategories = GetPostCategoriesUseCase(repository),
        getPosts = GetPostsUseCase(repository),
    )

    @Test
    fun `loads categories on creation`() {
        coEvery { repository.categories() } returns listOf(PostCategory(1, "News", "news"))

        val state = createViewModel().currentState

        assertFalse(state.categoriesLoading)
        assertEquals(listOf("news"), state.categories.map { it.uri })
    }

    @Test
    fun `categories failure just stops the loading`() {
        coEvery { repository.categories() } throws IllegalStateException("boom")

        val state = createViewModel().currentState

        assertFalse(state.categoriesLoading)
        assertTrue(state.categories.isEmpty())
    }

    @Test
    fun `selecting another category rebuilds the posts flow`() {
        val vm = createViewModel()
        val before = vm.currentState.posts

        vm.setEvent(PostsListState.Event.CategorySelected("news"))

        assertEquals("news", vm.currentState.selectedCategory)
        assertNotSame(before, vm.currentState.posts)
    }

    @Test
    fun `selecting the current category keeps the posts flow`() {
        val vm = createViewModel()
        val before = vm.currentState.posts

        vm.setEvent(PostsListState.Event.CategorySelected(null))

        assertSame(before, vm.currentState.posts)
    }

    @Test
    fun `selecting another sort rebuilds the posts flow and keeps the category`() {
        val vm = createViewModel()
        vm.setEvent(PostsListState.Event.CategorySelected("news"))
        val before = vm.currentState.posts

        vm.setEvent(PostsListState.Event.SortSelected(PostSort.BEST))

        assertEquals(PostSort.BEST, vm.currentState.sort)
        assertEquals("news", vm.currentState.selectedCategory)
        assertNotSame(before, vm.currentState.posts)
    }

    @Test
    fun `selecting the current sort keeps the posts flow`() {
        val vm = createViewModel()
        val before = vm.currentState.posts

        vm.setEvent(PostsListState.Event.SortSelected(PostSort.NEW))

        assertSame(before, vm.currentState.posts)
    }

    @Test
    fun `selecting a post opens its details`() {
        createViewModel().setEvent(PostsListState.Event.PostSelected(8))

        verify(exactly = 1) { nav.navigateDetail(PostKey(8)) }
    }

    private data class PostKey(val postId: Int) : NavKey
}
