package su.afk.yummy.tv.feature.posts.mobile.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.posts.model.PostAuthor
import su.afk.yummy.tv.domain.posts.model.PostCategory
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.domain.posts.model.PostSummary
import su.afk.yummy.tv.feature.posts.list.PostsListState
import su.afk.yummy.tv.feature.posts.presentation.R

class PostsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<PostsListState.Event>()

    private val category = PostCategory(id = 1, title = "Новости сайта", uri = "site-news")

    private val post = PostSummary(
        id = 31,
        title = "Тестовая публикация",
        previewImageUrl = null,
        contentPreview = "Краткое содержание",
        author = PostAuthor(id = 2, nickname = "Автор", avatarUrl = null),
        category = category,
        createdAt = 0L,
    )

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(
        state: PostsListState.State = PostsListState.State(
            posts = flowOf(PagingData.empty(sourceLoadStates = loaded)),
        ),
    ) {
        composeRule.setContent {
            ScreenPreviewTheme {
                PostsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsSortTabsAndEmptyMessage() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.posts_new)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.posts_old)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.posts_best)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.posts_empty)).assertIsDisplayed()
    }

    @Test
    fun postIsShownAndClickSendsPostSelected() {
        setScreen(PostsListState.State(posts = flowOf(PagingData.from(listOf(post), sourceLoadStates = loaded))))

        composeRule.onNodeWithText("Тестовая публикация").assertIsDisplayed()
        composeRule.onNodeWithText("Тестовая публикация").performClick()

        assertTrue(PostsListState.Event.PostSelected(31) in events)
    }

    @Test
    fun categoryChipSendsCategorySelected() {
        setScreen(PostsListState.State(
                posts = flowOf(PagingData.empty(sourceLoadStates = loaded)),
                categories = persistentListOf(category),
            ))

        composeRule.onNodeWithText("Новости сайта").performClick()

        assertTrue(PostsListState.Event.CategorySelected("site-news") in events)
    }

    @Test
    fun allChipResetsCategory() {
        setScreen(PostsListState.State(
                posts = flowOf(PagingData.empty(sourceLoadStates = loaded)),
                categories = persistentListOf(category),
                selectedCategory = "site-news",
            ))

        composeRule.onNodeWithText(string(R.string.posts_all)).performClick()

        assertTrue(PostsListState.Event.CategorySelected(null) in events)
    }

    @Test
    fun bestTabSendsSortSelected() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.posts_best)).performClick()
        composeRule.waitForIdle()

        assertTrue(PostsListState.Event.SortSelected(PostSort.BEST) in events)
    }
}
