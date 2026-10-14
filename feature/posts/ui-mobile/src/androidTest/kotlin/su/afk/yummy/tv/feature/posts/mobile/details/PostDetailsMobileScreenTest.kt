package su.afk.yummy.tv.feature.posts.mobile.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.posts.model.PostAuthor
import su.afk.yummy.tv.domain.posts.model.PostCategory
import su.afk.yummy.tv.domain.posts.model.PostDetails
import su.afk.yummy.tv.domain.posts.model.PostReaction
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.domain.posts.model.RelatedPostAnime
import su.afk.yummy.tv.feature.posts.details.PostDetailsState
import su.afk.yummy.tv.feature.posts.presentation.R

class PostDetailsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<PostDetailsState.Event>()

    private val details = PostDetails(
        id = 31,
        title = "Тестовая публикация",
        contentHtml = "<p>Текст публикации</p>",
        previewImageUrl = null,
        author = PostAuthor(id = 2, nickname = "Автор", avatarUrl = null),
        category = PostCategory(id = 1, title = "Новости", uri = "news"),
        createdAt = 0L,
        editedAt = null,
        relatedAnime = listOf(RelatedPostAnime(id = 8, title = "Связанное аниме", posterUrl = null, year = 2020, rating = 8.0)),
        reaction = PostReaction(likes = 3, dislikes = 1, vote = PostVote.NONE),
        views = 100,
        comments = 5,
    )

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setScreen(state: PostDetailsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                PostDetailsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndContent() {
        setScreen(PostDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithText(string(R.string.posts_publication)).assertIsDisplayed()
        composeRule.onNodeWithText("Тестовая публикация").assertIsDisplayed()
        composeRule.onNodeWithText("Текст публикации").assertIsDisplayed()
    }

    @Test
    fun missingDetailsShowEmptyMessage() {
        setScreen(PostDetailsState.State(loading = false, details = null))

        composeRule.onNodeWithText(string(R.string.posts_empty)).assertIsDisplayed()
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(PostDetailsState.State(loading = false, error = UiText.Res(R.string.posts_load_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(PostDetailsState.Event.RetrySelected in events)
    }

    @Test
    fun relatedAnimeClickSendsAnimeSelected() {
        setScreen(PostDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithText("Связанное аниме").performClick()

        assertTrue(PostDetailsState.Event.AnimeSelected(8) in events)
    }

    @Test
    fun commentsButtonSendsCommentsSelected() {
        setScreen(PostDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithText(string(R.string.posts_comments, "5")).performClick()

        assertTrue(PostDetailsState.Event.CommentsSelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(PostDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(PostDetailsState.Event.BackSelected in events)
    }
}
