package su.afk.yummy.tv.feature.bloggers.mobile.video

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.feature.bloggers.presentation.R
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsState

class BloggerVideoDetailsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<BloggerVideoDetailsState.Event>()

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

    private fun setScreen(state: BloggerVideoDetailsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                BloggerVideoDetailsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsVideoTitleAndDescription() {
        setScreen(BloggerVideoDetailsState.State(loading = false, video = video))

        composeRule.onNodeWithText("Тестовое видео").assertIsDisplayed()
        composeRule.onNodeWithText("Описание видео").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun creatorClickSendsBloggerSelected() {
        setScreen(BloggerVideoDetailsState.State(loading = false, video = video))

        composeRule.onNodeWithText("Блогер Тест").performScrollTo().performClick()

        assertTrue(BloggerVideoDetailsState.Event.BloggerSelected in events)
    }

    @Test
    fun commentsButtonSendsCommentsSelected() {
        setScreen(BloggerVideoDetailsState.State(loading = false, video = video))

        composeRule.onNodeWithText(string(R.string.blogger_video_comments, "6")).performScrollTo().performClick()

        assertTrue(BloggerVideoDetailsState.Event.CommentsSelected in events)
    }

    @Test
    fun heroWatchSendsWatchSelected() {
        setScreen(BloggerVideoDetailsState.State(loading = false, video = video))

        composeRule.onNodeWithContentDescription(string(R.string.blogger_video_watch)).performClick()

        assertTrue(BloggerVideoDetailsState.Event.WatchSelected in events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(BloggerVideoDetailsState.State(loading = false, error = UiText.Res(R.string.blogger_video_load_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(BloggerVideoDetailsState.Event.RetrySelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(BloggerVideoDetailsState.State(loading = false, video = video))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(BloggerVideoDetailsState.Event.BackSelected in events)
    }
}
