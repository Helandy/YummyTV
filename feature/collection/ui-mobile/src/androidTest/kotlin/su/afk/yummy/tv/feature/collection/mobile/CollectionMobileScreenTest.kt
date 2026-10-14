package su.afk.yummy.tv.feature.collection.mobile

import androidx.compose.ui.test.assertCountEquals
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
import su.afk.yummy.tv.domain.collection.model.CollectionAnimeItem
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.feature.collection.CollectionState
import su.afk.yummy.tv.feature.collection.presentation.R

class CollectionMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<CollectionState.Event>()

    private val collection = CollectionDetail(
        id = 1,
        ownerId = 7,
        title = "Моя коллекция",
        description = "Описание",
        isPublic = true,
        views = 10,
        posterUrl = null,
        likesCount = 2,
        dislikesCount = 0,
        vote = CollectionVote.NEUTRAL,
        animes = listOf(
            CollectionAnimeItem(id = 41, title = "Первое аниме", posterUrl = null, rating = 8.0, year = 2020),
        ),
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: CollectionState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                CollectionMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsCollectionTitleAndAnime() {
        setScreen(CollectionState.State(isLoading = false, collection = collection))

        // название есть и в топ-баре, и в шапке коллекции
        composeRule.onAllNodesWithText("Моя коллекция").assertCountEquals(2)
        composeRule.onNodeWithText("Первое аниме").assertIsDisplayed()
    }

    @Test
    fun animeClickSendsAnimeSelected() {
        setScreen(CollectionState.State(isLoading = false, collection = collection))

        composeRule.onNodeWithText("Первое аниме").performClick()

        assertTrue(CollectionState.Event.AnimeSelected(41) in events)
    }

    @Test
    fun commentsButtonSendsCommentsSelected() {
        setScreen(CollectionState.State(isLoading = false, collection = collection))

        composeRule.onNodeWithText(string(R.string.collection_comments)).performScrollTo().performClick()

        assertTrue(CollectionState.Event.CommentsSelected in events)
    }

    @Test
    fun ownerActionsAreHiddenForOtherUser() {
        setScreen(CollectionState.State(isLoading = false, collection = collection, currentUserId = 1))

        composeRule.onNodeWithText(string(R.string.collection_edit)).assertDoesNotExist()
    }

    @Test
    fun ownerEditSendsEditSelected() {
        setScreen(CollectionState.State(isLoading = false, collection = collection, currentUserId = 7))

        composeRule.onNodeWithText(string(R.string.collection_edit)).performClick()

        assertTrue(CollectionState.Event.EditSelected in events)
    }

    @Test
    fun ownerDeleteSendsDeleteSelected() {
        setScreen(CollectionState.State(isLoading = false, collection = collection, currentUserId = 7))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_delete)).performClick()

        assertTrue(CollectionState.Event.DeleteSelected in events)
    }

    @Test
    fun deleteDialogConfirmSendsDeleteConfirmed() {
        setScreen(
            CollectionState.State(
                isLoading = false,
                collection = collection,
                currentUserId = 7,
                isDeleteDialogVisible = true,
            ),
        )

        composeRule.onNodeWithText(string(R.string.collection_delete_dialog_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(DesignSystemR.string.common_delete)).onLast().performClick()

        assertTrue(CollectionState.Event.DeleteConfirmed in events)
    }

    @Test
    fun deleteDialogCancelSendsDeleteDismissed() {
        setScreen(CollectionState.State(isLoading = false, collection = collection, isDeleteDialogVisible = true))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_cancel)).performClick()

        assertTrue(CollectionState.Event.DeleteDismissed in events)
    }

    @Test
    fun editDialogIsShown() {
        setScreen(CollectionState.State(isLoading = false, collection = collection, isEditDialogVisible = true))

        composeRule.onNodeWithText(string(R.string.collection_edit_dialog_title)).assertIsDisplayed()
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(CollectionState.State(isLoading = false, error = UiText.Res(R.string.collection_load_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(CollectionState.Event.RetrySelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(CollectionState.State(isLoading = false, collection = collection))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(CollectionState.Event.BackSelected in events)
    }
}
