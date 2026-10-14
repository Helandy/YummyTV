package su.afk.yummy.tv.feature.reviews.mobile.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
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
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewDetails
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewAuthor
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsState
import su.afk.yummy.tv.feature.reviews.presentation.R

class ReviewDetailsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ReviewDetailsState.Event>()

    private val details = AnimeReviewDetails(
        review = AnimeReviewSummary(
            id = 12,
            animeId = 3,
            status = ReviewStatus.APPROVED,
            author = ReviewAuthor(id = 9, nickname = "Рецензент", avatarUrl = null),
            createdAtSeconds = 0L,
            updatedAtSeconds = 0L,
            views = 10,
            rating = null,
            reactions = ReviewReactions(likes = 3, dislikes = 1, vote = ReviewVote.NONE),
            html = "<p>Текст рецензии</p>",
            checkComment = null,
            commentable = true,
            animeTitle = "Тестовое аниме",
        ),
        animeTitle = "Тестовое аниме",
        animePosterUrl = null,
        commentsCount = 4,
    )

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setScreen(state: ReviewDetailsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                ReviewDetailsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndReviewText() {
        setScreen(ReviewDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithText(string(R.string.review_details_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Текст рецензии").assertIsDisplayed()
    }

    @Test
    fun missingDetailsShowEmptyMessage() {
        setScreen(ReviewDetailsState.State(loading = false, details = null))

        composeRule.onNodeWithText(string(R.string.reviews_empty)).assertIsDisplayed()
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(ReviewDetailsState.State(loading = false, error = UiText.Res(R.string.reviews_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(ReviewDetailsState.Event.RetrySelected in events)
    }

    @Test
    fun animeCardClickSendsAnimeSelected() {
        setScreen(ReviewDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithText("Тестовое аниме").performClick()

        assertTrue(ReviewDetailsState.Event.AnimeSelected(3) in events)
    }

    @Test
    fun commentsButtonSendsCommentsSelected() {
        setScreen(ReviewDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithText(string(R.string.review_comments, "4")).performScrollTo().performClick()

        assertTrue(ReviewDetailsState.Event.CommentsSelected in events)
    }

    @Test
    fun deleteIsHiddenForNonOwner() {
        setScreen(ReviewDetailsState.State(loading = false, details = details, currentUserId = 1))

        composeRule.onNodeWithText(string(R.string.review_comments, "4")).performScrollTo()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_delete)).assertDoesNotExist()
    }

    @Test
    fun ownerDeleteRequiresConfirmation() {
        setScreen(ReviewDetailsState.State(loading = false, details = details, currentUserId = 9))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_delete)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.review_delete_confirm)).assertIsDisplayed()
        assertTrue(ReviewDetailsState.Event.DeleteConfirmed !in events)
    }

    @Test
    fun confirmedDeleteSendsDeleteConfirmed() {
        setScreen(ReviewDetailsState.State(loading = false, details = details, currentUserId = 9))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_delete)).performScrollTo().performClick()
        // кнопка «Удалить» есть и на экране, и в диалоге: подтверждение — последняя в дереве
        val deleteLabel = string(DesignSystemR.string.common_delete)
        composeRule.onAllNodesWithText(deleteLabel).onLast().performClick()

        assertTrue(ReviewDetailsState.Event.DeleteConfirmed in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(ReviewDetailsState.State(loading = false, details = details))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(ReviewDetailsState.Event.BackSelected in events)
    }
}
