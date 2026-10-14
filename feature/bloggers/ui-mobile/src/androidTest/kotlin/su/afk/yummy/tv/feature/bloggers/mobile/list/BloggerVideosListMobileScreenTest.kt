package su.afk.yummy.tv.feature.bloggers.mobile.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListState
import su.afk.yummy.tv.feature.bloggers.presentation.R

class BloggerVideosListMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<BloggerVideosListState.Event>()

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private val creator = Blogger(id = 4, nickname = "Блогер Тест", avatarUrl = null)

    private val video = BloggerVideo(
        id = 70,
        title = "Тестовое видео",
        description = "Описание видео",
        previewUrl = null,
        iframeUrl = "https://www.youtube.com/embed/abcdefghijk",
        publishedAt = 0L,
        views = 1200L,
        hasSpoiler = false,
        category = BloggerVideoCategory(id = "review", title = "Обзор"),
        creator = creator,
        commentsCount = 6,
    )

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun stateWith(vararg items: BloggerVideo, animeId: Int? = null) = BloggerVideosListState.State(
        animeId = animeId,
        videos = flowOf(PagingData.from(items.toList(), sourceLoadStates = loaded)),
    )

    private fun setScreen(state: BloggerVideosListState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                BloggerVideosListMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndEmptyMessage() {
        setScreen(stateWith())

        composeRule.onNodeWithText(string(R.string.blogger_videos_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.blogger_videos_empty)).assertIsDisplayed()
    }

    @Test
    fun animeModeShowsAnimeTitle() {
        setScreen(stateWith(animeId = 5))

        composeRule.onNodeWithText(string(R.string.blogger_videos_anime_title)).assertIsDisplayed()
    }

    @Test
    fun videoClickSendsVideoSelected() {
        setScreen(stateWith(video))

        composeRule.onNodeWithText("Тестовое видео").performClick()

        assertTrue(BloggerVideosListState.Event.VideoSelected(70) in events)
    }

    @Test
    fun creatorClickSendsBloggerDetailsSelected() {
        setScreen(stateWith(video))

        composeRule.onNodeWithText("Блогер Тест").performClick()

        assertTrue(BloggerVideosListState.Event.BloggerDetailsSelected(4) in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(stateWith())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(BloggerVideosListState.Event.BackSelected in events)
    }
}
