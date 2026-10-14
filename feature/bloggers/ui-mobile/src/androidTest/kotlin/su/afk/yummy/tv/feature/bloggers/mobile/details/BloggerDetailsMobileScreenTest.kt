package su.afk.yummy.tv.feature.bloggers.mobile.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerDetails
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.feature.bloggers.details.BloggerDetailsState
import su.afk.yummy.tv.feature.bloggers.presentation.R

class BloggerDetailsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<BloggerDetailsState.Event>()

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

    private val blogger = BloggerDetails(
        id = 4,
        nickname = "Блогер Тест",
        avatarUrl = null,
        subscribers = 120,
        videosCount = 8,
        isSubscribed = false,
        categories = emptyList(),
    )

    private fun setScreen(state: BloggerDetailsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                BloggerDetailsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsBloggerCounters() {
        setScreen(BloggerDetailsState.State(loading = false, blogger = blogger))

        composeRule.onNodeWithText(string(R.string.blogger_subscribers, 120)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.blogger_videos_count, 8)).assertIsDisplayed()
    }

    @Test
    fun subscribeSendsSubscribeSelected() {
        setScreen(BloggerDetailsState.State(loading = false, blogger = blogger))

        composeRule.onNodeWithText(string(R.string.blogger_subscribe)).performClick()

        assertTrue(BloggerDetailsState.Event.SubscribeSelected in events)
    }

    @Test
    fun subscribedBloggerShowsUnsubscribe() {
        setScreen(BloggerDetailsState.State(loading = false, blogger = blogger.copy(isSubscribed = true)))

        composeRule.onNodeWithText(string(R.string.blogger_unsubscribe)).assertIsDisplayed()
    }

    @Test
    fun emptyVideosShowMessage() {
        setScreen(BloggerDetailsState.State(loading = false, blogger = blogger))

        composeRule.onNodeWithText(string(R.string.blogger_videos_empty)).assertIsDisplayed()
    }

    @Test
    fun videoClickSendsVideoSelected() {
        setScreen(
            BloggerDetailsState.State(loading = false, blogger = blogger, videos = persistentListOf(video)),
        )

        composeRule.onNodeWithText("Тестовое видео").performClick()

        assertTrue(BloggerDetailsState.Event.VideoSelected(70) in events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(BloggerDetailsState.State(loading = false, error = UiText.Res(R.string.blogger_details_load_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(BloggerDetailsState.Event.RetrySelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(BloggerDetailsState.State(loading = false, blogger = blogger))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(BloggerDetailsState.Event.BackSelected in events)
    }
}
