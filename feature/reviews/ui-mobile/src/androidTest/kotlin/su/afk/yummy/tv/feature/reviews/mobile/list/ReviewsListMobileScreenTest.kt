package su.afk.yummy.tv.feature.reviews.mobile.list

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
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewAuthor
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.feature.reviews.list.ReviewsListState
import su.afk.yummy.tv.feature.reviews.presentation.R

class ReviewsListMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ReviewsListState.Event>()

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private val review = AnimeReviewSummary(
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
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun stateOf(vararg items: AnimeReviewSummary) = ReviewsListState.State(
        reviews = flowOf(PagingData.from(items.toList(), sourceLoadStates = loaded)),
    )

    private fun setScreen(state: ReviewsListState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                ReviewsListMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleSortsAndEmptyMessage() {
        setScreen(stateOf())

        composeRule.onNodeWithText(string(R.string.reviews_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reviews_new)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reviews_old)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reviews_top)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reviews_empty)).assertIsDisplayed()
    }

    @Test
    fun generalFeedHidesOldSort() {
        setScreen(stateOf().copy(isGeneralFeed = true))

        composeRule.onNodeWithText(string(R.string.reviews_old)).assertDoesNotExist()
    }

    @Test
    fun topSortSendsSortSelected() {
        setScreen(stateOf())

        composeRule.onNodeWithText(string(R.string.reviews_top)).performClick()
        composeRule.waitForIdle()

        assertTrue(ReviewsListState.Event.SortSelected(ReviewSort.TOP) in events)
    }

    @Test
    fun reviewIsShownAndAuthorClickSendsAuthorSelected() {
        setScreen(stateOf(review))

        composeRule.onNodeWithText("Рецензент").performClick()

        assertTrue(ReviewsListState.Event.AuthorSelected(9) in events)
    }

    @Test
    fun likeSendsVoteSelected() {
        setScreen(stateOf(review))

        composeRule.onNodeWithContentDescription("${string(R.string.review_like)} 3").performClick()

        assertTrue(ReviewsListState.Event.VoteSelected(review, ReviewVote.LIKE) in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(stateOf())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(ReviewsListState.Event.BackSelected in events)
    }
}
