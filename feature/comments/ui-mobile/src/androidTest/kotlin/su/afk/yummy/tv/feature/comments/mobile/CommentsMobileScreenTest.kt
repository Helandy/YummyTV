package su.afk.yummy.tv.feature.comments.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentAuthor
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.presentation.R

class CommentsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<CommentsState.Event>()

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private val comment = Comment(
        id = 501,
        author = CommentAuthor(id = 3, name = "Комментатор", avatarSmallUrl = null, avatarBigUrl = null, avatarFullUrl = null),
        text = "Отличное аниме!",
        createdAtEpochSeconds = 0L,
        parentId = null,
        childrenCount = 0,
        likes = 2,
        dislikes = 0,
        vote = CommentVote.NEUTRAL,
        roles = emptyList(),
        deletedAtEpochSeconds = null,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun stateWith(vararg items: Comment, currentUserId: Int = 0) = CommentsState.State(
        comments = flowOf(
            PagingData.from(items.map { CommentsState.CommentUi(it) }, sourceLoadStates = loaded),
        ),
        currentUserId = currentUserId,
    )

    private fun setScreen(state: CommentsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                CommentsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleSortsAndEmptyMessage() {
        setScreen(stateWith())

        composeRule.onNodeWithText(string(R.string.comments_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.comments_sort_new)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.comments_sort_best)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.comments_empty)).assertIsDisplayed()
    }

    @Test
    fun commentIsShown() {
        setScreen(stateWith(comment))

        composeRule.onNodeWithText("Комментатор").assertIsDisplayed()
        composeRule.onNodeWithText("Отличное аниме!").assertIsDisplayed()
    }

    @Test
    fun sortClickSendsSortSelected() {
        setScreen(stateWith())

        composeRule.onNodeWithText(string(R.string.comments_sort_new)).performClick()
        composeRule.waitForIdle()

        assertTrue(CommentsState.Event.SortSelected(CommentSort.NEW) in events)
    }

    @Test
    fun signedOutUserSeesSignInHint() {
        setScreen(stateWith(currentUserId = 0))

        composeRule.onNodeWithText(string(R.string.comments_sign_in_hint)).assertIsDisplayed()
    }

    @Test
    fun typingSendsComposerTextChanged() {
        setScreen(stateWith(currentUserId = 1))

        composeRule.onNode(hasSetTextAction()).performTextInput("Привет")

        assertTrue(CommentsState.Event.ComposerTextChanged("Привет") in events)
    }

    @Test
    fun sendButtonSendsSubmitSelected() {
        setScreen(stateWith(currentUserId = 1).copy(composerText = "Привет"))

        composeRule.onNodeWithContentDescription(string(R.string.comments_send)).performClick()

        assertTrue(CommentsState.Event.SubmitSelected in events)
    }

    @Test
    fun deleteDialogConfirmSendsDeleteConfirmed() {
        setScreen(stateWith(comment, currentUserId = 3).copy(pendingDelete = comment))

        composeRule.onNodeWithText(string(R.string.comments_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_delete)).performClick()

        assertTrue(CommentsState.Event.DeleteConfirmed in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(stateWith())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(CommentsState.Event.BackSelected in events)
    }
}
